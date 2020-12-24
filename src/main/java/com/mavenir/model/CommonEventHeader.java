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


package org.xgvela.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonValue;


/**
 * fields common to all events
 *
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "domain",
    "eventId",
    "eventName",
    "localEventName",
    "eventType",
    "lastEpochMillis",
    "nfcNamingCode",
    "nfNamingCode",
    "priority",
    "reportingEntityId",
    "reportingEntityName",
    "sequence",
    "sourceId",
    "sourceName",
    "startEpochMillis",
    "version",
    "vesEventListenerVersion"
})
public class CommonEventHeader implements Serializable
{

    /**
     * the eventing domain associated with the event
     * (Required)
     *
     */
    @JsonProperty("domain")
    @JsonPropertyDescription("the eventing domain associated with the event")
    private CommonEventHeader.Domain domain;
    /**
     * event key that is unique to the event source
     * (Required)
     *
     */
    @JsonProperty("eventId")
    @JsonPropertyDescription("event key that is unique to the event source")
    private String eventId;
    /**
     * unique event name
     * (Required)
     *
     */
    @JsonProperty("eventName")
    @JsonPropertyDescription("unique event name")
    private String eventName;
    /**
     * unique local event name
     * (Required)
     *
     */
    @JsonProperty("localEventName")
    @JsonPropertyDescription("unique local event name")
    private String localEventName;
    /**
     * for example - applicationNf, guestOS, hostOS, platform
     *
     */
    @JsonProperty("eventType")
    @JsonPropertyDescription("for example - applicationNf, guestOS, hostOS, platform")
    private String eventType;
    /**
     * the latest unix time aka epoch time associated with the event from any component--as milliseconds elapsed since 1 Jan 1970 not including leap seconds
     * (Required)
     *
     */
    @JsonProperty("lastEpochMillis")
    @JsonPropertyDescription("the latest unix time aka epoch time associated with the event from any component--as milliseconds elapsed since 1 Jan 1970 not including leap seconds")
    private long lastEpochMillis;
    /**
     *  3 character network function component type, aligned with vfc naming standards
     *
     */
    @JsonProperty("nfcNamingCode")
    @JsonPropertyDescription("3 character network function component type, aligned with vfc naming standards")
    private String nfcNamingCode;
    /**
     *  4 character network function type, aligned with nf naming standards
     *
     */
    @JsonProperty("nfNamingCode")
    @JsonPropertyDescription("4 character network function type, aligned with nf naming standards")
    private String nfNamingCode;
    /**
     * processing priority
     * (Required)
     *
     */
    @JsonProperty("priority")
    @JsonPropertyDescription("processing priority")
    private CommonEventHeader.Priority priority;
    /**
     * UUID identifying the entity reporting the event, for example an OAM VM; must be populated by the ATT enrichment process
     *
     */
    @JsonProperty("reportingEntityId")
    @JsonPropertyDescription("UUID identifying the entity reporting the event, for example an OAM VM; must be populated by the ATT enrichment process")
    private String reportingEntityId;
    /**
     * name of the entity reporting the event, for example, an EMS name; may be the same as sourceName
     * (Required)
     *
     */
    @JsonProperty("reportingEntityName")
    @JsonPropertyDescription("name of the entity reporting the event, for example, an EMS name; may be the same as sourceName")
    private String reportingEntityName;
    /**
     * ordering of events communicated by an event source instance or 0 if not needed
     * (Required)
     *
     */
    @JsonProperty("sequence")
    @JsonPropertyDescription("ordering of events communicated by an event source instance or 0 if not needed")
    private Integer sequence;
    /**
     * UUID identifying the entity experiencing the event issue; must be populated by the ATT enrichment process
     *
     */
    @JsonProperty("sourceId")
    @JsonPropertyDescription("UUID identifying the entity experiencing the event issue; must be populated by the ATT enrichment process")
    private String sourceId;
    /**
     * name of the entity experiencing the event issue
     * (Required)
     *
     */
    @JsonProperty("sourceName")
    @JsonPropertyDescription("name of the entity experiencing the event issue")
    private String sourceName;
    /**
     * the earliest unix time aka epoch time associated with the event from any component--as milliseconds elapsed since 1 Jan 1970 not including leap seconds
     * (Required)
     *
     */
    @JsonProperty("startEpochMillis")
    @JsonPropertyDescription("the earliest unix time aka epoch time associated with the event from any component--as milliseconds elapsed since 1 Jan 1970 not including leap seconds")
    private long startEpochMillis;
    /**
     * version of the event header
     * (Required)
     *
     */
    @JsonProperty("version")
    @JsonPropertyDescription("version of the event header")
    private CommonEventHeader.Version version;
    /**
     * version of the VES Event Listener API
     * (Required)
     *
     */
    @JsonProperty("vesEventListenerVersion")
    @JsonPropertyDescription("version of the VES Event Listener API")
    private CommonEventHeader.VesEventListenerVersion vesEventListenerVersion;
    private final static long serialVersionUID = 4350289155187402330L;

    /**
     * No args constructor for use in serialization
     *
     */
    public CommonEventHeader() {
    }

    /**
     *
     * @param sourceId
     * @param eventId
     * @param nfcNamingCode
     * @param reportingEntityId
     * @param eventType
     * @param priority
     * @param startEpochMillis
     * @param version
     * @param reportingEntityName
     * @param sequence
     * @param domain
     * @param eventName
     * @param localEventName
     * @param vesEventListenerVersion
     * @param lastEpochMillis
     * @param sourceName
     * @param nfNamingCode
     */
    public CommonEventHeader(CommonEventHeader.Domain domain, String eventId, String eventName, String localEventName, String eventType, long lastEpochMillis, String nfcNamingCode, String nfNamingCode, CommonEventHeader.Priority priority, String reportingEntityId, String reportingEntityName, Integer sequence, String sourceId, String sourceName, long startEpochMillis, CommonEventHeader.Version version, CommonEventHeader.VesEventListenerVersion vesEventListenerVersion) {
        super();
        this.domain = domain;
        this.eventId = eventId;
        this.eventName = eventName;
        this.localEventName = localEventName;
        this.eventType = eventType;
        this.lastEpochMillis = lastEpochMillis;
        this.nfcNamingCode = nfcNamingCode;
        this.nfNamingCode = nfNamingCode;
        this.priority = priority;
        this.reportingEntityId = reportingEntityId;
        this.reportingEntityName = reportingEntityName;
        this.sequence = sequence;
        this.sourceId = sourceId;
        this.sourceName = sourceName;
        this.startEpochMillis = startEpochMillis;
        this.version = version;
        this.vesEventListenerVersion = vesEventListenerVersion;
    }

    /**
     * the eventing domain associated with the event
     * (Required)
     *
     */
    @JsonProperty("domain")
    public CommonEventHeader.Domain getDomain() {
        return domain;
    }

    /**
     * the eventing domain associated with the event
     * (Required)
     *
     */
    @JsonProperty("domain")
    public void setDomain(CommonEventHeader.Domain domain) {
        this.domain = domain;
    }

    public CommonEventHeader withDomain(CommonEventHeader.Domain domain) {
        this.domain = domain;
        return this;
    }

    /**
     * event key that is unique to the event source
     * (Required)
     *
     */
    @JsonProperty("eventId")
    public String getEventId() {
        return eventId;
    }

    /**
     * event key that is unique to the event source
     * (Required)
     *
     */
    @JsonProperty("eventId")
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public CommonEventHeader withEventId(String eventId) {
        this.eventId = eventId;
        return this;
    }

    /**
     * unique event name
     * (Required)
     *
     */
    @JsonProperty("eventName")
    public String getEventName() {
        return eventName;
    }
    /**
     * unique event name
     * (Required)
     *
     */
    @JsonProperty("eventName")
    public void setEventName(String eventName) {
        this.eventName = eventName;
    }
    public CommonEventHeader withEventName(String eventName) {
        this.eventName = eventName;
        return this;
    }
    /**
     * unique local event name
     * (Required)
     *
     */
    @JsonProperty("localEventName")
    public String getLocalEventName() {
        return localEventName;
    }


    /**
     * unique local event name
     * (Required)
     *
     */
    @JsonProperty("localEventName")
    public void setLocalEventName(String localEventName) {
        this.localEventName = localEventName;
    }

    public CommonEventHeader withLocalEventName(String localEventName) {
        this.localEventName = localEventName;
        return this;
    }

    /**
     * for example - applicationNf, guestOS, hostOS, platform
     *
     */
    @JsonProperty("eventType")
    public String getEventType() {
        return eventType;
    }

    /**
     * for example - applicationNf, guestOS, hostOS, platform
     *
     */
    @JsonProperty("eventType")
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public CommonEventHeader withEventType(String eventType) {
        this.eventType = eventType;
        return this;
    }

    /**
     * the latest unix time aka epoch time associated with the event from any component--as milliseconds elapsed since 1 Jan 1970 not including leap seconds
     * (Required)
     *
     */
    @JsonProperty("lastEpochMillis")
    public long getLastEpochMillis() {
        return lastEpochMillis;
    }

    /**
     * the latest unix time aka epoch time associated with the event from any component--as milliseconds elapsed since 1 Jan 1970 not including leap seconds
     * (Required)
     *
     */
    @JsonProperty("lastEpochMillis")
    public void setLastEpochMillis(long lastEpochMillis) {
        this.lastEpochMillis = lastEpochMillis;
    }

    public CommonEventHeader withLastEpochMillis(long lastEpochMillis) {
        this.lastEpochMillis = lastEpochMillis;
        return this;
    }

    /**
     *  3 character network function component type, aligned with vfc naming standards
     *
     */
    @JsonProperty("nfcNamingCode")
    public String getNfcNamingCode() {
        return nfcNamingCode;
    }

    /**
     *  3 character network function component type, aligned with vfc naming standards
     *
     */
    @JsonProperty("nfcNamingCode")
    public void setNfcNamingCode(String nfcNamingCode) {
        this.nfcNamingCode = nfcNamingCode;
    }

    public CommonEventHeader withNfcNamingCode(String nfcNamingCode) {
        this.nfcNamingCode = nfcNamingCode;
        return this;
    }

    /**
     *  4 character network function type, aligned with nf naming standards
     *
     */
    @JsonProperty("nfNamingCode")
    public String getNfNamingCode() {
        return nfNamingCode;
    }

    /**
     *  4 character network function type, aligned with nf naming standards
     *
     */
    @JsonProperty("nfNamingCode")
    public void setNfNamingCode(String nfNamingCode) {
        this.nfNamingCode = nfNamingCode;
    }

    public CommonEventHeader withNfNamingCode(String nfNamingCode) {
        this.nfNamingCode = nfNamingCode;
        return this;
    }

    /**
     * processing priority
     * (Required)
     *
     */
    @JsonProperty("priority")
    public CommonEventHeader.Priority getPriority() {
        return priority;
    }

    /**
     * processing priority
     * (Required)
     *
     */
    @JsonProperty("priority")
    public void setPriority(CommonEventHeader.Priority priority) {
        this.priority = priority;
    }

    public CommonEventHeader withPriority(CommonEventHeader.Priority priority) {
        this.priority = priority;
        return this;
    }

    /**
     * UUID identifying the entity reporting the event, for example an OAM VM; must be populated by the ATT enrichment process
     *
     */
    @JsonProperty("reportingEntityId")
    public String getReportingEntityId() {
        return reportingEntityId;
    }

    /**
     * UUID identifying the entity reporting the event, for example an OAM VM; must be populated by the ATT enrichment process
     *
     */
    @JsonProperty("reportingEntityId")
    public void setReportingEntityId(String reportingEntityId) {
        this.reportingEntityId = reportingEntityId;
    }

    public CommonEventHeader withReportingEntityId(String reportingEntityId) {
        this.reportingEntityId = reportingEntityId;
        return this;
    }

    /**
     * name of the entity reporting the event, for example, an EMS name; may be the same as sourceName
     * (Required)
     *
     */
    @JsonProperty("reportingEntityName")
    public String getReportingEntityName() {
        return reportingEntityName;
    }

    /**
     * name of the entity reporting the event, for example, an EMS name; may be the same as sourceName
     * (Required)
     *
     */
    @JsonProperty("reportingEntityName")
    public void setReportingEntityName(String reportingEntityName) {
        this.reportingEntityName = reportingEntityName;
    }

    public CommonEventHeader withReportingEntityName(String reportingEntityName) {
        this.reportingEntityName = reportingEntityName;
        return this;
    }

    /**
     * ordering of events communicated by an event source instance or 0 if not needed
     * (Required)
     *
     */
    @JsonProperty("sequence")
    public Integer getSequence() {
        return sequence;
    }

    /**
     * ordering of events communicated by an event source instance or 0 if not needed
     * (Required)
     *
     */
    @JsonProperty("sequence")
    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }

    public CommonEventHeader withSequence(Integer sequence) {
        this.sequence = sequence;
        return this;
    }

    /**
     * UUID identifying the entity experiencing the event issue; must be populated by the ATT enrichment process
     *
     */
    @JsonProperty("sourceId")
    public String getSourceId() {
        return sourceId;
    }

    /**
     * UUID identifying the entity experiencing the event issue; must be populated by the ATT enrichment process
     *
     */
    @JsonProperty("sourceId")
    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public CommonEventHeader withSourceId(String sourceId) {
        this.sourceId = sourceId;
        return this;
    }

    /**
     * name of the entity experiencing the event issue
     * (Required)
     *
     */
    @JsonProperty("sourceName")
    public String getSourceName() {
        return sourceName;
    }

    /**
     * name of the entity experiencing the event issue
     * (Required)
     *
     */
    @JsonProperty("sourceName")
    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public CommonEventHeader withSourceName(String sourceName) {
        this.sourceName = sourceName;
        return this;
    }

    /**
     * the earliest unix time aka epoch time associated with the event from any component--as milliseconds elapsed since 1 Jan 1970 not including leap seconds
     * (Required)
     *
     */
    @JsonProperty("startEpochMillis")
    public long getStartEpochMillis() {
        return startEpochMillis;
    }

    /**
     * the earliest unix time aka epoch time associated with the event from any component--as milliseconds elapsed since 1 Jan 1970 not including leap seconds
     * (Required)
     *
     */
    @JsonProperty("startEpochMillis")
    public void setStartEpochMillis(long startEpochMillis) {
        this.startEpochMillis = startEpochMillis;
    }

    public CommonEventHeader withStartEpochMillis(long startEpochMillis) {
        this.startEpochMillis = startEpochMillis;
        return this;
    }

    /**
     * version of the event header
     * (Required)
     *
     */
    @JsonProperty("version")
    public CommonEventHeader.Version getVersion() {
        return version;
    }

    /**
     * version of the event header
     * (Required)
     *
     */
    @JsonProperty("version")
    public void setVersion(CommonEventHeader.Version version) {
        this.version = version;
    }

    public CommonEventHeader withVersion(CommonEventHeader.Version version) {
        this.version = version;
        return this;
    }

    /**
     * version of the VES Event Listener API
     * (Required)
     *
     */
    @JsonProperty("vesEventListenerVersion")
    public CommonEventHeader.VesEventListenerVersion getVesEventListenerVersion() {
        return vesEventListenerVersion;
    }

    /**
     * version of the VES Event Listener API
     * (Required)
     *
     */
    @JsonProperty("vesEventListenerVersion")
    public void setVesEventListenerVersion(CommonEventHeader.VesEventListenerVersion vesEventListenerVersion) {
        this.vesEventListenerVersion = vesEventListenerVersion;
    }

    public CommonEventHeader withVesEventListenerVersion(CommonEventHeader.VesEventListenerVersion vesEventListenerVersion) {
        this.vesEventListenerVersion = vesEventListenerVersion;
        return this;
    }

    public enum Domain {

        FAULT("fault"),
        NOTIFICATION("notification"),
        THRESHOLD_CROSSING_ALERT("thresholdCrossingAlert");
        private final String value;
        private final static Map<String, CommonEventHeader.Domain> CONSTANTS = new HashMap<String, CommonEventHeader.Domain>();

        static {
            for (CommonEventHeader.Domain c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        private Domain(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        @JsonValue
        public String value() {
            return this.value;
        }

        @JsonCreator
        public static CommonEventHeader.Domain fromValue(String value) {
            CommonEventHeader.Domain constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

    public enum Priority {

        HIGH("High"),
        MEDIUM("Medium"),
        NORMAL("Normal"),
        LOW("Low");
        private final String value;
        private final static Map<String, CommonEventHeader.Priority> CONSTANTS = new HashMap<String, CommonEventHeader.Priority>();

        static {
            for (CommonEventHeader.Priority c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        private Priority(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        @JsonValue
        public String value() {
            return this.value;
        }

        @JsonCreator
        public static CommonEventHeader.Priority fromValue(String value) {
            CommonEventHeader.Priority constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

    public enum Version {

        _4_0("4.0"),
        _4_0_1("4.0.1"),
        _4_1("4.1");
        private final String value;
        private final static Map<String, CommonEventHeader.Version> CONSTANTS = new HashMap<String, CommonEventHeader.Version>();

        static {
            for (CommonEventHeader.Version c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        private Version(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        @JsonValue
        public String value() {
            return this.value;
        }

        @JsonCreator
        public static CommonEventHeader.Version fromValue(String value) {
            CommonEventHeader.Version constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

    public enum VesEventListenerVersion {

        _7_0("7.0"),
        _7_0_1("7.0.1"),
        _7_1("7.1");
        private final String value;
        private final static Map<String, CommonEventHeader.VesEventListenerVersion> CONSTANTS = new HashMap<String, CommonEventHeader.VesEventListenerVersion>();

        static {
            for (CommonEventHeader.VesEventListenerVersion c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        private VesEventListenerVersion(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        @JsonValue
        public String value() {
            return this.value;
        }

        @JsonCreator
        public static CommonEventHeader.VesEventListenerVersion fromValue(String value) {
            CommonEventHeader.VesEventListenerVersion constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
