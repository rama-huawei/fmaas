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

package org.xgvela.cnf.kafka;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import org.xgvela.cnf.Constants;
import org.xgvela.cnf.util.ESUtil;
import org.xgvela.cnf.util.JsonUtil;

@Component
public class EventListener {

	private static Logger LOG = LogManager.getLogger(EventListener.class);

	@Autowired
	JsonUtil jsonUtil;

	@Autowired
	ESUtil esUtil;

	@Autowired
	KafkaListenerEndpointRegistry registry;

	@KafkaListener(topics = Constants.KAFKA_TOPIC, containerFactory = "kafkaListenerContainerFactory")
	private void listen(@Payload String record, @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition,
			@Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) String receivedMessageKey) {

		try {
			LOG.debug("Event received on Partition - " + partition + ", Message - " + record + ", Key - "
					+ receivedMessageKey + ", Topic: " + Constants.KAFKA_TOPIC);
		} catch (Exception e) {
			LOG.error("Error occured while sending logs", e);
		}
		if (receivedMessageKey.startsWith("ALERT")) {
			jsonUtil.parsePrometheusAlerts(record);
		} else {
			esUtil.processMessage(record);
		}
	}

	public void listenerPause() {
		registry.getListenerContainer(Constants.LISTENER_CONTAINER_ID).pause();
		LOG.debug("Listener paused for topic EVENT");
	}

	public void listenerResume() {
		registry.getListenerContainer(Constants.LISTENER_CONTAINER_ID).resume();
		LOG.debug("Listener resumed for topic EVENT");
	}

}
