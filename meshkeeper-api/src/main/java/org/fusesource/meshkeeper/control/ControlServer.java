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
package org.fusesource.meshkeeper.control;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.fusesource.meshkeeper.MeshEvent;
import org.fusesource.meshkeeper.MeshEventListener;
import org.fusesource.meshkeeper.MeshKeeper;
import org.fusesource.meshkeeper.MeshKeeperFactory;
import org.fusesource.meshkeeper.MeshKeeper.Registry;
import org.fusesource.meshkeeper.distribution.DistributorFactory;
import org.fusesource.meshkeeper.distribution.provisioner.Provisioner;
import org.fusesource.meshkeeper.launcher.LaunchAgent;
import org.fusesource.meshkeeper.util.internal.FileSupport;

/**
 * ControlServer
 * <p>
 * Description: The control server hosts the servers used to facilitate the
 * distributed test system.
 * 
 * </p>
 * 
 * @author cmacnaug
 * @version 1.0
 */
public class ControlServer {

    public static final String CONTROLLER_PROP_FILE_NAME = "controller.properties";

    Log log = LogFactory.getLog(ControlServer.class);
    private static final ControlServiceFactory SERVICE_FACTORY = new ControlServiceFactory();

    public static final String DEFAULT_JMS_URI = "activemq:tcp://localhost:4041";
    public static final String DEFAULT_REMOTING_URI = "rmiviajms:" + DEFAULT_JMS_URI;
    public static final String DEFAULT_REGISTRY_URI = "zk:tcp://localhost:4040";
    public static final String DEFAULT_EVENT_URI = "eventviajms:" + DEFAULT_JMS_URI;

    public static final String REMOTING_URI_PATH = Registry.MESH_KEEPER_ROOT + "/control/remoting-uri";
    public static final String EVENTING_URI_PATH = Registry.MESH_KEEPER_ROOT + "/control/eventing-uri";
    public static final String REPOSITORY_URI_PATH = Registry.MESH_KEEPER_ROOT + "/control/repository-uri";

    ControlService rmiServer;
    ControlService registryServer;
    MeshKeeper meshKeeper;

    private String jmsUri = DEFAULT_JMS_URI;
    private String registryUri = DEFAULT_REGISTRY_URI;
    private String repositoryUri = System.getProperty("meshkeeper.repository.uri");

    private String directory = MeshKeeperFactory.getDefaultServerDirectory().getPath();
    private Thread shutdownHook;

    private Runnable preShutdownHook;

    private LaunchAgent embeddedAgent;

    private boolean startEmbeddedAgent;

    public static final String CONTROL_TOPIC = "meshkeeper.control";

    public enum ControlEvent {
        SHUTDOWN; //When received shuts down the control server:

        public MeshEvent createEvent(String source, Object attachment) {
            return new MeshEvent(ordinal(), source, attachment);
        }

        public static ControlEvent getControlEvent(MeshEvent e) {
            if (e == null) {
                return null;
            }

            try {
                return ControlEvent.values()[e.getType()];
            } catch (Throwable thrown) {
                return null;
            }
        }
    }

    public void start() throws Exception {

        deleteControllerProps();
        shutdownHook = new Thread("MeshKeeper Control Server Shutdown Hook") {
            public void run() {
                log.debug("Executing Shutdown Hook for " + ControlServer.this);
                try {
                    ControlServer.this.destroy();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        //Start the jms server:
        log.info("Creating JMS Server at " + jmsUri);
        final String SLASH = File.separator;
        try {
            rmiServer = SERVICE_FACTORY.create(jmsUri);
            rmiServer.setDirectory(directory + SLASH + "jms");
            rmiServer.start();
            log.info("JMS Server started: " + rmiServer.getName());

        } catch (Exception e) {
            log.error(e);
            destroy();
            throw new Exception("Error starting JMS Server", e);
        }

        //Start the registry server:
        log.info("Creating Registry Server at " + registryUri);
        try {
            registryServer = SERVICE_FACTORY.create(registryUri);
            registryServer.setDirectory(directory + SLASH + "registry");
            registryServer.start();
            log.info("Registry Server started: " + registryServer.getName() + " uri: " + registryServer.getServiceUri());

        } catch (Exception e) {
            log.error("Error starting regisry server", e);
            destroy();
            throw new Exception("Error starting Registry Server", e);
        }

        //Connect to the registry and publish service connection info:
        try {

            log.info("Connecting to registry server at " + registryServer.getServiceUri());
            //registry = new RegistryFactory().create(registryServer.getServiceUri());

            String eventingUri = "eventviajms:" + rmiServer.getServiceUri();
            String remotingUri = "rmiviajms:" + rmiServer.getServiceUri();
            DistributorFactory factory = new DistributorFactory();
            factory.setRegistryUri(registryServer.getServiceUri());
            factory.setEventingUri(eventingUri);
            factory.setRemotingUri(remotingUri);
            factory.setDirectory(getDirectory());
            meshKeeper = factory.create();

            //Register the control services:

            //(note that we delete these first since
            //in some instances zoo-keeper doesn't shutdown cleanly and hangs
            //on to file handles so that the registry isn't purged:
            meshKeeper.registry().removeRegistryData(REMOTING_URI_PATH, true);
            meshKeeper.registry().addRegistryObject(REMOTING_URI_PATH, false, remotingUri);
            log.info("Registered RMI control server at " + REMOTING_URI_PATH + "=" + remotingUri);

            meshKeeper.registry().removeRegistryData(EVENTING_URI_PATH, true);
            meshKeeper.registry().addRegistryObject(EVENTING_URI_PATH, false, eventingUri);
            log.info("Registered event server at " + EVENTING_URI_PATH + "=" + eventingUri);

            meshKeeper.registry().removeRegistryData(REPOSITORY_URI_PATH, true);
            if (repositoryUri != null) {
                meshKeeper.registry().addRegistryObject(REPOSITORY_URI_PATH, false, repositoryUri);
                log.info("Registered repository uri at " + REPOSITORY_URI_PATH + "=" + repositoryUri);
            } else {
                log.info("Common repository uri was not set, some repository services may not be available");
            }

            //Let's save our controller properties to an output file
            //useful for discovering our registry connect url:
            saveControllerProps();

            //Let's open an event listener to handle service commands:
            meshKeeper.eventing().openEventListener(new MeshEventListener() {

                public void onEvent(MeshEvent e) {
                    switch (ControlEvent.getControlEvent(e)) {
                    case SHUTDOWN:
                        log.info("Got shutdown request: " + e);
                        //Fire off in a new thread (not the eventing thread):
                        new Thread("Controller Shutdown") {
                            public void run() {
                                try {
                                    ControlServer.this.destroy();
                                } catch (Exception e) {
                                    log.warn("Error during control server shutdown", e);
                                }
                            }
                        }.start();
                        break;
                    default:
                        log.warn("Got unknown control event: " + e);
                    }
                }
            }, CONTROL_TOPIC);

            if (startEmbeddedAgent && embeddedAgent == null) {
                embeddedAgent = new LaunchAgent();
            }

            if (embeddedAgent != null) {
                embeddedAgent.setMeshKeeper(meshKeeper);
                embeddedAgent.setDirectory(new File(getDirectory()));
                embeddedAgent.start();
            }

            log.info("MeshKeeper Successfully started. The Registry Service is listening on: " + getRegistryUri());

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            destroy();
            throw new Exception("Error registering control server", e);
        }
    }

    public void setEmbeddedLaunchAgent(LaunchAgent embeddedAgent) {
        this.embeddedAgent = embeddedAgent;
    }

    public LaunchAgent getEmbeddedLaunchAgent() {
        return embeddedAgent;
    }

    public void setPreShutdownHook(Runnable runnable) {
        this.preShutdownHook = runnable;
    }

    private final void deleteControllerProps() {
        FileSupport.recursiveDelete(new File(getDirectory(), CONTROLLER_PROP_FILE_NAME));
    }

    private final void saveControllerProps() throws IOException {
        //Let's dump some controller properties to our working directory:
        Properties props = new Properties();
        props.put(MeshKeeperFactory.MESHKEEPER_REGISTRY_PROPERTY, registryServer.getServiceUri());
        String provisionerId = System.getProperty(Provisioner.MESHKEEPER_PROVISIONER_ID_PROPERTY);
        if (provisionerId != null) {
            log.info("Writing provisioner id: " + provisionerId);
            props.put(Provisioner.MESHKEEPER_PROVISIONER_ID_PROPERTY, provisionerId);
        } else {
            props.put(Provisioner.MESHKEEPER_PROVISIONER_ID_PROPERTY, "none");
        }

        File f = new File(getDirectory(), CONTROLLER_PROP_FILE_NAME);
        if (f.exists()) {
            f.delete();
        }
        f.getParentFile().mkdirs();
        f.deleteOnExit();
        PrintStream fout = new PrintStream(f);
        props.store(fout, null);
        fout.flush();
        fout.close();
    }

    public void destroy() throws Exception {

        if (Thread.currentThread() != shutdownHook) {
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
        }

        deleteControllerProps();

        if (preShutdownHook != null) {
            preShutdownHook.run();
            preShutdownHook = null;
        }

        Exception first = null;

        if (embeddedAgent != null) {
            try {
                embeddedAgent.stop();
            } catch (Exception e) {
                first = first == null ? e : first;
            }
        }

        if (meshKeeper != null) {
            try {
                meshKeeper.destroy();
            } catch (Exception e) {
                first = first == null ? e : first;
            } finally {
                meshKeeper = null;
            }
        }

        log.info("Shutting down registry server");
        if (registryServer != null) {
            try {
                registryServer.destroy();
            } catch (Exception e) {
                first = first == null ? e : first;
            } finally {
                registryServer = null;
            }
        }

        log.info("Shutting down rmi server");
        if (rmiServer != null) {
            try {
                rmiServer.destroy();
            } catch (Exception e) {
                first = first == null ? e : first;
            } finally {
                rmiServer = null;
            }
        }

        synchronized (this) {
            notifyAll();
        }

        if (first != null) {
            throw first;
        }
    }

    public void join() throws InterruptedException {
        synchronized (this) {
            wait();
        }
    }

    public void setRepositoryUri(String repositoryUri) {
        if (repositoryUri == null || repositoryUri.trim().length() == 0) {
            this.repositoryUri = null;
        } else {
            this.repositoryUri = repositoryUri;
        }
    }

    public String getRepositoryUri() {
        return repositoryUri;
    }

    public String getJmsUri() {
        return jmsUri;
    }

    public void setJmsUri(String jmsProvider) {
        this.jmsUri = jmsProvider;
    }

    public String getRegistryUri() {
        if (registryServer != null) {
            return registryServer.getServiceUri();
        }
        return registryUri;
    }

    /**
     * @param startEmbeddedAgent
     *            Whether an embedded agent will be started
     */
    public void setStartEmbeddedAgent(boolean startEmbeddedAgent) {
        this.startEmbeddedAgent = startEmbeddedAgent;
    }

    /**
     * @return Whether an embedded agent will be started.
     */
    public boolean getStartEmbeddedAgent() {
        return startEmbeddedAgent;
    }

    /**
     * @return The connect uri that should be used to connect to the registry.
     */
    public String getRegistryConnectUri() {
        if (registryServer != null) {
            return registryServer.getServiceUri();
        } else {
            return registryUri;
        }
    }

    public void setRegistryUri(String registryProvider) {
        this.registryUri = registryProvider;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public String getDirectory() {
        return directory;
    }

    /**
     * @return
     */
    public MeshKeeper getMeshKeeper() {
        // TODO Auto-generated method stub
        return meshKeeper;
    }
}
