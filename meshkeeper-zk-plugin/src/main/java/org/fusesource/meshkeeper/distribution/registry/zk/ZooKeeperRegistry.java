/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.meshkeeper.distribution.registry.zk;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URI;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.KeeperException.NodeExistsException;
import org.apache.zookeeper.KeeperException.NotEmptyException;
import org.apache.zookeeper.data.Stat;
import org.fusesource.meshkeeper.RegistryWatcher;
import org.fusesource.meshkeeper.distribution.registry.AbstractRegistryClient;

/**
 * ZooKeeperRegistry
 * <p>
 * Description:
 * </p>
 * 
 * @author cmacnaug
 * @version 1.0
 */
class ZooKeeperRegistry extends AbstractRegistryClient {

    Log log = LogFactory.getLog(this.getClass());
    HashMap<String, ZooKeeperChildWatcher> watcherMap = new HashMap<String, ZooKeeperChildWatcher>();
    private CountDownLatch connected = new CountDownLatch(1);

    private String connectUrl = "localhost:2181";
    private String userid = "guest";
    private String password = "";
    private long connectTimeout = 30000;
    private int sessionTimeout = 30000;
    private ZooKeeper zk;

    public ZooKeeper getZooKeeper() {
        return zk;
    }

    public void start() throws Exception {
        synchronized (this) {
            if (zk == null) {
                //ZK doesn't like schemes, so just take host and port
                URI uri = new URI(connectUrl);

                zk = new ZooKeeper(uri.getAuthority() + ":" + uri.getPort(), sessionTimeout, new Watcher() {
                    public void process(WatchedEvent event) {
                        switch (event.getState()) {
                        case SyncConnected:
                            connected.countDown();
                            break;

                        }
                    }
                });
                zk.addAuthInfo("digest", (userid + ":" + password).getBytes());

            }
        }

        // Wait for the client to establish a connection.
        if (connectTimeout > 0) {
            if (!connected.await(connectTimeout, TimeUnit.MILLISECONDS)) {
                throw new IOException("Failed to connect to ZooKeeper at " + connectUrl + " within " + connectTimeout + " milliseconds.");
            }
        } else {
            connected.await();
        }
    }

    public void destroy() throws Exception {
        synchronized (this) {
            if (zk != null) {
                zk.close();
            }
            connected.countDown();
            connected = new CountDownLatch(1);
            zk = null;
        }
    }

    public String addRegistryObject(String path, boolean sequential, Serializable o) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(baos);
        os.writeObject(o);
        os.close();
        return addRegistryData(path, sequential, baos.toByteArray());
    }

    @SuppressWarnings("unchecked")
    public <T> T getRegistryObject(String path) throws Exception {
        byte[] data = getRegistryData(path);
        if (data == null) {
            return null;
        }
        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(data));
        return (T) in.readObject();
    }

    public byte[] getRegistryData(String path) throws Exception {
        checkConnected();
        Stat stat = new Stat();
        try {
            return zk.getData(path, false, stat);
        } catch (NoNodeException nne) {
            return null;
        }
    }

    public String addRegistryData(String path, boolean sequential, byte[] data) throws Exception {
        checkConnected();
        if (log.isWarnEnabled() && data.length > 20000) {
            log.warn("Warning -- long data length for " + path + ": " + data.length);
        }

        if (log.isDebugEnabled()) {
            log.debug("Registering " + path + " length=" + (data != null ? data.length : 0));
        }
        try {
            if (sequential) {
                return zk.create(path, data, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
            } else {
                return zk.create(path, data, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
            }
        } catch (NoNodeException nne) {
            createParentPath(path);
            return addRegistryData(path, sequential, data);
        }
    }

    public void removeRegistryData(String path, boolean recursive) throws Exception {
        checkConnected();
        try {
            if (log.isDebugEnabled()) {
                log.debug("Removing: " + path);
            }

            //ZK doesn't allow you to delete the root, throw a NotEmptyExecption to 
            //delete any children.
            if (path.equals("/")) {
                for (String child : zk.getChildren(path, false)) {
                    if (child.equals("zookeeper")) {
                        continue;
                    }
                    removeRegistryData("/" + child, true);
                }
                return;
            }
            zk.delete(path, -1);
            //Delete ancestors:
            deleteEmptyAncestors(path);
        } catch (NoNodeException nne) {
            //Done.
        } catch (NotEmptyException nee) {
            //If it's not recursive and not empty, just set data null.
            if (!recursive) {
                zk.setData(path, null, -1);
            } else {
                for (String child : zk.getChildren(path, false)) {
                    removeRegistryData(path + "/" + child, true);
                }
            }
        }
    }

    public synchronized void addRegistryWatcher(String path, RegistryWatcher watcher) throws Exception {
        checkConnected();
        if (path.endsWith("/")) {
            path.substring(0, path.length() - 1);
        }

        createParentPath(path + "/");
        ZooKeeperChildWatcher w = watcherMap.get(path);
        if (w == null) {
            w = new ZooKeeperChildWatcher(zk, path);
            watcherMap.put(path, w);
        }
        w.addWatcher(watcher);

    }

    public synchronized void removeRegistryWatcher(String path, RegistryWatcher watcher) {
        ZooKeeperChildWatcher w = watcherMap.get(path);
        if (w == null) {
            return;
        } else if (w.removeWatcher(watcher)) {
            watcherMap.remove(path);
        }
    }

    /**
     * Deletes a path and its parent nodes if they are empty.
     * 
     * @param path
     *            The path to delete.
     */
    private void deleteEmptyAncestors(String path) throws KeeperException, InterruptedException {
        int ls = path.lastIndexOf("/");

        //Don't create if there is no parent 
        if (ls > 1) {
            String parent = path.substring(0, ls);
            try {
                zk.delete(path, -1);
            } catch (KeeperException.NotEmptyException nee) {
                return;
            }
            deleteEmptyAncestors(parent);
        }
    }

    private void createParentPath(String path) throws KeeperException, InterruptedException {
        int ls = path.lastIndexOf("/");

        //Don't create if there is no parent 
        if (ls > 1) {
            String parent = path.substring(0, ls);
            try {

                zk.create(parent, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            } catch (NodeExistsException e) {
                return;
            } catch (NoNodeException nne) {
                createParentPath(parent);
                zk.create(parent, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
            if (log.isDebugEnabled()) {
                log.debug("Created: " + parent);
            }
        }
    }

    private void checkConnected() throws Exception {
        if (connected.getCount() > 0) {
            throw new Exception("Not Connected");
        }
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

    public String toString() {
        return "ZooKeeperRegistry@" + connectUrl;
    }

}
