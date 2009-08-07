package org.fusesource.cloudlaunch.registry.zk;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.ACL;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.DisposableBean;

import java.net.URI;
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

    private String connectUrl = "localhost:2181";
    private String userid = "guest";
    private String password = "";

    private ZooKeeperRegistry registry;
    long connectTimeout = 30000;
    int sessionTimeout = 30000;

    public Object getObject() throws Exception {
        return registry;
    }

    public Class getObjectType() {
        return ZooKeeperRegistry.class;
    }

    public boolean isSingleton() {
        return true;
    }

    CountDownLatch connected = new CountDownLatch(1);

    public void afterPropertiesSet() throws Exception {
        start();
    }

    public void start() throws Exception {
        //ZK doesn't like schemes, so just take host and port
        URI uri = new URI(connectUrl);
        ZooKeeper zk = new ZooKeeper(uri.getAuthority() + ":" + uri.getPort(), sessionTimeout, new Watcher() {
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
        
        registry = new ZooKeeperRegistry();
        registry.setZooKeeper(zk);
    }

    public void destroy() throws Exception {
        if (registry != null) {
            registry.close();
        }
    }

    public ZooKeeperRegistry getRegistry() throws Exception {
        if (registry == null) {
            start();
        }
        return registry;
    }

    public String getConnectUrl() {
        return connectUrl;
    }

    public void setConnectUrl(String connectUrl) {
        this.connectUrl = connectUrl;
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

    public int getSessionTimeout() {
        return sessionTimeout;
    }

    public void setSessionTimeout(int sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }

}
