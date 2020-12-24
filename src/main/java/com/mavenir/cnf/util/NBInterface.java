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

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.xgvela.cnf.Constants;
import org.xgvela.cnf.kafka.KafkaPush;
import org.xgvela.model.CommonEventHeader;
import org.xgvela.model.Event;
import org.xgvela.model.FaultFields;
import org.xgvela.model.MEF;
import org.xgvela.model.NotificationFields;
import org.xgvela.model.ThresholdCrossingAlertFields;
import org.xgvela.model.CommonEventHeader.Domain;

@Component
public class NBInterface {

	private static Logger LOG = LogManager.getLogger(NBInterface.class);

//	public static String nbiType = "REST";
//	public static String nbiUrl = "http://10.1.34.101:8087";

	@Autowired
	JsonUtil jsonUtil;

	@Autowired
	ESUtil esUtil;

	@Autowired
	KafkaPush kafkaPush;

	@Autowired
	RestTemplate restTemplate;

	@SuppressWarnings("unchecked")
	public void createMEFObject(Map<String, Object> eventMap) {

		// if (nbiType != null && nbiType.equalsIgnoreCase("REST")) {

		// LOG.info("NBI Type is enabled and marked as REST, creating NB Event
		// Object...");
		ObjectMapper objectMapper = jsonUtil.getMapper();
		Map<String, Object> thresholdCrossingAlertsFieldsMap = (Map<String, Object>) eventMap.get(Constants.TCA_FIELDS);
		ThresholdCrossingAlertFields thresholdCrossingAlertFields = objectMapper
				.convertValue(thresholdCrossingAlertsFieldsMap, ThresholdCrossingAlertFields.class);
		Map<String, Object> notificationFieldsMap = (Map<String, Object>) eventMap.get(Constants.NOTIFICATION_FIELDS);
		NotificationFields notificationFields = objectMapper.convertValue(notificationFieldsMap,
				NotificationFields.class);
		Map<String, Object> faultFieldsMap = (Map<String, Object>) eventMap.get(Constants.FAULT_FIELDS);
		FaultFields faultFields = objectMapper.convertValue(faultFieldsMap, FaultFields.class);
		Map<String, Object> commonEventHeaderMap = (Map<String, Object>) eventMap.get(Constants.COMMON_EVT_HDR);
		CommonEventHeader commonEventHeader = objectMapper.convertValue(commonEventHeaderMap, CommonEventHeader.class);

		Event event = null;
		String nfLabel = Constants.EMPTY_STRING;
		String localNfID = Constants.EMPTY_STRING;
		if ((commonEventHeaderMap.get(Constants.DOMAIN)).toString().equalsIgnoreCase((Domain.FAULT).toString())) {
			event = new Event(commonEventHeader, faultFields, null, null);
			if (faultFields.getAlarmAdditionalInformation().getAdditionalProperties().containsKey(Constants.NF_LABEL))
			nfLabel = faultFields.getAlarmAdditionalInformation().getAdditionalProperties().get(Constants.NF_LABEL)
					.toString();
		} else if ((commonEventHeaderMap.get(Constants.DOMAIN)).toString()
				.equalsIgnoreCase(Domain.THRESHOLD_CROSSING_ALERT.toString())) {
			event = new Event(commonEventHeader, null, thresholdCrossingAlertFields, null);
			if (thresholdCrossingAlertFields.getAdditionalFields().getAdditionalProperties()
					.containsKey(Constants.NF_LABEL))
				nfLabel = thresholdCrossingAlertFields.getAdditionalFields().getAdditionalProperties()
						.get(Constants.NF_LABEL).toString();

		} else if ((commonEventHeaderMap.get(Constants.DOMAIN)).toString()
				.equalsIgnoreCase(Domain.NOTIFICATION.toString())) {
			event = new Event(commonEventHeader, null, null, notificationFields);
			if(notificationFields.getAdditionalFields().getAdditionalProperties().containsKey(Constants.NF_LABEL))
			nfLabel = notificationFields.getAdditionalFields().getAdditionalProperties().get(Constants.NF_LABEL)
					.toString();
		}
		if (!nfLabel.isEmpty())
			localNfID = nfLabel.substring(nfLabel.lastIndexOf("=") + 1).trim();
		int sequence = commonEventHeader.getSequence();
		MEF mefEvent = new MEF(event);

		try {
			String mefJson = objectMapper.writeValueAsString(mefEvent);
			LOG.debug("VES:\n" + mefJson);
			if (commonEventHeaderMap.containsKey(Constants.LOCAL_EVT_NAME)) {
				sendToNBI(mefJson, commonEventHeaderMap.get(Constants.DOMAIN).toString(),
						commonEventHeaderMap.get(Constants.LOCAL_EVT_NAME).toString(),
						commonEventHeaderMap.get(Constants.EVENT_NAME).toString(),
						commonEventHeaderMap.get(Constants.NF_NAMING_CODE).toString(),
						commonEventHeaderMap.get(Constants.NFC_NAMING_CODE).toString(), localNfID,sequence,false);
			} else {
				sendToNBI(mefJson, commonEventHeaderMap.get(Constants.DOMAIN).toString(),
						commonEventHeaderMap.get(Constants.EVENT_NAME).toString(),
						commonEventHeaderMap.get(Constants.EVENT_NAME).toString(),
						commonEventHeaderMap.get(Constants.NF_NAMING_CODE).toString(),
						commonEventHeaderMap.get(Constants.NFC_NAMING_CODE).toString(), localNfID,sequence,false);
			}
		} catch (JsonProcessingException e) {
			LOG.error(e.getMessage());
		}
		// return (int) commonEventHeaderMap.get(Constants.SEQUENCE);
		// }
		// return 0;
	}

	public void sendToNBI(String mefJson, String domain, String localEventName, String eventName, String nfNamingCode,
			String nfcNamingCode, String localNfID, int sequence, boolean replay) {
		kafkaPush.pushToKafkaEvents(mefJson, domain, localEventName, eventName, nfNamingCode, nfcNamingCode, localNfID,sequence,replay);
		/*
		 * if (nbiUrl == null || nbiUrl.isEmpty()) { LOG.
		 * error("NBI is enabled as REST but NBI URL is not configured; unable to send to NBI"
		 * ); return; }
		 *
		 * HttpHeaders headers = new HttpHeaders();
		 * headers.setContentType(MediaType.APPLICATION_JSON); HttpEntity<String> entity
		 * = new HttpEntity<String>(mefJson, headers);
		 *
		 * LOG.info("Sending Event to URL: " + nbiUrl);
		 *
		 * try { ResponseEntity<String> response = restTemplate.exchange(nbiUrl,
		 * HttpMethod.POST, entity, String.class); LOG.debug(response); } catch
		 * (Exception e) { LOG.error(e.getMessage()); }
		 */
	}
}
