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

package org.xgvela.cnf.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.metrics.max.Max;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.xgvela.cnf.Constants;
import org.xgvela.cnf.elastic.ElasticSearchClientConfiguration;
import org.xgvela.cnf.kafka.KafkaConfiguration;
import org.xgvela.dbadapter.DbAdaptor;
import org.xgvela.dbadapter.zk.LeaderElection;
import org.xgvela.dbadapter.zk.ZKUtil;
import org.xgvela.model.AdditionalParameter;
import org.xgvela.model.AdditionalParameter.Criticality;
import org.xgvela.model.AlarmAdditionalInformation;
import org.xgvela.model.CommonEventHeader;
import org.xgvela.model.CommonEventHeader.Domain;
import org.xgvela.model.CommonEventHeader.Priority;
import org.xgvela.model.FaultFields;
import org.xgvela.model.FaultFields.EventSeverity;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.nats.client.Message;
import io.nats.client.Subscription;
import org.xgvela.model.NotificationFields;
import org.xgvela.model.ThresholdCrossingAlertFields;

@Component
public class JsonUtil {

	private static Logger LOG = LogManager.getLogger(JsonUtil.class);

	@Autowired
	ESUtil esUtil;

	@Autowired
	ElasticSearchClientConfiguration esClient;

	@Autowired
	DateUtil dateUtil;

	@Autowired
	RestTemplate restTemplate;

	@Autowired
	K8sClient k8sClient;

	@Autowired
	ZKUtil zkUtil;

	@Autowired
	K8sUtil k8sUtil;
	public static int globalSequence;
	private static final String DEFAULT = "default";
	public static String reportingEntityName = "fault-service";
	public static String reportingEntityId = String.valueOf(System.getenv("K8S_POD_ID"));
	public static String meId = Constants.EMPTY_STRING;
	public static String meDn = Constants.EMPTY_STRING;
	public static String initReplayUrl = Constants.EMPTY_STRING;
	public static String replayResetUrl = Constants.EMPTY_STRING;

	public static int maxRetries;
	public static Map<String, String> cimEventdefCmapData = new HashMap<>();

	private static ObjectMapper mapper;

	public static ObjectMapper getMapper() {
		if (mapper == null) {
			mapper = new ObjectMapper();
		}
		return mapper;
	}

	public void parsePrometheusAlerts(String alerts) {

		ObjectMapper mapper = getMapper();

		try {
			JsonNode alertList = mapper.readTree(alerts).get("alerts");

			Iterator<JsonNode> alertsArray = alertList.elements();
			while (alertsArray.hasNext()) {
				JsonNode alert = alertsArray.next();
				Map<String, Object> document = getDocument(alert);

				if (document == null) {
					LOG.error("Returned document was null; unable to parse message");
					continue;
				}

				String date = alert.get(Constants.STARTS_AT).asText().substring(0, 10);
				esUtil.handleDoc(document, date);
			}
		} catch (JsonMappingException e) {
			LOG.error(e.getMessage(), e);
		} catch (JsonProcessingException e) {
			LOG.error(e.getMessage(), e);
		}
	}

	private Map<String, Object> createEventMap() {

		LOG.debug("Creating Event Map.");

		Map<String, Object> eventMap = new HashMap<>();
		Map<String, Object> commonEventHeaderMap = new HashMap<>();
		Map<String, Object> faultFieldsMap = new HashMap<>();
		Map<String, Object> sourceHeadersMap = new HashMap<>();
		Map<String, Object> eventDefinitionsMap = new HashMap<>();
		Map<String, Object> eventCommonMap = new HashMap<>();
		Map<String, Object> tcaFieldsMap = new HashMap<>();
		Map<String, Object> notificationFieldsMap = new HashMap<>();

		eventMap.put(Constants.COMMON_EVT_HDR, commonEventHeaderMap);
		eventMap.put(Constants.FAULT_FIELDS, faultFieldsMap);
		eventMap.put(Constants.SRC_HEADERS, sourceHeadersMap);
		eventMap.put(Constants.EVENT_DEF, eventDefinitionsMap);
		eventMap.put(Constants.EVT_COMMON, eventCommonMap);
		eventMap.put(Constants.TCA_FIELDS, tcaFieldsMap);
		eventMap.put(Constants.NOTIFICATION_FIELDS, notificationFieldsMap);

		return eventMap;
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> getDocument(JsonNode alert) {

		Map<String, Object> eventMap = createEventMap();
		Map<String, Object> srcHdr = (Map<String, Object>) eventMap.get(Constants.SRC_HEADERS);
		Map<String, Object> comEvtHdr = (Map<String, Object>) eventMap.get(Constants.COMMON_EVT_HDR);
		Map<String, Object> faultFields = (Map<String, Object>) eventMap.get(Constants.FAULT_FIELDS);

		// Map<String, Object> faultFields = (Map<String, Object>)
		// eventMap.get(Constants.FAULT_FIELDS);
		Map<String, Object> evtDef = (Map<String, Object>) eventMap.get(Constants.EVENT_DEF);
		Map<String, Object> tcaFields = (Map<String, Object>) eventMap.get(Constants.TCA_FIELDS);

		JsonNode annotations = alert.get(Constants.ANNOTATIONS);
		String localEventName, sourceId;

		// mandatory fields - sourceId and eventName
		try {
			localEventName = annotations.get(Constants.EVENT_NAME).asText();
			sourceId = annotations.get(Constants.SOURCE_ID).asText();
		} catch (NullPointerException e) {
			LOG.error("Mandatory field missing in annotations, unable to parse alert.");
			LOG.error(e.getMessage());
			return null;
		}

		// put message into additionalText under eventDef
		if (annotations.has(Constants.MESSAGE)) {
			String message = annotations.get(Constants.MESSAGE).asText();
			evtDef.put(Constants.ADD_TXT, message);
			eventMap.put(Constants.EVENT_DEF, evtDef);
		}

		long startEpochMillis;
		String startTime = Constants.EMPTY_STRING;
		// resolved case
		if (alert.get(Constants.STATUS).asText().equals("resolved")) {

			// mark severity as CLEAR
			tcaFields.put(Constants.EVENT_SEVERITY, EventSeverity.CLEAR.toString());
			eventMap.put(Constants.TCA_FIELDS, tcaFields);

			String endEpoch = alert.get(Constants.ENDS_AT).asText();
			startTime = endEpoch;
			DateTimeFormatter dtfFormatter = dateUtil.getDtf(endEpoch);
			startEpochMillis = Instant.from(dtfFormatter.parse(endEpoch)).toEpochMilli();

		} else { // firing case
			String startEpoch = alert.get(Constants.STARTS_AT).asText();
			DateTimeFormatter dtfFormatter = dateUtil.getDtf(startEpoch);
			startTime = startEpoch;
			Instant result = Instant.from(dtfFormatter.parse(startEpoch));
			startEpochMillis = result.toEpochMilli();
		}
		LOG.debug("startEpochMillis" + String.valueOf(startEpochMillis));
		// default
		comEvtHdr.put(Constants.DOMAIN, Domain.THRESHOLD_CROSSING_ALERT);
		comEvtHdr.put(Constants.PRIORITY, Priority.NORMAL);
		comEvtHdr.put(Constants.SEQUENCE, 0);
		comEvtHdr.put(Constants.SOURCE_NAME, DEFAULT);
		comEvtHdr.put(Constants.VERSION, CommonEventHeader.Version._4_1);
		comEvtHdr.put(Constants.VES_EVENT_LIS_VERSION, CommonEventHeader.VesEventListenerVersion._7_1);

		// parsed
		comEvtHdr.put(Constants.LOCAL_EVT_NAME, localEventName);
		comEvtHdr.put(Constants.LAST_EPOCH, startEpochMillis);
		comEvtHdr.put(Constants.SOURCE_ID, sourceId);
		comEvtHdr.put(Constants.START_EPOCH, startEpochMillis);

		comEvtHdr = fillReportingFields(comEvtHdr);

		String cmapNamespace = null, cmapName = null, eventName = Constants.EMPTY_STRING;
		if (annotations.has(Constants.NW_FN_TYPE) && !annotations.get(Constants.NW_FN_TYPE).asText().isEmpty()) {
			String nfType = annotations.get(Constants.NW_FN_TYPE).asText();
			if (annotations.has(Constants.VENDOR_ID) && !annotations.get(Constants.VENDOR_ID).asText().isEmpty()) {
				String vendorId = annotations.get(Constants.VENDOR_ID).asText();
				eventName = nfType.substring(0, 1).toUpperCase() + nfType.substring(1) + "-"
						+ vendorId.substring(0, 1).toUpperCase() + vendorId.substring(1) + "_" + localEventName;
			} else {
				eventName = nfType.substring(0, 1).toUpperCase() + nfType.substring(1) + "_" + localEventName;
			}
		}
		if (annotations.has(Constants.NW_FN_TYPE)) {
			comEvtHdr.put(Constants.NF_NAMING_CODE, annotations.get(Constants.NW_FN_TYPE).asText());
		} else {
			LOG.error("Annotation missing \"nfType\" key");
		}

		if (annotations.has(Constants.NF_SVC_TYPE)) {
			comEvtHdr.put(Constants.NFC_NAMING_CODE, annotations.get(Constants.NF_SVC_TYPE).asText());
		} else {
			LOG.error("Annotation missing \"nfServiceType\" key");
		}
		comEvtHdr.put(Constants.EVENT_NAME, eventName);

		AlarmAdditionalInformation alarmAdditionalInfoList = new AlarmAdditionalInformation();
		alarmAdditionalInfoList.setAdditionalProperty(Constants.ME_ID, meId);
		if (annotations.has(Constants.UHN)) {
			alarmAdditionalInfoList.setAdditionalProperty(Constants.UHN, annotations.get(Constants.UHN).asText());
		}
		if (annotations.has(Constants.CNFC_UUID)) {
			alarmAdditionalInfoList.setAdditionalProperty(Constants.CNFC_UUID,
					annotations.get(Constants.CNFC_UUID).asText());
		}
		if (annotations.has(Constants.POD_UUID)) {
			alarmAdditionalInfoList.setAdditionalProperty(Constants.POD_UUID,
					annotations.get(Constants.POD_UUID).asText());
		}
		String nfLabel = Constants.EMPTY_STRING;
		if (annotations.has(Constants.NW_ID)) {
			nfLabel = meDn + ",NetworkFunction=" + annotations.get(Constants.NW_ID).asText();
			alarmAdditionalInfoList.setAdditionalProperty(Constants.NF_LABEL, nfLabel);
			alarmAdditionalInfoList.setAdditionalProperty(Constants.NW_ID, getUUID(nfLabel));
		}
		faultFields.put(Constants.ALARM_ADD_INFO, alarmAdditionalInfoList);
		tcaFields.put(Constants.ADD_FIELDS, alarmAdditionalInfoList);
		if (annotations.has(Constants.NAMESPACE)) {

			LOG.info("Application Threshold Alert received.");
			cmapNamespace = annotations.get(Constants.NAMESPACE).asText();
			srcHdr.put(Constants.NW_FN_PREFIX, cmapNamespace);
			String msvc = null;
			if (annotations.has(Constants.MICROSERVICE)) {
				msvc = annotations.get(Constants.MICROSERVICE).asText();
				srcHdr.put(Constants.MICROSERVICE, msvc);
			} else {
				LOG.error("Annotation missing \"microservice\" key, unable to parse alert.");
				return null;
			}

			String svcVersion = null;
			if (annotations.has(Constants.SVC_VERSION)) {
				svcVersion = annotations.get(Constants.SVC_VERSION).asText("");
				srcHdr.put(Constants.SVC_VERSION, svcVersion);
				LOG.info("svcVersion: " + svcVersion);
			} else {
				LOG.info("Annotation missing \"svcVersion\" key for microservice " + msvc);
			}
			if (svcVersion != null && !svcVersion.isEmpty()) {
				cmapName = msvc + "-" + svcVersion + Constants.EVT_CMAP_SUFFIX;
			} else {
				cmapName = msvc + Constants.EVT_CMAP_SUFFIX;
			}

			if (annotations.has(Constants.POD_ID)) {
				srcHdr.put(Constants.POD_ID, annotations.get(Constants.POD_ID).asText());

			} else {
				LOG.error("Annotation missing \"podId\" key, unable to parse alert.");
				return null;
			}

		} else {

			LOG.info("Cluster Alert (CaaS/PaaS/system_tca) received.");
			comEvtHdr.put(Constants.NF_NAMING_CODE, "");
			comEvtHdr.put(Constants.NFC_NAMING_CODE, "");

			String originType = annotations.get(Constants.ORIGIN_TYPE).asText();
			String svcVersion = null;
			if (originType.equalsIgnoreCase("paas")) {
				if (System.getenv(Constants.PAAS_NS) != null)
					cmapNamespace = System.getenv(Constants.PAAS_NS);
				else
					cmapNamespace = "xgvela-paas";
				cmapName = "paas" + Constants.EVT_CMAP_SUFFIX;
			} else if (originType.equalsIgnoreCase("caas")) {
				if (System.getenv(Constants.CAAS_NS) != null)
					cmapNamespace = System.getenv(Constants.CAAS_NS);
				else
					cmapNamespace = "kube-system";
				cmapName = "caas" + Constants.EVT_CMAP_SUFFIX;
			} else if (originType.equalsIgnoreCase("system_tca")) {
				if (System.getenv(Constants.K8S_NAMESPACE) != null)
					cmapNamespace = System.getenv(Constants.K8S_NAMESPACE);
				else
					cmapNamespace = "xgvela-xgvela1-mgmt-xgvela-xgvela1";
				if (System.getenv(Constants.MGMT_VERSION) != null) {
					svcVersion = System.getenv(Constants.MGMT_VERSION);
					cmapName = "xgvela-mgmt" + "-" + svcVersion + Constants.EVT_CMAP_SUFFIX;
				} else {
					LOG.error("Environment variable missing \"MGMT_VERSION\", unable to parse xgvela system alert.");
					return null;
				}
			}
		}

		JsonNode eventDef = k8sUtil.getEventDef(cmapName, cmapNamespace, localEventName, Constants.EMPTY_STRING);
		if (eventDef == null) {
			if (annotations.has(Constants.NAMESPACE)) {
				String msvc = annotations.get(Constants.MICROSERVICE).asText();
				cmapName = msvc + Constants.EVT_CMAP_SUFFIX;
			} else
				cmapName = "xgvela-mgmt" + Constants.EVT_CMAP_SUFFIX;
			eventDef = k8sUtil.getEventDef(cmapName, cmapNamespace, localEventName, Constants.EMPTY_STRING);
			if (eventDef == null)
				return null;
		}

		eventMap.put(Constants.COMMON_EVT_HDR, comEvtHdr);
		eventMap.put(Constants.TCA_FIELDS, tcaFields);
		eventMap.put(Constants.FAULT_FIELDS, faultFields);

		eventMap.put(Constants.SRC_HEADERS, srcHdr);
		eventMap = fillFromEventDef(eventMap, eventDef, startTime);

		return eventMap;
	}

	public String getTmaasAnnotation(String req) {
		JsonNode annotations = null;
		try {
			annotations = mapper.readTree(k8sClient.getClient().pods()
					.inNamespace(System.getenv(Constants.K8S_NAMESPACE)).withName(System.getenv(Constants.K8S_POD_ID))
					.get().getMetadata().getAnnotations().get(Constants.ANNOTATION_TMAAS));
		} catch (JsonMappingException e) {
			LOG.error(e.getMessage(), e);
		} catch (JsonProcessingException e) {
			LOG.error(e.getMessage(), e);
		}
		if (req.equalsIgnoreCase(Constants.XGVELA_ID))
			return annotations.get(Constants.XGVELA_ID).asText();
		else if (req.equalsIgnoreCase(Constants.NW_ID))
			return annotations.get(Constants.NW_ID).asText();
		else if (req.equalsIgnoreCase(Constants.NF_SERVICE_ID))
			return annotations.get(Constants.NF_SERVICE_ID).asText();
		else if (req.equalsIgnoreCase(Constants.VENDOR_ID))
			return annotations.get(Constants.VENDOR_ID).asText();
		else if (req.equalsIgnoreCase(Constants.NF_CLASS))
			return annotations.get(Constants.NF_CLASS).asText();
		else if (req.equalsIgnoreCase(Constants.NF_TYPE))
			return annotations.get(Constants.NF_TYPE).asText();
		else
			return Constants.EMPTY_STRING;
	}

	public String getServiceName(String microservice) {
		String name = Constants.EMPTY_STRING;
		try {
			return k8sClient.getClient().services().inNamespace(System.getenv(Constants.K8S_NAMESPACE))
					.withLabel("microSvcName", microservice).list().getItems().get(0).getMetadata().getName();
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
		return name;
	}

	public synchronized int getNext() {
		if (globalSequence != Integer.MAX_VALUE)
			return ++globalSequence;
		else {
			globalSequence = 1;
			return globalSequence;
		}
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> getDocument(String message) {
		LOG.debug("Creating Event document for application event.");

		Map<String, Object> event = createEventMap();
		event = fillCimFields(event, message);

		Map<String, Object> srcHdr = (Map<String, Object>) event.get(Constants.SRC_HEADERS);
		Map<String, Object> comEvtHdr = (Map<String, Object>) event.get(Constants.COMMON_EVT_HDR);

		String microservice = (String) srcHdr.get(Constants.MICROSERVICE);
		String nfPrefix = (String) srcHdr.get(Constants.NW_FN_PREFIX);
		String nf = (String) srcHdr.get(Constants.NW_FN);
		String localEventName = (String) comEvtHdr.get(Constants.LOCAL_EVT_NAME);
		String svcVersion = (String) srcHdr.get(Constants.SVC_VERSION);
		JsonNode eventDef = null;

		if (svcVersion != null && !svcVersion.isEmpty()) {
			eventDef = k8sUtil.getEventDef(microservice + "-" + svcVersion + Constants.EVT_CMAP_SUFFIX, nfPrefix,
					localEventName, nf + Constants.EVT_CMAP_SUFFIX);
		} else {
			eventDef = k8sUtil.getEventDef(microservice + Constants.EVT_CMAP_SUFFIX, nfPrefix, localEventName,
					nf + Constants.EVT_CMAP_SUFFIX);
		}
		if (eventDef == null) {
			eventDef = k8sUtil.getEventDef(microservice + Constants.EVT_CMAP_SUFFIX, nfPrefix, localEventName,
					nf + Constants.EVT_CMAP_SUFFIX);
			if (eventDef == null)
				return null;
		}
		event = fillFromEventDef(event, eventDef, Constants.EMPTY_STRING);
		return event;
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> fillFromEventDef(Map<String, Object> eventMap, JsonNode eventDef, String startTime) {

		LOG.debug("Filling Event-Map from Event Definition as fetched from ConfigMap. ");

		Map<String, Object> faultFields = (Map<String, Object>) eventMap.get(Constants.FAULT_FIELDS);
		Map<String, Object> tcaFields = (Map<String, Object>) eventMap.get(Constants.TCA_FIELDS);
		Map<String, Object> comEvtHdr = (Map<String, Object>) eventMap.get(Constants.COMMON_EVT_HDR);
		Map<String, Object> evtDef = (Map<String, Object>) eventMap.get(Constants.EVENT_DEF);
		Map<String, Object> notificationFields = (Map<String, Object>) eventMap.get(Constants.NOTIFICATION_FIELDS);
		Map<String, Object> srcHdr = (Map<String, Object>) eventMap.get(Constants.SRC_HEADERS);

		comEvtHdr.put(Constants.EVENT_TYPE, eventDef.get(Constants.EVENT_TYPE).asText());

		evtDef.put(Constants.PRB_CAUSE, eventDef.get(Constants.PRB_CAUSE).asText());
		evtDef.put(Constants.ROOT_CAUSE, eventDef.get(Constants.ROOT_CAUSE).asText());
		evtDef.put(Constants.TREND_IND, eventDef.get(Constants.TREND_IND).asText());
		evtDef.put(Constants.CORR_NOTIF, eventDef.get(Constants.CORR_NOTIF).asText());
		evtDef.put(Constants.PRO_REP_ACT, eventDef.get(Constants.PRO_REP_ACT).asText());
		String addTxt = eventDef.get(Constants.ADD_TXT).asText();
		// to handle 'message' from prometheus alert annotation
		if (evtDef.containsKey(Constants.ADD_TXT)) {
			addTxt = evtDef.get(Constants.ADD_TXT) + "/" + addTxt;
		}
		evtDef.put(Constants.ADD_TXT, addTxt);
		AlarmAdditionalInformation additionInfo = null;
		if (!faultFields.containsKey(Constants.ALARM_ADD_INFO))
			additionInfo = new AlarmAdditionalInformation();
		else
			additionInfo = (AlarmAdditionalInformation) faultFields.get(Constants.ALARM_ADD_INFO);
		if (!eventDef.get(Constants.PRO_REP_ACT).asText().isEmpty())
			additionInfo.setAdditionalProperty(Constants.PRO_REP_ACT, eventDef.get(Constants.PRO_REP_ACT).asText());
		if (!eventDef.get(Constants.PRB_CAUSE).asText().isEmpty())
			additionInfo.setAdditionalProperty(Constants.PRB_CAUSE, eventDef.get(Constants.PRB_CAUSE).asText());
		if (!eventDef.get(Constants.ADD_TXT).asText().isEmpty())
			additionInfo.setAdditionalProperty(Constants.ADD_TXT, eventDef.get(Constants.ADD_TXT).asText());
		if (!getCorrelationId((String) evtDef.get(Constants.CORR_NOTIF), (String) comEvtHdr.get(Constants.SOURCE_ID))
				.isEmpty())
			additionInfo.setAdditionalProperty(Constants.CORRELATION_ID, getCorrelationId(
					(String) evtDef.get(Constants.CORR_NOTIF), (String) comEvtHdr.get(Constants.SOURCE_ID)));
		if (eventDef.has(Constants.SVC_TYPE_AFFECTED))
			additionInfo.setAdditionalProperty(Constants.SVC_TYPE_AFFECTED,
					eventDef.get(Constants.SVC_TYPE_AFFECTED).asText());
		if (eventDef.has(Constants.ALARM_HIERARCHY))
			additionInfo.setAdditionalProperty(Constants.ALARM_HIERARCHY,
					eventDef.get(Constants.ALARM_HIERARCHY).asText());
		if (eventDef.has(Constants.CLASSIFICATION))
			additionInfo.setAdditionalProperty(Constants.CLASSIFICATION,
					eventDef.get(Constants.CLASSIFICATION).asText());
		if (eventDef.has(Constants.ALARM_TYPE))
			additionInfo.setAdditionalProperty(Constants.ALARM_TYPE, eventDef.get(Constants.ALARM_TYPE).asText());
		notificationFields.put(Constants.ADD_FIELDS, additionInfo);
		faultFields.put(Constants.ALARM_ADD_INFO, additionInfo);
		tcaFields.put(Constants.ADD_FIELDS, additionInfo);
		if (eventDef.has(Constants.DOMAIN)) {
			comEvtHdr.put(Constants.DOMAIN, eventDef.get(Constants.DOMAIN).asText(""));
		}
		if ((comEvtHdr.get(Constants.DOMAIN)).toString().equalsIgnoreCase((Domain.FAULT).toString())) {
			// to handle 'resolved' case from prometheus alert
			if (!faultFields.containsKey(Constants.EVENT_SEVERITY))
				if (eventDef.has(Constants.EVENT_SEVERITY)) {
					faultFields.put(Constants.EVENT_SEVERITY, eventDef.get(Constants.EVENT_SEVERITY).asText());
				} else {
					faultFields.put(Constants.EVENT_SEVERITY, eventDef.get(Constants.PERCEIVED_SEVERITY).asText());
				}
			faultFields.put(Constants.SPECIFIC_PRB, eventDef.get(Constants.SPECIFIC_PRB).asText());
			// faultFields.putIfAbsent(Constants.EVENT_SRC_TYPE, "NF");
			// in case of prometheus alerts, it will already be
			// configured as paas or caas
			faultFields.put(Constants.ALARM_CONDITION, comEvtHdr.get(Constants.LOCAL_EVT_NAME));
			faultFields.put(Constants.ALARM_INTFC, Constants.EMPTY_STRING);
			faultFields.put(Constants.FAULT_FIELDS_VER, FaultFields.FaultFieldsVersion._4_0);
			faultFields.put(Constants.EVENT_CATEGORY, eventDef.get(Constants.EVENT_TYPE).asText());
			faultFields.put(Constants.VF_STATUS, FaultFields.VfStatus.ACTIVE);

		} else if ((comEvtHdr.get(Constants.DOMAIN)).toString()
				.equalsIgnoreCase((Domain.THRESHOLD_CROSSING_ALERT).toString())) {
			if (!tcaFields.containsKey(Constants.EVENT_SEVERITY))
				if (eventDef.has(Constants.EVENT_SEVERITY)) {
					tcaFields.put(Constants.EVENT_SEVERITY, eventDef.get(Constants.EVENT_SEVERITY).asText());
				} else {
					tcaFields.put(Constants.EVENT_SEVERITY, eventDef.get(Constants.PERCEIVED_SEVERITY).asText());
				}
			if (tcaFields.get(Constants.EVENT_SEVERITY).equals("CLEAR")) {
				tcaFields.put(Constants.ALERT_ACTION, ThresholdCrossingAlertFields.AlertAction.CLEAR);
			} else {
				tcaFields.put(Constants.ALERT_ACTION, ThresholdCrossingAlertFields.AlertAction.SET);
			}
			tcaFields.put(Constants.ALERT_DES, eventDef.get(Constants.SPECIFIC_PRB).asText());
			tcaFields.put(Constants.ALERT_TYPE, ThresholdCrossingAlertFields.AlertType.SERVICE_ANOMALY);
			tcaFields.put(Constants.COLLECTION_TS, dateUtil.getTimestamp());
			String substring[] = startTime.split("\\.");
			if (substring[(substring.length - 1)].length() != 10) {
				SimpleDateFormat ts_formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS");
				try {
					startTime = ts_formatter.format(ts_formatter.parse(startTime)) + "Z";
				} catch (ParseException e) {
					LOG.error(e);
				}
			}
			tcaFields.put(Constants.EVT_START_TS, startTime);
			tcaFields.put(Constants.TH_CR_FLD_VERSION,
					ThresholdCrossingAlertFields.ThresholdCrossingFieldsVersion._4_0);
			Criticality crit = AdditionalParameter.Criticality.MAJ;
			org.xgvela.model.HashMap map = new org.xgvela.model.HashMap();
			map.setAdditionalProperty("NA", "NA");
			if (tcaFields.get(Constants.EVENT_SEVERITY).equals("CRITICAL"))
				crit = AdditionalParameter.Criticality.CRIT;
			AdditionalParameter addParam = new AdditionalParameter(crit, map, "NA");
			List<AdditionalParameter> addParamList = new ArrayList<AdditionalParameter>();
			addParamList.add(addParam);
			tcaFields.put(Constants.ADD_PARAM, addParamList);
		} else if ((comEvtHdr.get(Constants.DOMAIN)).toString().equalsIgnoreCase((Domain.NOTIFICATION).toString())) {
			if (!notificationFields.containsKey(Constants.CHANGE_TYPE))
				notificationFields.put(Constants.CHANGE_TYPE, comEvtHdr.get(Constants.LOCAL_EVT_NAME));
			notificationFields.put(Constants.NOTI_FIELDS_VERSION, NotificationFields.NotificationFieldsVersion._2_0);
		}

		eventMap.put(Constants.COMMON_EVT_HDR, comEvtHdr);
		eventMap.put(Constants.FAULT_FIELDS, faultFields);
		eventMap.put(Constants.EVENT_DEF, evtDef);
		eventMap.put(Constants.TCA_FIELDS, tcaFields);
		eventMap.put(Constants.NOTIFICATION_FIELDS, notificationFields);

		LOG.debug("Mapping populated with event definition.");

		return eventMap;
	}

	private String getCorrelationId(String corrNotifNode, String sourceId) {
		String correlationId = Constants.EMPTY_STRING;
		if (!corrNotifNode.isEmpty()) {
			List<String> corrNotifList = new ArrayList<String>(Arrays.asList(corrNotifNode.split(",")));
			for (String corrNotif : corrNotifList)
				if (correlationId.equalsIgnoreCase(Constants.EMPTY_STRING))
					correlationId = getEventId(sourceId, corrNotif);
				else
					correlationId = correlationId + "," + getEventId(sourceId, corrNotif);
		}
		return correlationId;
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> fillCimFields(Map<String, Object> eventMap, String message) {

		LOG.debug("Filling Event-Map from CIM message.");

		Map<String, Object> faultFields = (Map<String, Object>) eventMap.get(Constants.FAULT_FIELDS);
		Map<String, Object> comEvtHdr = (Map<String, Object>) eventMap.get(Constants.COMMON_EVT_HDR);
		Map<String, Object> evtDef = (Map<String, Object>) eventMap.get(Constants.EVENT_DEF);
		Map<String, Object> notificationFields = (Map<String, Object>) eventMap.get(Constants.NOTIFICATION_FIELDS);

		ObjectMapper objectMapper = getMapper();

		// parsing cim message
		JsonNode eventNode = null;
		try {
			eventNode = objectMapper.readTree(message);
		} catch (JsonMappingException e) {
			LOG.error(e.getMessage());
		} catch (JsonProcessingException e) {
			LOG.error(e.getMessage());
		}

		// get sourceHeaders as Map
		JsonNode sourceHeaders = eventNode.get(Constants.SRC_HEADERS);
		Map<String, Object> srcHdr = objectMapper.convertValue(sourceHeaders, new TypeReference<Map<String, Object>>() {
		});

		String mgdObj = Constants.EMPTY_STRING;
		if (eventNode.has(Constants.MGD_OBJ) && eventNode.get(Constants.MGD_OBJ) != null)
			mgdObj = getObjectAsString(eventNode.get(Constants.MGD_OBJ));
		if (eventNode.has(Constants.STATE_CH_DEF) && eventNode.get(Constants.STATE_CH_DEF) != null)
			notificationFields = getNotificationFields(notificationFields, eventNode.get(Constants.STATE_CH_DEF));
		srcHdr.put(Constants.MGD_OBJ, mgdObj);
		srcHdr.put(Constants.CONTAINER_ID, eventNode.get(Constants.CONTAINER_ID).asText());
		eventMap.put(Constants.SRC_HEADERS, srcHdr);

		// get addInfo as Map
		AlarmAdditionalInformation alarmAdditionalInfoList = new AlarmAdditionalInformation();
		if (eventNode.has(Constants.ADD_INFO) && eventNode.get(Constants.ADD_INFO) != null) {
			JsonNode alarmAdditionalInfo = eventNode.get(Constants.ADD_INFO);
			extractAdditionalInfo(alarmAdditionalInfoList, alarmAdditionalInfo);
			faultFields.put(Constants.ALARM_ADD_INFO, alarmAdditionalInfoList);
			notificationFields.put(Constants.ADD_FIELDS, alarmAdditionalInfoList);
			String addInfo = getObjectAsString(alarmAdditionalInfo);
			evtDef.put(Constants.ADD_INFO, addInfo);

		}
		Map<String, String> additionalInfoMap = new HashMap<String, String>();
		if (alarmAdditionalInfoList != null)
			additionalInfoMap = alarmAdditionalInfoList.getAdditionalProperties();
		// if (!additionalInfoMap.containsKey(Constants.ME_ID)) {
		String nfDn = Constants.EMPTY_STRING, nfServiceDn = Constants.EMPTY_STRING,
				nfServiceInstanceDn = Constants.EMPTY_STRING;
		alarmAdditionalInfoList.setAdditionalProperty(Constants.ME_LABEL, meDn);
		alarmAdditionalInfoList.setAdditionalProperty(Constants.ME_ID, meId);
		if (!additionalInfoMap.containsKey(Constants.NW_ID) && srcHdr.containsKey(Constants.NW_ID)) {
			nfDn = meDn + ",NetworkFunction=" + srcHdr.get(Constants.NW_ID).toString();
			if (!additionalInfoMap.containsKey(Constants.NW_FN_TYPE) && srcHdr.containsKey(Constants.NW_FN_TYPE))
				alarmAdditionalInfoList.setAdditionalProperty(Constants.NW_FN_TYPE,
						srcHdr.get(Constants.NW_FN_TYPE).toString());
			alarmAdditionalInfoList.setAdditionalProperty(Constants.NF_LABEL, nfDn);
			alarmAdditionalInfoList.setAdditionalProperty(Constants.NW_ID, getUUID(nfDn));
			if (!additionalInfoMap.containsKey(Constants.NF_SERVICE_ID)
					&& srcHdr.containsKey(Constants.NF_SERVICE_ID)) {
				if (!additionalInfoMap.containsKey(Constants.NF_SVC_TYPE) && srcHdr.containsKey(Constants.NF_SVC_TYPE))
					alarmAdditionalInfoList.setAdditionalProperty(Constants.NF_SVC_TYPE,
							srcHdr.get(Constants.NF_SVC_TYPE).toString());
				nfServiceDn = nfDn + ",NFService=" + srcHdr.get(Constants.NF_SERVICE_ID);
				alarmAdditionalInfoList.setAdditionalProperty(Constants.NF_SERVICE_LABEL, nfServiceDn);
				alarmAdditionalInfoList.setAdditionalProperty(Constants.NF_SERVICE_ID, getUUID(nfServiceDn));
				if (!additionalInfoMap.containsKey(Constants.NF_SERVICE_INSTANCE_ID)
						&& srcHdr.containsKey(Constants.POD_ID)) {
					nfServiceInstanceDn = nfServiceDn + ",NFServiceInstance=" + srcHdr.get(Constants.POD_ID);
					alarmAdditionalInfoList.setAdditionalProperty(Constants.NF_SERVICE_INSTANCE_LABEL,
							nfServiceInstanceDn);
					alarmAdditionalInfoList.setAdditionalProperty(Constants.NF_SERVICE_INSTANCE_ID,
							getUUID(nfServiceInstanceDn));
				}
			}
		}
		/*
		 * else { alarmAdditionalInfoList.setAdditionalProperty(Constants.ME_ID, meId);
		 * if (!additionalInfoMap.containsKey(Constants.NW_ID))
		 * alarmAdditionalInfoList.setAdditionalProperty(Constants.NW_ID,
		 * getUUID((String) srcHdr.get(Constants.NW_FN))); if
		 * (!additionalInfoMap.containsKey(Constants.NF_SERVICE_ID))
		 * alarmAdditionalInfoList.setAdditionalProperty(Constants.NF_SERVICE_ID,
		 * getUUID((String) srcHdr.get(Constants.MICROSERVICE))); if
		 * (!additionalInfoMap.containsKey(Constants.NF_SERVICE_INSTANCE_ID))
		 * alarmAdditionalInfoList.setAdditionalProperty(Constants.
		 * NF_SERVICE_INSTANCE_ID, getUUID((String) srcHdr.get(Constants.POD_ID)));
		 *
		 * }
		 *
		 * }
		 */
		if (srcHdr.containsKey(Constants.POD_UUID)) {
			alarmAdditionalInfoList.setAdditionalProperty(Constants.POD_UUID,
					srcHdr.get(Constants.POD_UUID).toString());
		}
		if (srcHdr.containsKey(Constants.UHN)) {
			alarmAdditionalInfoList.setAdditionalProperty(Constants.UHN, srcHdr.get(Constants.UHN).toString());
		}
		if (srcHdr.containsKey(Constants.CNFC_UUID)) {
			alarmAdditionalInfoList.setAdditionalProperty(Constants.CNFC_UUID,
					srcHdr.get(Constants.CNFC_UUID).toString());
		}
		faultFields.put(Constants.ALARM_ADD_INFO, alarmAdditionalInfoList);
		notificationFields.put(Constants.ADD_FIELDS, alarmAdditionalInfoList);

		String localEventName = eventNode.get(Constants.EVENT_NAME).asText();
		String eventName = Constants.EMPTY_STRING;
		if (srcHdr.containsKey(Constants.NW_FN_TYPE) && !srcHdr.get(Constants.NW_FN_TYPE).toString().isEmpty()) {
			String nfType = srcHdr.get(Constants.NW_FN_TYPE).toString();
			if (srcHdr.containsKey(Constants.VENDOR_ID) && !srcHdr.get(Constants.VENDOR_ID).toString().isEmpty()) {
				String vendorId = srcHdr.get(Constants.VENDOR_ID).toString();
				eventName = nfType.substring(0, 1).toUpperCase() + nfType.substring(1) + "-"
						+ vendorId.substring(0, 1).toUpperCase() + vendorId.substring(1) + "_" + localEventName;
			}

			else {
				eventName = nfType.substring(0, 1).toUpperCase() + nfType.substring(1) + "_" + localEventName;
			}
		}

		long startEpochMillis = eventNode.get(Constants.EVENT_TS).asLong();
		long lastEpochMillis = startEpochMillis;
		// String sourceId = getSourceId(eventNode, mgdObj);

		comEvtHdr.put(Constants.DOMAIN, Domain.FAULT);
		comEvtHdr.put(Constants.LOCAL_EVT_NAME, localEventName);
		comEvtHdr.put(Constants.EVENT_NAME, eventName);
		comEvtHdr.put(Constants.LAST_EPOCH, lastEpochMillis);
		comEvtHdr.put(Constants.PRIORITY, Priority.NORMAL);

		comEvtHdr.put(Constants.REP_NAME, reportingEntityName);
		comEvtHdr.put(Constants.REP_ID, reportingEntityId);

		// comEvtHdr.put(Constants.REP_ID, reportingEntityId);
		comEvtHdr.put(Constants.SEQUENCE, 0);

		if (eventNode.has(Constants.SOURCE_NAME))
			comEvtHdr.put(Constants.SOURCE_NAME, eventNode.get(Constants.SOURCE_NAME).asText());
		else {
			if (srcHdr.containsKey(Constants.XGVELA_ID) && srcHdr.containsKey(Constants.NW_ID)
					&& srcHdr.containsKey(Constants.NF_SERVICE_ID) && srcHdr.containsKey(Constants.POD_ID)) {
				comEvtHdr.put(Constants.SOURCE_NAME, getSourceName(srcHdr, mgdObj));
			} else {
				if (srcHdr.containsKey(Constants.POD_ID))
					comEvtHdr.put(Constants.SOURCE_NAME, srcHdr.get(Constants.POD_ID).toString());
				else
					comEvtHdr.put(Constants.SOURCE_NAME, getSourceId(eventNode, mgdObj));
			}
		}
		if (eventNode.has(Constants.SOURCE_ID))
			comEvtHdr.put(Constants.SOURCE_ID, eventNode.get(Constants.SOURCE_ID).asText());
		else
			comEvtHdr.put(Constants.SOURCE_ID, getUUID(comEvtHdr.get(Constants.SOURCE_NAME).toString()));
		comEvtHdr.put(Constants.START_EPOCH, startEpochMillis);
		comEvtHdr.put(Constants.VERSION, CommonEventHeader.Version._4_1);
		comEvtHdr.put(Constants.VES_EVENT_LIS_VERSION, CommonEventHeader.VesEventListenerVersion._7_1);
		if (additionalInfoMap.containsKey(Constants.NW_FN_TYPE)) {
			comEvtHdr.put(Constants.NF_NAMING_CODE, additionalInfoMap.get(Constants.NW_FN_TYPE));
			if (additionalInfoMap.containsKey(Constants.NF_SVC_TYPE))
				comEvtHdr.put(Constants.NFC_NAMING_CODE, additionalInfoMap.get(Constants.NF_SVC_TYPE));
		} else {
			if (srcHdr.containsKey(Constants.NW_FN_TYPE))
				comEvtHdr.put(Constants.NF_NAMING_CODE, srcHdr.get(Constants.NW_FN_TYPE));
			if (srcHdr.containsKey(Constants.NF_SVC_TYPE))
				comEvtHdr.put(Constants.NFC_NAMING_CODE, srcHdr.get(Constants.NF_SVC_TYPE));
		}
		if (eventNode.has(Constants.EVENT_SRC_TYPE))
			faultFields.put(Constants.EVENT_SRC_TYPE, eventNode.get(Constants.EVENT_SRC_TYPE));
		else if (srcHdr.containsKey(Constants.NW_FN_TYPE))
			faultFields.put(Constants.EVENT_SRC_TYPE, srcHdr.get(Constants.NW_FN_TYPE));
		else
			faultFields.put(Constants.EVENT_SRC_TYPE, "NF");
		eventMap.put(Constants.COMMON_EVT_HDR, comEvtHdr);
		eventMap.put(Constants.FAULT_FIELDS, faultFields);
		eventMap.put(Constants.EVENT_DEF, evtDef);
		eventMap.put(Constants.NOTIFICATION_FIELDS, notificationFields);

		LOG.debug("Mapping populated with CIM headers");

		return eventMap;
	}

	private void extractAdditionalInfo(AlarmAdditionalInformation alarmAdditionalInfoList,
			JsonNode alarmAdditionalInfo) {
		Iterator<JsonNode> elements = alarmAdditionalInfo.elements();
		while (elements.hasNext()) {
			JsonNode element = elements.next();
			String name = element.get("name").asText();
			String value = element.get("value").asText();
			alarmAdditionalInfoList.setAdditionalProperty(name, value);
		}
	}

	public String getUUID(String value) {
		return UUID.nameUUIDFromBytes(value.getBytes()).toString();
	}

	private String getSourceName(Map<String, Object> srcHdr, String mgdObj) {
		String dnPrefix = Constants.EMPTY_STRING;
		if (System.getenv(Constants.DN_PREFIX) != null) {
			dnPrefix = System.getenv(Constants.DN_PREFIX);
		} else
			dnPrefix = "xgvela";

		if (mgdObj.isEmpty())
			return dnPrefix + ",ManagedElement=me-" + srcHdr.get(Constants.XGVELA_ID) + ",NetworkFunction="
					+ srcHdr.get(Constants.NW_ID) + ",NFService=" + srcHdr.get(Constants.NF_SERVICE_ID)
					+ ",NFServiceInstance=" + srcHdr.get(Constants.POD_ID);
		else
			return dnPrefix + ",ManagedElement=me-" + srcHdr.get(Constants.XGVELA_ID) + ",NetworkFunction="
					+ srcHdr.get(Constants.NW_ID) + ",NFService=" + srcHdr.get(Constants.NF_SERVICE_ID)
					+ ",NFServiceInstance=" + srcHdr.get(Constants.POD_ID) + ","
					+ mgdObj.substring(0, mgdObj.length() - 1).replace(":", "=").replace("/", ",");

	}

	private Map<String, Object> getNotificationFields(Map<String, Object> notificationFields, JsonNode node) {
		Iterator<JsonNode> elements = node.elements();
		while (elements.hasNext()) {
			JsonNode element = elements.next();
			if (element.get("name").asText().equals("changeIdentifier"))
				notificationFields.put(Constants.CHANGE_IDENTIFIER, element.get("value").asText());
			else if (element.get("name").asText().equals("newState"))
				notificationFields.put(Constants.NEW_STATE, element.get("value").asText());
			else if (element.get("name").asText().equals("oldState"))
				notificationFields.put(Constants.OLD_STATE, element.get("value").asText());
			else if (element.get("name").asText().equals("changeType"))
				notificationFields.put(Constants.CHANGE_TYPE, element.get("value").asText());
		}
		return notificationFields;
	}

	public String getEventId(String sourceId, String eventName) {

		return DigestUtils.md5Hex(eventName + sourceId);
	}

	private String getObjectAsString(JsonNode node) {
		String objString = Constants.EMPTY_STRING;
		Iterator<JsonNode> elements = node.elements();
		while (elements.hasNext()) {
			JsonNode element = elements.next();
			String name = element.get("name").asText();
			String value = element.get("value").asText();
			objString = objString + name + ":" + value + "/";
		}
		if (!objString.equalsIgnoreCase(Constants.EMPTY_STRING))
			return objString.substring(0, objString.length() - 1);
		else
			return objString;
	}

	private String getSourceId(JsonNode eventNode, String mgdObj) {
		String sourceId = "cluster";
		String nf = eventNode.get(Constants.SRC_HEADERS).get(Constants.NW_FN).asText("nf");
		String msvc = eventNode.get(Constants.SRC_HEADERS).get(Constants.MICROSERVICE).asText("msvc");
		String svcVersion = null;
		if (eventNode.get(Constants.SRC_HEADERS).has(Constants.SVC_VERSION)
				&& eventNode.get(Constants.SRC_HEADERS).get(Constants.SVC_VERSION) != null)
			svcVersion = eventNode.get(Constants.SRC_HEADERS).get(Constants.SVC_VERSION).asText("v0");
		String pid = eventNode.get(Constants.SRC_HEADERS).get(Constants.POD_ID).asText("pid");
		String cid = eventNode.get(Constants.CONTAINER_ID).asText("cid");

		if (svcVersion != null && !svcVersion.isEmpty() && !svcVersion.equalsIgnoreCase("v0")) {
			sourceId = sourceId + "/" + nf + "/" + msvc + "/" + svcVersion + "/" + pid + "/" + cid + "/";
		} else {
			sourceId = sourceId + "/" + nf + "/" + msvc + "/" + pid + "/" + cid + "/";
		}
		sourceId += mgdObj;

		if (mgdObj.isEmpty())
			sourceId = sourceId.substring(0, sourceId.length() - 1);

		return sourceId;
	}

	public Map<String, Object> fillReportingFields(Map<String, Object> comEvtHdrMap) {
		comEvtHdrMap.put(Constants.REP_ID, reportingEntityId);
		comEvtHdrMap.put(Constants.REP_NAME, reportingEntityName);

		return comEvtHdrMap;
	}

	public void initSequence() throws InterruptedException {
		TimeUnit.SECONDS.sleep(15);
		if (DbAdaptor.storageEngine.equalsIgnoreCase("zookeeper")) {
			globalSequence = zkUtil.getSequence();
			LOG.debug("Set value of globalSequence to : " + globalSequence);
		} else {
			RestHighLevelClient client = esClient.getConnection();
			SearchRequest searchRequest = new SearchRequest("events*");
			searchRequest.indicesOptions(IndicesOptions.LENIENT_EXPAND_OPEN);
			SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
			searchSourceBuilder.query(QueryBuilders.matchAllQuery()).size(1000);
			AggregationBuilder aggregation = AggregationBuilders.max("sequence").field("commonEventHeader.sequence");
			searchSourceBuilder.aggregation(aggregation);
			searchSourceBuilder.fetchSource(false);
			searchRequest.source(searchSourceBuilder);
			LOG.debug("Search request: " + searchRequest.toString());
			SearchResponse searchResponse = null;
			int countRetry = 0;
			while (true) {
				try {
					searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
					break;
				} catch (Exception e) {
					if (countRetry == JsonUtil.maxRetries) {
						LOG.error(
								"Error occured while executing query on Elastic Search, max retries attempted. Exiting the service!!",
								e);
						System.out.println(
								"Error occured while executing query on Elastic Search, max retries attempted. Exiting the service!!");
						System.exit(0);
					}
					countRetry++;
					LOG.debug(
							"Error occured while executing query on Elastic Search, retrying..... count:" + countRetry,
							e);
				}
			}
			Aggregations aggregations = searchResponse.getAggregations();
			Max maximum = aggregations.get("sequence");
			if (maximum.getValue() != Double.NEGATIVE_INFINITY) {
				globalSequence = (int) maximum.getValue();
				LOG.debug("Set value of globalSequence to : " + globalSequence);
			} else {
				globalSequence = 0;
				LOG.debug("Set value of globalSequence to : " + globalSequence);
			}
		}

	}

	public void initSubscription() {
		subscribeToNats();
		ObjectMapper objectMapper = getMapper();
		ObjectNode jsonBody = mapper.createObjectNode();
		jsonBody.putArray("localEventName").add("NFServiceInstanceDeleted");
		// jsonBody.putArray("nfNamingCode").add("xgvela");
		// jsonBody.putArray("nfcNamingCode").add("topo-engine");
		String jsonString = Constants.EMPTY_STRING;
		try {
			jsonString = objectMapper.writeValueAsString(jsonBody);
		} catch (JsonProcessingException e1) {
			LOG.error("Error occured while parsing JSON object: " + e1);
		}
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> entity = new HttpEntity<String>(jsonString, headers);
		String subsUrl = "http://localhost:6060/api/v1/_operations/event/subscriptions";
		LOG.info("Sending Subscription Request to URL: " + subsUrl + " with body: " + jsonString);

		try {
			ResponseEntity<String> response = restTemplate.exchange(subsUrl, HttpMethod.POST, entity, String.class);
			LOG.debug(response);
		} catch (Exception e) {
			LOG.error(e.getMessage());
		}

	}

	private void subscribeToNats() {
		Subscription subs;
		subs = NatsUtil.natsConnection.subscribe("EVENT-NOTIFICATION");
		LOG.debug("Subscribed to NATS topic EVENT-NOTIFICATION");
		Duration sec = Duration.ZERO;
		ObjectMapper objectMapper = getMapper();
		class MyThread extends Thread {
			public void run() {
				while (true) {
					try {
						Message msg = subs.nextMessage(sec);
						if (msg != null) {
							String data = new String(msg.getData());
							LOG.debug("NATS message recieved on topic EVENT-NOTIFICATION:" + data);
							try {
								JsonNode jsonData = objectMapper.readTree(data);
								String sourceName = jsonData.get("event").get("commonEventHeader").get("sourceName")
										.asText();
								String podId = getPodID(sourceName);
								checkAlarms(podId);
							} catch (JsonProcessingException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					} catch (IllegalStateException | InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

			}

		}
		MyThread thread = new MyThread();
		thread.start();
	}

	public void checkAlarms(String podId) {
		if (DbAdaptor.storageEngine.equalsIgnoreCase("elasticsearch")) {
			RestHighLevelClient client = esClient.getConnection();
			SearchRequest searchRequest = new SearchRequest("alarms*");
			searchRequest.indicesOptions(IndicesOptions.LENIENT_EXPAND_OPEN);
			SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
			searchSourceBuilder.query(QueryBuilders.termQuery("sourceHeaders.podId", podId));
			searchRequest.source(searchSourceBuilder);
			LOG.debug("Search request: " + searchRequest.toString());
			SearchResponse searchResponse = null;
			int countRetry = 0;
			while (true) {
				try {
					searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
					break;
				} catch (Exception e) {
					if (countRetry == JsonUtil.maxRetries) {
						LOG.error(
								"Error occured while executing query on Elastic Search, max retries attempted. Exiting the service!!",
								e);
						System.out.println(
								"Error occured while executing query on Elastic Search, max retries attempted. Exiting the service!!");
						System.exit(0);
					}
					countRetry++;
					LOG.debug(
							"Error occured while executing query on Elastic Search, retrying..... count:" + countRetry,
							e);
				}
			}
			SearchHit[] results = searchResponse.getHits().getHits();
			for (SearchHit hit : results) {
				ObjectMapper mapper = new ObjectMapper();
				Map<String, Object> getExistingDoc = hit.getSourceAsMap();
				Map<String, Object> tcaFieldsMap = (Map<String, Object>) getExistingDoc.get(Constants.TCA_FIELDS);
				Map<String, Object> faultFieldsMap = (Map<String, Object>) getExistingDoc.get(Constants.FAULT_FIELDS);
				Map<String, Object> comEvtHdrMap = (Map<String, Object>) getExistingDoc.get(Constants.COMMON_EVT_HDR);
				if ((comEvtHdrMap.get(Constants.DOMAIN)).toString().equalsIgnoreCase((Domain.FAULT).toString()))
					faultFieldsMap.put(Constants.EVENT_SEVERITY, "CLEAR");
				else if ((comEvtHdrMap.get(Constants.DOMAIN)).toString()
						.equalsIgnoreCase(Domain.THRESHOLD_CROSSING_ALERT.toString())) {
					tcaFieldsMap.put(Constants.EVENT_SEVERITY, "CLEAR");
					tcaFieldsMap.put(Constants.ALERT_ACTION, ThresholdCrossingAlertFields.AlertAction.CLEAR);
				}
				Map<String, Object> eventDefinition = (Map<String, Object>) getExistingDoc.get(Constants.EVENT_DEF);
				Byte severityCode = 6;
				eventDefinition.put(Constants.SEVERITY_CODE, severityCode);
				Map<String, Object> comment = new HashMap<String, Object>();
				comment.put("commentUserId", Constants.EMPTY_STRING);
				comment.put("commentSystemId", "FMAAS");
				comment.put("commentText", "Manual Clear");
				comment.put("commentTime", dateUtil.getTimestamp());
				getExistingDoc.put("comment", comment);
				getExistingDoc.put(Constants.EVENT_DEF, eventDefinition);
				getExistingDoc.put(Constants.FAULT_FIELDS, faultFieldsMap);
				getExistingDoc.put(Constants.TCA_FIELDS, tcaFieldsMap);
				getExistingDoc.put(Constants.COMMON_EVT_HDR, comEvtHdrMap);
				esUtil.manualClear(getExistingDoc);
			}
		} else {
			if (LeaderElection.isLeader) {
				ArrayList<String> alarms = zkUtil.getAlarmsforPodId(podId);
				// KafkaConfiguration kf = new KafkaConfiguration();
				// ObjectMapper mapper = new ObjectMapper();
				for (String s : alarms) {
					/*
					 * ArrayList<String> documents = new ArrayList<>(); String sequence =
					 * zkUtil.getAlarmSequence(s); String offset = zkUtil.getOffset(sequence);
					 * documents.addAll(kf.consumerByOffset(Long.parseLong(offset),
					 * Long.parseLong(offset))); JsonNode eventNode = null; try { eventNode =
					 * mapper.readTree(documents.get(0)); } catch (JsonMappingException e) {
					 * LOG.error(e.getMessage()); } catch (JsonProcessingException e) {
					 * LOG.error(e.getMessage()); } Map<String, Object> event =
					 * mapper.convertValue(eventNode, new TypeReference<Map<String, Object>>() { });
					 */
					Map<String, Object> getExistingDoc = new HashMap<>();
					Map<String, Object> comEvtHdrMap = zkUtil.getCommonEventHeaderZK(s);
					Map<String, Object> tcaFieldsMap = new HashMap<>();
					Map<String, Object> faultFieldsMap = new HashMap<>();
					if ((comEvtHdrMap.get(Constants.DOMAIN)).toString().equalsIgnoreCase((Domain.FAULT).toString())) {
						faultFieldsMap = zkUtil.getFaultFieldsZK(s);
						faultFieldsMap.put(Constants.EVENT_SEVERITY, "CLEAR");
					} else if ((comEvtHdrMap.get(Constants.DOMAIN)).toString()
							.equalsIgnoreCase(Domain.THRESHOLD_CROSSING_ALERT.toString())) {
						tcaFieldsMap = zkUtil.getTCAFieldsZK(s);
						tcaFieldsMap.put(Constants.EVENT_SEVERITY, "CLEAR");
						tcaFieldsMap.put(Constants.ALERT_ACTION, ThresholdCrossingAlertFields.AlertAction.CLEAR);
					}
					Byte severityCode = 6;
					zkUtil.updateSeverityCode(s, Byte.toString(severityCode));
					Map<String, Object> eventDefinition = zkUtil.getEventDefZK(s);
					Map<String, Object> comment = new HashMap<String, Object>();
					comment.put("commentUserId", Constants.EMPTY_STRING);
					comment.put("commentSystemId", "FMAAS");
					comment.put("commentText", "Manual Clear");
					comment.put("commentTime", dateUtil.getTimestamp());
					getExistingDoc.put("comment", comment);
					getExistingDoc.put(Constants.EVENT_DEF, eventDefinition);
					getExistingDoc.put(Constants.FAULT_FIELDS, faultFieldsMap);
					getExistingDoc.put(Constants.TCA_FIELDS, tcaFieldsMap);
					getExistingDoc.put(Constants.COMMON_EVT_HDR, comEvtHdrMap);
					esUtil.manualClear(getExistingDoc);
				}
			}
		}

	}

	private String getPodID(String sourceName) {
		String nfServiceInstance = sourceName.substring(sourceName.indexOf("NFServiceInstance"));
		String podId = nfServiceInstance.substring(nfServiceInstance.indexOf("=") + 1).trim();
		return podId;
	}

	public void initAlarmClear() {
		if (DbAdaptor.storageEngine.equalsIgnoreCase("elasticsearch")) {
			RestHighLevelClient client = esClient.getConnection();
			SearchRequest searchRequest = new SearchRequest("alarms*");
			searchRequest.indicesOptions(IndicesOptions.LENIENT_EXPAND_OPEN);
			SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
			searchSourceBuilder.query(QueryBuilders.matchAllQuery());
			searchRequest.source(searchSourceBuilder);
			LOG.debug("Search request: " + searchRequest.toString());
			SearchResponse searchResponse = null;
			int countRetry = 0;
			while (true) {
				try {
					searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
					break;
				} catch (Exception e) {
					if (countRetry == JsonUtil.maxRetries) {
						LOG.error(
								"Error occured while executing query on Elastic Search, max retries attempted. Exiting the service!!",
								e);
						System.out.println(
								"Error occured while executing query on Elastic Search, max retries attempted. Exiting the service!!");
						System.exit(0);
					}
					countRetry++;
					LOG.debug(
							"Error occured while executing query on Elastic Search, retrying..... count:" + countRetry,
							e);
				}
			}
			SearchHit[] results = searchResponse.getHits().getHits();
			ArrayList<String> stresults = new ArrayList<String>();
			for (SearchHit hit : results) {
				Map<String, Object> getExistingDoc = hit.getSourceAsMap();
				Map<String, Object> sourceHeadersMap = (Map<String, Object>) getExistingDoc.get(Constants.SRC_HEADERS);
				String namespace = getNamespace(sourceHeadersMap);
				String podId = (String) sourceHeadersMap.get(Constants.POD_ID);
				LOG.debug("namespace: " + namespace + ", podId: " + podId);
				KubernetesClient kClient = k8sClient.getClient();
				if ((namespace != null && !namespace.isEmpty()) && (podId != null && !podId.isEmpty())) {
					if (kClient.pods().inNamespace(namespace).withName(podId).get() == null) {
						Map<String, Object> tcaFieldsMap = (Map<String, Object>) getExistingDoc
								.get(Constants.TCA_FIELDS);
						Map<String, Object> faultFieldsMap = (Map<String, Object>) getExistingDoc
								.get(Constants.FAULT_FIELDS);
						Map<String, Object> comEvtHdrMap = (Map<String, Object>) getExistingDoc
								.get(Constants.COMMON_EVT_HDR);
						if ((comEvtHdrMap.get(Constants.DOMAIN)).toString().equalsIgnoreCase((Domain.FAULT).toString()))
							faultFieldsMap.put(Constants.EVENT_SEVERITY, "CLEAR");
						else if ((comEvtHdrMap.get(Constants.DOMAIN)).toString()
								.equalsIgnoreCase(Domain.THRESHOLD_CROSSING_ALERT.toString())) {
							tcaFieldsMap.put(Constants.EVENT_SEVERITY, "CLEAR");
							tcaFieldsMap.put(Constants.ALERT_ACTION, ThresholdCrossingAlertFields.AlertAction.CLEAR);
						}
						Map<String, Object> eventDefinition = (Map<String, Object>) getExistingDoc
								.get(Constants.EVENT_DEF);
						Byte severityCode = 6;
						eventDefinition.put(Constants.SEVERITY_CODE, severityCode);
						Map<String, Object> comment = new HashMap<String, Object>();
						comment.put("commentUserId", Constants.EMPTY_STRING);
						comment.put("commentSystemId", "FMAAS");
						comment.put("commentText", "Manual Clear");
						comment.put("commentTime", dateUtil.getTimestamp());
						getExistingDoc.put("comment", comment);
						getExistingDoc.put(Constants.EVENT_DEF, eventDefinition);
						getExistingDoc.put(Constants.FAULT_FIELDS, faultFieldsMap);
						getExistingDoc.put(Constants.TCA_FIELDS, tcaFieldsMap);
						getExistingDoc.put(Constants.COMMON_EVT_HDR, comEvtHdrMap);
						esUtil.manualClear(getExistingDoc);
					}
				}

			}
		} else {
			if (LeaderElection.isLeader) {
				List<String> alarms = zkUtil.getAlarmIdList();
				// KafkaConfiguration kf = new KafkaConfiguration();
				// ObjectMapper mapper = new ObjectMapper();
				for (String s : alarms) {
					/*
					 * ArrayList<String> documents = new ArrayList<>(); String sequence =
					 * zkUtil.getAlarmSequence(s); String offset = zkUtil.getOffset(sequence);
					 * documents.addAll(kf.consumerByOffset(Long.parseLong(offset),
					 * Long.parseLong(offset))); JsonNode eventNode = null; Map<String, Object>
					 * event = mapper.convertValue(eventNode, new TypeReference<Map<String,
					 * Object>>() { });
					 */
					Map<String, Object> getExistingDoc = new HashMap<>();
					String namespace = zkUtil.getNamespace(s);
					String podId = zkUtil.getPodId(s);
					LOG.debug("namespace: " + namespace + ", podId: " + podId);
					KubernetesClient kClient = k8sClient.getClient();
					if ((namespace != null && !namespace.isEmpty()) && (podId != null && !podId.isEmpty())) {
						if (kClient.pods().inNamespace(namespace).withName(podId).get() == null) {
							Map<String, Object> comEvtHdrMap = zkUtil.getCommonEventHeaderZK(s);
							Map<String, Object> tcaFieldsMap = new HashMap<>();
							Map<String, Object> faultFieldsMap = new HashMap<>();
							if ((comEvtHdrMap.get(Constants.DOMAIN)).toString()
									.equalsIgnoreCase((Domain.FAULT).toString())) {
								faultFieldsMap = zkUtil.getFaultFieldsZK(s);
								faultFieldsMap.put(Constants.EVENT_SEVERITY, "CLEAR");
							} else if ((comEvtHdrMap.get(Constants.DOMAIN)).toString()
									.equalsIgnoreCase(Domain.THRESHOLD_CROSSING_ALERT.toString())) {
								tcaFieldsMap = zkUtil.getTCAFieldsZK(s);
								tcaFieldsMap.put(Constants.EVENT_SEVERITY, "CLEAR");
								tcaFieldsMap.put(Constants.ALERT_ACTION,
										ThresholdCrossingAlertFields.AlertAction.CLEAR);
							}
							Byte severityCode = 6;
							zkUtil.updateSeverityCode(s, Byte.toString(severityCode));
							Map<String, Object> eventDefinition = zkUtil.getEventDefZK(s);
							Map<String, String> comment = new HashMap<>();
							comment.put("commentUserId", Constants.EMPTY_STRING);
							comment.put("commentSystemId", "FMAAS");
							comment.put("commentText", "Manual Clear");
							comment.put("commentTime", dateUtil.getTimestamp());
							zkUtil.addComment(s, comment);
							getExistingDoc.put("comment", comment);
							getExistingDoc.put(Constants.EVENT_DEF, eventDefinition);
							getExistingDoc.put(Constants.FAULT_FIELDS, faultFieldsMap);
							getExistingDoc.put(Constants.TCA_FIELDS, tcaFieldsMap);
							getExistingDoc.put(Constants.COMMON_EVT_HDR, comEvtHdrMap);
							esUtil.manualClear(getExistingDoc);
						}
					}

				}

			}
		}
	}

	private String getNamespace(Map<String, Object> sourceHeadersMap) {

		return (String) sourceHeadersMap.get(Constants.NW_FN_PREFIX);
	}

	public void initResetReplay() {
		LOG.debug("Sending replay reset request to VESGW");
		ResponseEntity<String> response = restTemplate.exchange(replayResetUrl, HttpMethod.GET, null, String.class);
		LOG.debug("Response: " + response.getBody() + ", StatusCode: " + response.getStatusCode() + ", Value: "
				+ response.getStatusCodeValue());
	}

}
