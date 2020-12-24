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
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonValue;


/**
 * performance counter
 *
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "criticality",
    "hashMap",
    "thresholdCrossed"
})
public class AdditionalParameter implements Serializable
{

    /**
     *
     * (Required)
     *
     */
    @JsonProperty("criticality")
    private AdditionalParameter.Criticality criticality;
    /**
     * an associative array which is an array of key:value pairs
     * (Required)
     *
     */
    @JsonProperty("hashMap")
    @JsonPropertyDescription("an associative array which is an array of key:value pairs")
    private org.xgvela.model.HashMap hashMap;
    /**
     *
     * (Required)
     *
     */
    @JsonProperty("thresholdCrossed")
    private String thresholdCrossed;
    private final static long serialVersionUID = -8095792746732014524L;

    /**
     * No args constructor for use in serialization
     *
     */
    public AdditionalParameter() {
    }

    /**
     *
     * @param criticality
     * @param thresholdCrossed
     * @param hashMap
     */
    public AdditionalParameter(AdditionalParameter.Criticality criticality, org.xgvela.model.HashMap hashMap, String thresholdCrossed) {
        super();
        this.criticality = criticality;
        this.hashMap = hashMap;
        this.thresholdCrossed = thresholdCrossed;
    }

    /**
     *
     * (Required)
     *
     */
    @JsonProperty("criticality")
    public AdditionalParameter.Criticality getCriticality() {
        return criticality;
    }

    /**
     *
     * (Required)
     *
     */
    @JsonProperty("criticality")
    public void setCriticality(AdditionalParameter.Criticality criticality) {
        this.criticality = criticality;
    }

    public AdditionalParameter withCriticality(AdditionalParameter.Criticality criticality) {
        this.criticality = criticality;
        return this;
    }

    /**
     * an associative array which is an array of key:value pairs
     * (Required)
     *
     */
    @JsonProperty("hashMap")
    public org.xgvela.model.HashMap getHashMap() {
        return hashMap;
    }

    /**
     * an associative array which is an array of key:value pairs
     * (Required)
     *
     */
    @JsonProperty("hashMap")
    public void setHashMap(org.xgvela.model.HashMap hashMap) {
        this.hashMap = hashMap;
    }

    public AdditionalParameter withHashMap(org.xgvela.model.HashMap hashMap) {
        this.hashMap = hashMap;
        return this;
    }

    /**
     *
     * (Required)
     *
     */
    @JsonProperty("thresholdCrossed")
    public String getThresholdCrossed() {
        return thresholdCrossed;
    }

    /**
     *
     * (Required)
     *
     */
    @JsonProperty("thresholdCrossed")
    public void setThresholdCrossed(String thresholdCrossed) {
        this.thresholdCrossed = thresholdCrossed;
    }

    public AdditionalParameter withThresholdCrossed(String thresholdCrossed) {
        this.thresholdCrossed = thresholdCrossed;
        return this;
    }

    public enum Criticality {

        CRIT("CRIT"),
        MAJ("MAJ");
        private final String value;
        private final static Map<String, AdditionalParameter.Criticality> CONSTANTS = new java.util.HashMap<String, AdditionalParameter.Criticality>();

        static {
            for (AdditionalParameter.Criticality c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        private Criticality(String value) {
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
        public static AdditionalParameter.Criticality fromValue(String value) {
            AdditionalParameter.Criticality constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
