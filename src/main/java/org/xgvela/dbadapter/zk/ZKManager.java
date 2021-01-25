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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZKUtil;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Stat;
import org.springframework.stereotype.Component;

import org.xgvela.cnf.Constants;

@Component
public class ZKManager {

	private static ZooKeeper zkeeper;
	private static ZookeeperConnection zkConnection;
	private static Logger LOG = LogManager.getLogger(ZKManager.class);

	public void initialize() {
		zkConnection = new ZookeeperConnection();
		String host = System.getenv(Constants.ZK_FQDN);
		try {
			zkeeper = zkConnection.connect(host);
			LOG.debug("Connection to zookeeper established successfully");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void closeConnection() {
		try {
			zkConnection.close();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void create(String path, byte[] data) throws KeeperException, InterruptedException {
		if(zkeeper==null) {
			initialize();
		}
		String zNode[] = path.split("/");
		String currentPath = "/";
		for (String i : zNode) {
			if (!i.isEmpty()) {
				currentPath += i;
				if (zkeeper.exists(currentPath, false) == null) {
					zkeeper.create(currentPath, data, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
				}
				currentPath += "/";
			}
		}
		try {
			LOG.debug("zNode created at path:" + path + " with data:" + new String(data, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public Object getZNodeData(String path, boolean watchFlag) throws KeeperException, InterruptedException {
		if(zkeeper==null) {
			initialize();
		}
		byte[] b = null;
		b = zkeeper.getData(path, null, null);
		try {
			return new String(b, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public void update(String path, byte[] data) throws KeeperException, InterruptedException {
		if(zkeeper==null) {
			initialize();
		}
		int version = zkeeper.exists(path, true).getVersion();
		zkeeper.setData(path, data, version);
	}

	public List<String> getListOFChildren(String path, boolean watchFlag) throws KeeperException, InterruptedException {
		if(zkeeper==null) {
			initialize();
		}
		List<String> b = new ArrayList<>();
		b = zkeeper.getChildren(path, null);
		return b;
	}

	public boolean ifExists(String path) {
		if(zkeeper==null) {
			initialize();
		}
		try {
			if (zkeeper.exists(path, null) != null)
				return true;
			else
				return false;
		} catch (KeeperException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	public void removeZnode(String path) {
		if(zkeeper==null) {
			initialize();
		}
		if(ifExists(path)) {
			int version;
			try {
				version = zkeeper.exists(path, true).getVersion();
				zkeeper.delete(path, version);
			} catch (InterruptedException | KeeperException e) {
				e.printStackTrace();
			}
		}

	}

	public void deleteRecursive(String pathRoot) {
		if(zkeeper==null) {
			initialize();
		}
		try {
			ZKUtil.deleteRecursive(zkeeper, pathRoot);
		} catch (InterruptedException | KeeperException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public long getCreateDate(String path) {
		if(zkeeper==null) {
			initialize();
		}
		byte[] b = null;
		Stat stat = new Stat();
		try {
			b = zkeeper.getData(path, null, stat);
		} catch (KeeperException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return stat.getCtime();
	}

}
