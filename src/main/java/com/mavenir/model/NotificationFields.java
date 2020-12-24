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
import javax.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * notification fields
 *
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "additionalFields", "changeIdentifier", "changeType", "newState", "oldState",
		"notificationFieldsVersion" })
public class NotificationFields implements Serializable {
	/**
	 * an associative array which is an array of key:value pairs
	 *
	 */
	@JsonProperty("additionalFields")
	@JsonPropertyDescription("an associative array which is an array of key:value pairs")
	@NotNull
	private AlarmAdditionalInformation additionalFields;

	/**
	 * system or session identifier associated with the change (Required)
	 *
	 */
	@JsonProperty("changeIdentifier")
	@NotNull
	private String changeIdentifier;
	/**
	 * describes what has changed for the entity (Required)
	 *
	 */
	@JsonProperty("changeType")
	@NotNull
	private String changeType;
	/**
	 * new state of the entity
	 *
	 */
	@JsonProperty("newState")
	private String newState;
	/**
	 * previous state of the entity
	 *
	 */
	@JsonProperty("oldState")
	private String oldState;
	/**
	 * version of the notificationFields block (Required)
	 *
	 */
	@JsonProperty("notificationFieldsVersion")
	@NotNull
	private NotificationFields.NotificationFieldsVersion notificationFieldsVersion;
	private final static long serialVersionUID = -3209413583366987565L;

	/**
	 * No args constructor for use in serialization
	 *
	 */
	public NotificationFields() {
	}

	/**
	 *
	 * @param additionalFields
	 * @param oldState
	 * @param notificationFieldsVersion
	 * @param changeType
	 * @param changeIdentifier
	 * @param newState
	 */
	public NotificationFields(AlarmAdditionalInformation additionalFields, String changeIdentifier, String changeType,
			String newState, String oldState, NotificationFields.NotificationFieldsVersion notificationFieldsVersion) {
		super();
		this.additionalFields = additionalFields;
		this.changeIdentifier = changeIdentifier;
		this.changeType = changeType;
		this.newState = newState;
		this.oldState = oldState;
		this.notificationFieldsVersion = notificationFieldsVersion;
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

	public NotificationFields withAdditionalFields(AlarmAdditionalInformation additionalFields) {
		this.additionalFields = additionalFields;
		return this;
	}

	/**
	 * system or session identifier associated with the change (Required)
	 *
	 */
	@JsonProperty("changeIdentifier")
	public String getChangeIdentifier() {
		return changeIdentifier;
	}

	/**
	 * system or session identifier associated with the change (Required)
	 *
	 */
	@JsonProperty("changeIdentifier")
	public void setChangeIdentifier(String changeIdentifier) {
		this.changeIdentifier = changeIdentifier;
	}

	public NotificationFields withChangeIdentifier(String changeIdentifier) {
		this.changeIdentifier = changeIdentifier;
		return this;
	}

	/**
	 * describes what has changed for the entity (Required)
	 *
	 */
	@JsonProperty("changeType")
	public String getChangeType() {
		return changeType;
	}

	/**
	 * describes what has changed for the entity (Required)
	 *
	 */
	@JsonProperty("changeType")
	public void setChangeType(String changeType) {
		this.changeType = changeType;
	}

	public NotificationFields withChangeType(String changeType) {
		this.changeType = changeType;
		return this;
	}

	/**
	 * new state of the entity
	 *
	 */
	@JsonProperty("newState")
	public String getNewState() {
		return newState;
	}

	/**
	 * new state of the entity
	 *
	 */
	@JsonProperty("newState")
	public void setNewState(String newState) {
		this.newState = newState;
	}

	public NotificationFields withNewState(String newState) {
		this.newState = newState;
		return this;
	}

	/**
	 * previous state of the entity
	 *
	 */
	@JsonProperty("oldState")
	public String getOldState() {
		return oldState;
	}

	/**
	 * previous state of the entity
	 *
	 */
	@JsonProperty("oldState")
	public void setOldState(String oldState) {
		this.oldState = oldState;
	}

	public NotificationFields withOldState(String oldState) {
		this.oldState = oldState;
		return this;
	}

	/**
	 * version of the notificationFields block (Required)
	 *
	 */
	@JsonProperty("notificationFieldsVersion")
	public NotificationFields.NotificationFieldsVersion getNotificationFieldsVersion() {
		return notificationFieldsVersion;
	}

	/**
	 * version of the notificationFields block (Required)
	 *
	 */
	@JsonProperty("notificationFieldsVersion")
	public void setNotificationFieldsVersion(NotificationFields.NotificationFieldsVersion notificationFieldsVersion) {
		this.notificationFieldsVersion = notificationFieldsVersion;
	}

	public NotificationFields withNotificationFieldsVersion(
			NotificationFields.NotificationFieldsVersion notificationFieldsVersion) {
		this.notificationFieldsVersion = notificationFieldsVersion;
		return this;
	}

	public int describeContents() {
		return 0;
	}

	public enum NotificationFieldsVersion {

		_2_0("2.0");

		private final String value;
		private final static Map<String, NotificationFields.NotificationFieldsVersion> CONSTANTS = new HashMap<String, NotificationFields.NotificationFieldsVersion>();

		static {
			for (NotificationFields.NotificationFieldsVersion c : values()) {
				CONSTANTS.put(c.value, c);
			}
		}

		private NotificationFieldsVersion(String value) {
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
		public static NotificationFields.NotificationFieldsVersion fromValue(String value) {
			NotificationFields.NotificationFieldsVersion constant = CONSTANTS.get(value);
			if (constant == null) {
				throw new IllegalArgumentException(value);
			} else {
				return constant;
			}
		}

	}

}
