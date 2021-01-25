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
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;


/**
 * the root level of the common event format
 *
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "commonEventHeader",
    "faultFields",
    "thresholdCrossingAlertFields",
    "notificationFields"
})
public class Event implements Serializable
{

    /**
     * fields common to all events
     * (Required)
     *
     */
    @JsonProperty("commonEventHeader")
    @JsonPropertyDescription("fields common to all events")
    private CommonEventHeader commonEventHeader;
    /**
     * fields specific to fault events
     *
     */
    @JsonProperty("faultFields")
    @JsonPropertyDescription("fields specific to fault events")
    private FaultFields faultFields;
    /**
     * fields specific to threshold crossing alert events
     *
     */
    @JsonProperty("thresholdCrossingAlertFields")
    @JsonPropertyDescription("fields specific to threshold crossing alert events")
    private ThresholdCrossingAlertFields thresholdCrossingAlertFields;
    /**
     * notification fields
     *
     */
    @JsonProperty("notificationFields")
    @JsonPropertyDescription("fields specific to notification events")
    private NotificationFields notificationFields;
    private final static long serialVersionUID = 3677120051023819984L;
    /**
     * No args constructor for use in serialization
     *
     */
    public Event() {
    }

    /**
     *
     * @param commonEventHeader
     * @param thresholdCrossingAlertFields
     * @param notificationFields
     * @param faultFields
     */
    public Event(CommonEventHeader commonEventHeader, FaultFields faultFields, ThresholdCrossingAlertFields thresholdCrossingAlertFields, NotificationFields notificationFields) {
        super();
        this.commonEventHeader = commonEventHeader;
        this.faultFields = faultFields;
        this.thresholdCrossingAlertFields = thresholdCrossingAlertFields;
        this.notificationFields = notificationFields;
    }

    /**
     * fields common to all events
     * (Required)
     *
     */
    @JsonProperty("commonEventHeader")
    public CommonEventHeader getCommonEventHeader() {
        return commonEventHeader;
    }

    /**
     * fields common to all events
     * (Required)
     *
     */
    @JsonProperty("commonEventHeader")
    public void setCommonEventHeader(CommonEventHeader commonEventHeader) {
        this.commonEventHeader = commonEventHeader;
    }

    public Event withCommonEventHeader(CommonEventHeader commonEventHeader) {
        this.commonEventHeader = commonEventHeader;
        return this;
    }

    /**
     * fields specific to fault events
     *
     */
    @JsonProperty("faultFields")
    public FaultFields getFaultFields() {
        return faultFields;
    }

    /**
     * fields specific to fault events
     *
     */
    @JsonProperty("faultFields")
    public void setFaultFields(FaultFields faultFields) {
        this.faultFields = faultFields;
    }

    public Event withFaultFields(FaultFields faultFields) {
        this.faultFields = faultFields;
        return this;
    }

    /**
     * fields specific to threshold crossing alert events
     *
     */
    @JsonProperty("thresholdCrossingAlertFields")
    public ThresholdCrossingAlertFields getThresholdCrossingAlertFields() {
        return thresholdCrossingAlertFields;
    }

    /**
     * fields specific to threshold crossing alert events
     *
     */
    @JsonProperty("thresholdCrossingAlertFields")
    public void setThresholdCrossingAlertFields(ThresholdCrossingAlertFields thresholdCrossingAlertFields) {
        this.thresholdCrossingAlertFields = thresholdCrossingAlertFields;
    }

    public Event withThresholdCrossingAlertFields(ThresholdCrossingAlertFields thresholdCrossingAlertFields) {
        this.thresholdCrossingAlertFields = thresholdCrossingAlertFields;
        return this;
    }

    /**
     * notification fields
     *
     */
    @JsonProperty("notificationFields")
    public NotificationFields getNotificationFields() {
        return notificationFields;
    }

    /**
     * notification fields
     *
     */
    @JsonProperty("notificationFields")
    public void setNotificationFields(NotificationFields notificationFields) {
        this.notificationFields = notificationFields;
    }

    public Event withNotificationFields(NotificationFields notificationFields) {
        this.notificationFields = notificationFields;
        return this;
    }


}
