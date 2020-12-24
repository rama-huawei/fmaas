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
 * fields specific to fault events
 *
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "alarmAdditionalInformation",
    "alarmCondition",
    "alarmInterfaceA",
    "eventCategory",
    "eventSeverity",
    "eventSourceType",
    "faultFieldsVersion",
    "specificProblem",
    "vfStatus"
})
public class FaultFields implements Serializable
{

    /**
     * an associative array which is an array of key:value pairs
     *
     */
    @JsonProperty("alarmAdditionalInformation")
    @JsonPropertyDescription("an associative array which is an array of key:value pairs")
    private AlarmAdditionalInformation alarmAdditionalInformation;
    /**
     * alarm condition reported by the device
     * (Required)
     *
     */
    @JsonProperty("alarmCondition")
    @JsonPropertyDescription("alarm condition reported by the device")
    private String alarmCondition;
    /**
     * card, port, channel or interface name of the device generating the alarm
     *
     */
    @JsonProperty("alarmInterfaceA")
    @JsonPropertyDescription("card, port, channel or interface name of the device generating the alarm")
    private String alarmInterfaceA;
    /**
     * Event category, for example: license, link, routing, security, signaling
     *
     */
    @JsonProperty("eventCategory")
    @JsonPropertyDescription("Event category, for example: license, link, routing, security, signaling")
    private String eventCategory;
    /**
     * event severity
     * (Required)
     *
     */
    @JsonProperty("eventSeverity")
    @JsonPropertyDescription("event severity")
    private FaultFields.EventSeverity eventSeverity;
    /**
     * type of event source; examples: card, host, other, port, portThreshold, router, slotThreshold, switch, virtualMachine, virtualNetworkFunction
     * (Required)
     *
     */
    @JsonProperty("eventSourceType")
    @JsonPropertyDescription("type of event source; examples: card, host, other, port, portThreshold, router, slotThreshold, switch, virtualMachine, virtualNetworkFunction")
    private String eventSourceType;
    /**
     * version of the faultFields block
     * (Required)
     *
     */
    @JsonProperty("faultFieldsVersion")
    @JsonPropertyDescription("version of the faultFields block")
    private FaultFields.FaultFieldsVersion faultFieldsVersion;
    /**
     * short description of the alarm or problem
     * (Required)
     *
     */
    @JsonProperty("specificProblem")
    @JsonPropertyDescription("short description of the alarm or problem")
    private String specificProblem;
    /**
     * virtual function status enumeration
     * (Required)
     *
     */
    @JsonProperty("vfStatus")
    @JsonPropertyDescription("virtual function status enumeration")
    private FaultFields.VfStatus vfStatus;
    private final static long serialVersionUID = 4235923119328675723L;

    /**
     * No args constructor for use in serialization
     *
     */
    public FaultFields() {
    }

    /**
     *
     * @param eventSeverity
     * @param alarmCondition
     * @param faultFieldsVersion
     * @param eventCategory
     * @param specificProblem
     * @param alarmInterfaceA
     * @param alarmAdditionalInformation
     * @param eventSourceType
     * @param vfStatus
     */
    public FaultFields(AlarmAdditionalInformation alarmAdditionalInformation, String alarmCondition, String alarmInterfaceA, String eventCategory, FaultFields.EventSeverity eventSeverity, String eventSourceType, FaultFields.FaultFieldsVersion faultFieldsVersion, String specificProblem, FaultFields.VfStatus vfStatus) {
        super();
        this.alarmAdditionalInformation = alarmAdditionalInformation;
        this.alarmCondition = alarmCondition;
        this.alarmInterfaceA = alarmInterfaceA;
        this.eventCategory = eventCategory;
        this.eventSeverity = eventSeverity;
        this.eventSourceType = eventSourceType;
        this.faultFieldsVersion = faultFieldsVersion;
        this.specificProblem = specificProblem;
        this.vfStatus = vfStatus;
    }

    /**
     * an associative array which is an array of key:value pairs
     *
     */
    @JsonProperty("alarmAdditionalInformation")
    public AlarmAdditionalInformation getAlarmAdditionalInformation() {
        return alarmAdditionalInformation;
    }

    /**
     * an associative array which is an array of key:value pairs
     *
     */
    @JsonProperty("alarmAdditionalInformation")
    public void setAlarmAdditionalInformation(AlarmAdditionalInformation alarmAdditionalInformation) {
        this.alarmAdditionalInformation = alarmAdditionalInformation;
    }

    public FaultFields withAlarmAdditionalInformation(AlarmAdditionalInformation alarmAdditionalInformation) {
        this.alarmAdditionalInformation = alarmAdditionalInformation;
        return this;
    }

    /**
     * alarm condition reported by the device
     * (Required)
     *
     */
    @JsonProperty("alarmCondition")
    public String getAlarmCondition() {
        return alarmCondition;
    }

    /**
     * alarm condition reported by the device
     * (Required)
     *
     */
    @JsonProperty("alarmCondition")
    public void setAlarmCondition(String alarmCondition) {
        this.alarmCondition = alarmCondition;
    }

    public FaultFields withAlarmCondition(String alarmCondition) {
        this.alarmCondition = alarmCondition;
        return this;
    }

    /**
     * card, port, channel or interface name of the device generating the alarm
     *
     */
    @JsonProperty("alarmInterfaceA")
    public String getAlarmInterfaceA() {
        return alarmInterfaceA;
    }

    /**
     * card, port, channel or interface name of the device generating the alarm
     *
     */
    @JsonProperty("alarmInterfaceA")
    public void setAlarmInterfaceA(String alarmInterfaceA) {
        this.alarmInterfaceA = alarmInterfaceA;
    }

    public FaultFields withAlarmInterfaceA(String alarmInterfaceA) {
        this.alarmInterfaceA = alarmInterfaceA;
        return this;
    }

    /**
     * Event category, for example: license, link, routing, security, signaling
     *
     */
    @JsonProperty("eventCategory")
    public String getEventCategory() {
        return eventCategory;
    }

    /**
     * Event category, for example: license, link, routing, security, signaling
     *
     */
    @JsonProperty("eventCategory")
    public void setEventCategory(String eventCategory) {
        this.eventCategory = eventCategory;
    }

    public FaultFields withEventCategory(String eventCategory) {
        this.eventCategory = eventCategory;
        return this;
    }

    /**
     * event severity
     * (Required)
     *
     */
    @JsonProperty("eventSeverity")
    public FaultFields.EventSeverity getEventSeverity() {
        return eventSeverity;
    }

    /**
     * event severity
     * (Required)
     *
     */
    @JsonProperty("eventSeverity")
    public void setEventSeverity(FaultFields.EventSeverity eventSeverity) {
        this.eventSeverity = eventSeverity;
    }

    public FaultFields withEventSeverity(FaultFields.EventSeverity eventSeverity) {
        this.eventSeverity = eventSeverity;
        return this;
    }

    /**
     * type of event source; examples: card, host, other, port, portThreshold, router, slotThreshold, switch, virtualMachine, virtualNetworkFunction
     * (Required)
     *
     */
    @JsonProperty("eventSourceType")
    public String getEventSourceType() {
        return eventSourceType;
    }

    /**
     * type of event source; examples: card, host, other, port, portThreshold, router, slotThreshold, switch, virtualMachine, virtualNetworkFunction
     * (Required)
     *
     */
    @JsonProperty("eventSourceType")
    public void setEventSourceType(String eventSourceType) {
        this.eventSourceType = eventSourceType;
    }

    public FaultFields withEventSourceType(String eventSourceType) {
        this.eventSourceType = eventSourceType;
        return this;
    }

    /**
     * version of the faultFields block
     * (Required)
     *
     */
    @JsonProperty("faultFieldsVersion")
    public FaultFields.FaultFieldsVersion getFaultFieldsVersion() {
        return faultFieldsVersion;
    }

    /**
     * version of the faultFields block
     * (Required)
     *
     */
    @JsonProperty("faultFieldsVersion")
    public void setFaultFieldsVersion(FaultFields.FaultFieldsVersion faultFieldsVersion) {
        this.faultFieldsVersion = faultFieldsVersion;
    }

    public FaultFields withFaultFieldsVersion(FaultFields.FaultFieldsVersion faultFieldsVersion) {
        this.faultFieldsVersion = faultFieldsVersion;
        return this;
    }

    /**
     * short description of the alarm or problem
     * (Required)
     *
     */
    @JsonProperty("specificProblem")
    public String getSpecificProblem() {
        return specificProblem;
    }

    /**
     * short description of the alarm or problem
     * (Required)
     *
     */
    @JsonProperty("specificProblem")
    public void setSpecificProblem(String specificProblem) {
        this.specificProblem = specificProblem;
    }

    public FaultFields withSpecificProblem(String specificProblem) {
        this.specificProblem = specificProblem;
        return this;
    }

    /**
     * virtual function status enumeration
     * (Required)
     *
     */
    @JsonProperty("vfStatus")
    public FaultFields.VfStatus getVfStatus() {
        return vfStatus;
    }

    /**
     * virtual function status enumeration
     * (Required)
     *
     */
    @JsonProperty("vfStatus")
    public void setVfStatus(FaultFields.VfStatus vfStatus) {
        this.vfStatus = vfStatus;
    }

    public FaultFields withVfStatus(FaultFields.VfStatus vfStatus) {
        this.vfStatus = vfStatus;
        return this;
    }

    public enum EventSeverity {

        CRITICAL("CRITICAL"),
        MAJOR("MAJOR"),
        MINOR("MINOR"),
        WARNING("WARNING"),
        CLEAR("CLEAR"),
        NORMAL("NORMAL"), INFO("INFO");
        private final String value;
        private final static Map<String, FaultFields.EventSeverity> CONSTANTS = new HashMap<String, FaultFields.EventSeverity>();

        static {
            for (FaultFields.EventSeverity c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        private EventSeverity(String value) {
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
        public static FaultFields.EventSeverity fromValue(String value) {
            FaultFields.EventSeverity constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

    public enum FaultFieldsVersion {

        _4_0("4.0");
        private final String value;
        private final static Map<String, FaultFields.FaultFieldsVersion> CONSTANTS = new HashMap<String, FaultFields.FaultFieldsVersion>();

        static {
            for (FaultFields.FaultFieldsVersion c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        private FaultFieldsVersion(String value) {
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
        public static FaultFields.FaultFieldsVersion fromValue(String value) {
            FaultFields.FaultFieldsVersion constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

    public enum VfStatus {

        ACTIVE("Active"),
        IDLE("Idle"),
        PREPARING_TO_TERMINATE("Preparing to terminate"),
        READY_TO_TERMINATE("Ready to terminate"),
        REQUESTING_TERMINATION("Requesting termination");
        private final String value;
        private final static Map<String, FaultFields.VfStatus> CONSTANTS = new HashMap<String, FaultFields.VfStatus>();

        static {
            for (FaultFields.VfStatus c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        private VfStatus(String value) {
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
        public static FaultFields.VfStatus fromValue(String value) {
            FaultFields.VfStatus constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
