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
package org.fusesource.cloudlaunch.registry.zk;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.KeeperException.NodeExistsException;
import org.apache.zookeeper.data.Stat;
import org.fusesource.cloudlaunch.registry.Registry;
import org.fusesource.cloudlaunch.registry.RegistryWatcher;

/**
 * ZooKeeperRegistry
 * <p>
 * Description:
 * </p>
 * 
 * @author cmacnaug
 * @version 1.0
 */
public class ZooKeeperRegistry implements Registry {

    HashMap<String, ZooKeeperChildWatcher> watcherMap = new HashMap<String, ZooKeeperChildWatcher>();

    private ZooKeeper zk;

    public ZooKeeper getZooKeeper() {
        return zk;
    }

    public void setZooKeeper(ZooKeeper zk) {
        this.zk = zk;

    }

    public void connect() throws Exception {

    }

    public void close() throws Exception {
        zk.close();
    }

    public String addObject(String path, boolean sequential, Serializable o) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(baos);
        os.writeObject(o);
        os.close();
        return addData(path, sequential, baos.toByteArray());
    }

    @SuppressWarnings("unchecked")
    public <T> T getObject(String path) throws Exception {
        Stat stat = new Stat();
        byte[] data = zk.getData(path, false, stat);
        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(data));
        return (T) in.readObject();
    }

    public String addData(String path, boolean sequential, byte[] data) throws Exception {
        try {
            if (sequential) {
                return zk.create(path, data, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
            } else {
                return zk.create(path, data, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
        } catch (NoNodeException nne) {
            createParentPath(path);
            return addData(path, sequential, data);
        }
    }

    public void remove(String path, boolean recursive) throws Exception {
        try {
            zk.delete(path, -1);
            //Delete ancestors:
            deleteEmptyAncestors(path);
        } catch (NoNodeException nne) {
            //Done.
        }
    }

    public synchronized void addRegistryWatcher(String path, RegistryWatcher watcher) throws Exception {
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
                createParentPath(path);
            }
        }
    }

}
