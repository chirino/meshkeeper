/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.meshkeeper.distribution.registry.vm;

import java.io.Serializable;

import org.fusesource.meshkeeper.RegistryWatcher;
import org.fusesource.meshkeeper.distribution.registry.RegistryClient;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * VMRegistry
 * <p>
 * Description: An VM Registry implementation
 * </p>
 * 
 * @author cmacnaug
 * @version 1.0
 */
public class VMRegistry implements RegistryClient {

    private static final VMRegistryServer SERVER = new VMRegistryServer();
    private HashMap<String, HashSet<RegistryWatcher>> watchers = new HashMap<String, HashSet<RegistryWatcher>>();
    AtomicBoolean started = new AtomicBoolean(false);

    /*
     * (non-Javadoc)
     * 
     * @see org.fusesource.meshkeeper.distribution.registry.Registry#start()
     */
    public void start() throws Exception {
        started.compareAndSet(false, true);
        synchronized (this) {
            for (Map.Entry<String, HashSet<RegistryWatcher>> e : watchers.entrySet()) {
                for (RegistryWatcher w : e.getValue()) {
                    SERVER.removeRegistryWatcher(e.getKey(), w);
                }
            }
            watchers.clear();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.fusesource.meshkeeper.distribution.registry.Registry#destroy()
     */
    public void destroy() throws Exception {
        started.set(false);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.fusesource.meshkeeper.distribution.registry.Registry#addData(java
     * .lang.String, boolean, byte[])
     */
    public String addData(String path, boolean sequential, byte[] data) throws Exception {
        checkStarted();
        return SERVER.addData(path, sequential, data);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.fusesource.meshkeeper.distribution.registry.Registry#addObject(java
     * .lang.String, boolean, java.io.Serializable)
     */
    public String addObject(String path, boolean sequential, Serializable o) throws Exception {
        checkStarted();
        return SERVER.addObject(path, sequential, o);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.fusesource.meshkeeper.distribution.registry.Registry#getObject(java
     * .lang.String)
     */
    @SuppressWarnings("unchecked")
    public <T> T getObject(String path) throws Exception {
        checkStarted();
        return (T)SERVER.getObject(path);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.fusesource.meshkeeper.distribution.registry.Registry#getData(java
     * .lang.String)
     */
    public byte[] getData(String path) throws Exception {
        checkStarted();
        return SERVER.getData(path);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.fusesource.meshkeeper.distribution.registry.Registry#remove(java
     * .lang.String, boolean)
     */
    public void remove(String path, boolean recursive) throws Exception {
        checkStarted();
        SERVER.remove(path, recursive);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.fusesource.meshkeeper.distribution.registry.Registry#addRegistryWatcher
     * (java.lang.String,
     * org.fusesource.meshkeeper.distribution.registry.RegistryWatcher)
     */
    public void addRegistryWatcher(String path, RegistryWatcher watcher) throws Exception {
        checkStarted();
        boolean added = false;
        //Track our added watchers so we can clean them up:
        synchronized (this) {
            HashSet<RegistryWatcher> registered = watchers.get(path);
            if (registered == null) {
                registered = new HashSet<RegistryWatcher>();
                watchers.put(path, registered);
            }
            if (registered.add(watcher)) {
                added = true;
            }
        }

        if (added) {
            SERVER.addRegistryWatcher(path, watcher);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @seeorg.fusesource.meshkeeper.distribution.registry.Registry#
     * removeRegistryWatcher(java.lang.String,
     * org.fusesource.meshkeeper.distribution.registry.RegistryWatcher)
     */
    public synchronized void removeRegistryWatcher(String path, RegistryWatcher watcher) throws Exception {
        checkStarted();
        
        boolean removed = false;
        //Track our added watchers so we can clean them up:
        synchronized (this) {
            HashSet<RegistryWatcher> registered = watchers.get(path);
            if (registered != null) {
                if (registered.remove(watcher)) {
                    removed = true;
                }
            }
        }

        if (removed) {
            SERVER.removeRegistryWatcher(path, watcher);
        }
    }

    private void checkStarted() throws Exception {
        if (!started.get()) {
            throw new Exception("Not Connected");
        }
    }
}
