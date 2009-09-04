/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.meshkeeper.control;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.fusesource.meshkeeper.distribution.registry.Registry;
import org.fusesource.meshkeeper.distribution.registry.RegistryFactory;


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

    Log log = LogFactory.getLog(ControlServer.class);
    private static final ControlServiceFactory SERVICE_FACTORY = new ControlServiceFactory();
    public static final String DEFAULT_JMS_PROVIDER_URI= "activemq:tcp://localhost:4041";
    public static final String DEFAULT_REGISTRY_PROVIDER_URI = "zk:tcp://localhost:4040";
    public static final String DEFAULT_RMI_URI = "rmiviajms:" + DEFAULT_JMS_PROVIDER_URI;
    public static final String DEFAULT_REGISTRY_URI = DEFAULT_REGISTRY_PROVIDER_URI;
    public static final String DEFAULT_EVENT_URI = "eventviajms:" + DEFAULT_JMS_PROVIDER_URI;
    public static final String EXPORTER_CONNECT_URI_PATH = "/control/exporter-uri";
    public static final String EVENT_CONNECT_URI_PATH = "/control/event-uri";
    public static final String COMMON_REPO_URL_PATH = "/control/common-repo-url";
    
    ControlService rmiServer;
    ControlService registryServer;
    Registry registry;

    private String jmsProviderUri = DEFAULT_JMS_PROVIDER_URI;
    private String registryProviderUri = DEFAULT_REGISTRY_PROVIDER_URI;
    private String dataDirectory = ".";
    private String commonRepoUrl;
    private Thread shutdownHook;

    public void start() throws Exception {
        
        dataDirectory = dataDirectory + File.separator + "control-server";
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
        log.info("Creating JMS Server at " + jmsProviderUri);
        try {
            rmiServer = SERVICE_FACTORY.create(jmsProviderUri);
            rmiServer.setDataDirectory(dataDirectory + File.separator + "jms");
            rmiServer.start();
            log.info("JMS Server started: " + rmiServer.getName());
            
        } catch (Exception e) {
            log.error(e);
            destroy();
            throw new Exception("Error starting JMS Server", e);
        }
        
        //Start the registry server:
        log.info("Creating Registry Server at " + registryProviderUri);
        try {
            registryServer = SERVICE_FACTORY.create(registryProviderUri);
            registryServer.setDataDirectory(dataDirectory + File.separator + "registry");
            registryServer.start();
            log.info("Registry Server started: " + registryServer.getName());
            
        } catch (Exception e) {
            log.error(e);
            destroy();
            throw new Exception("Error starting Registry Server", e);
        }

        //Connect to the registry and publish service connection info:
        try {
            
            registry = new RegistryFactory().create("zk:" + registryProviderUri);

            //Register the control services:
            
            //(note that we delete these first since
            //in some instances zoo-keeper doesn't shutdown cleanly and hangs
            //on to file handles so that the registry isn't purged:
            registry.remove(EXPORTER_CONNECT_URI_PATH, true);
            registry.addObject(EXPORTER_CONNECT_URI_PATH, false, new String("rmiviajms:" + rmiServer.getServiceUri()));
            log.info("Registered RMI control server at " + EXPORTER_CONNECT_URI_PATH + "=rmiviajms:" + rmiServer.getServiceUri());
            
            registry.remove(EVENT_CONNECT_URI_PATH, true);
            registry.addObject(EVENT_CONNECT_URI_PATH, false, new String("eventviajms:" + rmiServer.getServiceUri()));
            log.info("Registered event server at " + EVENT_CONNECT_URI_PATH + "=eventviajms:" + rmiServer.getServiceUri());
            
            registry.remove(COMMON_REPO_URL_PATH, true);
            registry.addObject(COMMON_REPO_URL_PATH, false, commonRepoUrl);
            log.info("Registered common repo url at " + COMMON_REPO_URL_PATH + "=" + commonRepoUrl);
            
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            destroy();
            throw new Exception("Error registering control server", e);
        }
    }

    public void destroy() throws Exception {

        if (Thread.currentThread() != shutdownHook) {
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
        }

        if (registry != null) {
            registry.destroy();
            registry = null;
        }

        log.info("Shutting down registry server");
        if (registryServer != null) {
            try {
                registryServer.destroy();
            } finally {
                registryServer = null;
            }
        }

        log.info("Shutting down rmi server");
        if (rmiServer != null) {
            try {
                rmiServer.destroy();
            } finally {
                rmiServer = null;
            }
        }

    }

    public void setCommonRepoUrl(String commonRepoUrl) {
        this.commonRepoUrl = commonRepoUrl;
    }
    
    public String getCommonRepoUrl() {
        return commonRepoUrl;
    }
    
    public String getJmsProviderUri() {
        return jmsProviderUri;
    }

    public void setJmsProviderUri(String jmsProviderUri) {
        this.jmsProviderUri = jmsProviderUri;
    }

    public String getRegistryProviderUri() {
        return registryProviderUri;
    }

    public void setRegistryProviderUri(String registryProviderUri) {
        this.registryProviderUri = registryProviderUri;
    }

    public void setDataDirectory(String dataDirectory) {
        this.dataDirectory = dataDirectory;
    }

    public String getDataDirectory() {
        return dataDirectory;
    }
}
