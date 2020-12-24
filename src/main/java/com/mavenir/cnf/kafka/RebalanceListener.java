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

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.kafka.listener.ConsumerAwareRebalanceListener;
import org.springframework.stereotype.Component;

@Component
public class RebalanceListener implements ConsumerAwareRebalanceListener {

	private static Logger LOG = LogManager.getLogger(RebalanceListener.class);

	private static String currentPartitions = "";
	public static CopyOnWriteArrayList<Integer> assignedPartitions = new CopyOnWriteArrayList<>();

	public void onPartitionsRevokedBeforeCommit(Consumer<?, ?> consumer, Collection<TopicPartition> partitions) {
		LOG.debug("---onPartitionsRevokedBeforeCommit---");
		consumer.commitAsync();
	}

	public void onPartitionsRevokedAfterCommit(Consumer<?, ?> consumer, Collection<TopicPartition> partitions) {
		LOG.debug("---onPartitionsRevokedAfterCommit---");
		consumer.commitAsync();
	}

	public void onPartitionsAssigned(Consumer<?, ?> consumer, Collection<TopicPartition> partitions) {

		currentPartitions = "";
		assignedPartitions.clear();

		for (TopicPartition topicPartition : partitions) {
			currentPartitions += String.valueOf(topicPartition.partition()) + ",";
			assignedPartitions.add(topicPartition.partition());
		}

		LOG.debug("---onPartitionsAssigned--- " + currentPartitions);
		consumer.commitAsync();
	}
}
