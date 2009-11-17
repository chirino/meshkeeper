/**
 *  Copyright (C) 2009 Progress Software, Inc. All rights reserved.
 *  http://fusesource.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.fusesource.meshkeeper.distribution;

import static org.fusesource.meshkeeper.Expression.string;
import static org.fusesource.meshkeeper.Expression.sysProperty;

import java.io.IOException;
import java.io.Serializable;
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
import org.fusesource.meshkeeper.HostProperties;
import org.fusesource.meshkeeper.JavaLaunch;
import org.fusesource.meshkeeper.LaunchDescription;
import org.fusesource.meshkeeper.MeshContainer;
import org.fusesource.meshkeeper.MeshKeeper;
import org.fusesource.meshkeeper.MeshKeeperFactory;
import org.fusesource.meshkeeper.MeshProcess;
import org.fusesource.meshkeeper.MeshProcessListener;
import org.fusesource.meshkeeper.RegistryWatcher;
import org.fusesource.meshkeeper.MeshKeeper.DistributionRef;
import org.fusesource.meshkeeper.MeshKeeper.Launcher;
import org.fusesource.meshkeeper.classloader.ClassLoaderFactory;
import org.fusesource.meshkeeper.classloader.ClassLoaderServer;
import org.fusesource.meshkeeper.classloader.ClassLoaderServerFactory;
import org.fusesource.meshkeeper.launcher.LaunchAgent;
import org.fusesource.meshkeeper.launcher.LaunchAgentService;
import org.fusesource.meshkeeper.launcher.LaunchClientService;
import org.fusesource.meshkeeper.launcher.MeshContainerService;
import org.fusesource.meshkeeper.util.DefaultProcessListener;

;

/**
 * LaunchClient
 * <p>
 * Description:
 * </p>
 * 
 * @author cmacnaug
 * @version 1.0
 */
class LaunchClient extends AbstractPluginClient implements MeshKeeper.Launcher, LaunchClientService {

    Log log = LogFactory.getLog(this.getClass());

    RegistryWatcher agentWatcher;
    private long killTimeout = 1000 * 5;
    private long launchTimeout = 1000 * 60;
    private long bindTimeout = 1000 * 10;
    private HashMap<String, LaunchAgentService> knownAgents = new HashMap<String, LaunchAgentService>();
    private HashMap<String, HostProperties> agentProps = new HashMap<String, HostProperties>();

    private HashSet<MeshProcessWatcher> runningProcesses = new HashSet<MeshProcessWatcher>();

    private AtomicBoolean closed = new AtomicBoolean();
    private final HashMap<String, LaunchAgentService> boundAgents = new HashMap<String, LaunchAgentService>();
    private final HashMap<String, HashSet<Integer>> reservedPorts = new HashMap<String, HashSet<Integer>>();
    private String name;
    private DistributionRef<LaunchClientService> distributionRef;

    private ClassLoaderServer classLoaderServer;
    private ClassLoaderFactory bootStrapClassLoaderFactory;
    private ClassLoader bootStrapClassLoader;
    private int meshContainerCounter;

    public void start() throws Exception {
        distributionRef = meshKeeper.distribute(LAUNCHER_REGISTRY_PATH + "/" + System.getProperty("user.name"), true, (LaunchClientService) this, LaunchClientService.class);
        name = distributionRef.getRegistryPath().substring(distributionRef.getRegistryPath().lastIndexOf("/") + 1);
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

                                if (log.isDebugEnabled()) {
                                    log.debug("DISCOVERED: " + props.getAgentId());
                                }
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

        meshKeeper.registry().addRegistryWatcher(LaunchAgentService.LAUNCH_AGENT_REGISTRY_PATH, agentWatcher);
    }

    /**
     * Requests the specified number of tcp ports from the specified process
     * launcher.
     * 
     * @param agentName The name of the process launcher
     * @param count The number of ports.
     * @return The reserved ports
     * @throws Exception If there is an error reserving the requested number of
     *             ports.
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
        for (String agentName : reservedPorts.keySet().toArray(new String[] {})) {
            releaseAllPorts(agentName);
        }

        try {
            releaseAllAgents();
        } catch (Exception e) {
            e.printStackTrace();
            //            listener.onTRException("Error releasing agents.", e);
        }

        killRunningProcesses();

        meshKeeper.undistribute(this);
        meshKeeper.registry().removeRegistryWatcher(LaunchAgentService.LAUNCH_AGENT_REGISTRY_PATH, agentWatcher);
        //Clear out any container registrations: 
        meshKeeper.registry().removeRegistryData(distributionRef.getRegistryPath(), true);
        meshKeeper.registry().removeRegistryData(MESHCONTAINER_REGISTRY_PATH + "/" + name, true);

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
            LaunchAgentService pl = meshKeeper.registry().getRegistryObject(LaunchAgentService.LAUNCH_AGENT_REGISTRY_PATH + "/" + agentName);
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
        MeshProcessWatcher watcher = new MeshProcessWatcher(listener, agentId);
        addWatchedProcess(watcher);
        try {
            watcher.setProcess(agent.launch(launch, distributionRef.getRegistryPath(), watcher.getProxy()));
        } catch (Exception e) {
            watcher.cleanup();
            throw e;
        }

        return watcher.getProcess();

    }

    public static class MeshContainerLaunch extends JavaLaunch {
        private String regPath;
    }

    public LaunchDescription createLaunchDescription() {
        return new LaunchDescription();
    }

    public JavaLaunch createJavaLaunch(String mainClass, String... args) {
        return setupJavaLaunch(new JavaLaunch(), mainClass, args);
    }

    public JavaLaunch createMeshContainerLaunch() throws Exception {
        MeshContainerLaunch launch = new MeshContainerLaunch();
        launch.regPath = MESHCONTAINER_REGISTRY_PATH + "/" + name + "/" + ++meshContainerCounter;
        setupBootstrapJavaLaunch(launch, org.fusesource.meshkeeper.launcher.MeshContainer.class.getName(), launch.regPath);
        return launch;
    }

    public JavaLaunch createBootstrapJavaLaunch(String mainClass, String... args) throws Exception {
        return setupBootstrapJavaLaunch(new JavaLaunch(), mainClass, args);
    }

    private JavaLaunch setupJavaLaunch(JavaLaunch launch, String mainClass, String... args) {
        launch.setMainClass(mainClass);
        launch.addArgs(args);

        //Add System properties to be evaluated on the agent side:
        for (String propName : LaunchAgent.PROPAGATED_SYSTEM_PROPERTIES) {
            launch.addSystemProperty(sysProperty(propName, null));
        }
        //And the registry connect uri
        launch.addSystemProperty(sysProperty(MeshKeeperFactory.MESHKEEPER_REGISTRY_PROPERTY, string(meshKeeper.getRegistryConnectUri())));
        //Set the UUID value
        launch.addSystemProperty(MeshKeeperFactory.MESHKEEPER_UUID_PROPERTY, meshKeeper.getUUID());
        return launch;
    }

    private JavaLaunch setupBootstrapJavaLaunch(JavaLaunch launch, String mainClass, String... args) {
        launch.setBootstrapClassLoaderFactory(getBootstrapClassLoaderFactory().getRegistryPath());
        return setupJavaLaunch(launch, mainClass, args);
    }

    public MeshContainer launchMeshContainer(String agentId) throws Exception {
        return launchMeshContainer(agentId, null, null);
    }

    public MeshContainer launchMeshContainer(String agentId, MeshProcessListener listener) throws Exception {
        return launchMeshContainer(agentId, null, listener);
    }

    public MeshContainer launchMeshContainer(String agentId, JavaLaunch launch, MeshProcessListener listener) throws Exception {
        if (launch == null) {
            launch = createMeshContainerLaunch();
        }

        if (!(launch instanceof MeshContainerLaunch)) {
            throw new IllegalStateException("Invalid JavaLaunch, not created via createMeshContainerLaunch");
        }

        String regPath = ((MeshContainerLaunch) launch).regPath;
        MeshProcess proc = launchProcess(agentId, launch.toLaunchDescription(), listener);
        try {
            MeshContainerService proxy = meshKeeper.registry().waitForRegistration(regPath, launchTimeout);
            MeshContainerImpl mc = new MeshContainerImpl(proc, proxy);
            return mc;
        } catch (Exception e) {
            proc.kill();
            throw e;
        }

    }

    public void println(MeshProcess process, String line) {
        byte[] data = (line + "\n").getBytes();
        try {
            process.write(MeshProcess.FD_STD_IN, data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sets the classloader that will be used to bootstrap java launches. This
     * classloader will be used for launched {@link MeshContainer}s and launched
     * {@link Runnable}s.
     * 
     * The launcher will internally create {@link ClassLoaderServer} and
     * {@link ClassLoaderFactory} to host the specified classloader. the
     * {@link ClassLoaderServer}'s lifecycle will be tied to that of this
     * {@link Launcher}
     * 
     * If the user explicitly sets a {@link ClassLoaderFactory} via
     * {@link #setBootstrapClassLoaderFactory(ClassLoaderFactory)} that factory
     * will be used instead. Otherwise calls to
     * {@link #getBootstrapClassLoaderFactory()} will return the
     * {@link ClassLoaderFactory} associated witht the specified
     * {@link ClassLoader}.
     * 
     * @param classLoader The classloader to be used for bootstrapping.
     * @throws Exception if the classloader can't be used for bootstrapping.
     */
    public synchronized void setBootstrapClassLoader(ClassLoader classLoader) throws Exception {

        if (bootStrapClassLoader != classLoader) {
            bootStrapClassLoader = classLoader;
            bootStrapClassLoaderFactory = null;
        }
    }

    /**
     * Gets the classloader that will be used to bootstrap java launches. This
     * classloader will be used for launched {@link MeshContainer}s and launched
     * {@link Runnable}s.
     * 
     * @return the current bootstrap {@link ClassLoader}.
     */
    public synchronized ClassLoader getBootstrapClassLoader() {
        if (bootStrapClassLoader == null) {
            bootStrapClassLoader = getClass().getClassLoader();
        }
        return bootStrapClassLoader;
    }

    /**
     * Sets the {@link ClassLoaderFactory} that will be used to bootstrap java
     * launches. This classloader will be used for launched
     * {@link MeshContainer}s and launched {@link Runnable}s.
     * 
     * The caller is responsible for managing the lifecycle of the associated
     * {@link ClassLoaderServer}
     * 
     * @param bootStrapClassLoaderFactory The factory stub to use for
     *            bootstrapping java launches.
     */
    public synchronized void setBootstrapClassLoaderFactory(ClassLoaderFactory bootStrapClassLoaderFactory) {
        this.bootStrapClassLoaderFactory = bootStrapClassLoaderFactory;
    }

    /**
     * Gets the {@link ClassLoaderFactory} that will be used to bootstrap java
     * launches. This classloader will be used for launched
     * {@link MeshContainer}s and launched {@link Runnable}s. If one hasn't yet
     * been created it will be created on demand using the {@link ClassLoader}
     * returned by {@link #getBootstrapClassLoader()}
     * 
     * @param factory The factory stub to use for bootstrapping java launches.
     */
    public synchronized ClassLoaderFactory getBootstrapClassLoaderFactory() {
        if (bootStrapClassLoaderFactory == null) {
            ClassLoader loader = getBootstrapClassLoader();

            if (classLoaderServer == null) {
                try {
                    classLoaderServer = ClassLoaderServerFactory.create("basic:", getMeshKeeper());
                    classLoaderServer.start();
                } catch (Exception e) {
                    throw new RuntimeException("Error creating classloader server", e);
                }

            }

            try {
                bootStrapClassLoaderFactory = classLoaderServer.export(loader, "/classloader/" + name, 100);
            } catch (Exception e) {
                throw new RuntimeException("Error creating classloader factory", e);
            }

        }
        return bootStrapClassLoaderFactory;
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

    private class MeshContainerImpl implements MeshContainer {
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
        public <T extends Serializable> T host(String name, T object, Class<?>... serviceInterfaces) throws Exception {
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
        public <R extends java.lang.Runnable & Serializable> void run(R r) throws Exception {
            container.run(r);
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.fusesource.meshkeeper.MeshProcess#close(int)
         */
        public <T, C extends java.util.concurrent.Callable<T> & Serializable> T call(C c) throws Exception {
            return container.call(c);
        }

        public void close() {
            try {
                kill();
            } catch (Exception e) {
                log.warn("error closing meshcontainer", e);
            }
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

    private synchronized void addWatchedProcess(MeshProcessWatcher watched) {
        runningProcesses.add(watched);
    }

    private synchronized void removeWatchedProcess(MeshProcessWatcher watched) {
        runningProcesses.remove(watched);
        notifyAll();
    }

    private void killRunningProcesses() {
        while (true) {
            MeshProcessWatcher[] running = null;
            synchronized (this) {
                if (runningProcesses.isEmpty()) {
                    return;
                }
                log.warn("Killing " + runningProcesses.size() + " processes");
                running = new MeshProcessWatcher[runningProcesses.size()];
                running = runningProcesses.toArray(running);
            }

            for (MeshProcessWatcher w : running) {
                try {
                    w.getProcess().kill();
                } catch (Exception e) {
                    w.cleanup();
                }
            }

            synchronized (this) {
                while (!runningProcesses.isEmpty()) {
                    int count = runningProcesses.size();
                    try {
                        wait(killTimeout);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    if (count == runningProcesses.size()) {
                        log.warn("Timed out waiting to kill processes");
                        return;
                    }
                }
            }
        }
    }

    private class MeshProcessWatcher implements MeshProcessListener {
        private final MeshProcessListener delegate;
        private MeshProcessListener proxy = null;
        private AtomicBoolean running = new AtomicBoolean(true);
        private MeshProcess process;

        MeshProcessWatcher(MeshProcessListener delegate, String id) {
            if (delegate == null) {
                this.delegate = new DefaultProcessListener(id);
            } else {
                this.delegate = delegate;
            }
        }

        public synchronized MeshProcessListener getProxy() throws Exception {
            if (proxy == null) {
                proxy = meshKeeper.remoting().export(this, MeshProcessListener.class);
            }
            return proxy;
        }

        public synchronized void setProcess(MeshProcess process) {
            this.process = process;
        }

        public synchronized MeshProcess getProcess() {
            return process;
        }

        public void cleanup() {
            synchronized (this) {
                if (proxy != null) {
                    try {
                        meshKeeper.remoting().unexport(this);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            removeWatchedProcess(this);
        }

        public void onProcessError(Throwable thrown) {
            delegate.onProcessError(thrown);
        }

        public void onProcessExit(int exitCode) {
            delegate.onProcessExit(exitCode);
            running.set(false);
            cleanup();
        }

        public void onProcessInfo(String message) {
            delegate.onProcessInfo(message);
        }

        public void onProcessOutput(int fd, byte[] output) {
            delegate.onProcessOutput(fd, output);
        }

    }

    public void ping() {

    }

}
