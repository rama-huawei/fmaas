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

package org.xgvela.dbadapter.zk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.zookeeper.KeeperException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.xgvela.cnf.Constants;
import org.xgvela.cnf.kafka.KafkaConfiguration;
import org.xgvela.cnf.util.DateUtil;
import org.xgvela.cnf.util.ESUtil;
import org.xgvela.model.AdditionalParameter;
import org.xgvela.model.AlarmAdditionalInformation;
import org.xgvela.model.ThresholdCrossingAlertFields;
import org.xgvela.model.CommonEventHeader.Domain;

@Component
public class ZKUtil {

	private static Logger LOG = LogManager.getLogger(ZKUtil.class);

	@Autowired
	ZKManager zkManager;

	@Autowired
	ESUtil esUtil;

	@Autowired
	DateUtil dateUtil;

	public void createIntialNode() {
		zkManager.initialize();
		/*try {
			zkManager.create("/events", "".getBytes());
		} catch (KeeperException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
		try {
			zkManager.create("/alarms", "".getBytes());
		} catch (KeeperException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public int getSequence() {
		List<String> b = new ArrayList<>();
		List<Integer> ab = new ArrayList<>();
		try {
			b = zkManager.getListOFChildren("/events", false);
			if (b.isEmpty())
				return 0;
			for (String i : b)
				ab.add(Integer.valueOf(i));
			Collections.sort(ab);
			return Integer.valueOf(ab.get(b.size() - 1));
		} catch (KeeperException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 0;

	}

	public Map<String, String> getSeverityCodeToCount(String id) {
		HashMap<String, String> severityCodeToCount = new HashMap<>();
		List<String> b = getAlarmIdList();
		try {
			if (b.isEmpty())
				return severityCodeToCount;
			for (String s : b) {
				boolean ack = checkIfAck(s);
				if (id.equals(Constants.GET_ALARMS_ENUM.allActiveAndUnacknowledgedAlarms.toString())) {
					if (ack)
						continue;
				} else if (id.equals(Constants.GET_ALARMS_ENUM.allActiveAndAcknowledgedAlarms.toString())) {
					if (!ack)
						continue;
				}
				int severity = Integer.valueOf(
						(String) zkManager.getZNodeData("/alarms/" + s + "/eventDefinition/severityCode", false));
				if (severityCodeToCount.containsKey(String.valueOf(severity))) {
					int count = Integer.valueOf(severityCodeToCount.get(String.valueOf(severity))) + 1;
					severityCodeToCount.put(String.valueOf(severity), String.valueOf(count));
				} else {
					severityCodeToCount.put(String.valueOf(severity), String.valueOf(1));
				}
			}
		} catch (KeeperException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return severityCodeToCount;
	}

	public List<String> getAlarmIdList() {
		List<String> b = new ArrayList<>();
		try {
			b = zkManager.getListOFChildren("/alarms", false);
		} catch (KeeperException |

				InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return b;
	}

	public String getAlarmSequence(String s) {
		try {
			return (String) zkManager.getZNodeData("/alarms/" + s + "/commonEventHeader/sequence", false);
		} catch (KeeperException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return Constants.EMPTY_STRING;
	}

	public String getOffset(String sequence) {
		try {
			if (zkManager.ifExists("/events/" + sequence))
				return (String) zkManager.getZNodeData("/events/" + sequence, false);
		} catch (KeeperException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return Constants.EMPTY_STRING;

	}

	public boolean checkIfAck(String s) {
		try {
			if ((zkManager.getListOFChildren("/alarms/" + s + "/ackstate", false)).size() != 0) {

				if (((String) zkManager.getZNodeData("/alarms/" + s + "/ackstate/ackstate", false))
						.equalsIgnoreCase("acknowledged"))
					return true;
			}
		} catch (KeeperException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}

	public boolean addComment(String alarmId, Map<String, String> commentMap) {
		boolean check = zkManager.ifExists("/alarms/" + alarmId);
		if (check) {
			try {
				if (zkManager.ifExists("/alarms/" + alarmId + "/comment/commentText")) {
					zkManager.update("/alarms/" + alarmId + "/comment/commentText",
							commentMap.get("commentText").toString().getBytes());
					zkManager.update("/alarms/" + alarmId + "/comment/commentTime",
							commentMap.get("commentTime").toString().getBytes());
					zkManager.update("/alarms/" + alarmId + "/comment/commentUserId",
							commentMap.get("commentUserId").toString().getBytes());
					zkManager.update("/alarms/" + alarmId + "/comment/commentSystemId",
							commentMap.get("commentSystemId").toString().getBytes());
				} else {
					zkManager.create("/alarms/" + alarmId + "/comment/commentText",
							commentMap.get("commentText").toString().getBytes());
					zkManager.create("/alarms/" + alarmId + "/comment/commentTime",
							commentMap.get("commentTime").toString().getBytes());
					zkManager.create("/alarms/" + alarmId + "/comment/commentUserId",
							commentMap.get("commentUserId").toString().getBytes());
					zkManager.create("/alarms/" + alarmId + "/comment/commentSystemId",
							commentMap.get("commentSystemId").toString().getBytes());
				}
			} catch (KeeperException | InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		return check;
	}

	public boolean addAckState(String alarmId, Map<String, String> ackState) {
		boolean check = zkManager.ifExists("/alarms/" + alarmId);
		if (check) {
			try {
				if (zkManager.ifExists("/alarms/" + alarmId + "/ackstate/ackstate")) {
					zkManager.update("/alarms/" + alarmId + "/ackstate/ackstate",
							ackState.get("ackstate").toString().getBytes());
					zkManager.update("/alarms/" + alarmId + "/ackstate/ackSystemId",
							ackState.get("ackSystemId").toString().getBytes());
					zkManager.update("/alarms/" + alarmId + "/ackstate/ackUserId",
							ackState.get("ackUserId").toString().getBytes());

				} else {
					zkManager.create("/alarms/" + alarmId + "/ackstate/ackstate",
							ackState.get("ackstate").toString().getBytes());
					zkManager.create("/alarms/" + alarmId + "/ackstate/ackSystemId",
							ackState.get("ackSystemId").toString().getBytes());
					zkManager.create("/alarms/" + alarmId + "/ackstate/ackUserId",
							ackState.get("ackUserId").toString().getBytes());
				}
			} catch (KeeperException | InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		return check;
	}

	public boolean clearAlarmZK(String alarmId, Map<String, String> map) {
		boolean check = zkManager.ifExists("/alarms/" + alarmId);
		if (check) {
			/*
			 * String sequence = getAlarmSequence(alarmId); String offset =
			 * getOffset(sequence); ArrayList<String> documents = new ArrayList<>();
			 * KafkaConfiguration kf = new KafkaConfiguration();
			 * documents.addAll(kf.consumerByOffset(Long.parseLong(offset),
			 * Long.parseLong(offset))); ObjectMapper mapper = new ObjectMapper(); JsonNode
			 * eventNode = null; try { eventNode = mapper.readTree(documents.get(0)); }
			 * catch (JsonMappingException e) { LOG.error(e.getMessage()); } catch
			 * (JsonProcessingException e) { LOG.error(e.getMessage()); } Map<String,
			 * Object> event = mapper.convertValue(eventNode, new TypeReference<Map<String,
			 * Object>>() { });
			 */
			Map<String, Object> getExistingDoc = new HashMap<>();
			Map<String, Object> comEvtHdrMap = getCommonEventHeaderZK(alarmId);
			Map<String, Object> tcaFieldsMap = new HashMap<>();
			Map<String, Object> faultFieldsMap = new HashMap<>();
			if ((comEvtHdrMap.get(Constants.DOMAIN)).toString().equalsIgnoreCase((Domain.FAULT).toString())) {
				faultFieldsMap = getFaultFieldsZK(alarmId);
				faultFieldsMap.put(Constants.EVENT_SEVERITY, "CLEAR");
			} else if ((comEvtHdrMap.get(Constants.DOMAIN)).toString()
					.equalsIgnoreCase(Domain.THRESHOLD_CROSSING_ALERT.toString())) {
				tcaFieldsMap = getTCAFieldsZK(alarmId);
				tcaFieldsMap.put(Constants.EVENT_SEVERITY, "CLEAR");
				tcaFieldsMap.put(Constants.ALERT_ACTION, ThresholdCrossingAlertFields.AlertAction.CLEAR);
			}
			Byte severityCode = 6;
			updateSeverityCode(alarmId, Byte.toString(severityCode));
			Map<String, Object> eventDefinition = getEventDefZK(alarmId);
			Map<String, String> comment = new HashMap<String, String>();
			comment.put("commentUserId", map.get("clearUserId").toString());
			comment.put("commentSystemId", map.get("clearSystemId").toString());
			comment.put("commentText", "Manual Clear");
			comment.put("commentTime", dateUtil.getTimestamp());
			addComment(alarmId, comment);
			getExistingDoc.put("comment", comment);
			getExistingDoc.put(Constants.EVENT_DEF, eventDefinition);
			getExistingDoc.put(Constants.FAULT_FIELDS, faultFieldsMap);
			getExistingDoc.put(Constants.TCA_FIELDS, tcaFieldsMap);
			getExistingDoc.put(Constants.COMMON_EVT_HDR, comEvtHdrMap);
			esUtil.manualClear(getExistingDoc);

		}
		return check;
	}

	public void deleteAlarmZK(String id) {
		zkManager.deleteRecursive("/alarms/" + id);
	}

	public ArrayList<String> getAlarmsforPodId(String podId) {
		List<String> alarms = getAlarmIdList();
		ArrayList<String> alarmsWithPodId = new ArrayList<>();
		for (String i : alarms) {
			try {
				if (((String) zkManager.getZNodeData("/alarms/" + i + "/sourceHeaders/podId", false))
						.equalsIgnoreCase(podId))
					alarmsWithPodId.add(i);
			} catch (KeeperException | InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return alarmsWithPodId;
	}

	public String getNamespace(String s) {
		try {
			return (String) zkManager.getZNodeData("/alarms/" + s + "/sourceHeaders/" + Constants.NW_FN_PREFIX, false);
		} catch (KeeperException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return Constants.EMPTY_STRING;
	}

	public String getPodId(String s) {
		try {
			return (String) zkManager.getZNodeData("/alarms/" + s + "/sourceHeaders/" + Constants.POD_ID, false);
		} catch (KeeperException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return Constants.EMPTY_STRING;
	}

	public void updateSeverityCode(String id, String data) {
		try {
			zkManager.update("/alarms/" + id + "/" + Constants.EVENT_DEF + "/" + Constants.SEVERITY_CODE,
					data.getBytes());
		} catch (KeeperException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public Map<String, Object> getEventDefZK(String id) {
		Map<String, Object> eventDef = new HashMap<>();
		try {
			for (String s : zkManager.getListOFChildren("/alarms/" + id + "/" + Constants.EVENT_DEF, false)) {
				eventDef.put(s, zkManager.getZNodeData("/alarms/" + id + "/" + Constants.EVENT_DEF + "/" + s, false));
			}
		} catch (KeeperException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return eventDef;
	}

	public void deleteNodes() {
		try {
			List<String> event = zkManager.getListOFChildren("/alarms", false);
			for (String s : event) {
				if (dateUtil.checkIfLessThanRetained(dateUtil.epochToDate(zkManager.getCreateDate("/alarms/" + s)))) {
					Map<String, Object> getExistingDoc = new HashMap<>();
					Map<String, Object> comEvtHdrMap = getCommonEventHeaderZK(s);
					Map<String, Object> tcaFieldsMap = new HashMap<>();
					Map<String, Object> faultFieldsMap = new HashMap<>();
					if ((comEvtHdrMap.get(Constants.DOMAIN)).toString().equalsIgnoreCase((Domain.FAULT).toString())) {
						faultFieldsMap = getFaultFieldsZK(s);
						faultFieldsMap.put(Constants.EVENT_SEVERITY, "CLEAR");
					} else if ((comEvtHdrMap.get(Constants.DOMAIN)).toString()
							.equalsIgnoreCase(Domain.THRESHOLD_CROSSING_ALERT.toString())) {
						tcaFieldsMap = getTCAFieldsZK(s);
						tcaFieldsMap.put(Constants.EVENT_SEVERITY, "CLEAR");
						tcaFieldsMap.put(Constants.ALERT_ACTION, ThresholdCrossingAlertFields.AlertAction.CLEAR);
					}
					Byte severityCode = 6;
					updateSeverityCode(s, Byte.toString(severityCode));
					Map<String, Object> eventDefinition = getEventDefZK(s);
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
					LOG.debug("Deleting znode " + "/alarms/" + s);
					zkManager.deleteRecursive("/alarms/" + s);
				}
			}
			event = null;
		} catch (KeeperException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public Map<String, Object> getCommonEventHeaderZK(String id) {
		Map<String, Object> commonEventHeader = new HashMap<>();
		try {
			for (String s : zkManager.getListOFChildren("/alarms/" + id + "/" + Constants.COMMON_EVT_HDR, false)) {
				commonEventHeader.put(s,
						zkManager.getZNodeData("/alarms/" + id + "/" + Constants.COMMON_EVT_HDR + "/" + s, false));
			}
		} catch (KeeperException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return commonEventHeader;
	}

	public Map<String, Object> getFaultFieldsZK(String id) {
		Map<String, Object> faultFields = new HashMap<>();
		try {
			for (String s : zkManager.getListOFChildren("/alarms/" + id + "/" + Constants.FAULT_FIELDS, false)) {
				if (s.equalsIgnoreCase(Constants.ALARM_ADD_INFO)) {
					AlarmAdditionalInformation alarmAdditionalInfoList = new AlarmAdditionalInformation();
					for (String additionalInfo : zkManager.getListOFChildren(
							"/alarms/" + id + "/" + Constants.FAULT_FIELDS + "/" + Constants.ALARM_ADD_INFO, false)) {
						alarmAdditionalInfoList.setAdditionalProperty(additionalInfo,
								(String) zkManager.getZNodeData(
										"/alarms/" + id + "/" + Constants.FAULT_FIELDS + "/" + s + "/" + additionalInfo,
										false));
					}
					faultFields.put(Constants.ALARM_ADD_INFO, alarmAdditionalInfoList);
					continue;
				}
				faultFields.put(s,
						zkManager.getZNodeData("/alarms/" + id + "/" + Constants.FAULT_FIELDS + "/" + s, false));
			}
		} catch (KeeperException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return faultFields;
	}

	public Map<String, Object> getTCAFieldsZK(String id) {
		Map<String, Object> tcaFields = new HashMap<>();
		try {
			for (String s : zkManager.getListOFChildren("/alarms/" + id + "/" + Constants.TCA_FIELDS, false)) {
				if (s.equalsIgnoreCase(Constants.ADD_FIELDS)) {
					AlarmAdditionalInformation alarmAdditionalInfoList = new AlarmAdditionalInformation();
					for (String additionalInfo : zkManager.getListOFChildren(
							"/alarms/" + id + "/" + Constants.TCA_FIELDS + "/" + Constants.ADD_FIELDS, false)) {
						alarmAdditionalInfoList.setAdditionalProperty(additionalInfo,
								(String) zkManager.getZNodeData(
										"/alarms/" + id + "/" + Constants.TCA_FIELDS + "/" + s + "/" + additionalInfo,
										false));
					}
					tcaFields.put(Constants.ADD_FIELDS, alarmAdditionalInfoList);
					continue;
				}
				if (s.equalsIgnoreCase(Constants.ADD_PARAM)) {
					AdditionalParameter addParam = new AdditionalParameter();
					addParam.setCriticality(AdditionalParameter.Criticality.fromValue((String) zkManager.getZNodeData(
								"/alarms/" + id + "/" + Constants.TCA_FIELDS + "/" + s + "/criticality", false)));
					addParam.setThresholdCrossed((String) zkManager.getZNodeData(
								"/alarms/" + id + "/" + Constants.TCA_FIELDS + "/" + s + "/thresholdCrossed", false));
					org.xgvela.model.HashMap map = new org.xgvela.model.HashMap();
					map.setAdditionalProperty("NA", "NA");
					addParam.setHashMap(map);
					List<AdditionalParameter> addParamList = new ArrayList<AdditionalParameter>();
					addParamList.add(addParam);
					tcaFields.put(Constants.ADD_PARAM, addParamList);
					continue;
				}

				tcaFields.put(s, zkManager.getZNodeData("/alarms/" + id + "/" + Constants.TCA_FIELDS + "/" + s, false));
			}
		} catch (KeeperException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return tcaFields;
	}

}
