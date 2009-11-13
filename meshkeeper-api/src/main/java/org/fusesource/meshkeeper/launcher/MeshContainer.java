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
package org.fusesource.meshkeeper.launcher;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.fusesource.meshkeeper.MeshKeeper;
import org.fusesource.meshkeeper.MeshKeeperFactory;
import org.fusesource.meshkeeper.MeshKeeper.DistributionRef;
import org.fusesource.meshkeeper.distribution.PluginClassLoader;
import org.fusesource.meshkeeper.MeshContainer.Callable;
import org.fusesource.meshkeeper.MeshContainer.Hostable;
import org.fusesource.meshkeeper.MeshContainer.MeshContainerContext;
import org.fusesource.meshkeeper.MeshContainer.Runnable;

/**
 * MeshContainer
 * <p>
 * Description:
 * </p>
 * 
 * @author cmacnaug
 * @version 1.0
 */
public class MeshContainer implements MeshContainerService, MeshContainerContext {

    private static MeshKeeper mesh;
    private static final Log LOG = LogFactory.getLog(MeshContainer.class);
    private static boolean isInMeshContainer = false;

    private HashMap<String, Object> hosted = new HashMap<String, Object>();
    private String name;

    private CountDownLatch closeLatch = new CountDownLatch(1);

    private MeshContainer(String name) {
        this.name = name;
        isInMeshContainer = true;
    }

    public synchronized <T extends Serializable> T host(String name, T object, Class<?>... interfaces) throws Exception {

        T proxy = null;
        try {
            if (!hosted.containsKey(name)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(this + " Hosting: " + name + ":" + object);
                }
                proxy = (T) mesh.remoting().export(object);
                try {
                    hosted.put(name, object);
                } finally {
                    initializeObject(object);
                }
            } else {
                throw new Exception("Already hosting an object with name " + name);
            }
        } catch (Exception e) {
            LOG.warn("Error hosting " + name, e);
            if (proxy != null) {
                unhost(name);
                throw e;
            }
        }

        return proxy;
    }

    public synchronized void unhost(String name) throws Exception {

        Object d = hosted.get(name);
        if (d != null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(this + " Hosting: " + name + ":" + d);
            }
            try {
                mesh.remoting().unexport(d);
            } finally {
                destroyObject(d);
            }
        }
    }

    /**
     * Runs the {@link Runnable} in the container. The {@link Runnable} must
     * also implement {@link Serializable}.
     * 
     * @param r The {@link Runnable}
     * @throws Exception
     */
    public <R extends java.lang.Runnable & Serializable> void run(final R r) throws Exception {
        java.lang.Runnable wrapper = r;

        //Handle Hostables
        if (r instanceof Hostable) {
            initializeObject(r);
            wrapper = new java.lang.Runnable() {

                public void run() {
                    try {
                        r.run();
                    } finally {
                        try {
                            destroyObject(r);
                        } catch (Exception e) {
                            LOG.warn("Runnable destroy error", e);
                        }
                    }
                }
            };

            ((Hostable) r).initialize(this);
        }

        mesh.getExecutorService().execute(wrapper);
    }

    /**
     * Invokes the {@link Callable} in the container. The {@link Callable} must
     * also implement {@link Serializable}.
     * 
     * @param <T>
     * @param c The {@link Callable}
     * @return The result
     * @throws Exception If there is an exception
     */
    public <T, C extends java.util.concurrent.Callable<T> & Serializable> T call(C c) throws Exception {
        initializeObject(c);
        Exception first = null;
        try {
            return c.call();
        } catch (Exception e) {
            first = e;
            throw e;
        } finally {
            try {
                destroyObject(c);
            } catch (Exception e) {
                if (first != null) {
                    throw first;
                } else {
                    throw e;
                }
            }
        }
    }

    private void initializeObject(Object o) throws Exception {
        if (o instanceof Hostable) {
            ((Hostable) o).initialize(this);
        }
    }

    private void destroyObject(Object o) throws Exception {
        if (o instanceof Hostable) {
            ((Hostable) o).destroy(this);
        }
    }

    public void close() {
        closeLatch.countDown();
    }

    /**
     * @return The container's meshkeeper.
     */
    public static MeshKeeper getMeshKeeper() {
        return mesh;
    }

    /**
     * @return true if this is a meshcontainer launch
     */
    public static boolean isInMeshContainer() {
        return isInMeshContainer;
    }

    /*
     * (non-Javadoc)
     * 
     * @seeorg.fusesource.meshkeeper.MeshContainer.MeshContainerContext#
     * getContainerMeshKeeper()
     */
    public MeshKeeper getContainerMeshKeeper() {
        return mesh;
    }

    public String getContainerName() {
        return name;
    }

    public String toString() {
        return name;
    }

    public static final void main(String[] args) {

        if (args.length == 0) {
            System.err.println("Expected registry path");
        }
        String path = args[0];

        MeshContainer container = new MeshContainer(path);

        try {
            //Running from RemoteBootstrapper?
            if (Boolean.getBoolean(RemoteBootstrap.BOOTSTRAP_PROPERTY)) {
                //If we're running from the bootstrapper set the classloader
                //to the bootstrap classloader:
                MeshContainer.mesh = RemoteBootstrap.getMeshKeeper();
                mesh.setUserClassLoader(new MeshContainerClassLoader());
            } else {
                MeshContainer.mesh = MeshKeeperFactory.createMeshKeeper();
            }
            DistributionRef<MeshContainerService> ref = MeshContainer.getMeshKeeper().distribute(path, false, (MeshContainerService) container, MeshContainerService.class);
            MeshContainer.LOG.debug("Started MeshContainer: " + ref.getRegistryPath() + " cl: " + container.getClass().getClassLoader());
            container.closeLatch.await();
        } catch (Exception e) {
            LOG.error("MeshContainer error: ", e);
        } finally {
            try {
                if (mesh != null) {
                    mesh.destroy();
                }
            } catch (Exception e) {
                LOG.error("MeshContainer error: ", e);
            }
        }
    }

    private static class MeshContainerClassLoader extends ClassLoader {
        ArrayList<ClassLoader> delegates = new ArrayList<ClassLoader>(2);

        MeshContainerClassLoader() {
            super(MeshContainer.class.getClassLoader());

            //Search first in bootstrap class loader:
            delegates.add(RemoteBootstrap.getClassLoader());
            //Then try the plugin class loader:
            delegates.add(PluginClassLoader.getDefaultPluginLoader());
        }

        protected Class<?> findClass(String name) throws ClassNotFoundException {
            //System.out.println("Finding class: " + name);
            //Look for an already loaded class:
            try {
                return super.findClass(name);
            } catch (ClassNotFoundException cnfe) {
            }

            for (ClassLoader delegate : delegates) {
                //Try the delegates
                try {
                    return delegate.loadClass(name);
                } catch (ClassNotFoundException cnfe) {
                }
            }

            throw new ClassNotFoundException(name);

        }

        protected URL findResource(String name) {
            //Look for an already loaded class:
            URL url = super.findResource(name);

            for (ClassLoader delegate : delegates) {
                if (url == null) {
                    url = delegate.getResource(name);
                } else {
                    break;
                }
            }
            return url;
        }

        protected Enumeration<URL> findResources(String name) throws IOException {
            Enumeration<URL> urls = null;
            try {
                urls = super.findResources(name);
            } catch (IOException ioe) {
            }

            for (ClassLoader delegate : delegates) {
                if (urls == null) {
                    //Try the plugin classloader:
                    try {
                        urls = delegate.getResources(name);
                    } catch (IOException ioe) {
                    }
                } else {
                    break;
                }
            }
            return urls;
        }
    }

}
