/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.cloudlaunch.control;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeoutException;

import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.fusesource.cloudlaunch.HostProperties;
import org.fusesource.cloudlaunch.LaunchDescription;
import org.fusesource.cloudlaunch.Process;
import org.fusesource.cloudlaunch.ProcessListener;
import org.fusesource.cloudlaunch.rmi.IRemoteProcessLauncher;
import org.fusesource.cloudlaunch.rmi.RemoteLauncherClient;
import org.fusesource.cloudlaunch.zk.ZooKeeperChildWatcher;
import org.fusesource.cloudlaunch.zk.ZooKeeperExporter;

/**
 * LaunchClient
 * <p>
 * Description:
 * </p>
 * 
 * @author cmacnaug
 * @version 1.0
 */
public class LaunchClient {

    private RemoteLauncherClient comm;
    private ZooKeeper zooKeeper;
    ZooKeeperChildWatcher zooKeeperWatcher;
    private HashMap<String, HostProperties> knownAgents = new HashMap<String, HostProperties>();

    public void start() {
        comm = new RemoteLauncherClient(System.getProperty("user.name") + "-" + System.currentTimeMillis());
        comm.setBindTimeout(5000);
        comm.setLaunchTimeout(10000);
        comm.setKillTimeout(5000);

        ZooKeeperChildWatcher zooKeeperWatcher = new ZooKeeperChildWatcher(zooKeeper, IRemoteProcessLauncher.REGISTRY_PATH) {

            @Override
            public void onChildrenChanged(String path, List<String> children) {
                synchronized (knownAgents) {
                    for (String agentId : children) {
                        if (!knownAgents.containsKey(agentId)) {
                            Stat stat = new Stat();
                            try {
                                byte[] data = zk.getData(path + "/" + agentId, false, stat);
                                IRemoteProcessLauncher irpl = ZooKeeperExporter.unmarshall(data);
                                HostProperties props = irpl.getHostProperties();
                                knownAgents.put(agentId, props);

                                System.out.println("DISCOVERED: " + props.getAgentId());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    knownAgents.keySet().retainAll(children);
                    knownAgents.notifyAll();
                }
            }

        };
        zooKeeperWatcher.start();
    }

    public void destroy() {
        zooKeeperWatcher.stop();
        knownAgents.clear();
        comm.close();
    }

    public void waitForAvailableAgents(long timeout) throws InterruptedException, TimeoutException {
        synchronized (knownAgents) {
            long start = System.currentTimeMillis();
            while (timeout > 0 && knownAgents.isEmpty()) {
                knownAgents.wait(timeout);
                if (knownAgents.size() > 0) {
                    return;
                }
                timeout -= System.currentTimeMillis() - start;
            }

            if (knownAgents.isEmpty()) {
                throw new TimeoutException();
            }
        }
    }

    public HostProperties[] getAvailableAgents() {
        synchronized (knownAgents) {
            return knownAgents.values().toArray(new HostProperties[knownAgents.size()]);
        }
    }

    public Process launch(String agentId, final LaunchDescription launch, ProcessListener handler) throws Exception {
        return comm.launchProcess(agentId, launch, handler);
    }

    public void setZooKeeper(ZooKeeper zooKeeper) {
        this.zooKeeper = zooKeeper;
    }

    public ZooKeeper getZooKeeper() {
        return zooKeeper;
    }

    public void close() throws Exception {

        System.out.println("Shutting down control com");
        comm.close();
    }

}
