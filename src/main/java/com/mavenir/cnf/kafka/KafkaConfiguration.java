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

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import org.xgvela.cnf.Constants;

@Configuration
@EnableKafka
public class KafkaConfiguration {

	private static Logger LOG = LogManager.getLogger(KafkaConfiguration.class);

	public static final String bootstrapServers = System.getenv(Constants.KAFKA_SVC_FQDN);

	private static final String groupId = "faultservice";

	@Autowired
	RebalanceListener consumerRebalanceListener;

	@Bean
	public Map<String, Object> consumerConfigs() {
		Map<String, Object> props = new HashMap<String, Object>();
		LOG.debug("Using Kafka bootstrap servers: " + bootstrapServers);
		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		return props;
	}

	@Bean
	public ConsumerFactory<String, String> consumerFactory() {
		return new DefaultKafkaConsumerFactory<String, String>(consumerConfigs(), new StringDeserializer(),
				new StringDeserializer());
	}

	@Bean
	public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {

		ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<String, String>();
		factory.setConsumerFactory(consumerFactory());
		factory.getContainerProperties().setConsumerRebalanceListener(consumerRebalanceListener);

		return factory;
	}

	@Bean
	public KafkaTemplate<String, String> kafkaTemplate() {
		return new KafkaTemplate<String, String>(producerFactory());
	}

	@Bean
	public ProducerFactory<String, String> producerFactory() {
		return new DefaultKafkaProducerFactory<String, String>(producerConfigs());
	}

	@Bean
	public Map<String, Object> producerConfigs() {
		Map<String, Object> props = new HashMap<String, Object>();
		LOG.debug("Using Kafka bootstrap servers: " + bootstrapServers);
		props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		return props;
	}

	public ArrayList<String> consumerByOffset(long a, long b) {
		String topic = "FMAASEVENTS";
		TopicPartition tp = new TopicPartition(topic, 0);
		ArrayList<String> records = new ArrayList<>();
		try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerConfigs())) {
			consumer.subscribe(Arrays.asList(topic), new ConsumerRebalanceListener() {
				@Override
				public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
				}

				@Override
				public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
					// Move to the desired start offset
					consumer.seek(tp, a);
				}
			});
			boolean run = true;
			while (run) {
				ConsumerRecords<String, String> crs = consumer.poll(Duration.ofMillis(a));
				for (ConsumerRecord<String, String> record : crs) {
					if(record.offset()>=a && record.offset()<=b )
					records.add(record.value());
					if (record.offset() >= b) {
						// Reached the end offsey, stop consuming
						run = false;
						break;
					}
				}
			}
			return records;
		}
	}
}
