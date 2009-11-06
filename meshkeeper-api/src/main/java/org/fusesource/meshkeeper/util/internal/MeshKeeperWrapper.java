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
package org.fusesource.meshkeeper.util.internal;

import java.util.concurrent.ScheduledExecutorService;

import org.fusesource.meshkeeper.MeshKeeper;

/**
 * Creates a MeshKeeper object which delegates all it's method calls to another
 * MeshKeeper object.
 * 
 * @author chirino
 */
public class MeshKeeperWrapper implements MeshKeeper {

    protected final MeshKeeper next;

    public MeshKeeperWrapper(MeshKeeper next) {
        this.next = next;
    }

    public void destroy() throws Exception {
        next.destroy();
    }

    public <T, S extends T> DistributionRef<T> distribute(String path, boolean sequential, S distributable, Class<?>... serviceInterfaces) throws Exception {
        return next.distribute(path, sequential, (T)distributable, serviceInterfaces);
    }

    public Eventing eventing() {
        return next.eventing();
    }

    public String getRegistryConnectUri() {
        return next.getRegistryConnectUri();
    }

    public Launcher launcher() {
        return next.launcher();
    }

    public Registry registry() {
        return next.registry();
    }

    public Remoting remoting() {
        return next.remoting();
    }

    public Repository repository() {
        return next.repository();
    }

    public void start() throws Exception {
        next.start();
    }

    public void undistribute(Object distributable) throws Exception {
        next.undistribute(distributable);
    }

    public ScheduledExecutorService getExecutorService() {
        return next.getExecutorService();
    }

    public void setUserClassLoader(ClassLoader classLoader) {
        next.setUserClassLoader(classLoader);
    }

    public ClassLoader getUserClassLoader() {
        return next.getUserClassLoader();
    }
    
    public String getUUID() {
        return next.getUUID();
    }
    
    public String setUUID(String prefix) {
        return next.setUUID(prefix);
    }

}