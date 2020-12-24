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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.context.request.async.DeferredResult.DeferredResultHandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.xgvela.cnf.kafka.KafkaPush;
import org.xgvela.cnf.util.ESUtil;

@RestController
public class Endpoint {

	private static Logger LOG = LogManager.getLogger(Endpoint.class);

	@Autowired
	KafkaPush kafkaPush;

	@Autowired
	ESUtil esUtil;

	@PostMapping("/v1/alerts/")
	public @ResponseBody ResponseEntity<String> postAlerts(@RequestBody String alerts) {

		try {
			LOG.info("Alerts received: " + alerts);
		} catch (Exception e) {
			LOG.error("Error occured while sending logs", e);
		}
		DeferredResult<Boolean> flag = kafkaPush.pushToKafka(alerts, "ALERT");
		flag.setResultHandler(new DeferredResultHandler() {

			@Override
			public void handleResult(Object result) {
				if (result.equals(Boolean.FALSE)) {
					LOG.info("Unable to send message to Kafka.");
				} else if (result.equals(Boolean.TRUE)) {
					LOG.info("Message successfully sent to Kafka.");
				} else {
					LOG.info(result.toString());
				}
			}
		});
		return ResponseEntity.ok().build();
	}

	@GetMapping("/api/v1/alarms/clearActiveAlarm")
	public @ResponseBody ResponseEntity<String> clearAlarm(@RequestBody String requestBody)
			throws JsonProcessingException {

		LOG.info("Clear Alarm request received: " + requestBody);
		return new ResponseEntity<String>(esUtil.clearHandler(requestBody), HttpStatus.OK);
	}

}
