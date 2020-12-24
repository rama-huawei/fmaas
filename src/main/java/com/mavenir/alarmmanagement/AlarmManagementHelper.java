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

package org.xgvela.alarmmanagement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.xgvela.cnf.Constants;
import org.xgvela.cnf.elastic.ElasticSearchClientConfiguration;
import org.xgvela.cnf.kafka.EventListener;
import org.xgvela.cnf.kafka.KafkaConfiguration;
import org.xgvela.cnf.kafka.KafkaPush;
import org.xgvela.cnf.kafka.KafkaTopic;
import org.xgvela.cnf.util.DateUtil;
import org.xgvela.cnf.util.ESUtil;
import org.xgvela.cnf.util.JsonUtil;
import org.xgvela.cnf.util.NBInterface;
import org.xgvela.dbadapter.DbAdaptor;
import org.xgvela.dbadapter.zk.ZKUtil;
import org.xgvela.model.CommonEventHeader;
import org.xgvela.model.Event;
import org.xgvela.model.FaultFields;
import org.xgvela.model.MEF;
import org.xgvela.model.NotificationFields;
import org.xgvela.model.FaultFields.EventSeverity;
import org.xgvela.model.InitReplayRequest;
import org.xgvela.model.ThresholdCrossingAlertFields;
import org.xgvela.model.CommonEventHeader.Domain;

@Component
public class AlarmManagementHelper {
	@Autowired
	JsonUtil jsonUtil;
	@Autowired
	ElasticSearchClientConfiguration esClient;
	@Autowired
	ESUtil esUtil;
	@Autowired
	DateUtil dateUtil;
	@Autowired
	NBInterface nbiInterface;
	@Autowired
	EventListener eventListener;
	@Autowired
	KafkaTopic kafkaTopic;
	@Autowired
	RestTemplate restTemplate;
	@Autowired
	KafkaPush kafkaPush;
	@Autowired
	ZKUtil zkUtil;

	private static Logger LOG = LogManager.getLogger(AlarmManagementHelper.class);
	public static Boolean replayOngoing = false;

	public String getAlarmCount(String id) {
		Map<String, String> severityCodeToCount = new HashMap<String, String>(6);
		Map<String, String> severityToCount = new HashMap<String, String>(6);
		int total = 0;
		if (DbAdaptor.storageEngine.equalsIgnoreCase("elasticsearch")) {
			RestHighLevelClient client = esClient.getConnection();
			SearchRequest searchRequest = new SearchRequest("alarms*");
			searchRequest.indicesOptions(IndicesOptions.LENIENT_EXPAND_OPEN);
			SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
			searchSourceBuilder = queryBuilder(id, searchSourceBuilder);
			AggregationBuilder aggregation = AggregationBuilders.terms("severityCode")
					.field("eventDefinition.severityCode");
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
			LOG.debug("Search Response: " + searchResponse.toString());
			Aggregations aggregations = searchResponse.getAggregations();
			Terms severityCode = aggregations.get("severityCode");
			List<? extends Bucket> Buckets = severityCode.getBuckets();
			for (Terms.Bucket bucket : Buckets) {
				LOG.debug(bucket.getKey() + " : " + bucket.getDocCount());
				severityCodeToCount.put(bucket.getKeyAsString(), String.valueOf(bucket.getDocCount()));
				total += bucket.getDocCount();
			}
		} else {
			severityCodeToCount = zkUtil.getSeverityCodeToCount(id);
			Set<String> keys = severityCodeToCount.keySet();
			for (String s : keys) {
				total = total + Integer.valueOf(severityCodeToCount.get(s));
			}
		}
		severityToCount = severityCodeToSeverity(severityToCount, severityCodeToCount, total);
		ObjectMapper mapper = jsonUtil.getMapper();
		try {
			mapper.enable(SerializationFeature.INDENT_OUTPUT);
			String count = mapper.writeValueAsString(severityToCount);
			mapper.disable(SerializationFeature.INDENT_OUTPUT);
			LOG.debug("response for getAlarmCount request: " + count);
			return count;
		} catch (JsonProcessingException e) {
			LOG.error("Unable to convert Map to JSON");
		}

		return "Unable to convert Map to JSON";
	}

	private Map<String, String> severityCodeToSeverity(Map<String, String> severityToCount,
			Map<String, String> severityCodeToCount, int total) {
		severityToCount.put(EventSeverity.CRITICAL.toString(), severityCodeToCount.getOrDefault("1", "0"));
		severityToCount.put(EventSeverity.MAJOR.toString(), severityCodeToCount.getOrDefault("2", "0"));
		severityToCount.put(EventSeverity.MINOR.toString(), severityCodeToCount.getOrDefault("3", "0"));
		severityToCount.put(EventSeverity.WARNING.toString(), severityCodeToCount.getOrDefault("4", "0"));
		severityToCount.put(EventSeverity.INFO.toString(), severityCodeToCount.getOrDefault("5", "0"));
		severityToCount.put(EventSeverity.CLEAR.toString(), severityCodeToCount.getOrDefault("6", "0"));
		severityToCount.put("TOTAL", String.valueOf(total));
		return severityToCount;
	}

	public String getAlarms(String id) {
		ArrayList<String> stresults = new ArrayList<String>();
		if (DbAdaptor.storageEngine.equalsIgnoreCase("elasticsearch")) {
			RestHighLevelClient client = esClient.getConnection();
			SearchRequest searchRequest = new SearchRequest("alarms*");
			searchRequest.indicesOptions(IndicesOptions.LENIENT_EXPAND_OPEN);
			SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
			searchSourceBuilder = queryBuilder(id, searchSourceBuilder);
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
			LOG.debug("Search Response: " + searchResponse.toString());
			SearchHit[] results = searchResponse.getHits().getHits();
			for (SearchHit hit : results) {
				ObjectMapper mapper = new ObjectMapper();
				JsonNode actualObj = null;
				try {
					actualObj = mapper.readTree(hit.getSourceAsString().toString());
				} catch (JsonMappingException e) {
					LOG.error("Error in parsing search response: " + e);
				} catch (JsonProcessingException e) {
					LOG.error("Error in parsing search response: " + e);
				}
				stresults = getVesFormat(actualObj, stresults, false);

			}
		} else {
			List<String> alarmId = new ArrayList<>();
			alarmId = zkUtil.getAlarmIdList();
			ObjectMapper mapper = jsonUtil.getMapper();
			for (String s : alarmId) {
				boolean ack = zkUtil.checkIfAck(s);
				if (id.equals(Constants.GET_ALARMS_ENUM.allActiveAndUnacknowledgedAlarms.toString())) {
					if (ack)
						continue;
				} else if (id.equals(Constants.GET_ALARMS_ENUM.allActiveAndAcknowledgedAlarms.toString())) {
					if (!ack)
						continue;
				}
				Map<String, Object> getExistingDoc = new HashMap<>();
				Map<String, Object> comEvtHdrMap = zkUtil.getCommonEventHeaderZK(s);
				getExistingDoc.put(Constants.COMMON_EVT_HDR, comEvtHdrMap);
				Map<String, Object> tcaFieldsMap = new HashMap<>();
				Map<String, Object> faultFieldsMap = new HashMap<>();
				if ((comEvtHdrMap.get(Constants.DOMAIN)).toString().equalsIgnoreCase((Domain.FAULT).toString())) {
					faultFieldsMap = zkUtil.getFaultFieldsZK(s);
					getExistingDoc.put(Constants.FAULT_FIELDS, faultFieldsMap);
				} else if ((comEvtHdrMap.get(Constants.DOMAIN)).toString()
						.equalsIgnoreCase(Domain.THRESHOLD_CROSSING_ALERT.toString())) {
					tcaFieldsMap = zkUtil.getTCAFieldsZK(s);
					getExistingDoc.put(Constants.TCA_FIELDS, tcaFieldsMap);
				}
				Map<String, Object> event = new HashMap<>();
				event.put("event", getExistingDoc);
				try {
					stresults.add(mapper.writeValueAsString(event));
				} catch (JsonProcessingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		Map<String, String> errorMap = new HashMap<String, String>();
		Map<String, Map<String, String>> errorResponse = new HashMap<String, Map<String, String>>();
		ObjectMapper mapper = jsonUtil.getMapper();
		if (!stresults.isEmpty()) {
			try {
				mapper.enable(SerializationFeature.INDENT_OUTPUT);
				String strResults = mapper.writeValueAsString(stresults);
				mapper.disable(SerializationFeature.INDENT_OUTPUT);
				LOG.debug("response for getAlarms request: " + strResults.replaceAll("\\\\", ""));
				return strResults.replaceAll("\\\\", "");
			} catch (JsonProcessingException e) {
				LOG.error("Unable to convert ArrayList to JSON" + e);
			}
		} else {
			LOG.error("No alarm found with Id " + id);
			errorMap.put("errorReason", "no alarms present for this get request");
			errorResponse.put("error", errorMap);
			try {
				mapper.enable(SerializationFeature.INDENT_OUTPUT);
				String strResults = mapper.writeValueAsString(errorResponse);
				mapper.disable(SerializationFeature.INDENT_OUTPUT);
				LOG.debug("response for getAlarms request: " + strResults.replaceAll("\\\\", ""));
				return strResults.replaceAll("\\\\", "");
			} catch (JsonProcessingException e) {
				LOG.error("Unable to convert ArrayList to JSON" + e);
			}

		}
		return "Unable to convert ArrayList to JSON";
	}

	private SearchSourceBuilder queryBuilder(String id, SearchSourceBuilder searchSourceBuilder) {
		if (id.equals(Constants.GET_ALARMS_ENUM.allActiveAlarms.toString()))
			searchSourceBuilder.query(QueryBuilders.matchAllQuery()).size(1000);
		else if (id.equals(Constants.GET_ALARMS_ENUM.allActiveAndUnacknowledgedAlarms.toString()))
			searchSourceBuilder.query(QueryBuilders.commonTermsQuery("ackstate.ackstate", "unacknowledged")).size(1000);
		else if (id.equals(Constants.GET_ALARMS_ENUM.allActiveAndAcknowledgedAlarms.toString()))
			searchSourceBuilder.query(QueryBuilders.commonTermsQuery("ackstate.ackstate", "acknowledged")).size(1000);
		else
			searchSourceBuilder.query(QueryBuilders.matchAllQuery()).size(1000);
		return searchSourceBuilder;
	}

	public String addComment(String alarmId, Map<String, String> commentMap) {
		ArrayList<String> stresults = new ArrayList<String>();
		stresults.add(modifyAlarm(alarmId, commentMap, null).replaceAll("[\\n]+", "").replaceAll("\\\\", ""));
		return getResponse(stresults);
	}

	private String getResponse(ArrayList<String> stresults) {
		Map<String, ArrayList<String>> errorResponse = new HashMap<String, ArrayList<String>>();
		ObjectMapper mapper = jsonUtil.getMapper();
		String strRes = Constants.EMPTY_STRING;
		for (String alarm : stresults) {
			if (alarm.contains("alarm"))
				errorResponse.put("error", stresults);
		}
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		if (!errorResponse.isEmpty()) {
			try {
				strRes = mapper.writeValueAsString(errorResponse);
			} catch (JsonProcessingException e) {
				LOG.error("Unable to convert HashMap to String" + e);
			}
		}
		mapper.disable(SerializationFeature.INDENT_OUTPUT);
		return strRes.replaceAll("[\\n]+", "").replaceAll("\\\\", "");
	}

	private String modifyAlarm(String alarmId, Map<String, String> commentMap, Map<String, String> ackState) {
		ArrayList<String> stresults = new ArrayList<String>();
		if (DbAdaptor.storageEngine.equalsIgnoreCase("elasticsearch")) {
			RestHighLevelClient client = esClient.getConnection();
			SearchHit[] results = getSearchResults(alarmId, client);
			for (SearchHit hit : results) {

				Map<String, Object> getExistingDoc = hit.getSourceAsMap();
				if (commentMap != null)
					getExistingDoc.put("comment", commentMap);
				else if (ackState != null)
					getExistingDoc.put("ackstate", ackState);
				esUtil.handleDoc(getExistingDoc, dateUtil.getCurrentDate());
				ObjectMapper mapper = new ObjectMapper();
				JsonNode actualObj = null;
				try {
					actualObj = mapper.readTree(hit.getSourceAsString().toString());
				} catch (JsonMappingException e) {
					e.printStackTrace();
				} catch (JsonProcessingException e) {
					e.printStackTrace();
				}
				stresults = getVesFormat(actualObj, stresults, false);
			}
		} else {
			boolean ifExist = false;
			if (commentMap != null)
				ifExist = zkUtil.addComment(alarmId, commentMap);
			else if (ackState != null)
				ifExist = zkUtil.addAckState(alarmId, ackState);
			if (ifExist) {
				stresults.add("new");
				Map<String, Object> getExistingDoc = new HashMap<>();
				Map<String, Object> comEvtHdrMap = zkUtil.getCommonEventHeaderZK(alarmId);
				Map<String, Object> tcaFieldsMap = new HashMap<>();
				Map<String, Object> faultFieldsMap = new HashMap<>();
				if ((comEvtHdrMap.get(Constants.DOMAIN)).toString().equalsIgnoreCase((Domain.FAULT).toString())) {
					faultFieldsMap = zkUtil.getFaultFieldsZK(alarmId);
				} else if ((comEvtHdrMap.get(Constants.DOMAIN)).toString()
						.equalsIgnoreCase(Domain.THRESHOLD_CROSSING_ALERT.toString())) {
					tcaFieldsMap = zkUtil.getTCAFieldsZK(alarmId);
				}
				Map<String, Object> eventDefinition = zkUtil.getEventDefZK(alarmId);
				getExistingDoc.put(Constants.EVENT_DEF, eventDefinition);
				getExistingDoc.put(Constants.FAULT_FIELDS, faultFieldsMap);
				getExistingDoc.put(Constants.TCA_FIELDS, tcaFieldsMap);
				getExistingDoc.put(Constants.COMMON_EVT_HDR, comEvtHdrMap);
				nbiInterface.createMEFObject(getExistingDoc);
			}
		}

		Map<String, String> errorMap = new HashMap<String, String>();
		ObjectMapper mapper = jsonUtil.getMapper();
		if (stresults.isEmpty()) {
			LOG.error("No alarm found with alarmId " + alarmId);
			errorMap.put("alarmId", alarmId);
			errorMap.put("errorReason", "no alarm present for this alarmId");
		}
		try {
			mapper.enable(SerializationFeature.INDENT_OUTPUT);
			String strResults = mapper.writeValueAsString(errorMap);
			mapper.disable(SerializationFeature.INDENT_OUTPUT);
			return strResults;
		} catch (JsonProcessingException e) {
			LOG.error("Unable to convert HashMap to String" + e);
		}
		return "Unable to convert HashMap to String";
	}

	private SearchHit[] getSearchResults(String alarmId, RestHighLevelClient client) {

		SearchRequest searchRequest = new SearchRequest("alarms*");
		searchRequest.indicesOptions(IndicesOptions.LENIENT_EXPAND_OPEN);
		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
		searchSourceBuilder.query(QueryBuilders.matchPhraseQuery("commonEventHeader.eventId", alarmId)).size(1000);
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
				LOG.debug("Error occured while executing query on Elastic Search, retrying..... count:" + countRetry,
						e);
			}
		}
		SearchHit[] results = searchResponse.getHits().getHits();
		return results;
	}

	public String addMultipleComment(ArrayList<String> alarmIdList, Map<String, String> commentMap) {
		ArrayList<String> stresults = new ArrayList<String>();
		for (String alarmId : alarmIdList) {
			stresults.add(modifyAlarm(alarmId, commentMap, null).replaceAll("[\\n]+", "").replaceAll("\\\\", ""));
		}
		return getResponse(stresults);
	}

	public String addAckState(String alarmId, Map<String, String> ackState) {
		ArrayList<String> stresults = new ArrayList<String>();
		stresults.add(modifyAlarm(alarmId, null, ackState).replaceAll("[\\n]+", "").replaceAll("\\\\", ""));
		return getResponse(stresults);
	}

	@SuppressWarnings("unchecked")
	public String clearAlarmId(String alarmId, Map<String, String> map) {
		ArrayList<String> stresults = new ArrayList<String>();
		if (DbAdaptor.storageEngine.equalsIgnoreCase("elasticsearch")) {
			RestHighLevelClient client = esClient.getConnection();
			SearchHit[] results = getSearchResults(alarmId, client);
			for (SearchHit hit : results) {
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
				comment.put("commentUserId", map.get("clearUserId"));
				comment.put("commentSystemId", map.get("clearSystemId"));
				comment.put("commentText", "Manual Clear");
				comment.put("commentTime", dateUtil.getTimestamp());
				getExistingDoc.put("comment", comment);
				getExistingDoc.put(Constants.EVENT_DEF, eventDefinition);
				getExistingDoc.put(Constants.FAULT_FIELDS, faultFieldsMap);
				getExistingDoc.put(Constants.TCA_FIELDS, tcaFieldsMap);
				getExistingDoc.put(Constants.COMMON_EVT_HDR, comEvtHdrMap);
				esUtil.manualClear(getExistingDoc);
				ObjectMapper mapper = new ObjectMapper();
				JsonNode actualObj = null;
				try {
					actualObj = mapper.readTree(hit.getSourceAsString().toString());
				} catch (JsonMappingException e) {
					e.printStackTrace();
				} catch (JsonProcessingException e) {
					e.printStackTrace();
				}
				stresults = getVesFormat(actualObj, stresults, false);
			}
		} else {
			if (zkUtil.clearAlarmZK(alarmId, map))
				stresults.add("new");
		}
		Map<String, String> errorMap = new HashMap<String, String>();
		ObjectMapper mapper = jsonUtil.getMapper();
		if (stresults.isEmpty()) {
			LOG.error("No alarm found with alarmId " + alarmId);
			errorMap.put("alarmId", alarmId);
			errorMap.put("errorReason", "no alarm present for this alarm");
		}
		try {
			mapper.enable(SerializationFeature.INDENT_OUTPUT);
			String strResults = mapper.writeValueAsString(errorMap);
			mapper.disable(SerializationFeature.INDENT_OUTPUT);
			return strResults;
		} catch (JsonProcessingException e) {
			LOG.error("Unable to convert ArrayList to String" + e);
		}
		return "Unable to convert ArrayList to String";
	}

	public String addMultipleAckState(ArrayList<String> alarmIdList, Map<String, String> ackState) {
		ArrayList<String> stresults = new ArrayList<String>();
		for (String alarmId : alarmIdList) {
			stresults.add(modifyAlarm(alarmId, null, ackState).replaceAll("[\\n]+", "").replaceAll("\\\\", ""));
		}
		return getResponse(stresults);
	}

	public String multipleClearAlarm(ArrayList<String> alarmIdList, Map<String, String> map) {
		ArrayList<String> stresults = new ArrayList<String>();
		for (String alarmId : alarmIdList) {
			stresults.add(clearAlarmId(alarmId, map).replaceAll("[\\n]+", "").replaceAll("\\\\", ""));
		}
		return getResponse(stresults);
	}

	private ArrayList<String> getVesFormat(JsonNode actualObj, ArrayList<String> stresults, boolean replay) {
		ObjectMapper objectMapper = jsonUtil.getMapper();

		FaultFields faultFields = objectMapper.convertValue(actualObj.findValue("faultFields"), FaultFields.class);
		ThresholdCrossingAlertFields tcaFields = objectMapper.convertValue(actualObj.findValue("tcaFields"),
				ThresholdCrossingAlertFields.class);
		NotificationFields notificationFields = objectMapper.convertValue(actualObj.findValue("notificationFields"),
				NotificationFields.class);
		CommonEventHeader commonEventHeader = objectMapper.convertValue(actualObj.findValue("commonEventHeader"),
				CommonEventHeader.class);
		Event event = null;
		String nfLabel = Constants.EMPTY_STRING;
		String localNfID = Constants.EMPTY_STRING;
		if (commonEventHeader.getDomain().equals(Domain.THRESHOLD_CROSSING_ALERT)) {
			event = new Event(commonEventHeader, null, tcaFields, null);
			if (tcaFields.getAdditionalFields().getAdditionalProperties().containsKey(Constants.NF_LABEL))
				nfLabel = tcaFields.getAdditionalFields().getAdditionalProperties().get(Constants.NF_LABEL).toString();
		} else if (commonEventHeader.getDomain().equals(Domain.FAULT)) {
			event = new Event(commonEventHeader, faultFields, null, null);
			if (faultFields.getAlarmAdditionalInformation().getAdditionalProperties().containsKey(Constants.NF_LABEL))
				nfLabel = faultFields.getAlarmAdditionalInformation().getAdditionalProperties().get(Constants.NF_LABEL)
						.toString();
		} else if (commonEventHeader.getDomain().equals(Domain.NOTIFICATION)) {
			event = new Event(commonEventHeader, null, null, notificationFields);
			if (notificationFields.getAdditionalFields().getAdditionalProperties().containsKey(Constants.NF_LABEL))
				nfLabel = notificationFields.getAdditionalFields().getAdditionalProperties().get(Constants.NF_LABEL)
						.toString();
		}
		int sequence = commonEventHeader.getSequence();
		if (!nfLabel.isEmpty())
			localNfID = nfLabel.substring(nfLabel.lastIndexOf("=") + 1).trim();
		MEF mefEvent = new MEF(event);

		try {
			String mefJson = objectMapper.writeValueAsString(mefEvent);
			LOG.debug("VES:\n" + mefJson);
			stresults.add(mefJson);
			if (replay)
				nbiInterface.sendToNBI(mefJson, commonEventHeader.getDomain().toString(),
						commonEventHeader.getLocalEventName(), commonEventHeader.getEventName().toString(),
						commonEventHeader.getNfNamingCode().toString(), commonEventHeader.getNfcNamingCode().toString(),
						localNfID, sequence, false);

		} catch (JsonProcessingException e) {

			LOG.error("Unable to parse JSON" + e);
		}
		return stresults;
	}

	public String clearAlarm(String alarmId, Map<String, String> patchAsClearMap) {
		ArrayList<String> stresults = new ArrayList<String>();
		stresults.add(clearAlarmId(alarmId, patchAsClearMap).replaceAll("[\\n]+", "").replaceAll("\\\\", ""));
		return getResponse(stresults);
	}

	/*
	 * public String eventsReplay(int start, int end) { LOG.debug("Replay Ongoing");
	 * replayOngoing = true; eventListener.listenerPause(); ArrayList<String>
	 * stresults = new ArrayList<String>(); if
	 * (DbAdaptor.storageEngine.equalsIgnoreCase("elasticsearch")) {
	 * RestHighLevelClient client = esClient.getConnection(); SearchRequest
	 * searchRequest = new SearchRequest("events*");
	 * searchRequest.indicesOptions(IndicesOptions.LENIENT_EXPAND_OPEN);
	 * SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder(); if (end
	 * == 0) searchSourceBuilder.query(QueryBuilders.rangeQuery(
	 * "commonEventHeader.sequence").from(start)) .size(1000); else
	 * searchSourceBuilder.query(QueryBuilders.rangeQuery(
	 * "commonEventHeader.sequence").from(start).to(end)) .size(1000);
	 * searchSourceBuilder.sort("commonEventHeader.sequence");
	 * searchRequest.source(searchSourceBuilder); LOG.debug("Search request: " +
	 * searchRequest.toString()); SearchResponse searchResponse = null; int
	 * countRetry = 0; while (true) { try { searchResponse =
	 * client.search(searchRequest, RequestOptions.DEFAULT); break; } catch
	 * (Exception e) { if (countRetry == JsonUtil.maxRetries) { LOG.error(
	 * "Error occured while executing query on Elastic Search, max retries attempted. Exiting the service!!"
	 * , e); System.out.println(
	 * "Error occured while executing query on Elastic Search, max retries attempted. Exiting the service!!"
	 * ); System.exit(0); } countRetry++; LOG.debug(
	 * "Error occured while executing query on Elastic Search, retrying..... count:"
	 * + countRetry, e); } } SearchHit[] results =
	 * searchResponse.getHits().getHits();
	 *
	 * for (SearchHit hit : results) { ObjectMapper mapper = new ObjectMapper();
	 * JsonNode actualObj = null; try { actualObj =
	 * mapper.readTree(hit.getSourceAsString().toString()); } catch
	 * (JsonMappingException e) { LOG.error("Error in parsing search response: " +
	 * e); } catch (JsonProcessingException e) {
	 * LOG.error("Error in parsing search response: " + e); } stresults =
	 * getVesFormat(actualObj, stresults, true);
	 *
	 * } } else { KafkaConfiguration kf = new KafkaConfiguration(); String
	 * startOffset = zkUtil.getOffset(String.valueOf(start)); if
	 * (!startOffset.isEmpty()) { if (end == 0) {
	 * stresults.addAll(kf.consumerByOffset(Long.parseLong(startOffset),
	 * Long.parseLong(zkUtil.getOffset((String.valueOf(zkUtil.getSequence())))))); }
	 * else { String endOffset = zkUtil.getOffset(String.valueOf(end)); if
	 * (endOffset.isEmpty()) {
	 * stresults.addAll(kf.consumerByOffset(Long.parseLong(startOffset),
	 * Long.parseLong(zkUtil.getOffset((String.valueOf(zkUtil.getSequence())))))); }
	 * else { stresults.addAll(kf.consumerByOffset(Long.parseLong(startOffset),
	 * Long.parseLong(endOffset))); } } ObjectMapper objectMapper =
	 * jsonUtil.getMapper(); for (String document : stresults) { JsonNode eventNode
	 * = null; try { eventNode = objectMapper.readTree(document); } catch
	 * (JsonMappingException e) { LOG.error(e.getMessage()); } catch
	 * (JsonProcessingException e) { LOG.error(e.getMessage()); }
	 *
	 * FaultFields faultFields =
	 * objectMapper.convertValue(eventNode.findValue("faultFields"),
	 * FaultFields.class); ThresholdCrossingAlertFields tcaFields =
	 * objectMapper.convertValue(eventNode.findValue("tcaFields"),
	 * ThresholdCrossingAlertFields.class); NotificationFields notificationFields =
	 * objectMapper .convertValue(eventNode.findValue("notificationFields"),
	 * NotificationFields.class); CommonEventHeader commonEventHeader = objectMapper
	 * .convertValue(eventNode.findValue("commonEventHeader"),
	 * CommonEventHeader.class); Event event = null; String nfLabel =
	 * Constants.EMPTY_STRING; String localNfID = Constants.EMPTY_STRING; if
	 * (commonEventHeader.getDomain().equals(Domain.THRESHOLD_CROSSING_ALERT)) {
	 * event = new Event(commonEventHeader, null, tcaFields, null); if
	 * (tcaFields.getAdditionalFields().getAdditionalProperties().containsKey(
	 * Constants.NF_LABEL)) nfLabel =
	 * tcaFields.getAdditionalFields().getAdditionalProperties().get(Constants.
	 * NF_LABEL) .toString(); } else if
	 * (commonEventHeader.getDomain().equals(Domain.FAULT)) { event = new
	 * Event(commonEventHeader, faultFields, null, null); if
	 * (faultFields.getAlarmAdditionalInformation().getAdditionalProperties()
	 * .containsKey(Constants.NF_LABEL)) nfLabel =
	 * faultFields.getAlarmAdditionalInformation().getAdditionalProperties()
	 * .get(Constants.NF_LABEL).toString(); } else if
	 * (commonEventHeader.getDomain().equals(Domain.NOTIFICATION)) { event = new
	 * Event(commonEventHeader, null, null, notificationFields); if
	 * (notificationFields.getAdditionalFields().getAdditionalProperties()
	 * .containsKey(Constants.NF_LABEL)) nfLabel =
	 * notificationFields.getAdditionalFields().getAdditionalProperties()
	 * .get(Constants.NF_LABEL).toString(); } int sequence =
	 * commonEventHeader.getSequence(); if (!nfLabel.isEmpty()) localNfID =
	 * nfLabel.substring(nfLabel.lastIndexOf("=") + 1).trim(); MEF mefEvent = new
	 * MEF(event);
	 *
	 * try { String mefJson = objectMapper.writeValueAsString(mefEvent);
	 * LOG.debug("VES:\n" + mefJson); nbiInterface.sendToNBI(mefJson,
	 * commonEventHeader.getDomain().toString(),
	 * commonEventHeader.getLocalEventName(),
	 * commonEventHeader.getEventName().toString(),
	 * commonEventHeader.getNfNamingCode().toString(),
	 * commonEventHeader.getNfcNamingCode().toString(), localNfID, sequence,true);
	 *
	 * } catch (JsonProcessingException e) {
	 *
	 * LOG.error("Unable to parse JSON" + e); } }
	 *
	 * } } eventListener.listenerResume(); replayOngoing = false; Map<String,
	 * String> responseMap = new HashMap<String, String>(); Map<String, Map<String,
	 * String>> response = new HashMap<String, Map<String, String>>(); ObjectMapper
	 * mapper = jsonUtil.getMapper(); if (!stresults.isEmpty()) { try {
	 * mapper.enable(SerializationFeature.INDENT_OUTPUT); String strResults =
	 * mapper.writeValueAsString(stresults);
	 * mapper.disable(SerializationFeature.INDENT_OUTPUT); //
	 * LOG.debug("response for eventsResync request: " + //
	 * strResults.replaceAll("\\\\", "")); responseMap.put("message",
	 * "Event replay initiated"); response.put("success", responseMap); try {
	 * mapper.enable(SerializationFeature.INDENT_OUTPUT); String strResponse =
	 * mapper.writeValueAsString(response);
	 * mapper.disable(SerializationFeature.INDENT_OUTPUT);
	 * LOG.debug("response for eventsReplay request: " +
	 * strResponse.replaceAll("\\\\", "")); return
	 * strResponse.replaceAll("\\\\", ""); } catch (JsonProcessingException e) {
	 * LOG.error("Error occur while parsing response JSON" + e); } return
	 * "Replay Initiated"; } catch (JsonProcessingException e) {
	 * LOG.error("Unable to convert ArrayList to JSON" + e); } } else {
	 * LOG.error("No events found for requested start/end sequence Id");
	 * responseMap.put("errorReason",
	 * "no events present for requested start/end sequence Id");
	 * response.put("error", responseMap); try {
	 * mapper.enable(SerializationFeature.INDENT_OUTPUT); String strResults =
	 * mapper.writeValueAsString(response);
	 * mapper.disable(SerializationFeature.INDENT_OUTPUT);
	 * LOG.debug("response for eventsReplay request: " +
	 * strResults.replaceAll("\\\\", "")); return strResults.replaceAll("\\\\", "");
	 * } catch (JsonProcessingException e) {
	 * LOG.error("Error occur while parsing response JSON" + e); }
	 *
	 * } return "Error occured while processing eventsReplay"; }
	 */

	public String replayOngoing() {
		Map<String, String> errorMap = new HashMap<String, String>();
		Map<String, Map<String, String>> errorResponse = new HashMap<String, Map<String, String>>();
		errorMap.put("errorReason", "Replay in progress. Request cannot be served now. Please try after sometime.");
		errorResponse.put("error", errorMap);
		ObjectMapper mapper = jsonUtil.getMapper();
		LOG.error("Replay in progress. Request cannot be served now. Please try after sometime.");

		try {
			mapper.enable(SerializationFeature.INDENT_OUTPUT);
			String strResults = mapper.writeValueAsString(errorResponse);
			mapper.disable(SerializationFeature.INDENT_OUTPUT);
			LOG.debug("Response for Request: " + strResults.replaceAll("\\\\", ""));
			return strResults.replaceAll("\\\\", "");
		} catch (JsonProcessingException e) {
			LOG.error("Error occur while parsing response JSON" + e);
		}
		return "Error occured while processing Request";
	}

	public ResponseEntity<String> eventsReplayCollector(int collectorId, int start, int end) {
		ObjectMapper objectMapper = jsonUtil.getMapper();
		InitReplayRequest obj = null;
		if (end == 0)
			obj = new InitReplayRequest(collectorId, start);
		else
			obj = new InitReplayRequest(collectorId, start, end);
		HttpEntity<String> entity = null;
		try {
			entity = new HttpEntity<String>(objectMapper.writeValueAsString(obj));
			LOG.debug(objectMapper.writeValueAsString(obj));
		} catch (JsonProcessingException e) {
			LOG.error(e.getMessage(), e);
		}
		try {
			ResponseEntity<String> response = restTemplate.exchange(JsonUtil.initReplayUrl, HttpMethod.POST, entity,
					String.class);
			return response;
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			return new ResponseEntity<String>("Cannot Process Request right now, try later!!", headers,
					HttpStatus.SERVICE_UNAVAILABLE);
		}

		/*
		 * HttpHeaders headers = new HttpHeaders();
		 * headers.setContentType(MediaType.APPLICATION_JSON); if
		 * (JsonUtil.globalSequence < start) {
		 * LOG.error("No Records Available for requested sequence"); return new
		 * ResponseEntity<String>("No Records Available for requested sequence",
		 * headers, HttpStatus.NO_CONTENT); } if (end != 0) { if (end < start) {
		 * LOG.error("End sequence number cannot be smaller than Start Sequence");
		 * return new ResponseEntity<String>
		 * ("End sequence number cannot be smaller than Start Sequence", headers,
		 * HttpStatus.NOT_ACCEPTABLE); } } String topic = "REPLAY" + collectorId;
		 * kafkaTopic.createTopic(topic); InitReplayRequest obj = new
		 * InitReplayRequest(collectorId); HttpEntity<String> entity = null; try {
		 * entity = new HttpEntity<String>(objectMapper.writeValueAsString(obj)); }
		 * catch (JsonProcessingException e) { LOG.error(e.getMessage(), e); } try {
		 * ResponseEntity<String> response =
		 * restTemplate.exchange(JsonUtil.initReplayUrl, HttpMethod.POST, entity,
		 * String.class);
		 *
		 * LOG.debug("Response: " + response.getBody() + ", StatusCode: " +
		 * response.getStatusCode() + ", Value: " + response.getStatusCodeValue());
		 * JsonNode responseJson = null; if
		 * (response.getStatusCode().equals(HttpStatus.ACCEPTED)) { try { responseJson =
		 * objectMapper.readTree(response.getBody()); } catch (JsonProcessingException
		 * e) { LOG.error(e.getMessage(), e); } } if
		 * (responseJson.get("collectorStatus").asText().equalsIgnoreCase("UP")) { if
		 * (responseJson.get("replayOngoing").asText().equalsIgnoreCase("FALSE")) { if
		 * (end == 0) end =
		 * Integer.parseInt(responseJson.get("internalLastSequence").asText()) + 1;
		 * LOG.info("Replay Initiated for collectorId: " + collectorId);
		 * replayEvents(topic, collectorId, start, end); return new
		 * ResponseEntity<String>("Replay Initiated for collectorId: " + collectorId,
		 * headers, HttpStatus.ACCEPTED);
		 *
		 * } else { LOG.error("Replay Already in Process for collectorId:" +
		 * collectorId); return new
		 * ResponseEntity<String>("Replay Already in Process for collectorId:" +
		 * collectorId, headers, HttpStatus.NOT_ACCEPTABLE); } } else {
		 * LOG.error("Connection Down between VESGW and Collector for collectorId: " +
		 * collectorId); return new ResponseEntity<String>(
		 * "Connection Down between VESGW and Collector for collectorId: " +
		 * collectorId, headers, HttpStatus.NOT_ACCEPTABLE); } } catch (Exception e) {
		 * LOG.error("Cannot Process Request right now, try later!!"); return new
		 * ResponseEntity<String>("Cannot Process Request right now, try later!!",
		 * headers, HttpStatus.SERVICE_UNAVAILABLE); }
		 */

	}

	/*
	 * private void replayEvents(String topic, int collectorId, int start, int end)
	 * { if (DbAdaptor.storageEngine.equalsIgnoreCase("elasticsearch")) {
	 * RestHighLevelClient client = esClient.getConnection(); SearchRequest
	 * searchRequest = new SearchRequest("events*");
	 * searchRequest.indicesOptions(IndicesOptions.LENIENT_EXPAND_OPEN);
	 * SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder(); if (end
	 * == 0) searchSourceBuilder.query(QueryBuilders.rangeQuery(
	 * "commonEventHeader.sequence").from(start)) .size(1000); else
	 * searchSourceBuilder.query(QueryBuilders.rangeQuery(
	 * "commonEventHeader.sequence").from(start).to(end)) .size(1000);
	 * searchSourceBuilder.sort("commonEventHeader.sequence");
	 * searchRequest.source(searchSourceBuilder); LOG.debug("Search request: " +
	 * searchRequest.toString()); SearchResponse searchResponse = null; int
	 * countRetry = 0; while (true) { try { searchResponse =
	 * client.search(searchRequest, RequestOptions.DEFAULT); break; } catch
	 * (Exception e) { if (countRetry == JsonUtil.maxRetries) { LOG.error(
	 * "Error occured while executing query on Elastic Search, max retries attempted. Exiting the service!!"
	 * , e); System.out.println(
	 * "Error occured while executing query on Elastic Search, max retries attempted. Exiting the service!!"
	 * ); System.exit(0); } countRetry++; LOG.debug(
	 * "Error occured while executing query on Elastic Search, retrying..... count:"
	 * + countRetry, e); } } SearchHit[] results =
	 * searchResponse.getHits().getHits(); int totalEvents = results.length; int
	 * count = 0; for (SearchHit hit : results) { ObjectMapper mapper = new
	 * ObjectMapper(); JsonNode actualObj = null; try { actualObj =
	 * mapper.readTree(hit.getSourceAsString().toString()); } catch
	 * (JsonMappingException e) { LOG.error("Error in parsing search response: " +
	 * e); } catch (JsonProcessingException e) {
	 * LOG.error("Error in parsing search response: " + e); } count++; if (count ==
	 * totalEvents) sendToKafka(topic, actualObj, true); else sendToKafka(topic,
	 * actualObj, false); } } else { KafkaConfiguration kf = new
	 * KafkaConfiguration(); String startOffset =
	 * zkUtil.getOffset(String.valueOf(start)); ArrayList<String> stresults = new
	 * ArrayList<>(); if (!startOffset.isEmpty()) { if (end == 0) {
	 * stresults.addAll(kf.consumerByOffset(Long.parseLong(startOffset),
	 * Long.parseLong(zkUtil.getOffset((String.valueOf(zkUtil.getSequence())))))); }
	 * else { String endOffset = zkUtil.getOffset(String.valueOf(end)); if
	 * (endOffset.isEmpty()) {
	 * stresults.addAll(kf.consumerByOffset(Long.parseLong(startOffset),
	 * Long.parseLong(zkUtil.getOffset((String.valueOf(zkUtil.getSequence())))))); }
	 * else { stresults.addAll(kf.consumerByOffset(Long.parseLong(startOffset),
	 * Long.parseLong(endOffset))); } } } int totalEvents = stresults.size(); int
	 * count = 0; for (String hit : stresults) { ObjectMapper mapper = new
	 * ObjectMapper(); JsonNode actualObj = null; try { actualObj =
	 * mapper.readTree(hit); } catch (JsonMappingException e) {
	 * LOG.error("Error in parsing search response: " + e); } catch
	 * (JsonProcessingException e) { LOG.error("Error in parsing search response: "
	 * + e); } count++; if (count == totalEvents) sendToKafka(topic, actualObj,
	 * true); else sendToKafka(topic, actualObj, false); }
	 *
	 * } }
	 *
	 * private void sendToKafka(String topic, JsonNode actualObj, boolean
	 * replayComplete) { ObjectMapper objectMapper = jsonUtil.getMapper();
	 *
	 * FaultFields faultFields =
	 * objectMapper.convertValue(actualObj.findValue("faultFields"),
	 * FaultFields.class); ThresholdCrossingAlertFields tcaFields =
	 * objectMapper.convertValue(actualObj.findValue("tcaFields"),
	 * ThresholdCrossingAlertFields.class); NotificationFields notificationFields =
	 * objectMapper.convertValue(actualObj.findValue("notificationFields"),
	 * NotificationFields.class); CommonEventHeader commonEventHeader =
	 * objectMapper.convertValue(actualObj.findValue("commonEventHeader"),
	 * CommonEventHeader.class); Event event = null; if
	 * (commonEventHeader.getDomain().equals(Domain.THRESHOLD_CROSSING_ALERT)) {
	 * event = new Event(commonEventHeader, null, tcaFields, null); } else if
	 * (commonEventHeader.getDomain().equals(Domain.FAULT)) { event = new
	 * Event(commonEventHeader, faultFields, null, null); } else if
	 * (commonEventHeader.getDomain().equals(Domain.NOTIFICATION)) { event = new
	 * Event(commonEventHeader, null, null, notificationFields); } MEF mefEvent =
	 * new MEF(event); String mefJson = Constants.EMPTY_STRING; try { mefJson =
	 * objectMapper.writeValueAsString(mefEvent); } catch (JsonProcessingException
	 * e) { LOG.error("Unable to parse JSON" + e); } LOG.debug("VES:\n" + mefJson);
	 * kafkaPush.pushReplayEvents(topic, mefJson, replayComplete);
	 *
	 * }
	 */
}
