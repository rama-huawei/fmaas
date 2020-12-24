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

package org.xgvela.cnf;

import java.net.URI;

public class Constants {
	public static final String EVENT_MAPPING_TYPE = "event";
	public static final String EVENT_PREFIX = "events-";
	public static final String ALARM_PREFIX = "alarms-";
	public static final String EVT_CMAP_SUFFIX = "-eventdef-cfg";
	public static final String EVENT_ID = "eventId";
	public static final String EVENT_TS = "eventTime";
	public static final String SOURCE_ID = "sourceId";
	public static final String SOURCE_NAME = "sourceName";
	public static final String CONTAINER_ID = "containerId";
	public static final String POD_ID = "podId";
	public static final String SRC_HEADERS = "sourceHeaders";
	public static final String NW_FN = "nf";
	public static final String NW_FN_TYPE = "nfType";
	public static final String NW_FN_PREFIX = "nfPrefix";
	public static final String MICROSERVICE = "microservice";
	public static final String COMMON_EVT_HDR = "commonEventHeader";
	public static final String DOMAIN = "domain";
	public static final String EVENT_TYPE = "eventType";
	public static final String EVENT_NAME = "eventName";
	public static final String START_EPOCH = "startEpochMillis";
	public static final String LAST_EPOCH = "lastEpochMillis";
	public static final String REP_ID = "reportingEntityId";
	public static final String REP_NAME = "reportingEntityName";
	public static final String SEQUENCE = "sequence";
	public static final String FAULT_FIELDS = "faultFields";
	public static final String ALARM_ADD_INFO = "alarmAdditionalInformation";
	public static final String ALARM_CONDITION = "alarmCondition";
	public static final String ALARM_INTFC = "alarmInterfaceA";
	public static final String EVENT_SEVERITY = "eventSeverity";
	public static final String EVENT_CATEGORY = "eventCategory";
	public static final String EVENT_SRC_TYPE = "eventSourceType";
	public static final String SPECIFIC_PRB = "specificProblem";
	public static final String FAULT_FIELDS_VER = "faultFieldsVersion";
	public static final String VF_STATUS = "vfStatus";
	public static final String VERSION = "version";
	public static final String ADD_INFO = "additionalInfo";
	public static final String MGD_OBJ = "managedObject";
	public static final String PRIORITY = "priority";
	public static final String ANNOTATIONS = "annotations";
	public static final String NAMESPACE = "namespace";
	public static final String STARTS_AT = "startsAt";
	public static final String ENDS_AT = "endsAt";
	public static final String EVENTS = "events";
	public static final String EVENT_DEF = "eventDefinition";
	public static final String PRB_CAUSE = "probableCause";
	public static final String ROOT_CAUSE = "rootCauseIndicator";
	public static final String TREND_IND = "trendIndication";
	public static final String CORR_NOTIF = "correlatedNotifications";
	public static final String ADD_TXT = "additionalText";
	public static final String PRO_REP_ACT = "proposedRepairAction";
	public static final String ORIGIN_TYPE = "originType";
	public static final String STATUS = "status";
	public static final String SEVERITY_CODE = "severityCode";
	public static final String MESSAGE = "message";
	public static final String ACTIVITY = "ACTIVITY_FMaaS_";
	public static final String EVENT = "Notification: ";
	public static final String INIT = "Initialization: ";
	public static final String SVC_VERSION = "svcVersion";
	public static final String KAFKA_TOPIC = "EVENT";
	public static final String EMPTY_STRING = "";
	public static final String EVT_COMMON = "eventCommon";
	public static final String CLEAR_COMMENT = "clearComment";
	public static final String CLEAR_KEY = "clearKey";
	public static final String CLEAR_VALUES = "clearValues";
	public static final String CLEAR_STATUS = "clearStatus";
	public static final String TIMESTAMP = "clearTimestamp";
	public static final String TCA_FIELDS = "tcaFields";
	public static final String PERCEIVED_SEVERITY = "perceivedSeverity";
	public static final String VES_EVENT_LIS_VERSION = "vesEventListenerVersion";
	public static final String ALERT_ACTION = "alertAction";
	public static final String ALERT_DES = "alertDescription";
	public static final String ALERT_TYPE = "alertType";
	public static final String EVT_START_TS = "eventStartTimestamp";
	public static final String COLLECTION_TS = "collectionTimestamp";
	public static final String TH_CR_FLD_VERSION = "thresholdCrossingFieldsVersion";
	public static final String ACK_STATE = "ackstate";
	public static final String COMMENT = "comment";
	public static final String NOTIFICATION_FIELDS = "notificationFields";
	public static final String STATE_CH_DEF = "stateChangeDefinition";
	public static final String CHANGE_IDENTIFIER = "changeIdentifier";
	public static final String OLD_STATE = "oldState";
	public static final String NEW_STATE = "newState";
	public static final String CHANGE_TYPE = "changeType";
	public static final String STATE_CHANGE = "stateChange";
	public static final String NOTI_FIELDS_VERSION = "notificationFieldsVersion";
	public static final String ADD_PARAM = "additionalParameters";
	public static final String IN_SERVICE = "inService";
	public static final String XGVELA_ID = "xgvelaId";
	public static final String CORRELATION_ID = "correlationId";
	public static final String ME_ID = "meId";
	public static final String NW_ID = "nfId";
	public static final String NF_SERVICE_ID = "nfServiceId";
	public static final String NF_SERVICE_INSTANCE_ID = "nfServiceInstanceId";
	public static final String FMAAS_EVENTS = "FMAASEVENTS";
	public static final String LISTENER_CONTAINER_ID = "org.springframework.kafka.KafkaListenerEndpointContainer#0";
	public static final String NF_NAMING_CODE = "nfNamingCode";
	public static final String NFC_NAMING_CODE = "nfcNamingCode";
	public static final String NF_SVC_TYPE = "nfServiceType";
	public static final String LOCAL_EVT_NAME = "localEventName";
	public static final String VENDOR_ID = "vendorId";
	public static final String DN_PREFIX = "DN_PREFIX";
	public static final String ADD_FIELDS = "additionalFields";
	public static final String MGMT_VERSION = "MGMT_VERSION";
	public static final String PAAS_NS = "PAAS_NS";
	public static final String CAAS_NS = "CAAS_NS";
	public static final String K8S_NAMESPACE = "K8S_NAMESPACE";
	public static final String K8S_POD_ID = "K8S_POD_ID";
	public static final String ANNOTATION_TMAAS ="xgvela.com/tmaas";
	public static final String REPLAY = "replay";
	public static final String ES_PORT = "ES_PORT";
	public static final String ES_SCHEME = "ES_SCHEME";
	public static final String ES_HOST = "ES_HOST";
	public static final String KAFKA_SVC_FQDN = "KAFKA_SVC_FQDN";
	public static final String ES_VERSION = "ES_VERSION";
	public static final String ME_LABEL = "meLabel";
	public static final String NF_LABEL = "nfLabel";
	public static final String NF_SERVICE_LABEL = "nfServiceLabel";
	public static final String NF_SERVICE_INSTANCE_LABEL = "nfServiceInstanceLabel";
	public static final String RETRY_COUNT = "RETRY_COUNT";
	public static final String INIT_REPLAY_URL = "INIT_REPLAY_URL";
	public static final String REPLAY_COMPLETE = "replayComplete";
	public static final String REPLAY_RESET_URL = "REPLAY_RESET_URL";
	public static final String NF_CLASS = "nfClass";
	public static final String NF_TYPE = "nfType";
	public static final String LOCAL_NFID = "localNfID";
	public static final String ZK_FQDN = "ZK_FQDN";
	public static final String POD_UUID = "pod_uuid";
	public static final String UHN = "uhn";
	public static final String CNFC_UUID = "cnfc_uuid";
	public static final String SVC_TYPE_AFFECTED = "serviceTypeAffected";
	public static final String ALARM_HIERARCHY = "alarmHierarchy";
	public static final String CLASSIFICATION = "classification";
	public static final String ALARM_TYPE = "alarmType";




	public static enum ACK_STATE_ENUM {
		unacknowledged, acknowledged
	};

	public static enum GET_ALARMS_ENUM {
		allActiveAlarms, allActiveAndAcknowledgedAlarms, allActiveAndUnacknowledgedAlarms
	};

}
