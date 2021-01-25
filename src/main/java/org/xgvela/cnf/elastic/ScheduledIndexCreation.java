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

package org.xgvela.cnf.elastic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import javax.annotation.PostConstruct;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.action.admin.indices.get.GetIndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.xgvela.cnf.Constants;
import org.xgvela.cnf.util.DateUtil;
import org.xgvela.cnf.util.ESUtil;
import org.xgvela.cnf.util.JsonUtil;
import org.xgvela.dbadapter.DbAdaptor;
import org.xgvela.dbadapter.zk.LeaderElection;
import org.xgvela.dbadapter.zk.ZKUtil;
import org.xgvela.model.ThresholdCrossingAlertFields;
import org.xgvela.model.CommonEventHeader.Domain;
import org.xgvela.updateconfig.UpdateConfigHelper;

@Service
public class ScheduledIndexCreation {

	private static Logger LOG = LogManager.getLogger(ScheduledIndexCreation.class);

	private static String mapping = "";

	public static final CountDownLatch registrationLatch = new CountDownLatch(1);

	@Autowired
	ElasticSearchClientConfiguration esClient;

	@Autowired
	DateUtil dateUtil;

	@Autowired
	UpdateConfigHelper updateConfigHelper;

	@Autowired
	JsonUtil jsonUtil;

	@Autowired
	RestTemplate restTemplate;

	@Autowired
	ZKUtil zkUtil;

	@Autowired
	LeaderElection leaderElection;

	@Autowired
	ESUtil esUtil;

	@PostConstruct
	private void init() throws InterruptedException {
		// await xgvela registration
		registrationLatch.await();
		if (DbAdaptor.storageEngine.equalsIgnoreCase("elasticsearch")) {
			setMapping();
			LOG.info("Checking existence of today's index - ");
			createIndexHelper(dateUtil.getCurrentDate());
		} else {
			zkUtil.createIntialNode();
			leaderElection.leaderElection();
		}
		updateConfigHelper.initFmaasConfig();
		jsonUtil.initSequence();
		class MyThread extends Thread {
			public void run() {
				jsonUtil.initSubscription();
				jsonUtil.initAlarmClear();
				jsonUtil.initResetReplay();
			}

		}
		MyThread thread = new MyThread();
		thread.start();
	}

	@Scheduled(fixedRate = (12 * 60 * 60 * 1000)) // 12 hr * 60 min/hr * 60 s/min * 1000 ms/s
	private void scheduledTask() {
		if (DbAdaptor.storageEngine.equalsIgnoreCase("elasticsearch")) {
			LOG.info("Scheduled task running at: " + new Date());
			createIndexHelper(dateUtil.getNextDate());
		}
	}

	@Scheduled(fixedRate = (12 * 60 * 60 * 1000)) // 12 hr* 60 min/hr * 60 s/min * 1000 ms/s
	private void scheduledDeleteTask() {
		LOG.info("Scheduled purge task running for engine: " + DbAdaptor.storageEngine + " at " + new Date());
		long intial = System.currentTimeMillis();
		if (DbAdaptor.storageEngine.equalsIgnoreCase("elasticsearch")) {
			deleteIndex();
		} else {
			zkUtil.deleteNodes();
		}
		long ending = System.currentTimeMillis();
		LOG.info("purging activity completed in " + String.valueOf(ending - intial) + "ms");
		dateUtil.updateLastPurgeTime();
		dateUtil.updateNextScheduledPurgeTime();
	}

	public void deleteIndex() {
		RestHighLevelClient client = esClient.getConnection();
		GetIndexRequest getIndexRequest = new GetIndexRequest();
		getIndexRequest.indices("_all");
		int countRetry = 0;
		while (true) {
			try {
				GetIndexResponse searchResponse = client.indices().get(getIndexRequest, RequestOptions.DEFAULT);
				String[] hits = searchResponse.getIndices();
				for (String indexName : hits) {
					if (!(indexName.startsWith("alarms-") || indexName.startsWith("events-")))
						continue;
					String indexDate = indexName.substring(7);
					if (dateUtil.checkIfLessThanRetained(indexDate)) {
						if (!(indexName.startsWith("alarms-"))) {
							SearchRequest searchRequest = new SearchRequest(indexName);
							searchRequest.indicesOptions(IndicesOptions.LENIENT_EXPAND_OPEN);
							LOG.debug("Search request: " + searchRequest.toString());
							SearchResponse alarmSearchResponse = null;
							alarmSearchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
							SearchHit[] results = alarmSearchResponse.getHits().getHits();
							for (SearchHit hit : results) {
								ObjectMapper mapper = new ObjectMapper();
								Map<String, Object> getExistingDoc = hit.getSourceAsMap();
								Map<String, Object> tcaFieldsMap = (Map<String, Object>) getExistingDoc
										.get(Constants.TCA_FIELDS);
								Map<String, Object> faultFieldsMap = (Map<String, Object>) getExistingDoc
										.get(Constants.FAULT_FIELDS);
								Map<String, Object> comEvtHdrMap = (Map<String, Object>) getExistingDoc
										.get(Constants.COMMON_EVT_HDR);
								if ((comEvtHdrMap.get(Constants.DOMAIN)).toString()
										.equalsIgnoreCase((Domain.FAULT).toString()))
									faultFieldsMap.put(Constants.EVENT_SEVERITY, "CLEAR");
								else if ((comEvtHdrMap.get(Constants.DOMAIN)).toString()
										.equalsIgnoreCase(Domain.THRESHOLD_CROSSING_ALERT.toString())) {
									tcaFieldsMap.put(Constants.EVENT_SEVERITY, "CLEAR");
									tcaFieldsMap.put(Constants.ALERT_ACTION,
											ThresholdCrossingAlertFields.AlertAction.CLEAR);
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
						DeleteIndexRequest index = new DeleteIndexRequest(indexName);
						LOG.info("Sending Index delete request for Index: " + indexName);
						client.indices().delete(index, RequestOptions.DEFAULT);
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
				LOG.debug("Error occured while executing query on Elastic Search, retrying..... count:" + countRetry,
						e);
			}
		}
	}

	private void createIndexHelper(String date) {

		RestHighLevelClient client = esClient.getConnection();

		indexCreation(client, Constants.EVENT_PREFIX, date);
		indexCreation(client, Constants.ALARM_PREFIX, date);

	}

	private void indexCreation(RestHighLevelClient esClient, String indexPrefix, String date) {

		if (!indexExists(indexPrefix + date)) {
			createIndex(indexPrefix + date);

		} else {
			LOG.info(indexPrefix + date + " index already exists.");
		}
	}

	private void createIndex(String indexName) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> entity = new HttpEntity<String>(mapping, headers);
		String url = Constants.EMPTY_STRING;
		if (esClient.isLessThanSixPointEight()) {
			url = "http://" + ElasticSearchClientConfiguration.HOST + ":" + ElasticSearchClientConfiguration.PORT_ONE
					+ "//" + indexName;
		} else {
			url = "http://" + ElasticSearchClientConfiguration.HOST + ":" + ElasticSearchClientConfiguration.PORT_ONE
					+ "//" + indexName + "?include_type_name=true";
		}
		LOG.info("Sending Index creation request to URL: " + url);
		int countRetry = 0;
		while (true) {
			try {
				ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);
				LOG.debug("Response for index creation: " + response);
				break;
			} catch (Exception e) {
				if (countRetry == JsonUtil.maxRetries) {
					LOG.error(
							"Error occured while connecting to Elastic Search, max retries attempted. Exiting the service!!",
							e);
					System.out.println(
							"Error occured while connecting to Elastic Search, max retries attempted. Exiting the service!!");
					System.exit(0);
				}
				countRetry++;
				LOG.debug("Error occured while connecting to Elastic Search, retrying..... count:" + countRetry, e);
			}
		}
		/*
		 * CreateIndexRequest request = new CreateIndexRequest(indexName);
		 * request.settings( Settings.builder().put("index.number_of_shards",
		 * 1).put("index.number_of_replicas", 0).build());
		 *
		 * request.mapping(Constants.EVENT_MAPPING_TYPE, mapping, XContentType.JSON);
		 * esClient.indices().createAsync(request, RequestOptions.DEFAULT, new
		 * ActionListener<CreateIndexResponse>() {
		 *
		 * @Override public void onResponse(CreateIndexResponse createIndexResponse) {
		 * boolean ack = createIndexResponse.isAcknowledged(); boolean shardsAck =
		 * createIndexResponse.isShardsAcknowledged();
		 *
		 * LOG.info("Index created: " + createIndexResponse.index());
		 * LOG.info("Node acknowledgement: " + ack + ", Shards acknowledgement: " +
		 * shardsAck); }
		 *
		 * @Override public void onFailure(Exception e) {
		 * LOG.info("Failed to create index: " + indexName); LOG.info(e.getMessage(),
		 * e); } });
		 */

	}

	private boolean indexExists(String index) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		String url = Constants.EMPTY_STRING;
		if (esClient.isLessThanSixPointEight()) {
			url = "http://" + ElasticSearchClientConfiguration.HOST + ":" + ElasticSearchClientConfiguration.PORT_ONE
					+ "//" + index;
		} else {
			url = "http://" + ElasticSearchClientConfiguration.HOST + ":" + ElasticSearchClientConfiguration.PORT_ONE
					+ "//" + index + "?include_type_name=true";
		}
		try {
			ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, null, String.class);
			if (response.getStatusCode().equals(HttpStatus.OK)) {
				return true;
			} else
				return false;
		} catch (Exception e) {
			return false;
		}
		/*
		 * GetIndexRequest request = new GetIndexRequest(); request.indices(index);
		 *
		 * try { return esClient.indices().exists(request, RequestOptions.DEFAULT); }
		 * catch (IOException e) { LOG.error(e.getMessage(), e); } return false; }
		 */
	}

	private void setMapping() {
		if (mapping.isEmpty()) {
			try {
				if (esClient.isLessThanSixPointEight())
					mapping = new String(Files.readAllBytes(Paths.get("/opt/fault-service/conf/mapping.json")));
				else
					mapping = new String(Files.readAllBytes(Paths.get("/opt/fault-service/conf/mapping_es_gt68.json")));
			} catch (IOException e) {
				LOG.error(e.getMessage());
			}
		}
	}
}
