/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.meshkeeper.distribution;

import static org.fusesource.meshkeeper.Expression.file;
import static org.fusesource.meshkeeper.Expression.mop;
import static org.fusesource.meshkeeper.Expression.path;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.fusesource.meshkeeper.Distributable;
import org.fusesource.meshkeeper.HostProperties;
import org.fusesource.meshkeeper.JavaLaunch;
import org.fusesource.meshkeeper.LaunchDescription;
import org.fusesource.meshkeeper.MeshKeeper;
import org.fusesource.meshkeeper.MeshProcess;
import org.fusesource.meshkeeper.MeshProcessListener;
import org.fusesource.meshkeeper.MeshContainer;
import org.fusesource.meshkeeper.RegistryWatcher;
import org.fusesource.meshkeeper.classloader.ClassLoaderFactory;
import org.fusesource.meshkeeper.classloader.ClassLoaderServer;
import org.fusesource.meshkeeper.classloader.ClassLoaderServerFactory;
import org.fusesource.meshkeeper.classloader.Marshalled;
import org.fusesource.meshkeeper.launcher.LaunchAgent;
import org.fusesource.meshkeeper.launcher.LaunchAgentService;
import org.fusesource.meshkeeper.launcher.MeshContainerService;
import org.fusesource.meshkeeper.launcher.RemoteBootstrap;

/**
 * LaunchClient
 * <p>
 * Description:
 * </p>
 * 
 * @author cmacnaug
 * @version 1.0
 */
class LaunchClient implements MeshKeeper.Launcher {

    Log log = LogFactory.getLog(this.getClass());

    private MeshKeeper meshKeeper;
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
    private ClassLoaderServer classLoaderServer;
    private int meshContainerCounter;

    public void start() throws Exception {
        name = meshKeeper.registry().addRegistryObject(LAUNCHER_REGISTRY_PATH + System.getProperty("user.name"), true, null);
        name = name.substring(name.lastIndexOf("/") + 1);

        agentWatcher = new RegistryWatcher() {

            public void onChildrenChanged(String path, List<String> children) {
                synchronized (LaunchClient.this) {
                    for (String agentId : children) {
                        if (!knownAgents.containsKey(agentId)) {
                            try {
                                LaunchAgentService pl = meshKeeper.registry().getRegistryObject(path + "/" + agentId);
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

        meshKeeper.registry().addRegistryWatcher(LaunchAgentService.REGISTRY_PATH, agentWatcher);
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

        if (classLoaderServer != null) {
            classLoaderServer.stop();
        }

        //Release reserved ports:
        for (String agentName : reservedPorts.keySet()) {
            releaseAllPorts(agentName);
        }

        try {
            releaseAllAgents();
        } catch (Exception e) {
            e.printStackTrace();
            //            listener.onTRException("Error releasing agents.", e);
        }

        meshKeeper.registry().removeRegistryData(LAUNCHER_REGISTRY_PATH + name, true);
        meshKeeper.registry().removeRegistryWatcher(LaunchAgentService.REGISTRY_PATH, agentWatcher);
        //Clear out any container registrations: 
        meshKeeper.registry().removeRegistryData(MESHCONTAINER_REGISTRY_PATH + name, true);

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
            LaunchAgentService pl = meshKeeper.registry().getRegistryObject(LaunchAgentService.REGISTRY_PATH + "/" + agentName);
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

    public void releaseAllAgents() throws Exception {
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

    public MeshProcess launchProcess(String agentId, final LaunchDescription launch, MeshProcessListener listener) throws Exception {
        checkNotClosed();

        LaunchAgentService agent = getAgent(agentId);
        return agent.launch(launch, (MeshProcessListener) meshKeeper.remoting().export(listener));
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.fusesource.meshkeeper.MeshKeeper.Launcher#launchMeshContainer(java
     * .lang.String, org.fusesource.meshkeeper.MeshProcessListener)
     */
    public MeshContainer launchMeshContainer(String agentId, JavaLaunch launch, ClassLoader loader, MeshProcessListener listener) throws Exception {

        LaunchDescription ld = new LaunchDescription();
        ld.add(launch.getJvm());
        ld.add(launch.getJvmArgs());
        ld.setWorkingDirectory(ld.getWorkingDirectory());
        //This is likely not right when the launch agent is running remotely:
        ld.propageSystemProperties(LaunchAgent.PROPAGATED_SYSTEM_PROPERTIES);
        ld.add("-cp");
        ld.add(path(file(mop(PluginResolver.PROJECT_GROUP_ID + ":meshkeeper-api:" + PluginClassLoader.getModuleVersion())), file(launch.getClasspath())));
                
        ld.add(RemoteBootstrap.class.getName());
        ld.add("--cache");
        ld.add(file("./classloader-cache"));
        ld.add("--distributor");
        ld.add(meshKeeper.getDistributorUri());
        ClassLoaderServer cls = getClassLoaderServer();
        ClassLoaderFactory stub = cls.export(loader, 1);
        String clf = meshKeeper.registry().addRegistryObject("/launchclient-clf/" + System.getProperty("user.name"), true, stub);
        ld.add("--classloader");
        ld.add(clf);
        
        // Add the MeshContainer class to be launched:
        ld.add(org.fusesource.meshkeeper.launcher.MeshContainer.class.getName());

        String regPath = MESHCONTAINER_REGISTRY_PATH + name + "/" + ++meshContainerCounter;
        ld.add(regPath);

        MeshProcess proc = launchProcess(agentId, ld, listener);
        MeshContainerService proxy = meshKeeper.registry().waitForRegistration(regPath, launchTimeout);

        MeshContainerImpl mc = new MeshContainerImpl(proc, proxy);
        return mc;
    }

    public void println(MeshProcess process, String line) {
        byte[] data = (line + "\n").getBytes();
        try {
            process.write(MeshProcess.FD_STD_IN, data);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public Executor createRemoteExecutor(String agentId) throws Exception {
        checkNotClosed();
        final LaunchAgentService agent = getAgent(agentId);
        return new Executor() {
            public void execute(Runnable command) {
                try {
                    launch(agent, command, null);
                } catch (Exception e) {
                    throw new RejectedExecutionException(e);
                }
            }
        };
    }

    public MeshProcess launch(String agentId, Runnable runnable, MeshProcessListener handler) throws Exception {
        checkNotClosed();
        return launch(getAgent(agentId), runnable, handler);
    }

    private MeshProcess launch(LaunchAgentService agent, Runnable runnable, MeshProcessListener handler) throws Exception {
        checkNotClosed();
        ClassLoaderFactory factory = getClassLoaderServer().export(runnable.getClass().getClassLoader(), 100);
        Marshalled<Runnable> marshalled = new Marshalled<Runnable>(factory, runnable);
        return agent.launch(marshalled, handler);
    }

    public synchronized ClassLoaderServer getClassLoaderServer() throws Exception {
        if (classLoaderServer == null) {
            if (meshKeeper == null) {
                throw new IllegalArgumentException("distributor or classLoaderServer property must be set");
            }
            classLoaderServer = ClassLoaderServerFactory.create("basic:", meshKeeper);
            classLoaderServer.start();
        }
        return classLoaderServer;
    }

    public void setClassLoaderServer(ClassLoaderServer classLoaderServer) {
        this.classLoaderServer = classLoaderServer;
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

    public MeshKeeper getMeshKeeper() {
        return meshKeeper;
    }

    public void setMeshKeeper(MeshKeeper distributor) {
        this.meshKeeper = distributor;
    }

    private static class MeshContainerImpl implements MeshContainer {
        private final MeshProcess process;
        private final MeshContainerService container;

        MeshContainerImpl(MeshProcess processProxy, MeshContainerService containerProxy) {
            this.process = processProxy;
            this.container = containerProxy;

        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.fusesource.meshkeeper.MeshContainer#host(org.fusesource.meshkeeper
         * .Distributable)
         */
        public Distributable host(String name, Distributable object) throws Exception {
            return container.host(name, object);

        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.fusesource.meshkeeper.MeshContainer#unhost(org.fusesource.meshkeeper
         * .Distributable)
         */
        public void unhost(String name) {
            try {
                container.unhost(name);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.fusesource.meshkeeper.MeshContainer#run(java.lang.Runnable)
         */
        public void run(Runnable r) throws Exception {
            container.run(r);
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.fusesource.meshkeeper.MeshProcess#close(int)
         */
        public void close(int fd) throws IOException {
            process.close(fd);
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.fusesource.meshkeeper.MeshProcess#isRunning()
         */
        public boolean isRunning() throws Exception {
            return process.isRunning();
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.fusesource.meshkeeper.MeshProcess#kill()
         */
        public void kill() throws Exception {
            container.close();
            process.kill();
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.fusesource.meshkeeper.MeshProcess#open(int)
         */
        public void open(int fd) throws IOException {
            process.open(fd);

        }

        /*
         * (non-Javadoc)
         * 
         * @see org.fusesource.meshkeeper.MeshProcess#write(int, byte[])
         */
        public void write(int fd, byte[] data) throws IOException {
            process.write(fd, data);
        }

    }

}
