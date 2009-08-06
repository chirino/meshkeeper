package org.fusesource.cloudlaunch.control;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.DisposableBean;
import org.apache.zookeeper.server.NIOServerCnxn;
import org.apache.zookeeper.server.ServerStats;
import org.apache.zookeeper.server.persistence.FileTxnLog;

import java.io.File;

/**
 * @author chirino
 */
public class ZooKeeperServer implements InitializingBean, DisposableBean {

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
            recursiveDelete(file);
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
        serverFactory.getZooKeeperServer().shutdown();
        serverFactory.shutdown();
        ServerStats.unregister();
    }

    protected static void recursiveDelete(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++) {
                recursiveDelete(files[i]);
            }
        }
        file.delete();
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
