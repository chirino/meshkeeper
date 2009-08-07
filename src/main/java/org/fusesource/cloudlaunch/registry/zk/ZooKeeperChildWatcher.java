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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.AsyncCallback.ChildrenCallback;
import org.fusesource.cloudlaunch.registry.RegistryWatcher;

/**
 * ZooKeeperWatcher
 * <p>
 * Description:
 * </p>
 * 
 * @author cmacnaug
 * @version 1.0
 */
class ZooKeeperChildWatcher {

    protected final ZooKeeper zk;
    protected final String path;
    private final Watcher watcher;
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final ChildrenCallback callback;
    private final HashSet<RegistryWatcher> watchers = new HashSet<RegistryWatcher>(1);

    public ZooKeeperChildWatcher(ZooKeeper zk, String path) {
        this.zk = zk;
        this.path = path;
        this.watcher = new Watcher() {

                public void process(WatchedEvent event) {
                    switch (event.getType()) {
                    case NodeChildrenChanged:
                    {
                        watch();
                    }
                    default:
                    {
                        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!Got:" + event);
                    }
                    }
                }
        };

        this.callback = new ChildrenCallback() {

            public void processResult(int rc, String path, Object ctx, List<String> children) {
                if (!started.get()) {
                    return;
                }
                if (children == null) {
                    children = new ArrayList<String>();
                }
                
                for (RegistryWatcher watcher : watchers) {
                    watcher.onChildrenChanged(path, children);
                }
            }
        };

    }

    public void addWatcher(RegistryWatcher watcher) {
        if (watchers.add(watcher)) {
            start();
        }
    }

    /**
     * @param watcher
     *            Removes a watcher from the node.
     * @return true if there are no more watchers.
     */
    public boolean removeWatcher(RegistryWatcher watcher) {
        watchers.remove(watcher);
        if (watchers.isEmpty()) {
            stop();
            return true;
        }
        return false;
    }

    private void start() {
        if (started.compareAndSet(false, true)) {
            watch();
        }
    }

    private void stop() {
        started.set(false);
    }

    private void watch() {
        if (started.get()) {
            zk.getChildren(path, watcher, callback, callback);
        }
    }
}
