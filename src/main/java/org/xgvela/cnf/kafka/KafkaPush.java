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
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.web.context.request.async.DeferredResult;

import org.xgvela.alarmmanagement.AlarmManagementHelper;
import org.xgvela.cnf.Constants;
import org.xgvela.dbadapter.DbAdaptor;

@Component
public class KafkaPush {

	private static Logger LOG = LogManager.getLogger(KafkaPush.class);

	@Autowired
	AlarmManagementHelper alarmManagementHelper;

	@Autowired
	private KafkaTemplate<String, String> kafkaTemplate;

	@Autowired
	DbAdaptor dbAdaptor;

	public DeferredResult<Boolean> pushToKafka(String payload, String messageKey) {
		LOG.info("Pushing alerts to Kafka Topic: " + Constants.KAFKA_TOPIC);

		final Message<String> message = MessageBuilder.withPayload(payload)
				.setHeader(KafkaHeaders.TOPIC, Constants.KAFKA_TOPIC)
				.setHeader(KafkaHeaders.MESSAGE_KEY, messageKey + "-TCA" ).build();

		DeferredResult<Boolean> flag = new DeferredResult<>();
		ListenableFuture<SendResult<String, String>> future = kafkaTemplate.send(message);

		future.addCallback(new ListenableFutureCallback<SendResult<String, String>>() {

			public void onSuccess(SendResult<String, String> result) {
				LOG.info("Sent message with offset=[" + result.getRecordMetadata().offset() + " and partition:"
						+ result.getRecordMetadata().partition() + "]");
				flag.setResult(true);
			}

			public void onFailure(Throwable ex) {
				LOG.info("Unable to send message due to : " + ex.getMessage());
				flag.setResult(false);
			}
		});
		return flag;
	}

	public DeferredResult<Boolean> pushToKafkaEvents(String payload, String domain, String localEventName,
			String eventName, String nfNamingCode, String nfcNamingCode, String localNfID, int sequence, boolean replay) {
		LOG.info("Pushing events to Kafka Topic: " + Constants.FMAAS_EVENTS);

		final Message<String> message = MessageBuilder.withPayload(payload)
				.setHeader(KafkaHeaders.TOPIC, Constants.FMAAS_EVENTS).setHeader(Constants.DOMAIN, domain)
				.setHeader(Constants.EVENT_NAME, eventName).setHeader(Constants.LOCAL_EVT_NAME, localEventName)
				.setHeader(Constants.NF_NAMING_CODE, nfNamingCode).setHeader(Constants.NFC_NAMING_CODE, nfcNamingCode)
				.setHeader(Constants.LOCAL_NFID, localNfID)
				.setHeader(Constants.REPLAY, AlarmManagementHelper.replayOngoing.toString()).build();

		DeferredResult<Boolean> flag = new DeferredResult<>();
		ListenableFuture<SendResult<String, String>> future = kafkaTemplate.send(message);

		future.addCallback(new ListenableFutureCallback<SendResult<String, String>>() {

			public void onSuccess(SendResult<String, String> result) {
				LOG.info("Sent message with offset=[" + result.getRecordMetadata().offset() + " and partition:"
						+ result.getRecordMetadata().partition() + "]");
				flag.setResult(true);
				/*if (DbAdaptor.storageEngine.equalsIgnoreCase("zookeeper") && !replay) {
					dbAdaptor.addSequenceToZK(sequence, result.getRecordMetadata().offset());
				}
				*/
			}

			public void onFailure(Throwable ex) {
				LOG.info("Unable to send message due to : " + ex.getMessage());
				flag.setResult(false);
			}
		});
		return flag;
	}

	public DeferredResult<Boolean> pushReplayEvents(String topic, String payload, Boolean replayComplete) {
		LOG.info("Pushing events to Kafka Topic: " + topic);

		final Message<String> message = MessageBuilder.withPayload(payload).setHeader(KafkaHeaders.TOPIC, topic)
				.setHeader(Constants.REPLAY_COMPLETE, replayComplete).build();

		DeferredResult<Boolean> flag = new DeferredResult<>();
		ListenableFuture<SendResult<String, String>> future = kafkaTemplate.send(message);

		future.addCallback(new ListenableFutureCallback<SendResult<String, String>>() {

			public void onSuccess(SendResult<String, String> result) {
				LOG.info("Sent message with offset=[" + result.getRecordMetadata().offset() + "]");
				flag.setResult(true);
			}

			public void onFailure(Throwable ex) {
				LOG.info("Unable to send message due to : " + ex.getMessage());
				flag.setResult(false);
			}
		});
		return flag;
	}

}
