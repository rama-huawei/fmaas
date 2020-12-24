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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.xgvela.cnf.Constants;

@Component
public class DateUtil {

	private static Logger LOG = LogManager.getLogger(DateUtil.class);

	private static final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
	private static final SimpleDateFormat ts_formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	public static int dataRetentionPeriod = 7;
	public static Date lastPurgeTime = new Date();
	public static Date nextScheduledPurgeTime = new Date();

	@Autowired
	JsonUtil jsonUtil;

	public String getTimestamp() {
		return ts_formatter.format(new Date());
	}

	public String getCurrentDate() {
		return formatter.format(new Date());
	}

	public String getNextDate() {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date());
		calendar.add(Calendar.DATE, 1);

		return formatter.format(calendar.getTime());
	}

	public String getEventDate(String message) {
		ObjectMapper objectMapper = jsonUtil.getMapper();
		JsonNode event;
		try {
			event = objectMapper.readTree(message);
			long eventTimestamp = event.get(Constants.EVENT_TS).asLong(System.currentTimeMillis());

			return formatter.format(new Date(eventTimestamp));

		} catch (JsonMappingException e) {
			LOG.error(e.getMessage());
		} catch (JsonProcessingException e) {
			LOG.error(e.getMessage());
		}
		return null;
	}

	public DateTimeFormatter getDtf(String epoch) {
		String format = "yyyy-MM-dd'T'HH:mm:ss.";
		int precision = epoch.length() - 2 - epoch.lastIndexOf(".");
		while (precision >= 1) {
			format += "S";
			precision--;
		}
		format += "'Z'";

		DateTimeFormatter dtfFormatter = DateTimeFormatter.ofPattern(format).withZone(ZoneId.systemDefault());
		return dtfFormatter;
	}

	public long getEpochAsMillis(String epoch) {
		return Instant.from(getDtf(epoch).parse(epoch)).toEpochMilli();
	}

	public Date getLastRetainedDate() {
		Calendar calendar = Calendar.getInstance();
		try {
			calendar.setTime(formatter.parse(formatter.format(new Date())));
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		calendar.add(Calendar.DATE, -(dataRetentionPeriod - 1));
		return calendar.getTime();
	}

	public boolean checkIfLessThanRetained(String indexDate) {
		Date d1 = null;
		Date d2 = getLastRetainedDate();
		try {
			d1 = formatter.parse(indexDate);

		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (d1.compareTo(d2) < 0) {
			return true;
		}
		return false;

	}

	public String epochToDate(long epochMillis) {
		return formatter.format(new Date().from(Instant.ofEpochMilli(epochMillis)));
	}

	public void updateNextScheduledPurgeTime() {
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());
		cal.add(Calendar.HOUR_OF_DAY, 12);
		nextScheduledPurgeTime = cal.getTime();
	}

	public void updateLastPurgeTime() {
		lastPurgeTime = new Date();
	}

	public boolean checkIfLastPurgeInThirthyMin() {
		Calendar cal = Calendar.getInstance();
		Date d2 = lastPurgeTime;
		cal.setTime(d2);
		cal.add(Calendar.MINUTE, 30);
		if (cal.getTime().compareTo(new Date()) > 0) {
			return true;
		}
		return false;
	}
}