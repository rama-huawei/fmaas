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

package org.xgvela.cnf.elastic;

import java.io.IOException;

import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig.Builder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestClientBuilder.RequestConfigCallback;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.context.annotation.Configuration;
import org.xgvela.cnf.Constants;

@Configuration
public class ElasticSearchClientConfiguration {

	private static Logger LOG = LogManager.getLogger(ElasticSearchClientConfiguration.class);

	private static RestHighLevelClient restHighLevelClient;

	private static Boolean isLess = null;

	private static final String esVersion = System.getenv(Constants.ES_VERSION);

	public static final String HOST = System.getenv(Constants.ES_HOST);

	public static final int PORT_ONE = Integer.parseInt(System.getenv(Constants.ES_PORT));

	private static final String SCHEME = System.getenv(Constants.ES_SCHEME);

	public RestHighLevelClient getConnection() {

		if (restHighLevelClient == null) {

			if (esVersion != null)
				LOG.debug("Creating High-Level REST client for ES." + "Host:" + HOST + " , Port:" + PORT_ONE
						+ ", Scheme:" + SCHEME + " ,ES Version:" + esVersion);
			else
				LOG.debug("Creating High-Level REST client for ES." + "Host:" + HOST + " , Port:" + PORT_ONE
						+ ", Scheme:" + SCHEME);

			RestClientBuilder builder = RestClient.builder(new HttpHost(HOST, PORT_ONE, SCHEME));

			builder.setRequestConfigCallback(new RequestConfigCallback() {

				@Override
				public Builder customizeRequestConfig(Builder requestConfigBuilder) {
					return requestConfigBuilder.setConnectionRequestTimeout(0).setConnectTimeout(200 * 1000)
							.setSocketTimeout(200 * 1000);
				}
			});
			restHighLevelClient = new RestHighLevelClient(builder);
			LOG.debug("Successfully created High-Level REST client for ElasticSearch.");
		}
		return restHighLevelClient;
	}

	public void closeConnection() {
		try {
			restHighLevelClient.close();
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		}
		restHighLevelClient = null;
	}

	public Boolean isLessThanSixPointEight() {

		if (isLess == null) {
			if (esVersion != null) {
				if ((int) (esVersion.charAt(0) - '0') < 7) {
					if ((int) (esVersion.charAt(2) - '0') < 8) {
						isLess = true;
					} else {
						isLess = false;
					}
				} else if ((int) (esVersion.charAt(0) - '0') >= 7) {
					isLess = false;
				}
			} else {
				isLess = true;
			}
		}
		return isLess;
	}
}
