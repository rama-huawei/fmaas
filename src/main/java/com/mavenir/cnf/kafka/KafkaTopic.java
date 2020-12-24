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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

@Component
public class KafkaTopic {
	private static Logger LOG = LogManager.getLogger(KafkaTopic.class);

	public void createTopic(String topic) {
		Map<String, Object> props = new HashMap<String, Object>();
		props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaConfiguration.bootstrapServers);
		props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

		AdminClient adminClient = AdminClient.create(props);
		ListTopicsResult listTopics = adminClient.listTopics();
		Set<String> names = new HashSet<>();
		try {
			names = listTopics.names().get();
		} catch (InterruptedException | ExecutionException e) {
			LOG.error("Error occured while listing Kafka Topics", e);
		}
		boolean contains = names.contains(topic);
		if (!contains) {
			NewTopic newTopic = new NewTopic(topic, 1, (short) 1);

			List<NewTopic> newTopics = new ArrayList<NewTopic>();
			newTopics.add(newTopic);

			adminClient.createTopics(newTopics);
			LOG.info("Kafka topic " + topic + " created");
		} else {
			LOG.info("Kafka topic " + topic + " already exists");
		}
		adminClient.close();

	}

}
