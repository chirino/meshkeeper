/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.cloudlaunch.control;

import java.io.File;
import java.net.InetAddress;
import java.net.URI;

import org.apache.activemq.broker.BrokerService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.fusesource.cloudlaunch.distribution.registry.Registry;
import org.fusesource.cloudlaunch.distribution.registry.zk.ZooKeeperFactory;
import org.fusesource.cloudlaunch.distribution.registry.zk.ZooKeeperServer;
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

    public static final String DEFAULT_JMS_URL = "tcp://localhost:4041";
    public static final String DEFAULT_ZOOKEEPER_URL = "tcp://localhost:4040";
    public static final String DEFAULT_RMI_URI = "rmiviajms:" + DEFAULT_JMS_URL;
    public static final String DEFAULT_REGISTRY_URI = "zk:" + DEFAULT_ZOOKEEPER_URL;
    public static final String DEFAULT_EVENT_URI = "jms:" + DEFAULT_JMS_URL;
    public static final String EXPORTER_CONNECT_URI_PATH = "/control/exporter-uri";
    public static final String EVENT_CONNECT_URI_PATH = "/control/event-uri";
    public static final String COMMON_REPO_URL_PATH = "/control/common-repo-url";
    
    BrokerService controlBroker;
    ZooKeeperServer zkServer;
    Registry registry;

    private String jmsConnectUrl = DEFAULT_JMS_URL;
    private String zooKeeperConnectUrl = DEFAULT_ZOOKEEPER_URL;
    private String dataDirectory = ".";

    private Thread shutdownHook;

    private String externalJMSUrl;

    private String commonRepoUrl;

    public void start() throws Exception {
        
        dataDirectory = dataDirectory + File.separator + "control-server";
        shutdownHook = new Thread("CloudLaunch Control Server Shutdown Hook") {
            public void run() {
                log.debug("Executing Shutdown Hook for " + ControlServer.this);
                try {
                    ControlServer.this.destroy();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        //Start up a RMI control broker:
        log.info("Starting RMI Control Server on " + jmsConnectUrl);
        try {
            if (jmsConnectUrl != null) {
                log.info("RMI Server");
                controlBroker = new BrokerService();
                controlBroker.setBrokerName("CloudLaunchControlBroker");
                controlBroker.addConnector(jmsConnectUrl);
                controlBroker.setDataDirectory(dataDirectory + File.separator + "control-broker");
                controlBroker.setPersistent(false);
                controlBroker.setDeleteAllMessagesOnStartup(true);
                controlBroker.setUseJmx(false);
                controlBroker.start();
                log.info("Control Server started");
            }
        } catch (Exception e) {
            log.error(e);
            destroy();
            throw new Exception("Error starting RMI Server", e);
        }

        //Start a zoo-keeper server.
        log.info("Starting Control Registry at " + zooKeeperConnectUrl);
        try {
            if (zooKeeperConnectUrl != null) {
                URI uri = new URI(zooKeeperConnectUrl);

                log.info("Starting ZooKeeper Server");
                zkServer = new ZooKeeperServer();
                zkServer.setDirectory(dataDirectory + File.separator + "zoo-keeper");
                zkServer.setPurge(true);
                zkServer.setPort(uri.getPort());
                zkServer.start();
                log.info("ZooKeeper Server started");
            }
        } catch (Exception e) {
            log.error(e);
            destroy();
            throw new Exception("Error starting Zookeeper Server", e);
        }

        try {
            
            ZooKeeperFactory factory = new ZooKeeperFactory();
            Registry registry = factory.createRegistry("zk:" + zooKeeperConnectUrl);

            //Register the control services:
            
            //(note that we delete these first since
            //in some instances zoo-keeper doesn't shutdown cleanly and hangs
            //on to file handles so that the registry isn't purged:
            registry.remove(EXPORTER_CONNECT_URI_PATH, true);
            registry.addObject(EXPORTER_CONNECT_URI_PATH, false, new String("rmiviajms:" + getExternalJMSUrl()));
            log.info("Registered RMI control server at " + EXPORTER_CONNECT_URI_PATH + "=rmiviajms:" + getExternalJMSUrl());
            
            registry.remove(EVENT_CONNECT_URI_PATH, true);
            registry.addObject(EVENT_CONNECT_URI_PATH, false, new String("jms:" + getExternalJMSUrl()));
            log.info("Registered event server at " + EVENT_CONNECT_URI_PATH + "=jms:" + getExternalJMSUrl());
            
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
        if (zkServer != null) {
            try {
                zkServer.destroy();
            } finally {
                zkServer = null;
            }
        }

        log.info("Shutting down rmi server");
        if (controlBroker != null) {
            try {
                controlBroker.stop();
                controlBroker.waitUntilStopped();
            } finally {
                controlBroker = null;
            }
        }

    }

    private String getExternalJMSUrl() {
        if (externalJMSUrl == null) {
            try {
                URI uri = new URI(jmsConnectUrl);
                String actualHost = InetAddress.getLocalHost().getHostName();
                if (!actualHost.equalsIgnoreCase(uri.getHost())) {
                    externalJMSUrl = uri.getScheme() + "://" + actualHost + ":" + uri.getPort();
                } else {
                    externalJMSUrl = jmsConnectUrl;
                }
            } catch (Exception e) {
                log.warn("Error computing external rmi connect url will use " + jmsConnectUrl);
                externalJMSUrl = jmsConnectUrl;
            }
        }
        return externalJMSUrl;

    }

    public void setCommonRepoUrl(String commonRepoUrl) {
        this.commonRepoUrl = commonRepoUrl;
    }
    
    public String getCommonRepoUrl() {
        return commonRepoUrl;
    }
    
    public String getJmsConnectUrl() {
        return jmsConnectUrl;
    }

    public void setJmsConnectUrl(String jmsConnectUrl) {
        this.jmsConnectUrl = jmsConnectUrl;
    }

    public String getZooKeeperConnectUrl() {
        return zooKeeperConnectUrl;
    }

    public void setZooKeeperConnectUrl(String zooKeeperConnectUrl) {
        this.zooKeeperConnectUrl = zooKeeperConnectUrl;
    }

    public void setDataDirectory(String dataDirectory) {
        this.dataDirectory = dataDirectory;
    }

    public String getDataDirectory() {
        return dataDirectory;
    }
}
