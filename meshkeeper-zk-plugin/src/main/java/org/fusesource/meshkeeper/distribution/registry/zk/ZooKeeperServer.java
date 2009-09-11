/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.meshkeeper.distribution.registry.zk;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.zookeeper.server.NIOServerCnxn;
import org.apache.zookeeper.server.ServerStats;
import org.apache.zookeeper.server.persistence.FileTxnLog;
import org.fusesource.meshkeeper.control.ControlService;
import org.fusesource.meshkeeper.util.internal.FileSupport;

import java.io.File;
import java.net.InetAddress;

/**
 * @author chirino
 */
public class ZooKeeperServer implements ControlService {

    Log log = LogFactory.getLog(this.getClass());

    private int port = 2181;
    private String userid = "guest";
    private String password = "";
    private String directory = "zookeeper-server-data";
    private boolean purge;
    private String serviceUri;
    int tick = 10000;

    private NIOServerCnxn.Factory serverFactory;

    public void start() throws Exception {
        ServerStats.registerAsConcrete();
        File file = new File(directory);
        if (purge && file.exists()) {
            try {
                FileSupport.recursiveDelete(file.getCanonicalPath());
            } catch (Exception e) {
                log.error("Error purging store", e);
            }
        }
        file.mkdirs();

        // Reduces startup time..
        System.setProperty("zookeeper.preAllocSize", "100");
        FileTxnLog.setPreallocSize(100);
        org.apache.zookeeper.server.ZooKeeperServer zs = new org.apache.zookeeper.server.ZooKeeperServer(file, file, tick);
        serverFactory = new NIOServerCnxn.Factory(port);
        serverFactory.startup(zs);
        
        String actualHost = InetAddress.getLocalHost().getHostName();
        serviceUri = "zk:tcp://" + actualHost + ":" + zs.getClientPort();
    }

    public void destroy() throws Exception {
        if (serverFactory != null) {
            serverFactory.shutdown();
            serverFactory = null;
        }
        ServerStats.unregister();
        log.info("Destroyed");
    }

    public String getUserid() {
        return userid;
    }

    public void setUserid(String userid) {
        this.userid = userid;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public boolean isPurge() {
        return purge;
    }

    public void setPurge(boolean purge) {
        this.purge = purge;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getName() {
        return toString();
    }

    public String toString() {
        return "Zoo Keeper Registry Server";
    }

    public String getServiceUri() {
        return serviceUri;
    }


}
