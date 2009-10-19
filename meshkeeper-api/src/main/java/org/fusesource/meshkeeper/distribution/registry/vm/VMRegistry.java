/**
 *  Copyright (C) 2009 Progress Software, Inc. All rights reserved.
 *  http://fusesource.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.fusesource.meshkeeper.distribution.registry.vm;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.fusesource.meshkeeper.RegistryWatcher;
import org.fusesource.meshkeeper.distribution.registry.AbstractRegistryClient;

/**
 * VMRegistry
 * <p>
 * Description: An VM Registry implementation
 * </p>
 * 
 * @author cmacnaug
 * @version 1.0
 */
public class VMRegistry extends AbstractRegistryClient {

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
    public String addRegistryData(String path, boolean sequential, byte[] data) throws Exception {
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
    public String addRegistryObject(String path, boolean sequential, Serializable o) throws Exception {
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
    public <T> T getRegistryObject(String path) throws Exception {
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
    public byte[] getRegistryData(String path) throws Exception {
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
    public void removeRegistryData(String path, boolean recursive) throws Exception {
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
