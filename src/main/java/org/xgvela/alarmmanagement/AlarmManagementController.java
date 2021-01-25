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
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import com.fasterxml.jackson.databind.JsonNode;
import org.xgvela.cnf.Constants;
import org.xgvela.cnf.Constants.ACK_STATE_ENUM;
import org.xgvela.cnf.Constants.GET_ALARMS_ENUM;

@RestController
public class AlarmManagementController {
	@Autowired
	AlarmManagementHelper alarmManagementHelper;

	private static Logger LOG = LogManager.getLogger(AlarmManagementController.class);

	@GetMapping("/api/v1/fmaas/alarms/$alarmsCount")
	public @ResponseBody ResponseEntity<String> getAlarmCount(@RequestParam(required = false) String alarmAckState) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		String id = idValue(alarmAckState);
		if (id.equals(Constants.EMPTY_STRING))
			LOG.info("Getting Alarm Count for allActiveAlarms: ");
		else
			LOG.info("Getting Alarm Count for " + id + ": ");
		return new ResponseEntity<String>(alarmManagementHelper.getAlarmCount(id), headers, HttpStatus.OK);
	}

	@GetMapping("/api/v1/fmaas/alarms")
	public @ResponseBody ResponseEntity<String> getAlarms(@RequestParam(required = false) String alarmAckState) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		String id = idValue(alarmAckState);
		if (id.equals(Constants.EMPTY_STRING))
			LOG.info("Getting Alarms for allActiveAlarms: ");
		else
			LOG.info("Getting Alarms Count for " + id + ": ");
		return new ResponseEntity<String>(alarmManagementHelper.getAlarms(id), headers, HttpStatus.OK);
	}

	@PostMapping("/api/v1/fmaas/alarms")
	public @ResponseBody ResponseEntity<String> addMultipleComment(@RequestParam ArrayList<String> alarmId,
			@RequestBody JsonNode comment) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		LOG.info("Recieved addMultipleComment request with JSON data: " + comment);
		if (AlarmManagementHelper.replayOngoing)
			return new ResponseEntity<String>(alarmManagementHelper.replayOngoing(), headers, HttpStatus.OK);
		JsonNode strComment = null;
		if (comment.has("data")) {
			strComment = comment.get("data");
			if (strComment.has("commentTime") && strComment.has("commentText") && strComment.has("commentUserId")
					&& strComment.has("commentSystemId")) {
				Map<String, String> commentMap = getJsonAsMap(strComment);
				String response = alarmManagementHelper.addMultipleComment(alarmId, commentMap);
				if (response.contains("error")) {
					return new ResponseEntity<String>(response, headers, HttpStatus.OK);
				} else
					return new ResponseEntity<String>(comment.toPrettyString(), headers, HttpStatus.OK);
			} else
				return new ResponseEntity<String>("Invalid Data in JSON" + strComment, headers, HttpStatus.OK);

		} else
			return new ResponseEntity<String>("Invalid Json" + strComment, headers, HttpStatus.OK);

	}

	@PostMapping("/api/v1/fmaas/alarms/{alarmId}/comment")
	public @ResponseBody ResponseEntity<String> addComment(@PathVariable(value = "alarmId") String alarmId,
			@RequestBody JsonNode comment) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		LOG.info("Recieved addComment request for alarmId " + alarmId + "with JSON data: " + comment);
		if (AlarmManagementHelper.replayOngoing)
			return new ResponseEntity<String>(alarmManagementHelper.replayOngoing(), headers, HttpStatus.OK);
		JsonNode strComment = null;
		if (comment.has("data")) {
			strComment = comment.get("data");
			if (strComment.has("commentTime") && strComment.has("commentText") && strComment.has("commentUserId")
					&& strComment.has("commentSystemId")) {
				Map<String, String> commentMap = getJsonAsMap(strComment);
				if (alarmManagementHelper.addComment(alarmId, commentMap).contains("error"))
					return new ResponseEntity<String>(alarmManagementHelper.addComment(alarmId, commentMap), headers,
							HttpStatus.OK);
				else
					return new ResponseEntity<String>(comment.toPrettyString(), headers, HttpStatus.OK);
			} else
				return new ResponseEntity<String>("Invalid Data in JSON" + strComment, headers, HttpStatus.OK);

		} else
			return new ResponseEntity<String>("Invalid Json" + strComment, headers, HttpStatus.OK);
	}

	private Map<String, String> getJsonAsMap(JsonNode strComment) {
		Map<String, String> commentMap = new HashMap<String, String>();
		commentMap.put("commentTime", strComment.get("commentTime").asText());
		commentMap.put("commentText", strComment.get("commentText").asText());
		commentMap.put("commentUserId", strComment.get("commentUserId").asText());
		commentMap.put("commentSystemId", strComment.get("commentSystemId").asText());
		return commentMap;
	}

	@PatchMapping("/api/v1/fmaas/alarms/{alarmId}")
	public @ResponseBody ResponseEntity<String> addPatch(@PathVariable(value = "alarmId") String alarmId,
			@RequestBody JsonNode patch) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		LOG.info("Recieved addPatch request for alarmId " + alarmId + "with JSON data: " + patch);
		if (AlarmManagementHelper.replayOngoing)
			return new ResponseEntity<String>(alarmManagementHelper.replayOngoing(), headers, HttpStatus.OK);
		if (patch.has("ackstate") && patch.has("ackSystemId") && patch.has("ackUserId")) {
			Map<String, String> ackState = getJsonAsAckMap(patch);
			if (!getAckState(patch.get("ackstate").asText()).equals(Constants.EMPTY_STRING))
				return new ResponseEntity<String>(alarmManagementHelper.addAckState(alarmId, ackState), headers,
						HttpStatus.OK);
			else
				return new ResponseEntity<String>("Invalid Patch", HttpStatus.OK);
		} else if (patch.has("clearUserId") && patch.has("clearSystemId") && patch.has("perceivedSeverity")) {
			if (patch.get("perceivedSeverity").asText("").equals("cleared")) {
				return new ResponseEntity<String>(alarmManagementHelper.clearAlarm(alarmId, patchAsClearMap(patch)),
						headers, HttpStatus.OK);
			} else
				return new ResponseEntity<String>("Invalid Patch", headers, HttpStatus.OK);
		}
		return new ResponseEntity<String>("Invalid Patch", headers, HttpStatus.OK);

	}

	private Map<String, String> patchAsClearMap(JsonNode patch) {
		Map<String, String> clearMap = new HashMap<String, String>();
		clearMap.put("clearUserId", patch.get("clearUserId").asText());
		clearMap.put("clearSystemId", patch.get("clearSystemId").asText());
		clearMap.put("perceivedSeverity", patch.get("perceivedSeverity").asText());
		return clearMap;
	}

	private Map<String, String> getJsonAsAckMap(JsonNode patch) {
		Map<String, String> ackMap = new HashMap<String, String>();
		ackMap.put("ackstate", patch.get("ackstate").asText());
		ackMap.put("ackSystemId", patch.get("ackSystemId").asText());
		ackMap.put("ackUserId", patch.get("ackUserId").asText());
		return ackMap;
	}

	@PatchMapping("/api/v1/fmaas/alarms")
	public @ResponseBody ResponseEntity<String> addMultiplePatch(@RequestParam ArrayList<String> alarmId,
			@RequestBody JsonNode patch) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		if (AlarmManagementHelper.replayOngoing)
			return new ResponseEntity<String>(alarmManagementHelper.replayOngoing(), headers, HttpStatus.OK);
		LOG.info("Recieved addMultiplePatch request with JSON data: " + patch);
		if (patch.has("ackstate") && patch.has("ackSystemId") && patch.has("ackUserId")) {
			Map<String, String> ackState = getJsonAsAckMap(patch);
			if (!getAckState(patch.get("ackstate").asText()).equals(Constants.EMPTY_STRING))
				return new ResponseEntity<String>(alarmManagementHelper.addMultipleAckState(alarmId, ackState), headers,
						HttpStatus.OK);
			else
				return new ResponseEntity<String>("Invalid Patch", headers, HttpStatus.OK);
		} else if (patch.has("clearUserId") && patch.has("clearSystemId") && patch.has("perceivedSeverity")) {
			if (patch.get("perceivedSeverity").asText("").equals("cleared")) {
				return new ResponseEntity<String>(
						alarmManagementHelper.multipleClearAlarm(alarmId, patchAsClearMap(patch)), headers,
						HttpStatus.OK);
			} else
				return new ResponseEntity<String>("Invalid Patch", headers, HttpStatus.OK);
		}
		return new ResponseEntity<String>("Invalid Patch", headers, HttpStatus.OK);

	}


	 // multiple requests for events replay to be discussed and implemented later
	@GetMapping("/api/v1/fmaas/events/replay/{start}")
	public @ResponseBody ResponseEntity<String> geteventsResync(@PathVariable(value = "start") int start) {
		LOG.info("Recieved eventsReplay request from sequence " + start);
		return alarmManagementHelper.eventsReplayCollector(1,start, 0);
	}

	@GetMapping("/api/v1/fmaas/events/replay/{start}/{end}")
	public @ResponseBody ResponseEntity<String> geteventsResync(@PathVariable(value = "start") int start,
			@PathVariable(value = "end") int end) {
		LOG.info("Recieved eventsReplay request from sequence " + start + " to " + end);
		return alarmManagementHelper.eventsReplayCollector(1,start, end);
	}


	@GetMapping("/api/v2/fmaas/events/replay/{collectorId}/{start}")
	public @ResponseBody ResponseEntity<String> geteventsResyncCollector(@PathVariable(value = "start") int start,
			@PathVariable(value = "collectorId") int collectorId) {
		LOG.info("Recieved eventsReplay request for collectorId:" + collectorId + " from sequence " + start);
		return alarmManagementHelper.eventsReplayCollector(collectorId, start, 0);
	}

	@GetMapping("/api/v2/fmaas/events/replay/{collectorId}/{start}/{end}")
	public @ResponseBody ResponseEntity<String> geteventsResyncCollector(
			@PathVariable(value = "collectorId") int collectorId, @PathVariable(value = "start") int start,
			@PathVariable(value = "end") int end) {
		LOG.info("Recieved eventsReplay request for collectorId: " + collectorId + " from sequence " + start + " to "
				+ end);
		return alarmManagementHelper.eventsReplayCollector(collectorId, start, end);
	}

	private String getAckState(String ackState) {
		String id = Constants.EMPTY_STRING;
		for (ACK_STATE_ENUM c : Constants.ACK_STATE_ENUM.values()) {
			if (c.toString().equals(ackState)) {
				id = ackState;
			}
		}
		return id;
	}

	private String idValue(String alarmEnum) {
		String id = Constants.EMPTY_STRING;
		for (GET_ALARMS_ENUM c : Constants.GET_ALARMS_ENUM.values()) {
			if (c.toString().equals(alarmEnum)) {
				id = alarmEnum;
			}
		}
		return id;
	}

}
