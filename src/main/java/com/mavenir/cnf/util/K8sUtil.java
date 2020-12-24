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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.xgvela.cnf.Constants;

import io.fabric8.kubernetes.client.KubernetesClient;

@Component
public class K8sUtil {

	private static Logger LOG = LogManager.getLogger(K8sUtil.class);

	public static HashMap<String, String> eventNameToSeverity = new HashMap<>();

	@Autowired
	K8sClient k8sClient;

	@Autowired
	JsonUtil jsonUtil;

	public JsonNode getEventDef(String eventdefCmapName, String eventdefCmapNamespace, String localEventName,
			String nfEventdefCmapName) {

		LOG.info("ConfigMap: " + eventdefCmapName + ", Namespace: " + eventdefCmapNamespace + ", EventName: "
				+ localEventName + ", NF ConfigMap: " + nfEventdefCmapName);

		Map<String, String> eventdefCmapData = getCmap(eventdefCmapNamespace, eventdefCmapName);

		if (eventdefCmapData == null) {
			LOG.info("Microservice level ConfigMap (" + eventdefCmapName + ") not found");

			if (!nfEventdefCmapName.equals("")) {
				LOG.info("Looking up NF level ConfigMap (" + nfEventdefCmapName + ")");
				eventdefCmapData = getCmap(eventdefCmapNamespace, nfEventdefCmapName);
			}

			if (eventdefCmapData == null) {
				LOG.info("NF level ConfigMap also unavailable, unable to get Event Definition for \"" + localEventName
						+ "\"");
				LOG.info("Looking up cim ConfigMap");
				if (JsonUtil.cimEventdefCmapData != null)
					return getEventDef(JsonUtil.cimEventdefCmapData, localEventName);
				else
					return null;
			}
		}
		JsonNode eventDef = getEventDef(eventdefCmapData, localEventName);
		if (eventDef == null) {
			LOG.info("Looking up cim ConfigMap");
			if (JsonUtil.cimEventdefCmapData != null)
				return getEventDef(JsonUtil.cimEventdefCmapData, localEventName);
		}
		return eventDef;
	}

	private JsonNode getEventDef(Map<String, String> cmapData, String eventName) {

		LOG.info("Getting Event Definition for Event Name: " + eventName);
		ObjectMapper objMapper = jsonUtil.getMapper();

		Iterator<Entry<String, String>> iterator = cmapData.entrySet().iterator();

		while (iterator.hasNext()) {
			Entry<String, String> entry = iterator.next();
			try {
				JsonNode definition = null;
				if (!entry.getValue().equalsIgnoreCase("eventdef-cfg"))
					definition = objMapper.readTree(entry.getValue());
				else
					continue;
				JsonNode eventsNode = definition.get(Constants.EVENTS);

				Iterator<Entry<String, JsonNode>> evtIterator = eventsNode.fields();

				while (evtIterator.hasNext()) {
					Entry<String, JsonNode> evtDef = evtIterator.next();
					eventNameToSeverity.putIfAbsent(evtDef.getKey(),
							evtDef.getValue().get(Constants.PERCEIVED_SEVERITY).asText());

				}
				// LOG.debug("EventNameToSeverity: " + eventNameToSeverity);
				if (eventsNode.has(eventName)) {
					return eventsNode.get(eventName);
				}
			} catch (JsonMappingException e) {
				LOG.error(e.getMessage());
			} catch (JsonProcessingException e) {
				LOG.error(e.getMessage());
			}
		}
		LOG.info("No event matching Event Definition found for Event Name: " + eventName);
		return null;
	}

	public Map<String, String> getCmap(String cmapNamespace, String cmapName) {
		KubernetesClient client = k8sClient.getClient();

		LOG.info("Getting ConfigMap: " + cmapName + ", in Namespace: " + cmapNamespace);
		Map<String, String> cmapData;
		try {
			cmapData = client.configMaps().inNamespace(cmapNamespace).withName(cmapName).get().getData();
		} catch (Exception e) {
			LOG.error("Unable to get configmap: " + cmapName + ", in namespace: " + cmapNamespace);
			return null;
		}
		return cmapData;
	}
}
