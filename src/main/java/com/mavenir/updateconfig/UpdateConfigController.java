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

package org.xgvela.updateconfig;

import java.io.IOException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.fasterxml.jackson.databind.JsonNode;
import org.xgvela.cnf.Constants;
import org.xgvela.cnf.notification.NotificationUtil;
import org.xgvela.cnf.util.MetricsUtil;
import io.prometheus.client.Counter;

@RestController
public class UpdateConfigController {
	private static Logger LOG = LogManager.getLogger(UpdateConfigController.class);
	private static final Counter fmaasConfigUpdateattemptTotal = MetricsUtil
			.addCounter("fmaas_config_update_attempt_total", "The number of times FMaaS config update attempted");

	@Autowired
	UpdateConfigHelper updateConfigHelper;

	@PostMapping(path = "/updateConfig")
	@ResponseStatus(HttpStatus.OK)
	public @ResponseBody String updateConfig(@RequestBody JsonNode diffNode) throws IOException {
		fmaasConfigUpdateattemptTotal.inc();
		LOG.debug("Recieved request for config update");
		try {
			NotificationUtil.sendEvent("FmaasConfigUpdateAttempted", updateConfigHelper.getAttemptMgdObjs(diffNode));
			LOG.info(Constants.ACTIVITY + Constants.INIT + "FMaaS config update attempt starts ");
		} catch (Exception e1) {
			LOG.error(e1.getMessage(), e1 + "Error occured while raising FmaasConfigUpdateAttempted event");
		}
		LOG.info("Json object recieved from cim:" + diffNode.toString());
		Boolean done = false;
		if (diffNode.get("config-patch").asText() != null) {
			done = updateConfigHelper.updateFmaasConfig(diffNode.get("config-patch").asText());
		} else {
			LOG.error("config-patch is null, check diffnode provided by CIM");
		}
		String status = "failure";
		if (done) {
			status = "success";
		}
		updateConfigHelper.publishToCim(diffNode, done);
		LOG.debug("Config update request completed");
		return status;

	}
}
