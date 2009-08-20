/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.cloudlaunch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.fusesource.cloudlaunch.distribution.Distributor;
import org.fusesource.cloudlaunch.distribution.registry.RegistryWatcher;
import org.fusesource.cloudlaunch.launcher.LaunchAgentService;

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

    Log log = LogFactory.getLog(this.getClass());

    private Distributor distributor;
    RegistryWatcher agentWatcher;
    private long killTimeout = 1000 * 5;
    private long launchTimeout = 1000 * 60;
    private long bindTimeout = 1000 * 10;
    private HashMap<String, LaunchAgentService> knownAgents = new HashMap<String, LaunchAgentService>();
    private HashMap<String, HostProperties> agentProps = new HashMap<String, HostProperties>();

    private AtomicBoolean closed = new AtomicBoolean();
    private final HashMap<String, LaunchAgentService> boundAgents = new HashMap<String, LaunchAgentService>();
    private final HashMap<String, HashSet<Integer>> reservedPorts = new HashMap<String, HashSet<Integer>>();
    private String name;

    public void start() throws Exception {
        name = distributor.getRegistry().addObject("/launchclients/" + System.getProperty("user.name"), true, null);

        agentWatcher = new RegistryWatcher() {

            public void onChildrenChanged(String path, List<String> children) {
                synchronized (LaunchClient.this) {
                    for (String agentId : children) {
                        if (!knownAgents.containsKey(agentId)) {
                            try {
                                LaunchAgentService pl = distributor.getRegistry().getObject(path + "/" + agentId);
                                knownAgents.put(agentId, pl);
                                HostProperties props = pl.getHostProperties();
                                agentProps.put(agentId, props);

                                log.info("DISCOVERED: " + props.getAgentId());
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

        distributor.getRegistry().addRegistryWatcher(LaunchAgentService.REGISTRY_PATH, agentWatcher);
    }

    /**
     * Requests the specified number of tcp ports from the specified process
     * launcher.
     * 
     * @param agentName
     *            The name of the process launcher
     * @param count
     *            The number of ports.
     * @return The reserved ports
     * @throws Exception
     *             If there is an error reserving the requested number of ports.
     */
    public synchronized List<Integer> reserveTcpPorts(String agentName, int count) throws Exception {
        agentName = agentName.toUpperCase();
        LaunchAgentService agent = getAgent(agentName);

        List<Integer> ports = agent.reserveTcpPorts(count);
        HashSet<Integer> reserved = reservedPorts.get(agentName);
        if (reserved == null) {
            reserved = new HashSet<Integer>();
            reservedPorts.put(agentName, reserved);
        }
        reserved.addAll(ports);
        return ports;
    }

    /**
     * Releases previously reserved ports at the launcher.
     */
    public synchronized void releasePorts(String agentName, Collection<Integer> ports) throws Exception {
        agentName = agentName.toUpperCase();
        HashSet<Integer> reserved = reservedPorts.get(agentName);
        if (reserved != null) {
            reserved.removeAll(ports);
            if (reserved.isEmpty()) {
                reservedPorts.remove(agentName);
            }
        }
        LaunchAgentService agent = getAgent(agentName);
        agent.releaseTcpPorts(ports);

    }

    /**
     * Releases all ports that have been reserved on the specified launcher.
     */
    public synchronized void releaseAllPorts(String agentName) throws Exception {
        agentName = agentName.toUpperCase();
        HashSet<Integer> reserved = reservedPorts.remove(agentName);
        if (reserved != null) {
            getAgent(agentName).releaseTcpPorts(reserved);
        }
    }

    public synchronized void destroy() throws Exception {

        //Release reserved ports:
        for (String agentName : reservedPorts.keySet()) {
            releaseAllPorts(agentName);
        }

        try {
            releaseAll();
        } catch (Exception e) {
            e.printStackTrace();
            //            listener.onTRException("Error releasing agents.", e);
        }
        distributor.getRegistry().remove(name, false);
        distributor.getRegistry().removeRegistryWatcher(LaunchAgentService.REGISTRY_PATH, agentWatcher);
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

    private LaunchAgentService getAgent(String agentName) throws Exception {
        agentName = agentName.toUpperCase();
        LaunchAgentService launcher;
        synchronized (this) {
            launcher = knownAgents.get(agentName);
        }

        if (launcher == null) {
            LaunchAgentService pl = distributor.getRegistry().getObject(LaunchAgentService.REGISTRY_PATH + "/" + agentName);
            if (pl != null) {
                HostProperties props = pl.getHostProperties();
                synchronized (this) {
                    launcher = knownAgents.get(agentName);
                    if (launcher == null) {
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

        LaunchAgentService agent = getAgent(agentName);
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
        LaunchAgentService agent = boundAgents.remove(agentName);
        if (agent != null) {
            agent.unbind(name);
        }
    }

    public void releaseAll() throws Exception {
        checkNotClosed();

        ArrayList<String> failed = new ArrayList<String>();
        for (Map.Entry<String, LaunchAgentService> entry : boundAgents.entrySet()) {
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

        LaunchAgentService agent = getAgent(agentId);
        return agent.launch(launch, (ProcessListener) distributor.export(listener).getStub());
    }

    public static void println(Process process, String line) {
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
