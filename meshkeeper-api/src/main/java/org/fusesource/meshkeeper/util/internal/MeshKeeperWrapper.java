/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.meshkeeper.util.internal;

import java.util.concurrent.ScheduledExecutorService;

import org.fusesource.meshkeeper.Distributable;
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

    public <T extends Distributable> DistributionRef<T> distribute(String path, boolean sequential, T distributable) throws Exception {
        return next.distribute(path, sequential, distributable);
    }

    public Eventing eventing() {
        return next.eventing();
    }

    public String getDistributorUri() {
        return next.getDistributorUri();
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

    public void undistribute(Distributable distributable) throws Exception {
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

}