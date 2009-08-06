package org.fusesource.cloudlaunch.zk;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.ACL;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.DisposableBean;

import java.rmi.Remote;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;

/**
 * @author chirino
 */
public class ZooKeeperFactory implements FactoryBean, InitializingBean, DisposableBean {

    private String host = "localhost";
    private int port = 2181;
    private String userid = "guest";
    private String password = "";

    private ZooKeeper zk;
    long connectTimeout = 30000;

    public Object getObject() throws Exception {
        return zk;
    }

    public Class getObjectType() {
        return ZooKeeper.class;
    }

    public boolean isSingleton() {
        return true;
    }

    CountDownLatch connected = new CountDownLatch(1);

    public void afterPropertiesSet() throws Exception {
        start();
    }

    public void start() throws Exception {
        zk = new ZooKeeper(host, port, new Watcher() {
            public void process(WatchedEvent event) {
                switch (event.getState()) {
                case SyncConnected:
                    connected.countDown();
                    break;

                }
            }
        });

        zk.addAuthInfo("digest", (userid + ":" + password).getBytes());

        // Wait for the client to establish a connection.
        if (connectTimeout > 0) {
            if (!connected.await(connectTimeout, TimeUnit.MILLISECONDS)) {
                throw new IOException("Failed to connect to ZooKeeper within " + connectTimeout + " milliseconds.");
            }
        } else {
            connected.await();
        }
    }

    public void destroy() throws Exception {
        if (zk != null) {
            zk.close();
        }
    }

    public ZooKeeper getZooKeeper() throws Exception {
        if (zk == null) {
            start();
        }
        return zk;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
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

    public long getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(long connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

}
