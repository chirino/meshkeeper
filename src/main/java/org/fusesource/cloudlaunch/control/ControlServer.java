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

import java.io.File;

import org.apache.activemq.broker.BrokerService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.fusesource.rmiviajms.JMSRemoteObject;
import org.fusesource.rmiviajms.internal.ActiveMQRemoteSystem;

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
    BrokerService controlBroker;
    ZooKeeperServer zkServer;

    private String jmsConnectUrl;
    private String zookKeeperConnectUrl;
    private String dataDirectory = ".";
    
    public void start() throws Exception {
        //TODO should probably store the control broker url in ZooKeeper so that application
        //need only specify one url. 
        //Start up a RMI control broker:
        try {
            if (jmsConnectUrl != null) {
                log.info("Starting Control Server");
                controlBroker = new BrokerService();
                controlBroker.setBrokerName("CloudLaunchControlBroker");
                controlBroker.addConnector(jmsConnectUrl);
                controlBroker.setDataDirectory(dataDirectory + File.separator + "control-broker");
                controlBroker.setPersistent(false);
                controlBroker.setDeleteAllMessagesOnStartup(true);
                controlBroker.start();
                log.info("Control Server started");
                System.setProperty(ActiveMQRemoteSystem.CONNECT_URL_PROPNAME, jmsConnectUrl);
                
            }
        } catch (Exception e) {
            log.error("Error starting control server", e);
        }

        //Start a zoo-keeper server.
        try {
            if (zookKeeperConnectUrl != null) {
                log.info("Starting ZooKeeper Server");
                zkServer = new ZooKeeperServer();
                zkServer.setPurge(true);
                zkServer.setDirectory(dataDirectory + File.separator + "zoo-keeper");
                zkServer.start();
                log.info("ZooKeeper Server started");

            }
        } catch (Exception e) {
            log.error("Error starting control server", e);
        }
    }

    public void destroy() throws Exception {
        
        if (zkServer != null) {
            try {
                zkServer.destroy();
                
            } finally {
                zkServer = null;
            }
        }
        
        JMSRemoteObject.resetSystem();
        
        if (controlBroker != null) {
            try {
                controlBroker.stop();
                controlBroker.waitUntilStopped();
            } finally {
                controlBroker = null;
            }
        }
        
        
        
    }

    public String getJmsConnectUrl() {
        return jmsConnectUrl;
    }

    public void setJmsConnectUrl(String jmsConnectUrl) {
        this.jmsConnectUrl = jmsConnectUrl;
    }

    public String getZookKeeperConnectUrl() {
        return zookKeeperConnectUrl;
    }

    public void setZookKeeperConnectUrl(String zookKeeperConnectUrl) {
        this.zookKeeperConnectUrl = zookKeeperConnectUrl;
    }

    public void setDataDirectory(String dataDirectory) {
        this.dataDirectory = dataDirectory;
    }

    public String getDataDirectory() {
        return dataDirectory;
    }
}
