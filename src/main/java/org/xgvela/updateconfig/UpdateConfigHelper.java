// Copyright 2020 Mavenir
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.xgvela.updateconfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.xgvela.cnf.Constants;
import org.xgvela.cnf.elastic.ScheduledIndexCreation;
import org.xgvela.cnf.notification.KeyValueBean;
import org.xgvela.cnf.notification.NotificationUtil;
import org.xgvela.cnf.util.DateUtil;
import org.xgvela.cnf.util.JsonUtil;
import org.xgvela.cnf.util.K8sClient;
import org.xgvela.cnf.util.K8sUtil;
import org.xgvela.cnf.util.MetricsUtil;
import org.xgvela.cnf.util.NBInterface;
import org.xgvela.cnf.util.NatsUtil;
import org.xgvela.dbadapter.DbAdaptor;
import org.xgvela.dbadapter.zk.ZKUtil;

import io.fabric8.zjsonpatch.JsonPatch;
import io.nats.client.Connection;
import io.prometheus.client.Counter;

@Component
public class UpdateConfigHelper {
	@Autowired
	JsonUtil jsonUtil;
	@Autowired
	K8sUtil k8sUtil;
	@Autowired
	K8sClient k8sClient;
	@Autowired
	DateUtil dateUtil;
	@Autowired
	ZKUtil zkUtil;
	@Autowired
	ScheduledIndexCreation scheduledIndexCreation;
	private static Logger LOG = LogManager.getLogger(UpdateConfigHelper.class);
	public static JsonNode jsonObjectFile;
	public static JsonNode configJson;
	private static final Counter fmaasConfigUpdateSuccessTotal = MetricsUtil
			.addCounter("fmaas_config_update_success_total", "The number of times FMaaS config updated successfully");
	private static final Counter fmaasConfigUpdateFailureTotal = MetricsUtil
			.addCounter("fmaas_config_update_failure_total", "The number of times FMaaS config failed to get updated");

	public ArrayList<KeyValueBean> getAttemptMgdObjs(JsonNode recievedJson) {
		ArrayList<KeyValueBean> mgdObjs = new ArrayList<>();
		mgdObjs.add(new KeyValueBean("change-set-key", recievedJson.get("change-set-key").asText()));
		mgdObjs.add(new KeyValueBean("config-patch", recievedJson.get("config-patch").asText()));
		mgdObjs.add(new KeyValueBean("change-set-key", recievedJson.get("change-set-key").asText()));
		mgdObjs.add(new KeyValueBean("revision", recievedJson.get("revision").asText()));
		return mgdObjs;
	}

	public Boolean updateFmaasConfig(String diff) {
		LOG.debug("Updating configJson object");
		ObjectMapper mapper = new ObjectMapper();
		JsonNode patchObject = null;
		try {
			patchObject = mapper.readTree(diff);
		} catch (JsonProcessingException e) {
			LOG.error("Error occured while parsing patch jsonObject: " + e);
		}
		JsonNode jsonObject = null;
		try {
			jsonObject = mapper.readTree(jsonObjectFile.toString());
		} catch (JsonProcessingException e) {
			LOG.error("Error occured while parsing original jsonObject: " + e);
		}
		LOG.debug("jsonObject: " + jsonObject);
		LOG.debug("jsonDiff: " + patchObject);
		jsonObjectFile = JsonPatch.apply(patchObject, jsonObject);
		// jsonObjectFile = new JSONObject(jsonObject.toString());
		configJson = jsonObjectFile.get("config");
		LOG.info("Updated configJson successfully with updated value: " + configJson.toString());
		LOG.debug("Applying new config");
		applyConfig();
		LOG.info("Applied new config successfully, exiting updateFmaasConfig");
		return true;
	}

	private void applyConfig() {
		// NBInterface.nbiType = configJson.get("nbiType").asText();
		// NBInterface.nbiUrl = configJson.get("nbiTarget").asText();
		Configurator.setAllLevels("org.xgvela", Level.toLevel(configJson.get("logLevel").asText()));
		int newRetentionPeriod = Integer.parseInt(configJson.get("dataRetentionPeriod").asText());
		if (DateUtil.dataRetentionPeriod != newRetentionPeriod) {
			if (!dateUtil.checkIfLastPurgeInThirthyMin()) {
				DateUtil.dataRetentionPeriod = newRetentionPeriod;
				LOG.debug("triggering purge activity due to dynamic config change for engine: "
						+ DbAdaptor.storageEngine + " at " + new Date());
				long intial = System.currentTimeMillis();
				if (DbAdaptor.storageEngine.equalsIgnoreCase("elasticsearch")) {
					scheduledIndexCreation.deleteIndex();
				} else {
					zkUtil.deleteNodes();
				}
				long ending = System.currentTimeMillis();
				LOG.debug("purging activity completed in " + String.valueOf(ending - intial) + "ms");
				dateUtil.updateLastPurgeTime();
			} else {
				LOG.debug(
						"Purge activity ran within last 30 mins so this request will be considered eligible as part of next schedule time i.e."
								+ dateUtil.nextScheduledPurgeTime);
			}
		}
	}

	public void publishToCim(JsonNode recievedJson, Boolean done) {
		ObjectNode json = (ObjectNode) recievedJson;
		json.remove("config-patch");
		json.remove("data-key");

		if (done) {
			fmaasConfigUpdateSuccessTotal.inc();
			json.put("status", "success");
			json.put("remarks", "Successfully applied the config");
			try {
				NotificationUtil.sendEvent("FmaasConfigUpdateSuccess", getreturnMgdObjs(json));
				LOG.info(Constants.ACTIVITY + Constants.INIT + "FMaaS config updated Successfully ," + json.toString());
			} catch (Exception e1) {
				LOG.error(e1.getMessage(), e1 + "Error occured while raising FmaasConfigUpdateSuccess event");
			}
		} else {
			fmaasConfigUpdateFailureTotal.inc();
			json.put("status", "failure");
			json.put("remarks", "Failed to apply the config");
			try {
				NotificationUtil.sendEvent("FmaasConfigUpdateFailure", getreturnMgdObjs(json));
				LOG.info(Constants.ACTIVITY + Constants.INIT + "FMaaS config update Failed ");
			} catch (Exception e1) {
				LOG.error("Error occured while raising FmaasConfigUpdateFailure event", e1);
			}

		}
		LOG.debug("Config response for CIM:  " + json.toString());
		Connection natsConnection = NatsUtil.getConnection();
		try {
			natsConnection.publish("CONFIG", json.toString().getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			LOG.error("Error occured while publishing to NATS", e);
		}
		LOG.info("Published response to CIM");

	}

	public void initFmaasConfig() {
		try {
			LOG.info("Initialising FMaaS configuration");
			File file = new File("/fmaasconfig/fmaas.json");
			InputStream inputStream = new FileInputStream(file);
			ObjectMapper objectMapper = new ObjectMapper();
			jsonObjectFile = objectMapper.readTree(inputStream);
			configJson = jsonObjectFile.get("config");
			LOG.debug("configJson: " + configJson.toString());
			Configurator.setAllLevels("org.xgvela",
					Level.toLevel((configJson.get("logLevel")).asText().toLowerCase()));
			DateUtil.dataRetentionPeriod = Integer.parseInt(configJson.get("dataRetentionPeriod").asText());
			// NBInterface.nbiType = configJson.get("nbiType").asText();
			// NBInterface.nbiUrl = configJson.get("nbiTarget").asText();
			String dnPrefix = Constants.EMPTY_STRING;
			if (System.getenv(Constants.DN_PREFIX) != null) {
				dnPrefix = System.getenv(Constants.DN_PREFIX);
			} else
				dnPrefix = "xgvela";
			JsonUtil.meDn = dnPrefix + ",ManagedElement=me-" + jsonUtil.getTmaasAnnotation(Constants.XGVELA_ID);
			JsonUtil.reportingEntityName = JsonUtil.meDn + ",NetworkFunction="
					+ jsonUtil.getTmaasAnnotation(Constants.NW_ID) + ",NFService="
					+ jsonUtil.getTmaasAnnotation(Constants.NF_SERVICE_ID);
			JsonUtil.reportingEntityId = jsonUtil.getUUID(JsonUtil.reportingEntityName);
			LOG.debug("Set reportingEntityName: " + JsonUtil.reportingEntityName + " reportingEntityId: "
					+ JsonUtil.reportingEntityId);
			JsonUtil.meId = jsonUtil.getUUID(JsonUtil.meDn);
			LOG.debug("Set meDn: " + JsonUtil.meDn + " meId: " + JsonUtil.meId);
			String cimEventdefCmapNamespace = Constants.EMPTY_STRING;
			String cimEventdefCmapName = Constants.EMPTY_STRING;
			String svcVersion = Constants.EMPTY_STRING;
			if (System.getenv(Constants.K8S_NAMESPACE) != null)
				cimEventdefCmapNamespace = System.getenv(Constants.K8S_NAMESPACE);
			else
				cimEventdefCmapNamespace = "xgvela-xgvela1-mgmt-xgvela-xgvela1";
			if (System.getenv(Constants.MGMT_VERSION) != null)
				svcVersion = System.getenv(Constants.MGMT_VERSION);
			String vesGwSvc = jsonUtil.getServiceName("vesgw");
			if (vesGwSvc.isEmpty())
				vesGwSvc = "vesgw";
			cimEventdefCmapName = "xgvela-mgmt" + "-" + svcVersion + Constants.EVT_CMAP_SUFFIX;
			JsonUtil.cimEventdefCmapData = k8sUtil.getCmap(cimEventdefCmapNamespace, cimEventdefCmapName);
			if (JsonUtil.cimEventdefCmapData == null) {
				cimEventdefCmapName = "xgvela-mgmt" + Constants.EVT_CMAP_SUFFIX;
				JsonUtil.cimEventdefCmapData = k8sUtil.getCmap(cimEventdefCmapNamespace, cimEventdefCmapName);
			}
			JsonUtil.maxRetries = Integer.parseInt(System.getenv(Constants.RETRY_COUNT));
			JsonUtil.initReplayUrl = "http://" + vesGwSvc + "." + cimEventdefCmapNamespace
					+ ".svc.cluster.local:8095/setInitReplay";
			JsonUtil.replayResetUrl = "http://" + vesGwSvc + "." + cimEventdefCmapNamespace
					+ ".svc.cluster.local:8095/setResetReplay";
			LOG.info("Completed initialising FMaaS configuration");
		} catch (IOException e) {
			LOG.error("Error occured while parsing config from file", e);
		}
	}

	private ArrayList<KeyValueBean> getreturnMgdObjs(JsonNode json) {
		ArrayList<KeyValueBean> mgdObjs = new ArrayList<>();
		mgdObjs.add(new KeyValueBean("change-set-key", json.get("change-set-key").asText()));
		mgdObjs.add(new KeyValueBean("revision", json.get("revision").asText()));
		mgdObjs.add(new KeyValueBean("status", json.get("status").asText()));
		mgdObjs.add(new KeyValueBean("remarks", json.get("remarks").asText()));
		return mgdObjs;
	}

}