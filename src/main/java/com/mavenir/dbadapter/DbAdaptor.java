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

package org.xgvela.dbadapter;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.zookeeper.KeeperException;
import org.elasticsearch.action.DocWriteRequest.OpType;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.xgvela.cnf.Constants;
import org.xgvela.cnf.elastic.ElasticSearchClientConfiguration;
import org.xgvela.cnf.util.JsonUtil;
import org.xgvela.dbadapter.zk.ZKManager;
import org.xgvela.model.CommonEventHeader;
import org.xgvela.model.Event;
import org.xgvela.model.FaultFields;
import org.xgvela.model.NotificationFields;
import org.xgvela.model.ThresholdCrossingAlertFields;
import org.xgvela.model.CommonEventHeader.Domain;

@Component
public class DbAdaptor {

	private static Logger LOG = LogManager.getLogger(DbAdaptor.class);

	public static final String storageEngine = System.getenv("STORAGE_ENGINE");

	@Autowired
	ElasticSearchClientConfiguration esClient;

	@Autowired
	ZKManager zkManager;

	public void addDataToElasticSearch(String index, String source, String id) {
		IndexRequest indexRequest = new IndexRequest().setRefreshPolicy(RefreshPolicy.IMMEDIATE).id(id).index(index)
				.type(Constants.EVENT_MAPPING_TYPE).source(source, XContentType.JSON).opType(OpType.INDEX);
		int countRetry = 0;
		while (true) {
			try {
				RestHighLevelClient client = esClient.getConnection();
				IndexResponse indexResponse = client.index(indexRequest, RequestOptions.DEFAULT);
				LOG.debug("Index Response: " + indexResponse);
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
				LOG.debug("Error occured while executing query on Elastic Search, retrying..... count:" + countRetry,
						e);
			}
		}

	}

	public void addSequenceToZK(int sequence, long offset) {
		try {
			zkManager.create("/events/" + sequence, String.valueOf(offset).getBytes());
			LOG.debug("added sequence:" + sequence + " with offset:" + offset);
		} catch (KeeperException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void getoffset(int sequence) {

		try {
			Object bytes = zkManager.getZNodeData("/events/" + sequence, false);
			LOG.debug("got sequence:" + sequence + " with value:" + bytes.toString());
		} catch (KeeperException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void addAlarmToZK(String id, Map<String, Object> document) {
		try {
			ObjectMapper objectMapper = new ObjectMapper();
			@SuppressWarnings("unchecked")
			Map<String, Object> thresholdCrossingAlertsFieldsMap = (Map<String, Object>) document
					.get(Constants.TCA_FIELDS);
			ThresholdCrossingAlertFields thresholdCrossingAlertFields = objectMapper
					.convertValue(thresholdCrossingAlertsFieldsMap, ThresholdCrossingAlertFields.class);
			Map<String, Object> notificationFieldsMap = (Map<String, Object>) document
					.get(Constants.NOTIFICATION_FIELDS);
			NotificationFields notificationFields = objectMapper.convertValue(notificationFieldsMap,
					NotificationFields.class);
			Map<String, Object> faultFieldsMap = (Map<String, Object>) document.get(Constants.FAULT_FIELDS);
			FaultFields faultFields = objectMapper.convertValue(faultFieldsMap, FaultFields.class);
			Map<String, Object> commonEventHeaderMap = (Map<String, Object>) document.get(Constants.COMMON_EVT_HDR);
			Map<String, Object> sourceHeaderMap = (Map<String, Object>) document.get(Constants.SRC_HEADERS);
			Map<String, Object> commentMap = (Map<String, Object>) document.get(Constants.COMMENT);
			Map<String, Object> ackStateMap = (Map<String, Object>) document.get(Constants.ACK_STATE);
			Map<String, Object> eventCommonMap = (Map<String, Object>) document.get(Constants.EVT_COMMON);
			Map<String, Object> eventDefinationMap = (Map<String, Object>) document.get(Constants.EVENT_DEF);

			zkManager.create("/alarms/" + id + "/commonEventHeader/domain",
					commonEventHeaderMap.get(Constants.DOMAIN).toString().getBytes());
			zkManager.create("/alarms/" + id + "/commonEventHeader/eventId",
					commonEventHeaderMap.get(Constants.EVENT_ID).toString().getBytes());
			zkManager.create("/alarms/" + id + "/commonEventHeader/eventName",
					commonEventHeaderMap.get(Constants.EVENT_NAME).toString().getBytes());
			zkManager.create("/alarms/" + id + "/commonEventHeader/localEventName",
					commonEventHeaderMap.get(Constants.LOCAL_EVT_NAME).toString().getBytes());
			zkManager.create("/alarms/" + id + "/commonEventHeader/nfNamingCode",
					commonEventHeaderMap.get(Constants.NF_NAMING_CODE).toString().getBytes());
			zkManager.create("/alarms/" + id + "/commonEventHeader/nfcNamingCode",
					commonEventHeaderMap.get(Constants.NFC_NAMING_CODE).toString().getBytes());
			zkManager.create("/alarms/" + id + "/commonEventHeader/eventType",
					commonEventHeaderMap.get(Constants.EVENT_TYPE).toString().getBytes());
			zkManager.create("/alarms/" + id + "/commonEventHeader/lastEpochMillis",
					commonEventHeaderMap.get(Constants.LAST_EPOCH).toString().getBytes());
			zkManager.create("/alarms/" + id + "/commonEventHeader/priority",
					commonEventHeaderMap.get(Constants.PRIORITY).toString().getBytes());
			zkManager.create("/alarms/" + id + "/commonEventHeader/reportingEntityId",
					commonEventHeaderMap.get(Constants.REP_ID).toString().getBytes());
			zkManager.create("/alarms/" + id + "/commonEventHeader/reportingEntityName",
					commonEventHeaderMap.get(Constants.REP_NAME).toString().getBytes());
			zkManager.create("/alarms/" + id + "/commonEventHeader/sequence",
					commonEventHeaderMap.get(Constants.SEQUENCE).toString().getBytes());
			zkManager.create("/alarms/" + id + "/commonEventHeader/sourceId",
					commonEventHeaderMap.get(Constants.SOURCE_ID).toString().getBytes());
			zkManager.create("/alarms/" + id + "/commonEventHeader/sourceName",
					commonEventHeaderMap.get(Constants.SOURCE_NAME).toString().getBytes());
			zkManager.create("/alarms/" + id + "/commonEventHeader/startEpochMillis",
					commonEventHeaderMap.get(Constants.START_EPOCH).toString().getBytes());
			zkManager.create("/alarms/" + id + "/commonEventHeader/version",
					commonEventHeaderMap.get(Constants.VERSION).toString().getBytes());
			zkManager.create("/alarms/" + id + "/commonEventHeader/vesEventListenerVersion",
					commonEventHeaderMap.get(Constants.VES_EVENT_LIS_VERSION).toString().getBytes());
			zkManager.create("/alarms/" + id + "/commonEventHeader/vesEventListenerVersion",
					commonEventHeaderMap.get(Constants.VES_EVENT_LIS_VERSION).toString().getBytes());

			if ((commonEventHeaderMap.get(Constants.DOMAIN)).toString().equalsIgnoreCase((Domain.FAULT).toString())) {
				zkManager.create("/alarms/" + id + "/faultFields/alarmCondition",
						faultFields.getAlarmCondition().getBytes());
				zkManager.create("/alarms/" + id + "/faultFields/alarmInterfaceA",
						faultFields.getAlarmInterfaceA().getBytes());
				zkManager.create("/alarms/" + id + "/faultFields/eventCategory",
						faultFields.getEventCategory().getBytes());
				zkManager.create("/alarms/" + id + "/faultFields/eventSeverity",
						faultFields.getEventSeverity().toString().getBytes());
				zkManager.create("/alarms/" + id + "/faultFields/faultFieldsVersion",
						faultFields.getFaultFieldsVersion().toString().getBytes());
				zkManager.create("/alarms/" + id + "/faultFields/eventSourceType",
						faultFields.getEventSourceType().getBytes());
				zkManager.create("/alarms/" + id + "/faultFields/specificProblem",
						faultFields.getSpecificProblem().getBytes());
				zkManager.create("/alarms/" + id + "/faultFields/vfStatus",
						faultFields.getVfStatus().toString().getBytes());

				for (String s : faultFields.getAlarmAdditionalInformation().getAdditionalProperties().keySet())
					zkManager.create("/alarms/" + id + "/faultFields/alarmAdditionalInformation/" + s, faultFields
							.getAlarmAdditionalInformation().getAdditionalProperties().get(s).toString().getBytes());

			} else if ((commonEventHeaderMap.get(Constants.DOMAIN)).toString()
					.equalsIgnoreCase(Domain.THRESHOLD_CROSSING_ALERT.toString())) {
				zkManager.create("/alarms/" + id + "/tcaFields/additionalFields/alertAction",
						thresholdCrossingAlertFields.getAlertAction().toString().getBytes());
				zkManager.create("/alarms/" + id + "/tcaFields/alertDescription",
						thresholdCrossingAlertFields.getAlertDescription().getBytes());
				zkManager.create("/alarms/" + id + "/tcaFields/collectionTimestamp",
						thresholdCrossingAlertFields.getCollectionTimestamp().getBytes());
				zkManager.create("/alarms/" + id + "/tcaFields/alertType",
						thresholdCrossingAlertFields.getAlertType().toString().getBytes());
				zkManager.create("/alarms/" + id + "/tcaFields/eventSeverity",
						thresholdCrossingAlertFields.getEventSeverity().toString().getBytes());
				zkManager.create("/alarms/" + id + "/tcaFields/eventStartTimestamp",
						thresholdCrossingAlertFields.getEventStartTimestamp().getBytes());
				zkManager.create("/alarms/" + id + "/tcaFields/thresholdCrossingFieldsVersion",
						thresholdCrossingAlertFields.getThresholdCrossingFieldsVersion().toString().getBytes());
				zkManager.create("/alarms/" + id + "/tcaFields/additionalParameters/criticality",
						thresholdCrossingAlertFields.getAdditionalParameters().get(0).getCriticality().toString()
								.getBytes());
				zkManager.create("/alarms/" + id + "/tcaFields/additionalParameters/thresholdCrossed",
						thresholdCrossingAlertFields.getAdditionalParameters().get(0).getThresholdCrossed().toString()
								.getBytes());
				zkManager.create("/alarms/" + id + "/tcaFields/additionalParameters/hashMap/NA",
						thresholdCrossingAlertFields.getAdditionalParameters().get(0).getHashMap()
								.getAdditionalProperties().get("NA").toString().getBytes());

				for (String s : thresholdCrossingAlertFields.getAdditionalFields().getAdditionalProperties().keySet())
					zkManager.create("/alarms/" + id + "/tcaFields/additionalFields/" + s, thresholdCrossingAlertFields
							.getAdditionalFields().getAdditionalProperties().get(s).toString().getBytes());

			} else if ((commonEventHeaderMap.get(Constants.DOMAIN)).toString()
					.equalsIgnoreCase(Domain.NOTIFICATION.toString())) {
				zkManager.create("/alarms/" + id + "/notificationFields/changeIdentifier",
						notificationFields.getChangeIdentifier().getBytes());
				zkManager.create("/alarms/" + id + "/notificationFields/changeType",
						notificationFields.getChangeType().getBytes());
				zkManager.create("/alarms/" + id + "/notificationFields/newState",
						notificationFields.getNewState().getBytes());
				zkManager.create("/alarms/" + id + "/notificationFields/oldState",
						notificationFields.getOldState().getBytes());
				zkManager.create("/alarms/" + id + "/notificationFields/notificationFieldsVersion",
						notificationFields.getNotificationFieldsVersion().toString().getBytes());
				for (String s : notificationFields.getAdditionalFields().getAdditionalProperties().keySet())
					zkManager.create("/alarms/" + id + "/notificationFields/additionalFields/" + s, notificationFields
							.getAdditionalFields().getAdditionalProperties().get(s).toString().getBytes());
			}

			if (!sourceHeaderMap.isEmpty()) {
				if (sourceHeaderMap.containsKey("containerId"))
					zkManager.create("/alarms/" + id + "/sourceHeaders/containerId",
							sourceHeaderMap.get("containerId").toString().getBytes());
				if (sourceHeaderMap.containsKey("podId"))
					zkManager.create("/alarms/" + id + "/sourceHeaders/podId",
							sourceHeaderMap.get("podId").toString().getBytes());
				if (sourceHeaderMap.containsKey("microservice"))
					zkManager.create("/alarms/" + id + "/sourceHeaders/microservice",
							sourceHeaderMap.get("microservice").toString().getBytes());
				if (sourceHeaderMap.containsKey("nf"))
					zkManager.create("/alarms/" + id + "/sourceHeaders/nf",
							sourceHeaderMap.get("nf").toString().getBytes());
				if (sourceHeaderMap.containsKey("nfPrefix"))
					zkManager.create("/alarms/" + id + "/sourceHeaders/nfPrefix",
							sourceHeaderMap.get("nfPrefix").toString().getBytes());
				if (sourceHeaderMap.containsKey("nfType"))
					zkManager.create("/alarms/" + id + "/sourceHeaders/nfType",
							sourceHeaderMap.get("nfType").toString().getBytes());
				if (sourceHeaderMap.containsKey("svcVersion"))
					zkManager.create("/alarms/" + id + "/sourceHeaders/svcVersion",
							sourceHeaderMap.get("svcVersion").toString().getBytes());
			} else {
				zkManager.create("/alarms/" + id + "/sourceHeaders", "".getBytes());
			}
			if (!commentMap.isEmpty()) {
				if (commentMap.containsKey("commentTime"))
					zkManager.create("/alarms/" + id + "/comment/commentTime",
							commentMap.get("commentTime").toString().getBytes());
				if (commentMap.containsKey("commentUserId"))
					zkManager.create("/alarms/" + id + "/comment/commentUserId",
							commentMap.get("commentUserId").toString().getBytes());
				if (commentMap.containsKey("commentSystemId"))
					zkManager.create("/alarms/" + id + "/comment/commentSystemId",
							commentMap.get("commentSystemId").toString().getBytes());
				if (commentMap.containsKey("commentText"))
					zkManager.create("/alarms/" + id + "/comment/commentText",
							commentMap.get("commentText").toString().getBytes());
			} else {
				zkManager.create("/alarms/" + id + "/comment", "".getBytes());
			}

			if (!ackStateMap.isEmpty()) {
				if (ackStateMap.containsKey("ackUserId"))
					zkManager.create("/alarms/" + id + "/ackstate/ackUserId",
							ackStateMap.get("ackUserId").toString().getBytes());
				if (ackStateMap.containsKey("ackSystemId"))
					zkManager.create("/alarms/" + id + "/ackstate/ackSystemId",
							ackStateMap.get("ackSystemId").toString().getBytes());
				if (ackStateMap.containsKey("ackstate"))
					zkManager.create("/alarms/" + id + "/ackstate/ackstate",
							ackStateMap.get("ackstate").toString().getBytes());
			} else {
				zkManager.create("/alarms/" + id + "/ackstate", "".getBytes());
			}

			if (!eventCommonMap.isEmpty()) {
				if (eventCommonMap.containsKey("clearComment"))
					zkManager.create("/alarms/" + id + "/eventCommon/clearComment",
							eventCommonMap.get("clearComment").toString().getBytes());
			} else {
				zkManager.create("/alarms/" + id + "/eventCommon", "".getBytes());
			}

			if (!eventDefinationMap.isEmpty()) {
				if (eventDefinationMap.containsKey("severityCode"))
					zkManager.create("/alarms/" + id + "/eventDefinition/severityCode",
							eventDefinationMap.get("severityCode").toString().getBytes());
				if (eventDefinationMap.containsKey("probableCause"))
					zkManager.create("/alarms/" + id + "/eventDefinition/probableCause",
							eventDefinationMap.get("probableCause").toString().getBytes());
				if (eventDefinationMap.containsKey("rootCauseIndicator"))
					zkManager.create("/alarms/" + id + "/eventDefinition/rootCauseIndicator",
							eventDefinationMap.get("rootCauseIndicator").toString().getBytes());
				if (eventDefinationMap.containsKey("trendIndication"))
					zkManager.create("/alarms/" + id + "/eventDefinition/trendIndication",
							eventDefinationMap.get("trendIndication").toString().getBytes());
				if (eventDefinationMap.containsKey("correlatedNotifications"))
					zkManager.create("/alarms/" + id + "/eventDefinition/correlatedNotifications",
							eventDefinationMap.get("correlatedNotifications").toString().getBytes());
				if (eventDefinationMap.containsKey("additionalText"))
					zkManager.create("/alarms/" + id + "/eventDefinition/additionalText",
							eventDefinationMap.get("additionalText").toString().getBytes());
				if (eventDefinationMap.containsKey("proposedRepairAction"))
					zkManager.create("/alarms/" + id + "/eventDefinition/proposedRepairAction",
							eventDefinationMap.get("proposedRepairAction").toString().getBytes());
				if (eventDefinationMap.containsKey("additionalInfo"))
					zkManager.create("/alarms/" + id + "/eventDefinition/additionalInfo",
							eventDefinationMap.get("additionalInfo").toString().getBytes());
			} else {
				zkManager.create("/alarms/" + id + "/eventDefinition", "".getBytes());
			}

		} catch (KeeperException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public Boolean checkAlarmExistZK(String id) {
		return zkManager.ifExists("/alarms/" + id);
	}

	public void removeAlarmFromZk(String id) {
		if (checkAlarmExistZK(id)) {
			zkManager.deleteRecursive("/alarms/" + id);
		}

	}

}