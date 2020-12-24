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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * fields specific to threshold crossing alert events
 *
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "additionalFields", "additionalParameters", "alertAction", "alertDescription", "alertType",
		"collectionTimestamp", "eventSeverity", "eventStartTimestamp", "thresholdCrossingFieldsVersion" })
public class ThresholdCrossingAlertFields implements Serializable {
	/**
	 * an associative array which is an array of key:value pairs
	 *
	 */
	@JsonProperty("additionalFields")
	@JsonPropertyDescription("an associative array which is an array of key:value pairs")
	@NotNull
	private AlarmAdditionalInformation additionalFields;

	/**
	 * performance counters (Required)
	 *
	 */
	@JsonProperty("additionalParameters")
	@JsonPropertyDescription("performance counters")
	private List<AdditionalParameter> additionalParameters = new ArrayList<AdditionalParameter>();
	/**
	 * Event action (Required)
	 *
	 */
	@JsonProperty("alertAction")
	@JsonPropertyDescription("Event action")
	private ThresholdCrossingAlertFields.AlertAction alertAction;
	/**
	 * Unique short alert description such as IF-SHUB-ERRDROP (Required)
	 *
	 */
	@JsonProperty("alertDescription")
	@JsonPropertyDescription("Unique short alert description such as IF-SHUB-ERRDROP")
	private String alertDescription;
	/**
	 * Event type (Required)
	 *
	 */
	@JsonProperty("alertType")
	@JsonPropertyDescription("Event type")
	private ThresholdCrossingAlertFields.AlertType alertType;
	/**
	 * Time when the performance collector picked up the data; with RFC 2822
	 * compliant format: Sat, 13 Mar 2010 11:29:05 -0800 (Required)
	 *
	 */
	@JsonProperty("collectionTimestamp")
	@JsonPropertyDescription("Time when the performance collector picked up the data; with RFC 2822 compliant format: Sat, 13 Mar 2010 11:29:05 -0800")
	private String collectionTimestamp;
	/**
	 * event severity or priority (Required)
	 *
	 */
	@JsonProperty("eventSeverity")
	@JsonPropertyDescription("event severity or priority")
	private ThresholdCrossingAlertFields.EventSeverity eventSeverity;
	/**
	 * Time closest to when the measurement was made; with RFC 2822 compliant
	 * format: Sat, 13 Mar 2010 11:29:05 -0800 (Required)
	 *
	 */
	@JsonProperty("eventStartTimestamp")
	@JsonPropertyDescription("Time closest to when the measurement was made; with RFC 2822 compliant format: Sat, 13 Mar 2010 11:29:05 -0800")
	private String eventStartTimestamp;
	/**
	 * version of the thresholdCrossingAlertFields block (Required)
	 *
	 */
	@JsonProperty("thresholdCrossingFieldsVersion")
	@JsonPropertyDescription("version of the thresholdCrossingAlertFields block")
	private ThresholdCrossingAlertFields.ThresholdCrossingFieldsVersion thresholdCrossingFieldsVersion;
	private final static long serialVersionUID = -1949259442786178187L;

	/**
	 * No args constructor for use in serialization
	 *
	 */
	public ThresholdCrossingAlertFields() {
	}

	/**
	 *
	 * @param additionalFields
	 * @param eventSeverity
	 * @param alertAction
	 * @param alertType
	 * @param collectionTimestamp
	 * @param additionalParameters
	 * @param eventStartTimestamp
	 * @param thresholdCrossingFieldsVersion
	 * @param alertDescription
	 */
	public ThresholdCrossingAlertFields(AlarmAdditionalInformation additionalFields,
			List<AdditionalParameter> additionalParameters, ThresholdCrossingAlertFields.AlertAction alertAction,
			String alertDescription, ThresholdCrossingAlertFields.AlertType alertType, String collectionTimestamp,
			ThresholdCrossingAlertFields.EventSeverity eventSeverity, String eventStartTimestamp,
			ThresholdCrossingAlertFields.ThresholdCrossingFieldsVersion thresholdCrossingFieldsVersion) {
		super();
		this.additionalFields = additionalFields;
		this.additionalParameters = additionalParameters;
		this.alertAction = alertAction;
		this.alertDescription = alertDescription;
		this.alertType = alertType;
		this.collectionTimestamp = collectionTimestamp;
		this.eventSeverity = eventSeverity;
		this.eventStartTimestamp = eventStartTimestamp;
		this.thresholdCrossingFieldsVersion = thresholdCrossingFieldsVersion;
	}

	/**
	 * an associative array which is an array of key:value pairs
	 *
	 */
	@JsonProperty("additionalFields")
	public AlarmAdditionalInformation getAdditionalFields() {
		return additionalFields;
	}

	/**
	 * an associative array which is an array of key:value pairs
	 *
	 */
	@JsonProperty("additionalFields")
	public void setAdditionalFields(AlarmAdditionalInformation additionalFields) {
		this.additionalFields = additionalFields;
	}

	public ThresholdCrossingAlertFields withAdditionalFields(AlarmAdditionalInformation additionalFields) {
		this.additionalFields = additionalFields;
		return this;
	}

	/**
	 * performance counters (Required)
	 *
	 */
	@JsonProperty("additionalParameters")
	public List<AdditionalParameter> getAdditionalParameters() {
		return additionalParameters;
	}

	/**
	 * performance counters (Required)
	 *
	 */
	@JsonProperty("additionalParameters")
	public void setAdditionalParameters(List<AdditionalParameter> additionalParameters) {
		this.additionalParameters = additionalParameters;
	}

	public ThresholdCrossingAlertFields withAdditionalParameters(List<AdditionalParameter> additionalParameters) {
		this.additionalParameters = additionalParameters;
		return this;
	}

	/**
	 * Event action (Required)
	 *
	 */
	@JsonProperty("alertAction")
	public ThresholdCrossingAlertFields.AlertAction getAlertAction() {
		return alertAction;
	}

	/**
	 * Event action (Required)
	 *
	 */
	@JsonProperty("alertAction")
	public void setAlertAction(ThresholdCrossingAlertFields.AlertAction alertAction) {
		this.alertAction = alertAction;
	}

	public ThresholdCrossingAlertFields withAlertAction(ThresholdCrossingAlertFields.AlertAction alertAction) {
		this.alertAction = alertAction;
		return this;
	}

	/**
	 * Unique short alert description such as IF-SHUB-ERRDROP (Required)
	 *
	 */
	@JsonProperty("alertDescription")
	public String getAlertDescription() {
		return alertDescription;
	}

	/**
	 * Unique short alert description such as IF-SHUB-ERRDROP (Required)
	 *
	 */
	@JsonProperty("alertDescription")
	public void setAlertDescription(String alertDescription) {
		this.alertDescription = alertDescription;
	}

	public ThresholdCrossingAlertFields withAlertDescription(String alertDescription) {
		this.alertDescription = alertDescription;
		return this;
	}

	/**
	 * Event type (Required)
	 *
	 */
	@JsonProperty("alertType")
	public ThresholdCrossingAlertFields.AlertType getAlertType() {
		return alertType;
	}

	/**
	 * Event type (Required)
	 *
	 */
	@JsonProperty("alertType")
	public void setAlertType(ThresholdCrossingAlertFields.AlertType alertType) {
		this.alertType = alertType;
	}

	public ThresholdCrossingAlertFields withAlertType(ThresholdCrossingAlertFields.AlertType alertType) {
		this.alertType = alertType;
		return this;
	}

	/**
	 * Time when the performance collector picked up the data; with RFC 2822
	 * compliant format: Sat, 13 Mar 2010 11:29:05 -0800 (Required)
	 *
	 */
	@JsonProperty("collectionTimestamp")
	public String getCollectionTimestamp() {
		return collectionTimestamp;
	}

	/**
	 * Time when the performance collector picked up the data; with RFC 2822
	 * compliant format: Sat, 13 Mar 2010 11:29:05 -0800 (Required)
	 *
	 */
	@JsonProperty("collectionTimestamp")
	public void setCollectionTimestamp(String collectionTimestamp) {
		this.collectionTimestamp = collectionTimestamp;
	}

	public ThresholdCrossingAlertFields withCollectionTimestamp(String collectionTimestamp) {
		this.collectionTimestamp = collectionTimestamp;
		return this;
	}

	/**
	 * event severity or priority (Required)
	 *
	 */
	@JsonProperty("eventSeverity")
	public ThresholdCrossingAlertFields.EventSeverity getEventSeverity() {
		return eventSeverity;
	}

	/**
	 * event severity or priority (Required)
	 *
	 */
	@JsonProperty("eventSeverity")
	public void setEventSeverity(ThresholdCrossingAlertFields.EventSeverity eventSeverity) {
		this.eventSeverity = eventSeverity;
	}

	public ThresholdCrossingAlertFields withEventSeverity(ThresholdCrossingAlertFields.EventSeverity eventSeverity) {
		this.eventSeverity = eventSeverity;
		return this;
	}

	/**
	 * Time closest to when the measurement was made; with RFC 2822 compliant
	 * format: Sat, 13 Mar 2010 11:29:05 -0800 (Required)
	 *
	 */
	@JsonProperty("eventStartTimestamp")
	public String getEventStartTimestamp() {
		return eventStartTimestamp;
	}

	/**
	 * Time closest to when the measurement was made; with RFC 2822 compliant
	 * format: Sat, 13 Mar 2010 11:29:05 -0800 (Required)
	 *
	 */
	@JsonProperty("eventStartTimestamp")
	public void setEventStartTimestamp(String eventStartTimestamp) {
		this.eventStartTimestamp = eventStartTimestamp;
	}

	public ThresholdCrossingAlertFields withEventStartTimestamp(String eventStartTimestamp) {
		this.eventStartTimestamp = eventStartTimestamp;
		return this;
	}

	/**
	 * version of the thresholdCrossingAlertFields block (Required)
	 *
	 */
	@JsonProperty("thresholdCrossingFieldsVersion")
	public ThresholdCrossingAlertFields.ThresholdCrossingFieldsVersion getThresholdCrossingFieldsVersion() {
		return thresholdCrossingFieldsVersion;
	}

	/**
	 * version of the thresholdCrossingAlertFields block (Required)
	 *
	 */
	@JsonProperty("thresholdCrossingFieldsVersion")
	public void setThresholdCrossingFieldsVersion(
			ThresholdCrossingAlertFields.ThresholdCrossingFieldsVersion thresholdCrossingFieldsVersion) {
		this.thresholdCrossingFieldsVersion = thresholdCrossingFieldsVersion;
	}

	public ThresholdCrossingAlertFields withThresholdCrossingFieldsVersion(
			ThresholdCrossingAlertFields.ThresholdCrossingFieldsVersion thresholdCrossingFieldsVersion) {
		this.thresholdCrossingFieldsVersion = thresholdCrossingFieldsVersion;
		return this;
	}

	public enum AlertAction {

		CLEAR("CLEAR"), CONT("CONT"), SET("SET");

		private final String value;
		private final static Map<String, ThresholdCrossingAlertFields.AlertAction> CONSTANTS = new HashMap<String, ThresholdCrossingAlertFields.AlertAction>();

		static {
			for (ThresholdCrossingAlertFields.AlertAction c : values()) {
				CONSTANTS.put(c.value, c);
			}
		}

		private AlertAction(String value) {
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
		public static ThresholdCrossingAlertFields.AlertAction fromValue(String value) {
			ThresholdCrossingAlertFields.AlertAction constant = CONSTANTS.get(value);
			if (constant == null) {
				throw new IllegalArgumentException(value);
			} else {
				return constant;
			}
		}

	}

	public enum AlertType {

		CARD_ANOMALY("CARD-ANOMALY"), ELEMENT_ANOMALY("ELEMENT-ANOMALY"), INTERFACE_ANOMALY("INTERFACE-ANOMALY"),
		SERVICE_ANOMALY("SERVICE-ANOMALY");

		private final String value;
		private final static Map<String, ThresholdCrossingAlertFields.AlertType> CONSTANTS = new HashMap<String, ThresholdCrossingAlertFields.AlertType>();

		static {
			for (ThresholdCrossingAlertFields.AlertType c : values()) {
				CONSTANTS.put(c.value, c);
			}
		}

		private AlertType(String value) {
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
		public static ThresholdCrossingAlertFields.AlertType fromValue(String value) {
			ThresholdCrossingAlertFields.AlertType constant = CONSTANTS.get(value);
			if (constant == null) {
				throw new IllegalArgumentException(value);
			} else {
				return constant;
			}
		}

	}

	public enum EventSeverity {

		CRITICAL("CRITICAL"), MAJOR("MAJOR"), MINOR("MINOR"), WARNING("WARNING"), CLEAR("CLEAR"), INFO("INFO"),
		NORMAL("NORMAL");

		private final String value;
		private final static Map<String, ThresholdCrossingAlertFields.EventSeverity> CONSTANTS = new HashMap<String, ThresholdCrossingAlertFields.EventSeverity>();

		static {
			for (ThresholdCrossingAlertFields.EventSeverity c : values()) {
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
		public static ThresholdCrossingAlertFields.EventSeverity fromValue(String value) {
			ThresholdCrossingAlertFields.EventSeverity constant = CONSTANTS.get(value);
			if (constant == null) {
				throw new IllegalArgumentException(value);
			} else {
				return constant;
			}
		}

	}

	public enum ThresholdCrossingFieldsVersion {

		_4_0("4.0");

		private final String value;
		private final static Map<String, ThresholdCrossingAlertFields.ThresholdCrossingFieldsVersion> CONSTANTS = new HashMap<String, ThresholdCrossingAlertFields.ThresholdCrossingFieldsVersion>();

		static {
			for (ThresholdCrossingAlertFields.ThresholdCrossingFieldsVersion c : values()) {
				CONSTANTS.put(c.value, c);
			}
		}

		private ThresholdCrossingFieldsVersion(String value) {
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
		public static ThresholdCrossingAlertFields.ThresholdCrossingFieldsVersion fromValue(String value) {
			ThresholdCrossingAlertFields.ThresholdCrossingFieldsVersion constant = CONSTANTS.get(value);
			if (constant == null) {
				throw new IllegalArgumentException(value);
			} else {
				return constant;
			}
		}

	}

}
