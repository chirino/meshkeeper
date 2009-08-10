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

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.fusesource.cloudlaunch.HostProperties;
import org.fusesource.cloudlaunch.LaunchDescription;
import org.fusesource.cloudlaunch.Process;
import org.fusesource.cloudlaunch.ProcessLauncher;
import org.fusesource.cloudlaunch.ProcessListener;
import org.fusesource.cloudlaunch.registry.RegistryWatcher;
import org.fusesource.cloudlaunch.rmi.Distributor;

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

    private Distributor distributor;
    RegistryWatcher agentWatcher;
    private long killTimeout = 1000 * 5;
    private long launchTimeout = 1000 * 60;
    private long bindTimeout = 1000 * 10;
    private HashMap<String, ProcessLauncher> knownAgents = new HashMap<String, ProcessLauncher>();
    private HashMap<String, HostProperties> agentProps = new HashMap<String, HostProperties>();

    private AtomicBoolean closed = new AtomicBoolean();
    private final HashMap<String, ProcessLauncher> boundAgents = new HashMap<String, ProcessLauncher>();
    
    private String name;

    public void start() throws Exception {
        name = distributor.getRegistry().addObject("/launchclients/" + System.getProperty("user.name"), true, null);


        agentWatcher = new RegistryWatcher() {

            public void onChildrenChanged(String path, List<String> children) {
                synchronized (LaunchClient.this) {
                    for (String agentId : children) {
                        if (!knownAgents.containsKey(agentId)) {
                            try {
                                ProcessLauncher pl = distributor.getRegistry().getObject(path + "/" + agentId);
                                knownAgents.put(agentId, pl);
                                HostProperties props = pl.getHostProperties();
                                agentProps.put(agentId, props);

                                System.out.println("DISCOVERED: " + props.getAgentId());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    knownAgents.keySet().retainAll(children);
                    agentProps.keySet().retainAll(children);
                    LaunchClient.this.notifyAll();
                }
            }
        };

        distributor.getRegistry().addRegistryWatcher(ProcessLauncher.REGISTRY_PATH, agentWatcher);
    }

    public void destroy() throws Exception {
        try {
            releaseAll();
        } catch (Exception e) {
            e.printStackTrace();
            //            listener.onTRException("Error releasing agents.", e);
        }
        distributor.getRegistry().remove(name, false);
        distributor.getRegistry().removeRegistryWatcher(ProcessLauncher.REGISTRY_PATH, agentWatcher);
        knownAgents.clear();
        agentProps.clear();
        closed.set(true);
    }

    public void waitForAvailableAgents(long timeout) throws InterruptedException, TimeoutException {
        synchronized (this) {
            long start = System.currentTimeMillis();
            while (timeout > 0 && agentProps.isEmpty()) {
                wait(timeout);
                if (agentProps.size() > 0) {
                    return;
                }
                timeout -= System.currentTimeMillis() - start;
            }

            if (agentProps.isEmpty()) {
                throw new TimeoutException();
            }
        }
    }

    private ProcessLauncher getAgent(String agentName) throws Exception {
        agentName = agentName.toUpperCase();
        ProcessLauncher launcher;
        synchronized (this) {
            launcher = knownAgents.get(agentName);
        }
        
        if(launcher == null)
        {
            ProcessLauncher pl = distributor.getRegistry().getObject(ProcessLauncher.REGISTRY_PATH + "/" + agentName);
            if(pl != null)
            {
                HostProperties props = pl.getHostProperties();
                synchronized(this)
                {
                    launcher = knownAgents.get(agentName);
                    if(launcher == null)
                    {
                        launcher = pl;
                        knownAgents.put(agentName, pl);
                        agentProps.put(agentName, props);
                    }
                    notifyAll();
                }
            }
        }
        
        if (launcher == null) {
            throw new Exception("Agent not found:" + agentName);
        }
        return launcher;
       
    }

    public void bindAgent(String agentName) throws Exception {
        checkNotClosed();

        agentName = agentName.toUpperCase();
        if (boundAgents.containsKey(agentName)) {
            return;
        }

        ProcessLauncher agent = getAgent(agentName);
        agent.bind(name);
    }

    public HostProperties[] getAvailableAgents() {
        synchronized (knownAgents) {
            return agentProps.values().toArray(new HostProperties[agentProps.size()]);
        }
    }

    public void releaseAgent(String agentName) throws Exception {
        checkNotClosed();
        agentName = agentName.toUpperCase();
        ProcessLauncher agent = boundAgents.remove(agentName);
        if (agent != null) {
            agent.unbind(name);
        }
    }

    public void releaseAll() throws Exception {
        checkNotClosed();

        ArrayList<String> failed = new ArrayList<String>();
        for (Map.Entry<String, ProcessLauncher> entry : boundAgents.entrySet()) {
            try {
                entry.getValue().unbind(name);
            } catch (Exception ignore) {
                failed.add(entry.getKey());
            }
        }
        if (!failed.isEmpty()) {
            throw new Exception("Failed to release: " + failed);
        }
    }

    private void checkNotClosed() {
        if (closed.get()) {
            throw new IllegalStateException("closed");
        }
    }

    public Process launchProcess(String agentId, final LaunchDescription launch, ProcessListener listener) throws Exception {
        checkNotClosed();
        
        ProcessLauncher agent = getAgent(agentId);
        return agent.launch(launch, (ProcessListener) distributor.export(listener).getStub());
    }

    public static void println(Process process, String line) throws RemoteException {
        byte[] data = (line + "\n").getBytes();
        try {
            process.write(Process.FD_STD_IN, data);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    public long getBindTimeout() {
        return bindTimeout;
    }

    public void setBindTimeout(long bindTimeout) {
        this.bindTimeout = bindTimeout;
    }

    public long getLaunchTimeout() {
        return launchTimeout;
    }

    public void setLaunchTimeout(long launchTimeout) {
        this.launchTimeout = launchTimeout;
    }

    public long getKillTimeout() {
        return killTimeout;
    }

    public void setKillTimeout(long killTimeout) {
        this.killTimeout = killTimeout;
    }

    public Distributor getDistributor() {
        return distributor;
    }

    public void setDistributor(Distributor distributor) {
        this.distributor = distributor;
    }
}
