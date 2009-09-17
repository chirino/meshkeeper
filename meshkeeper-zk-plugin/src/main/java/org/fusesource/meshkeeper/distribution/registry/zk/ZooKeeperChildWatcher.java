/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.meshkeeper.distribution.registry.zk;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.AsyncCallback.ChildrenCallback;
import org.fusesource.meshkeeper.RegistryWatcher;

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
                        //System.out.println("NodeChildrenChangedEvent");
                        watch();
                        break;
                    }
                    default:
                    {
                        System.out.println("WARNING Got:" + event);
                    }
                    }
                }
        };

        this.callback = new ChildrenCallback() {

            public void processResult(int rc, String path, Object ctx, List<String> children) {
                handleChildUpdate(path, children);
            }
        };

    }
    
    private void handleChildUpdate(String path, List<String> children)
    {
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
            //System.out.println("Registering watch for: " + path);
            zk.getChildren(path, watcher, callback, callback);
//            try {
//                handleChildUpdate(path, zk.getChildren(path, watcher));
//            } catch (KeeperException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            } catch (InterruptedException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
        }
    }
}
