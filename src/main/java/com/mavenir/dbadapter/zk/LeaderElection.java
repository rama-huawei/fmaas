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

package org.xgvela.dbadapter.zk;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.leader.LeaderSelector;
import org.apache.curator.framework.recipes.leader.LeaderSelectorListener;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.retry.RetryNTimes;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.xgvela.cnf.Constants;

@Component
public class LeaderElection {
	private static Logger LOG = LogManager.getLogger(LeaderElection.class);
	public static boolean isLeader = false;
	private static String endPoints = System.getenv(Constants.ZK_FQDN);
	private CuratorFramework client = getClient();

	private CuratorFramework getClient() {
		if (client != null) {
			return client;
		}
		int sleepMsBetweenRetries = 100;
		int maxRetries = 3;
		RetryPolicy retryPolicy = new RetryNTimes(maxRetries, sleepMsBetweenRetries);

		CuratorFramework client = CuratorFrameworkFactory.newClient(endPoints, retryPolicy);
		client.start();
		return client;
	}
	public void leaderElection() {
		CuratorFramework client = getClient();
		LeaderSelector leaderSelector = new LeaderSelector(client,
		  "/fmaas/leader",
		  new LeaderSelectorListener() {
		      @Override
		      public void takeLeadership(
		        CuratorFramework client) throws Exception {
		    	  LOG.info("This pod is the leader");
		    	  isLeader = true;
		    	  while(true) {
		    	  }
		      }

			@Override
			public void stateChanged(CuratorFramework client, ConnectionState newState) {
				// TODO Auto-generated method stub

			}
		  });
		leaderSelector.autoRequeue();
		leaderSelector.start();
	}

}
