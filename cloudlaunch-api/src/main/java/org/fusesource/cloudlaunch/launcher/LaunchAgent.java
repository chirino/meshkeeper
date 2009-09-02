/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.cloudlaunch.launcher;

import java.io.File;
import java.io.IOException;
import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.fusesource.cloudlaunch.HostProperties;
import org.fusesource.cloudlaunch.LaunchDescription;
import org.fusesource.cloudlaunch.Process;
import org.fusesource.cloudlaunch.ProcessListener;
import org.fusesource.cloudlaunch.classloader.Marshalled;
import org.fusesource.cloudlaunch.distribution.Distributor;
import org.fusesource.cloudlaunch.distribution.PluginClassLoader;
import org.fusesource.cloudlaunch.distribution.resource.ResourceManager;
import org.fusesource.cloudlaunch.util.internal.FileUtils;
import org.fusesource.mop.support.ArtifactId;

/**
 * @author chirino
 */
public class LaunchAgent implements LaunchAgentService {
    public static final long CLEANUP_TIMEOUT = 60000;
    public static final String LOCAL_REPO_PROP = "org.fusesource.testrunner.localRepoDir";
    public static final Log LOG = LogFactory.getLog(LaunchAgent.class);

    private String exclusiveOwner;

    private String agentId; //The unique identifier for this agent (specified in ini file);
    private boolean started = false;
    private File dataDirectory = new File(".");

    //ProcessHandlers:
    private final Map<Integer, LocalProcess> processes = new HashMap<Integer, LocalProcess>();
    int pidCounter = 0;
    private Thread shutdownHook;

    private HostPropertiesImpl properties = new HostPropertiesImpl();

    private Monitor monitor = new Monitor(this);
    private Distributor distributor;

    public List<Integer> reserveTcpPorts(int count) throws Exception {
        return Arrays.asList(PortReserver.reservePorts(PortReserver.TCP, count));
    }
    
    public void releaseTcpPorts(Collection<Integer> ports){
        PortReserver.releasePorts(PortReserver.TCP, ports);
    }

    synchronized public void bind(String owner) throws Exception {
        if (exclusiveOwner == null) {
            exclusiveOwner = owner;
            LOG.info("Now bound to: " + exclusiveOwner);
        } else if (!exclusiveOwner.equals(owner)) {
            throw new Exception("Bind failure, already bound: " + exclusiveOwner);
        }
    }

    synchronized public void unbind(String owner) throws Exception {
        if (exclusiveOwner == null) {
        } else if (exclusiveOwner.equals(owner)) {
            LOG.info("Bind to " + exclusiveOwner + " released");
            exclusiveOwner = null;
        } else {
            throw new Exception("Release failure, different owner: " + exclusiveOwner);
        }
    }

    public Process launch(Marshalled<Runnable> runnable, ProcessListener handler) throws Exception {
        String path = distributor.getRegistry().addObject(getRegistryPath() + ":runnable", true, runnable);

        // Figure out the boostrap classpath using mop.
        String artifact = PluginClassLoader.CLOUDLAUNCH_GROUP_ID + ":cloudlaunch-api:" + PluginClassLoader.getModuleVersion();
        ArrayList<ArtifactId> artifactId = new ArrayList<ArtifactId>(1);
        artifactId.add(ArtifactId.parse(artifact));
        String classpath = PluginClassLoader.getMopRepository().classpath(artifactId);

        LaunchDescription ld = new LaunchDescription();
        ld.add("java");
        ld.add("-cp");
        ld.add(classpath);
        ld.add(RemoteBootstrap.class.getName());
        ld.add("--cache");
        ld.add(new File(getDataDirectory(), "bootstrap-cache").getCanonicalPath());
        ld.add("--distributor");
        ld.add(getDistributor().getRegistryUri());
        ld.add("--runnable");
        ld.add(path);
        return launch(ld, handler);
    }


    synchronized public Process launch(LaunchDescription launchDescription, ProcessListener handler) throws Exception {
        int pid = pidCounter++;
        LocalProcess rc = createLocalProcess(launchDescription, handler, pid);
        processes.put(pid, rc);
        try {
            rc.start();
        } catch (Exception e) {
            processes.remove(pid);
            throw e;
        }

        return (Process) distributor.export(rc).getStub();
    }

    protected LocalProcess createLocalProcess(LaunchDescription launchDescription, ProcessListener handler, int pid) throws Exception {
        return new LocalProcess(this, launchDescription, handler, pid);
    }

    public synchronized void start() throws Exception {
        if (started) {
            return;
        }

        System.getProperties().setProperty(LOCAL_REPO_PROP, distributor.getResourceManager().getLocalRepoDirectory().getCanonicalPath());

        started = true;
        if (agentId == null) {

            try {
                setAgentId(java.net.InetAddress.getLocalHost().getHostName());
            } catch (java.net.UnknownHostException uhe) {
                LOG.warn("Error determining hostname.");
                uhe.printStackTrace();
                setAgentId("UNDEFINED");
            }
        }

        shutdownHook = new Thread(getAgentId() + "-Shutdown") {
            public void run() {
                LOG.debug("Executing Shutdown Hook for " + LaunchAgent.this);
                try {
                    LaunchAgent.this.stop();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        properties.fillIn(this);

        Runtime.getRuntime().addShutdownHook(shutdownHook);

        monitor.start();

        distributor.register(this, getRegistryPath(), false);

        LOG.info("PROCESS LAUNCHER " + getAgentId() + " STARTED\n");

    }

    private String getRegistryPath() {
        return LaunchAgent.REGISTRY_PATH + "/" + getAgentId();
    }

    public synchronized void stop() throws Exception {
        if (!started) {
            return;
        }

        if (Thread.currentThread() != shutdownHook) {
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
        }

        started = false;

        for (LocalProcess process : processes.values()) {
            try {
                process.kill();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        processes.clear();

        monitor.requestCleanup();
        monitor.stop();

        distributor.unregister(this);
    }

    public ResourceManager getResourceManager() {
        return distributor.getResourceManager();
    }

    /**
     * Clears the launchers local resource cache.
     * 
     * @throws IOException
     *             If there is an error purging the cache.
     */
    public void purgeResourceRepository() throws IOException {
        distributor.getResourceManager().purgeLocalRepo();
    }

    /**
     * Sets the base directory where the agent puts it's data.
     * 
     * @param dataDirectory
     */
    public void setDataDirectory(File dataDirectory) {
        this.dataDirectory = dataDirectory;
    }

    /**
     * @return Gets the data directory where the launcher stores files.
     */
    public File getDataDirectory() {
        return dataDirectory;
    }

    /**
     * Gets properties about the agent and it's host machine.
     * 
     * @return
     */
    public HostProperties getHostProperties() {
        return properties;
    }

    /**
     * Sets the name of the agent id. Once set it cannot be changed.
     * 
     * @param id
     *            the name of the agent id.
     */
    public void setAgentId(String id) {
        if (agentId == null && id != null) {
            agentId = id.trim().toUpperCase();
        }
    }

    /**
     * @return This agent's id.
     */
    public String getAgentId() {
        return agentId;
    }

    public Distributor getDistributor() {
        return distributor;
    }

    public void setDistributor(Distributor distributor) {
        this.distributor = distributor;
    }

    public Map<Integer, LocalProcess> getProcesses() {
        return processes;
    }

    public String toString() {
        return "ProcessLauncer-" + getAgentId();
    }

    /**
     * @param timeout
     */
    public void checkForRogueProcesses(int timeout) {
        // TODO Need a mechanism of pinging the launcher
        // of a process (then kill processes for which
        // the controller doesn't respond.
    }

    private class Monitor implements Runnable {
        Log log = LogFactory.getLog(this.getClass());
        private final LaunchAgent processLauncher;

        Thread thread;
        private String tempDirectory;
        private boolean cleanupRequested = false;

        public Monitor(LaunchAgent processLauncher) {
            this.processLauncher = processLauncher;
        }

        public void start() {
            tempDirectory = processLauncher.getDataDirectory() + File.separator + processLauncher.getAgentId() + File.separator + "temp";
            thread = new Thread(this, processLauncher.getAgentId() + "-Process Monitor");
            thread.start();
        }

        public void stop() {
            thread.interrupt();
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        public void run() {
            while (true) {
                synchronized (this) {
                    try {
                        wait(LaunchAgent.CLEANUP_TIMEOUT);
                    } catch (InterruptedException ie) {
                        cleanupRequested = true;
                        return;
                    } finally {
                        processLauncher.checkForRogueProcesses(15000);
                        if (cleanupRequested) {
                            cleanUpTempFiles();
                            cleanupRequested = false;
                        }
                    }
                }

            }
        }

        public void cleanUpTempFiles() {
            //If we aren't running anything cleanup: temp parts
            Map<Integer, LocalProcess> processes = processLauncher.getProcesses();
            if (processes == null || processes.size() == 0) {
                File tempDir = new File(tempDirectory);
                String[] subDirs = tempDir != null ? tempDir.list() : null;

                log.debug("*************Cleaning up temporary parts*************");
                for (int i = 0; subDirs != null && i < subDirs.length; i++) {
                    try {
                        FileUtils.recursiveDelete(tempDir + File.separator + subDirs[i]);
                    } catch (Exception e) {
                        log.warn("ERROR cleaning up temporary parts:", e);
                    }
                }
            }
        }

        /**
         * Requests cleanup of temporary files
         */
        public synchronized void requestCleanup() {
            cleanupRequested = true;
            notify();
        }

        public String getTempDirectory() {
            return tempDirectory;
        }
    }
}