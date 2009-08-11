package org.fusesource.cloudlaunch.distribution.registry.zk;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.zookeeper.server.NIOServerCnxn;
import org.apache.zookeeper.server.ServerStats;
import org.apache.zookeeper.server.persistence.FileTxnLog;
import org.fusesource.cloudlaunch.util.internal.FileUtils;

import java.io.File;

/**
 * @author chirino
 */
public class ZooKeeperServer {

    Log log = LogFactory.getLog(this.getClass());

    private int port = 2181;
    private String userid = "guest";
    private String password = "";
    private String directory = "zookeeper-data";
    private boolean purge;
    int tick = 10000;

    private NIOServerCnxn.Factory serverFactory;

    public void afterPropertiesSet() throws Exception {

        start();
    }

    public void start() throws Exception {
        ServerStats.registerAsConcrete();
        File file = new File(directory);
        if (purge && file.exists()) {
            try {
                FileUtils.recursiveDelete(file.getCanonicalPath());
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
    }

    public void destroy() throws Exception {
        if (serverFactory != null) {
            serverFactory.shutdown();
            serverFactory = null;
        }
        ServerStats.unregister();
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
}
