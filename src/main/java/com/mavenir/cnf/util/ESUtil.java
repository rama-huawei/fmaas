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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.DocWriteRequest.OpType;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.ClearScrollRequest;
import org.elasticsearch.action.search.ClearScrollResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.xgvela.cnf.Constants;
import org.xgvela.cnf.elastic.ElasticSearchClientConfiguration;
import org.xgvela.dbadapter.DbAdaptor;
import org.xgvela.dbadapter.zk.ZKUtil;
import org.xgvela.model.AlarmAdditionalInformation;
import org.xgvela.model.CommonEventHeader;
import org.xgvela.model.Event;
import org.xgvela.model.NotificationFields;
import org.xgvela.model.ThresholdCrossingAlertFields;
import org.xgvela.model.CommonEventHeader.Domain;
import org.xgvela.model.FaultFields.EventSeverity;

@Component
public class ESUtil {

	private static Logger LOG = LogManager.getLogger(ESUtil.class);
	private static Set<String> sourceHeadersSet = new HashSet<>(Arrays.asList(Constants.POD_ID, Constants.CONTAINER_ID,
			Constants.MICROSERVICE, Constants.NW_FN, Constants.NW_FN_PREFIX, Constants.NW_FN_TYPE));
	private static Set<String> commoneEventHeaderSet = new HashSet<>(
			Arrays.asList(Constants.LOCAL_EVT_NAME, Constants.EVENT_ID));
	private static Map<String, Object> clearStatusMap = new HashMap<>();
	private static Map<String, Object> clearResponseMap = new HashMap<>();

	@Autowired
	ElasticSearchClientConfiguration esClient;

	@Autowired
	NBInterface nbi;

	@Autowired
	JsonUtil jsonUtil;

	@Autowired
	DateUtil dateUtil;

	@Autowired
	DbAdaptor dbAdaptor;

	@Autowired
	ZKUtil zkUtil;

	public void processMessage(String message) {

		Map<String, Object> document = jsonUtil.getDocument(message);
		if (document == null) {
			LOG.error("Returned document was null; unable to parse message");
			return;
		}

		String eventDate = dateUtil.getEventDate(message);
		handleDoc(document, eventDate);
	}

	@SuppressWarnings("unchecked")
	public void handleDoc(Map<String, Object> document, String date) {

		LOG.debug("Handling Event with date: " + date);
		Map<String, Object> comEvtHdrMap = (Map<String, Object>) document.get(Constants.COMMON_EVT_HDR);
		String sourceId = (String) comEvtHdrMap.get(Constants.SOURCE_ID);
		String localEventName = (String) comEvtHdrMap.get(Constants.LOCAL_EVT_NAME);
		long ts = (long) comEvtHdrMap.get(Constants.START_EPOCH);
		Map<String, Object> tcaFieldsMap = (Map<String, Object>) document.get(Constants.TCA_FIELDS);
		Map<String, Object> faultFieldsMap = (Map<String, Object>) document.get(Constants.FAULT_FIELDS);
		Map<String, Object> notificationFieldsMap = (Map<String, Object>) document.get(Constants.NOTIFICATION_FIELDS);
		Map<String, Object> sourceHeaders = (Map<String, Object>) document.get(Constants.SRC_HEADERS);
		EventSeverity eventSeverity = EventSeverity.INFO;
		if ((comEvtHdrMap.get(Constants.DOMAIN)).toString().equalsIgnoreCase((Domain.FAULT).toString())) {
			eventSeverity = EventSeverity.fromValue((String) faultFieldsMap.get(Constants.EVENT_SEVERITY));
		} else if ((comEvtHdrMap.get(Constants.DOMAIN)).toString()
				.equalsIgnoreCase(Domain.THRESHOLD_CROSSING_ALERT.toString())) {
			eventSeverity = EventSeverity.fromValue((String) tcaFieldsMap.get(Constants.EVENT_SEVERITY));
		}
		byte severityCode = getSeverityCode(eventSeverity);
		LOG.debug("Severity code for Event: " + localEventName + " is: " + severityCode);

		Map<String, Object> evtDefMap = (Map<String, Object>) document.get(Constants.EVENT_DEF);
		evtDefMap.put(Constants.SEVERITY_CODE, severityCode);

		String eventId = jsonUtil.getEventId(sourceId, localEventName);
		comEvtHdrMap.put(Constants.EVENT_ID, eventId);
		if (eventSeverity.equals(EventSeverity.INFO)
				|| comEvtHdrMap.get(Constants.DOMAIN).toString().equalsIgnoreCase((Domain.NOTIFICATION).toString())) {
			comEvtHdrMap.put(Constants.DOMAIN, Domain.NOTIFICATION);
			faultFieldsMap = new HashMap<String, Object>();
			tcaFieldsMap = new HashMap<String, Object>();
			getNotificationFields(notificationFieldsMap, comEvtHdrMap);
		}
		if ((comEvtHdrMap.get(Constants.DOMAIN)).toString().equalsIgnoreCase((Domain.FAULT).toString())) {
			notificationFieldsMap = new HashMap<String, Object>();
			tcaFieldsMap = new HashMap<String, Object>();
		} else if ((comEvtHdrMap.get(Constants.DOMAIN)).toString()
				.equalsIgnoreCase((Domain.THRESHOLD_CROSSING_ALERT).toString())) {
			notificationFieldsMap = new HashMap<String, Object>();
			faultFieldsMap = new HashMap<String, Object>();
		}

		document.put(Constants.COMMON_EVT_HDR, comEvtHdrMap);
		document.put(Constants.FAULT_FIELDS, faultFieldsMap);
		document.put(Constants.TCA_FIELDS, tcaFieldsMap);
		document.put(Constants.NOTIFICATION_FIELDS, notificationFieldsMap);
		document.put(Constants.EVENT_DEF, evtDefMap);
		Map<String, Object> ackstate = new HashMap<String, Object>();
		Map<String, Object> comment = new HashMap<String, Object>();
		if (!document.containsKey(Constants.ACK_STATE))
			document.put(Constants.ACK_STATE, ackstate);
		if (!document.containsKey(Constants.COMMENT))
			document.put(Constants.COMMENT, comment);
		// EVENT - document ID is unique
		updateIndex(eventId, Constants.EVENT_PREFIX + date, document);
		if (severityCode <= 4) // ALARM, document ID is same as event ID
			updateIndex(eventId, Constants.ALARM_PREFIX + date, document);

		if (severityCode == 6) {
			// CLEAR event
			clearAlarm(document);
		}
	}

	public void manualClear(Map<String, Object> document) {

		LOG.info("Manually Clearing Alarm related to CLEAR event: " + document.toString());
		Map<String, Object> comEvtHdrMap = (Map<String, Object>) document.get(Constants.COMMON_EVT_HDR);
		String sourceId = (String) comEvtHdrMap.get(Constants.SOURCE_ID);
		Map<String, Object> evtDefMap = (Map<String, Object>) document.get(Constants.EVENT_DEF);
		String corrNotifNode = (String) evtDefMap.get(Constants.CORR_NOTIF);
		String localEventName = (String) comEvtHdrMap.get(Constants.LOCAL_EVT_NAME);
		localEventName = (String) comEvtHdrMap.get(Constants.LOCAL_EVT_NAME);
		long ts = Long.valueOf(comEvtHdrMap.get(Constants.START_EPOCH).toString());
		String eventId = jsonUtil.getEventId(sourceId, localEventName);
		LOG.info("Deleting alarm: " + localEventName);
		deleteAlarm(eventId);
		LOG.info("Clear Event : " + localEventName + " raised");
		String correlationId = eventId;
		AlarmAdditionalInformation alarmAdditionalInfoList = new AlarmAdditionalInformation();
		comEvtHdrMap.put(Constants.EVENT_ID, getClearEventId(sourceId, localEventName));
		if ((comEvtHdrMap.get(Constants.DOMAIN)).toString().equalsIgnoreCase((Domain.FAULT).toString())) {
			Map<String, Object> faultFields = (Map<String, Object>) document.get(Constants.FAULT_FIELDS);
			if (DbAdaptor.storageEngine.equalsIgnoreCase("elasticsearch")) {
				Map<String, String> additionalInfo = (Map<String, String>) faultFields.get(Constants.ALARM_ADD_INFO);
				extractAdditionalFields(alarmAdditionalInfoList, additionalInfo);
			} else
				alarmAdditionalInfoList = (AlarmAdditionalInformation) faultFields.get(Constants.ALARM_ADD_INFO);
			alarmAdditionalInfoList.setAdditionalProperty(Constants.CORRELATION_ID, correlationId);
			faultFields.put(Constants.ALARM_ADD_INFO, alarmAdditionalInfoList);
		} else if ((comEvtHdrMap.get(Constants.DOMAIN)).toString()
				.equalsIgnoreCase((Domain.THRESHOLD_CROSSING_ALERT).toString())) {
			Map<String, Object> tcaFields = (Map<String, Object>) document.get(Constants.TCA_FIELDS);
			if (DbAdaptor.storageEngine.equalsIgnoreCase("elasticsearch")) {
				Map<String, String> additionalInfo = (Map<String, String>) tcaFields.get(Constants.ADD_FIELDS);
				extractAdditionalFields(alarmAdditionalInfoList, additionalInfo);
			} else
				alarmAdditionalInfoList = (AlarmAdditionalInformation) tcaFields.get(Constants.ADD_FIELDS);
			alarmAdditionalInfoList.setAdditionalProperty(Constants.CORRELATION_ID, correlationId);
			tcaFields.put(Constants.ADD_FIELDS, alarmAdditionalInfoList);
		}
		updateIndex(eventId, Constants.EVENT_PREFIX + dateUtil.getCurrentDate(), document);
		if (!corrNotifNode.isEmpty()) {
			List<String> corrNotifList = new ArrayList<String>(Arrays.asList(corrNotifNode.split(",")));
			corrNotifList.forEach(corrNotif -> {
				LOG.info("Correlated Event : " + corrNotif + " raised");
				comEvtHdrMap.put(Constants.LOCAL_EVT_NAME, corrNotif);
				document.put(Constants.COMMON_EVT_HDR, comEvtHdrMap);
				String corrEventId = jsonUtil.getEventId(sourceId, corrNotif);
				updateIndex(corrEventId, Constants.EVENT_PREFIX + dateUtil.getCurrentDate(), document);
			});

		}
	}

	private void extractAdditionalFields(AlarmAdditionalInformation alarmAdditionalInfoList,
			Map<String, String> additionalInfo) {
		Set<String> keys = additionalInfo.keySet();
		for (String s : keys) {
			alarmAdditionalInfoList.setAdditionalProperty(s, additionalInfo.get(s));
		}

	}

	private String getClearEventId(String sourceId, String localEventName) {
		return DigestUtils.md5Hex(localEventName + sourceId + "CLEAR");
	}

	private void getNotificationFields(Map<String, Object> notificationFieldsMap, Map<String, Object> comEvtHdrMap) {
		if (!notificationFieldsMap.containsKey(Constants.CHANGE_IDENTIFIER))
			notificationFieldsMap.put(Constants.CHANGE_IDENTIFIER, comEvtHdrMap.get(Constants.SOURCE_ID));
		if (!notificationFieldsMap.containsKey(Constants.CHANGE_TYPE))
			notificationFieldsMap.put(Constants.CHANGE_TYPE, comEvtHdrMap.get(Constants.LOCAL_EVT_NAME));
		notificationFieldsMap.put(Constants.NOTI_FIELDS_VERSION, NotificationFields.NotificationFieldsVersion._2_0);

	}

	public String getDocumentId(String eventId, int seq) {
		return DigestUtils.md5Hex(eventId + seq);
	}

	public void updateIndex(String id, String index, Map<String, Object> document) {
		if (index.startsWith(Constants.EVENT_PREFIX)) { // EVENT
			LOG.info("EVENT received !");
			indexDocument(id, index, document);

		} else { // ALARM

			LOG.info("ALARM received !");
			if (DbAdaptor.storageEngine.equalsIgnoreCase("elasticsearch")) {
				String existingIndex = Constants.EMPTY_STRING;
				existingIndex = checkDocExists(id);
				if (!existingIndex.equals(Constants.EMPTY_STRING)) {
					indexDocument(id, existingIndex, document);
				} else {
					LOG.info("Document with _id: " + id + " not found in any existing index");
					indexDocument(id, index, document);
				}
			}

			else {
				dbAdaptor.removeAlarmFromZk(id);
				dbAdaptor.addAlarmToZK(id, document);
			}

		}
	}

	@SuppressWarnings("unchecked")
	public void indexDocument(String id, String index, Map<String, Object> document) {
		ObjectMapper mapper = jsonUtil.getMapper();
		// to avoid sending VES twice for an alarm event; once while indexing in events
		// and once while indexing in alarms
		int seq = 0;
		if (index.startsWith(Constants.EVENT_PREFIX)) {
			Map<String, Object> commonEventHeaderMap = (Map<String, Object>) document.get(Constants.COMMON_EVT_HDR);
			commonEventHeaderMap.put(Constants.SEQUENCE, 0);
			if (!commonEventHeaderMap.containsKey((Constants.NF_NAMING_CODE)))
				commonEventHeaderMap.put(Constants.NF_NAMING_CODE, Constants.EMPTY_STRING);
			if (!commonEventHeaderMap.containsKey((Constants.NFC_NAMING_CODE)))
				commonEventHeaderMap.put(Constants.NFC_NAMING_CODE, Constants.EMPTY_STRING);
			if ((commonEventHeaderMap.get(Constants.DOMAIN)).toString().equalsIgnoreCase((Domain.FAULT).toString())) {
				getEventName("Fault", commonEventHeaderMap);
			} else if ((commonEventHeaderMap.get(Constants.DOMAIN)).toString()
					.equalsIgnoreCase(Domain.THRESHOLD_CROSSING_ALERT.toString())) {
				getEventName("Tca", commonEventHeaderMap);
				Map<String, Object> eventDefinitionMap = (Map<String, Object>) document.get(Constants.EVENT_DEF);
				Map<String, Object> tcaFields = (Map<String, Object>) document.get(Constants.TCA_FIELDS);
				AlarmAdditionalInformation alarmAdditionalInfoList = (AlarmAdditionalInformation) tcaFields
						.get(Constants.ADD_FIELDS);
				if (!checkIfCorrelationIdPresent(alarmAdditionalInfoList)) {
					if ((byte) eventDefinitionMap.get(Constants.SEVERITY_CODE) == 6) {
						String sourceId = (String) commonEventHeaderMap.get(Constants.SOURCE_ID);
						String correlationId = (String) commonEventHeaderMap.get(Constants.EVENT_ID);
						commonEventHeaderMap.put(Constants.EVENT_ID,
								getClearEventId(sourceId, (String) commonEventHeaderMap.get(Constants.EVENT_NAME)));
						alarmAdditionalInfoList.setAdditionalProperty(Constants.CORRELATION_ID, correlationId);
						tcaFields.put(Constants.ADD_FIELDS, alarmAdditionalInfoList);
					}
				}

			} else if ((commonEventHeaderMap.get(Constants.DOMAIN)).toString()
					.equalsIgnoreCase(Domain.NOTIFICATION.toString())) {
				getEventName("Notification", commonEventHeaderMap);
			}
			document.put(Constants.COMMON_EVT_HDR, commonEventHeaderMap);
			seq = (int) commonEventHeaderMap.get(Constants.SEQUENCE);
			// seq = nbi.createMEFObject(document);
			if (seq < 0)
				id = getDocumentId(id, seq);
		}
		String source = Constants.EMPTY_STRING;
		try {
			source = mapper.writeValueAsString(document);
		} catch (JsonProcessingException e) {
			LOG.error(e.getMessage());
		}

		if (DbAdaptor.storageEngine.equalsIgnoreCase("elasticsearch")) {
			LOG.info("Pushing document to index: " + index + ", \n" + source);
			dbAdaptor.addDataToElasticSearch(index, source, id);
		}
		if (index.startsWith(Constants.EVENT_PREFIX)) {
			nbi.createMEFObject(document);
		}
	}

	private boolean checkIfCorrelationIdPresent(AlarmAdditionalInformation alarmAdditionalInfoList) {
		Map<String, String> alarmAdditionInfoMap = alarmAdditionalInfoList.getAdditionalProperties();
		if (alarmAdditionInfoMap.containsKey(Constants.CORRELATION_ID))
			return true;
		return false;
	}

	private void getEventName(String domain, Map<String, Object> commonEventHeaderMap) {
		if (commonEventHeaderMap.get(Constants.EVENT_NAME).toString().isEmpty()) {
			commonEventHeaderMap.put(Constants.EVENT_NAME,
					domain + "_" + commonEventHeaderMap.get(Constants.LOCAL_EVT_NAME));
		} else {
			if (!commonEventHeaderMap.get(Constants.EVENT_NAME).toString().startsWith(domain + "_"))
				commonEventHeaderMap.put(Constants.EVENT_NAME,
						domain + "_" + commonEventHeaderMap.get(Constants.EVENT_NAME));
		}

	}

	@SuppressWarnings("unchecked")
	public void clearAlarm(Map<String, Object> document) {
		LOG.info("Clearing Alarm related to CLEAR event: " + document.toString());

		Map<String, Object> comEvtHdrMap = (Map<String, Object>) document.get(Constants.COMMON_EVT_HDR);
		String sourceId = (String) comEvtHdrMap.get(Constants.SOURCE_ID);
		Map<String, Object> evtDefMap = (Map<String, Object>) document.get(Constants.EVENT_DEF);
		String corrNotifNode = (String) evtDefMap.get(Constants.CORR_NOTIF);

		if (corrNotifNode.equalsIgnoreCase("all")) {
			String podId = (String) comEvtHdrMap.get(Constants.POD_ID);
			deleteAlarmWithParam(Constants.POD_ID, podId, Constants.EMPTY_STRING);
		} else {
			if (corrNotifNode.isEmpty()) {
				corrNotifNode = (String) comEvtHdrMap.get(Constants.LOCAL_EVT_NAME);
			}
			List<String> corrNotifList = new ArrayList<String>(Arrays.asList(corrNotifNode.split(",")));
			corrNotifList.forEach(corrNotif -> {
				String perceivedSeverity = K8sUtil.eventNameToSeverity.get(corrNotif);

				LOG.info("Correlated Event Name: " + corrNotif + ", Perceived severity: " + perceivedSeverity);

				String corrEventId = jsonUtil.getEventId(sourceId, corrNotif);
				deleteAlarm(corrEventId);
			});
		}
	}

	private Map<String, String> fillAlarmCountMap(Map<String, String> severityToCount, String total) {

		severityToCount.putIfAbsent(EventSeverity.CLEAR.toString(), "0");
		severityToCount.putIfAbsent(EventSeverity.CRITICAL.toString(), "0");
		severityToCount.putIfAbsent(EventSeverity.INFO.toString(), "0");
		severityToCount.putIfAbsent(EventSeverity.MAJOR.toString(), "0");
		severityToCount.putIfAbsent(EventSeverity.MINOR.toString(), "0");
		severityToCount.putIfAbsent(EventSeverity.WARNING.toString(), "0");
		severityToCount.put("TOTAL", total);

		return severityToCount;
	}

	private String getClearResponse(String clearKey, String clearValues) throws JsonProcessingException {
		ObjectMapper mapper = jsonUtil.getMapper();

		clearResponseMap.put(Constants.CLEAR_KEY, clearKey);
		clearResponseMap.put(Constants.CLEAR_VALUES, clearValues);
		clearResponseMap.put(Constants.CLEAR_STATUS, mapper.writeValueAsString(clearStatusMap));
		clearResponseMap.put(Constants.TIMESTAMP, dateUtil.getCurrentDate());

		return mapper.writeValueAsString(clearResponseMap);
	}

	public String clearHandler(String requestBody) throws JsonMappingException, JsonProcessingException {

		ObjectMapper mapper = jsonUtil.getMapper();
		JsonNode clearRequest = mapper.readTree(requestBody);

		if (clearRequest != null) {

			if (clearRequest.has(Constants.CLEAR_KEY) && clearRequest.has(Constants.CLEAR_VALUES)) {
				String clearKey = clearRequest.get(Constants.CLEAR_KEY).asText();
				String[] clearValues = clearRequest.get(Constants.CLEAR_VALUES).asText().split(",");

				String clearComment = Constants.EMPTY_STRING;
				if (clearRequest.has(Constants.CLEAR_COMMENT))
					clearComment = clearRequest.get(Constants.CLEAR_COMMENT).asText();
				for (String clearValue : clearValues)
					deleteAlarmWithParam(clearKey, clearValue, clearComment);

				return getClearResponse(clearKey, clearRequest.get(Constants.CLEAR_VALUES).asText());
			} else
				return "Request body is missing a mandatory key- either 'clearKey' or 'clearValues'";
		}
		return "Request is not a valid JSON";
	}

	public void deleteAlarmWithParam(String deleteKey, String deleteValue, String clearComment) {

		RestHighLevelClient client = esClient.getConnection();

		if (sourceHeadersSet.contains(deleteKey))
			deleteKey = Constants.SRC_HEADERS + "." + deleteKey;
		else if (commoneEventHeaderSet.contains(deleteKey))
			deleteKey = Constants.COMMON_EVT_HDR + "." + deleteKey;

		LOG.info("Looking up Alarms with " + deleteKey + ": " + deleteValue);

		final Scroll scroll = new Scroll(TimeValue.timeValueMinutes(1L));
		SearchRequest searchRequest = new SearchRequest("alarms*");
		searchRequest.scroll(scroll).indicesOptions(IndicesOptions.LENIENT_EXPAND_OPEN);
		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
		searchSourceBuilder.query(QueryBuilders.matchQuery(deleteKey, deleteValue)).size(50);
		searchRequest.source(searchSourceBuilder);

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
				LOG.debug("Error occured while executing query on Elastic Search, retrying..... count:" + countRetry,
						e);
			}
		}
		String scrollId = searchResponse.getScrollId();
		SearchHit[] searchHits = searchResponse.getHits().getHits();

		if (searchHits.length == 0)
			clearStatusMap.put(deleteValue, "Failed to find alarms matching the given key-value pair");

		while (searchHits != null && searchHits.length > 0) {
			LOG.info(searchHits.length + " alarms found");

			for (int i = 0; i < searchHits.length; i++) {
				deleteAlarm(searchHits[i].getId(), searchHits[i].getIndex());
				raiseClearEvent(client, searchHits[i], clearComment);

				Map<String, String> tempMap = new HashMap<>();

				Map<String, Object> searchHitMap = searchHits[i].getSourceAsMap();
				Map<String, Object> comEvtHdrMap = (Map<String, Object>) searchHitMap.get(Constants.COMMON_EVT_HDR);
				String eventId = (String) comEvtHdrMap.get(Constants.EVENT_ID);

				tempMap.put(Constants.EVENT_ID, eventId);
				tempMap.put("_id", searchHits[i].getId());
				tempMap.put("_index", searchHits[i].getIndex());

				ArrayList<Object> hitsList = null;
				if (clearStatusMap.containsKey(deleteValue))
					hitsList = (ArrayList<Object>) clearStatusMap.get(deleteValue);
				else
					hitsList = new ArrayList<>();

				hitsList.add(tempMap);
				clearStatusMap.put(deleteValue, hitsList);
			}

			SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
			scrollRequest.scroll(scroll);
			try {
				searchResponse = client.scroll(scrollRequest, RequestOptions.DEFAULT);
			} catch (IOException e) {
				LOG.error(e.getMessage(), e);
			}
			scrollId = searchResponse.getScrollId();
			searchHits = searchResponse.getHits().getHits();
		}

		ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
		clearScrollRequest.addScrollId(scrollId);
		ClearScrollResponse clearScrollResponse = null;
		try {
			clearScrollResponse = client.clearScroll(clearScrollRequest, RequestOptions.DEFAULT);
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		}
		boolean succeeded = clearScrollResponse.isSucceeded();
		LOG.debug("Clear Scroll Response: " + succeeded);
	}

	@SuppressWarnings("unchecked")
	private void raiseClearEvent(RestHighLevelClient client, SearchHit searchHit, String clearComment) {

		LOG.info(
				"Raising Clear Event for manual clear request - updating: Severity, SeverityCode, EventId, ClearComment, Reporting Fields and Timestamp.");

		Map<String, Object> sourceAsMap = searchHit.getSourceAsMap();

		// Set Clear Comment
		Map<String, Object> evtComMap = (Map<String, Object>) sourceAsMap.get(Constants.EVT_COMMON);
		evtComMap.put(Constants.CLEAR_COMMENT, clearComment);

		// Severity = CLEAR
		Map<String, Object> faultFieldsMap = (Map<String, Object>) sourceAsMap.get(Constants.FAULT_FIELDS);
		faultFieldsMap.put(Constants.EVENT_SEVERITY, EventSeverity.CLEAR.toString());

		// Severity code = 6
		Map<String, Object> evtDefMap = (Map<String, Object>) sourceAsMap.get(Constants.EVENT_DEF);
		evtDefMap.put(Constants.SEVERITY_CODE, 6);

		// String ackstate = (String) sourceAsMap.get(Constants.ACK_STATE);

		// Construct new EventId
		Map<String, Object> comEvtHdrMap = (Map<String, Object>) sourceAsMap.get(Constants.COMMON_EVT_HDR);
		String sourceId = (String) comEvtHdrMap.get(Constants.SOURCE_ID);
		String localEventName = (String) comEvtHdrMap.get(Constants.LOCAL_EVT_NAME);

		String eventId = jsonUtil.getEventId(sourceId, localEventName);
		comEvtHdrMap.put(Constants.EVENT_ID, eventId);

		// CLEAR event's timestamp
		long epochMillis = System.currentTimeMillis();
		comEvtHdrMap.put(Constants.START_EPOCH, epochMillis);
		comEvtHdrMap.put(Constants.LAST_EPOCH, epochMillis);

		comEvtHdrMap = jsonUtil.fillReportingFields(comEvtHdrMap);

		sourceAsMap.put(Constants.EVT_COMMON, evtComMap);
		sourceAsMap.put(Constants.COMMON_EVT_HDR, comEvtHdrMap);
		sourceAsMap.put(Constants.EVENT_DEF, evtDefMap);
		sourceAsMap.put(Constants.FAULT_FIELDS, faultFieldsMap);

		LOG.info("Clear Event has eventId: " + eventId + ", index: events-" + dateUtil.getCurrentDate());
		indexDocument(eventId, Constants.EVENT_PREFIX + dateUtil.getCurrentDate(), sourceAsMap);
	}

	public void deleteAlarmWithPodId(String podId) {

		RestHighLevelClient client = esClient.getConnection();
		LOG.info("Looking up Alarms with podId: " + podId);

		final Scroll scroll = new Scroll(TimeValue.timeValueMinutes(1L));
		SearchRequest searchRequest = new SearchRequest();
		searchRequest.scroll(scroll);
		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
		searchSourceBuilder.query(QueryBuilders.matchQuery(Constants.POD_ID, podId));
		searchRequest.source(searchSourceBuilder);

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
				LOG.debug("Error occured while executing query on Elastic Search, retrying..... count:" + countRetry,
						e);
			}
		}
		String scrollId = searchResponse.getScrollId();
		SearchHit[] searchHits = searchResponse.getHits().getHits();

		while (searchHits != null && searchHits.length > 0) {

			for (int i = 0; i < searchHits.length; i++) {
				deleteAlarm(searchHits[i].getId(), searchHits[i].getIndex());
			}

			SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
			scrollRequest.scroll(scroll);
			countRetry = 0;
			while (true) {
				try {
					searchResponse = client.scroll(scrollRequest, RequestOptions.DEFAULT);
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
			scrollId = searchResponse.getScrollId();
			searchHits = searchResponse.getHits().getHits();
		}

		ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
		clearScrollRequest.addScrollId(scrollId);
		ClearScrollResponse clearScrollResponse = null;
		countRetry = 0;
		while (true) {
			try {
				clearScrollResponse = client.clearScroll(clearScrollRequest, RequestOptions.DEFAULT);
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
		boolean succeeded = clearScrollResponse.isSucceeded();
		LOG.debug("Clear Scroll Response: " + succeeded);
	}

	public void deleteAlarm(String id) {
		if (DbAdaptor.storageEngine.equalsIgnoreCase("elasticsearch")) {
			LOG.info("Looking up Alarm with _id: " + id);
			RestHighLevelClient client = esClient.getConnection();

			SearchRequest searchRequest = new SearchRequest();
			SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
			searchSourceBuilder.fetchSource(false);
			searchSourceBuilder.terminateAfter(1);
			searchSourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));
			searchSourceBuilder.query(QueryBuilders.idsQuery(Constants.EVENT_MAPPING_TYPE).addIds(id));
			searchRequest.source(searchSourceBuilder);
			int countRetry = 0;
			while (true) {
				try {
					String index = Constants.EMPTY_STRING;
					SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
					SearchHits hits = searchResponse.getHits();
					LOG.info("Total hits for _id: " + id + " is: " + hits.getTotalHits());

					SearchHit[] searchHits = hits.getHits();

					for (SearchHit hit : searchHits) {
						index = hit.getIndex();
						if (index.startsWith(Constants.ALARM_PREFIX)) {
							deleteAlarm(id, index);
							break;
						}
					}
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
		} else {
			zkUtil.deleteAlarmZK(id);
		}
	}

	public void deleteAlarm(String id, String index) {
		LOG.info("Deleting Alarm with _id: " + id + " from index: " + index);
		RestHighLevelClient client = esClient.getConnection();

		DeleteRequest deleteRequest = new DeleteRequest(index, Constants.EVENT_MAPPING_TYPE, id);
		int countRetry = 0;
		while (true) {
			try {
				DeleteResponse delResponse = client.delete(deleteRequest, RequestOptions.DEFAULT);
				LOG.debug("Delete response: " + delResponse.toString());
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

	private Map<String, Object> getExistingDoc(RestHighLevelClient client, String index, String id) {
		GetRequest getRequest = new GetRequest(index, Constants.EVENT_MAPPING_TYPE, id);
		try {
			GetResponse getResponse = client.get(getRequest, RequestOptions.DEFAULT);
			if (getResponse.isExists()) {
				LOG.info("Document found: " + getResponse.toString());
				return getResponse.getSourceAsMap();
			}
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		}
		return null;
	}

	private String checkDocExists(String id) {
		RestHighLevelClient client = esClient.getConnection();
		LOG.info("Searching for document _id: " + id);
		SearchRequest searchRequest = new SearchRequest();
		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
		searchSourceBuilder.fetchSource(false);
		searchSourceBuilder.query(QueryBuilders.idsQuery().types(Constants.EVENT_MAPPING_TYPE).addIds(id));
		searchSourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));
		searchRequest.source(searchSourceBuilder);
		try {
			SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
			SearchHits hits = searchResponse.getHits();
			LOG.info("Total hits for _id: " + id + " is: " + hits.getTotalHits());

			SearchHit[] searchHits = hits.getHits();

			for (SearchHit hit : searchHits) {
				String index = hit.getIndex();
				if (index.startsWith(Constants.ALARM_PREFIX)) {
					return index;
				}
			}
		} catch (IOException e) {
			LOG.error(e.getMessage());
		}
		return Constants.EMPTY_STRING;
	}

	private byte getSeverityCode(EventSeverity severity) {
		LOG.info(severity);
		byte severityCode = 0;
		switch (severity) {
		case CRITICAL:
			severityCode = 1;
			break;
		case MAJOR:
			severityCode = 2;
			break;
		case MINOR:
			severityCode = 3;
			break;
		case WARNING:
			severityCode = 4;
			break;
		case CLEAR:
			severityCode = 6;
			break;
		case INFO:
			severityCode = 5;
			break;
		case NORMAL:
			severityCode = 5;
			break;

		}
		return severityCode;
	}
}
