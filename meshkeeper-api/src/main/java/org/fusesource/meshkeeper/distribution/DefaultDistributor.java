/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.meshkeeper.distribution;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.fusesource.meshkeeper.MeshKeeper;
import org.fusesource.meshkeeper.MeshEvent;
import org.fusesource.meshkeeper.MeshEventListener;
import org.fusesource.meshkeeper.RegistryWatcher;
import org.fusesource.meshkeeper.MeshKeeper.Remoting;
import org.fusesource.meshkeeper.MeshKeeper.Eventing;
import org.fusesource.meshkeeper.MeshKeeper.Registry;
import org.fusesource.meshkeeper.MeshKeeper.Repository;
import org.fusesource.meshkeeper.MeshArtifact;
import org.fusesource.meshkeeper.distribution.event.EventClient;
import org.fusesource.meshkeeper.distribution.registry.RegistryClient;
import org.fusesource.meshkeeper.distribution.remoting.RemotingClient;
import org.fusesource.meshkeeper.distribution.repository.RepositoryClient;

/**
 * Distributor
 * <p>
 * Description:
 * </p>
 * 
 * @author cmacnaug
 * @version 1.0
 */
class DefaultDistributor implements MeshKeeper, Eventing, Remoting, Repository, Registry {

    private Log log = LogFactory.getLog(this.getClass());

    private RemotingClient remoting;
    private RegistryClient registry;
    private EventClient eventClient;
    private RepositoryClient resourceManager;
    private LaunchClient launchClient;
    private ClassLoader userClassLoader;

    private String registryUri;
    private final HashMap<Object, DistributionRef<?>> distributed = new HashMap<Object, DistributionRef<?>>();

    private AtomicBoolean started = new AtomicBoolean(false);
    private AtomicBoolean destroyed = new AtomicBoolean(false);

    DefaultDistributor() {

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.fusesource.meshkeeper.MeshKeeper#getExecutorService()
     */
    public ScheduledExecutorService getExecutorService() {
        return DistributorFactory.getExecutorService();
    }

    public void setUserClassLoader(ClassLoader classLoader) {
        if (userClassLoader != classLoader) {
            userClassLoader = classLoader;
            remoting.setUserClassLoader(classLoader);
            eventClient.setUserClassLoader(classLoader);
            registry.setUserClassLoader(classLoader);
            if (launchClient != null) {
                launchClient.setUserClassLoader(classLoader);
            }
        }

    }

    public ClassLoader getUserClassLoader() {
        return userClassLoader;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.fusesource.meshkeeper.Distributor#getDistributorUri()
     */
    public String getDistributorUri() {
        return registryUri;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.fusesource.meshkeeper.MeshKeeper#getEventing()
     */
    public Eventing eventing() {
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.fusesource.meshkeeper.MeshKeeper#getRemoting()
     */
    public Remoting remoting() {
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.fusesource.meshkeeper.MeshKeeper#getRepository()
     */
    public Repository repository() {
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.fusesource.meshkeeper.MeshKeeper#getRegistry()
     */
    public Registry registry() {
        return this;
    }

    public synchronized Launcher launcher() {

        if (launchClient == null)
        {
            launchClient = new LaunchClient();
            launchClient.setMeshKeeper(this);
            try {
                launchClient.start();
            } catch (Exception e) {
                log.warn("Error starting launch client", e);
            }

            if (userClassLoader != null) {
                launchClient.setUserClassLoader(userClassLoader);
            }
        }
        return launchClient;
    }

    public void start() {
        if (destroyed.get()) {
            throw new IllegalStateException("Can't start destoyed MeshKeeper");
        }
        started.set(true);
    }

    public synchronized void destroy() throws Exception {
        if (destroyed.compareAndSet(false, true)) {
            log.info("Shutting down");
            if (launchClient != null) {
                launchClient.destroy();
            }

            eventClient.destroy();
            for (DistributionRef<?> ref : distributed.values()) {
                ref.unregister();
            }

            remoting.destroy();

            registry.destroy();

            log.info("Shut down");

        }
    }

    synchronized void setRemotingClient(RemotingClient remoting) {
        if (this.remoting == null) {
            this.remoting = remoting;
        }
    }

    void setRegistry(RegistryClient registry) {
        if (this.registry == null) {
            this.registry = registry;
        }
    }

    /**
     * @param resourceManager
     *            the resourceManager to set
     */
    void setResourceManager(RepositoryClient resourceManager) {
        this.resourceManager = resourceManager;
    }

    void setEventClient(EventClient eventClient) {
        this.eventClient = eventClient;
    }

    public String toString() {
        return "Distributor [exporter: " + remoting + " registry: " + registry + "]";
    }

    /**
     * @param registryUri
     *            the registryUri to set
     */
    void setRegistryUri(String registryUri) {
        this.registryUri = registryUri;
    }

    /**
     * Returns the URI of the meshkeeper registry to which this distributor is
     * connected.
     * 
     * @return the registryUri
     */
    public String getRegistryUri() {
        return registryUri;
    }

    @SuppressWarnings("unchecked")
    private <T, S extends T> DistributionRef<T> getRef(S object, boolean create, Class<?>... serviceInterfaces) {
        DistributionRef<T> ref = null;
        synchronized (distributed) {

            ref = (DistributionRef<T>) distributed.get(object);
            if (ref == null && create) {
                ref = new DistributionRef<T>(object, serviceInterfaces);
                distributed.put(object, ref);
            }
        }
        return ref;
    }

    /**
     * Exports an object, returning a stub that can be used to perform remote
     * method invocation for the object. This method additionally registers the
     * stub at the provided path in this distributor's registry.
     * 
     * @param <T>
     * @param object
     *            The object to export.
     * @param path
     *            The path in the registry at which to store the stub.
     * @return A reference to the distributed object.
     * @throws Exception
     *             If there is an error distributing the object.
     */
    public final <T, S extends T> DistributionRef<T> distribute(String path, boolean sequential, S object, Class<?>... serviceInterfaces) throws Exception {
        DistributionRef<T> ref = getRef((T) object, true, serviceInterfaces);
        ref.register(path, sequential);
        return ref;
    }

    /**
     * Unexports and unregisters a previously registered object.
     * 
     * @param object
     *            The object.
     * @throws Exception
     *             If there is an error exporting the object.
     */
    public final void undistribute(Object object) throws Exception {
        DistributionRef<?> ref = getRef(object, false);
        if (ref != null) {
            ref.unregister();
            synchronized (distributed) {
                distributed.remove(object);
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////
    //Registry Related Operations
    ////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Adds an object to the registry at the given path. If sequential is true
     * then the object will be added at the given location with a unique name.
     * Otherwise the object will be added at the location given by path.
     * 
     * @param path
     *            The path to add to.
     * @param sequential
     *            When true a unique child node is created at the given path
     * @param o
     *            The object to add.
     * @return The path at which the element was added.
     * @throws Exception
     *             If there is an error adding the node.
     */
    public String addRegistryObject(String path, boolean sequential, Serializable o) throws Exception {
        return registry.addRegistryObject(path, sequential, o);
    }

    /**
     * Gets the data at the specified node as an object.
     * 
     * @param <T>
     *            The type of the object expected.
     * @param path
     *            The path of the object.
     * @return The object at the given node.
     * @throws Exception
     *             If the object couldn't be retrieved.
     */
    @SuppressWarnings("unchecked")
    public <T> T getRegistryObject(String path) throws Exception {
        return (T) registry.getRegistryObject(path);
    }

    /**
     * Gets the data at the specified node.
     * 
     * @param path
     *            The path of the data.
     * @return The data at the given node.
     * @throws Exception
     *             If the object couldn't be retrieved.
     */
    public byte[] getRegistryData(String path) throws Exception {
        return registry.getRegistryData(path);
    }

    /**
     * Removes a node from the registry.
     * 
     * @param path
     *            The path to remove.
     * @param recursive
     *            If true then any children will also be removed.
     * @throws Exception
     *             If the path couldn't be removed.
     */
    public void removeRegistryData(String path, boolean recursive) throws Exception {
        registry.removeRegistryData(path, recursive);
    }

    /**
     * Adds data to the registry at the given path. If sequential is true then
     * the data will be added at the given location with a unique name.
     * Otherwise the data will be added at the location given by path.
     * 
     * @param path
     *            The path to add to.
     * @param sequential
     *            When true a unique child node is created at the given path
     * @param data
     *            The data. If null then a 0 byte array will be stored in the
     *            registry
     * @return The path at which the element was added.
     * @throws Exception
     *             If there is an error adding the node.
     */
    public String addRegistryData(String path, boolean sequential, byte[] data) throws Exception {
        return registry.addRegistryData(path, sequential, data);
    }

    /**
     * Adds a listener for changes in a path's child elements.
     * 
     * @param path
     * @param watcher
     */
    public void addRegistryWatcher(String path, RegistryWatcher watcher) throws Exception {
        registry.addRegistryWatcher(path, watcher);
    }

    /**
     * Removes a previously registered
     * 
     * @param path
     *            The path on which the listener was listening.
     * @param watcher
     *            The watcher
     */
    public void removeRegistryWatcher(String path, RegistryWatcher watcher) throws Exception {
        registry.removeRegistryWatcher(path, watcher);
    }

    /**
     * Convenience method that waits for a minimum number of objects to be
     * registered at the given registry path.
     * 
     * @param <T>
     * @param path
     *            The path
     * @param min
     *            The minimum number of objects to wait for.
     * @param timeout
     *            The maximum amount of time to wait.
     * @return The objects that were registered.
     * @throws Exception
     */
    public <T> Collection<T> waitForRegistrations(String path, int min, long timeout) throws TimeoutException, Exception {
        return registry.waitForRegistrations(path, min, timeout);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.fusesource.meshkeeper.MeshKeeper.Registry#waitForRegistration(java
     * .lang.String, long)
     */
    @SuppressWarnings("unchecked")
    public <T> T waitForRegistration(String path, long timeout) throws TimeoutException, Exception {
        return (T) registry.waitForRegistration(path, timeout);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////
    //RMI Related Operations
    ////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Exports an object, returning a proxy that can be used to perform remote
     * method invocation for the object.
     * 
     * @param <T>
     * @param object
     *            The object to export.
     * @return A reference to the distributed object.
     * @throws Exception
     *             If there is an error distributing the object.
     */
    public final <T> T export(T object, Class<?>... serviceInterfaces) throws Exception {
        DistributionRef<T> ref = getRef(object, true, serviceInterfaces);
        ref.export();
        return ref.stub;
    }

    /**
     * Exports a object returning an RMI proxy to it, but to a specific address.
     * This allows users to register multiple objects sharing the same
     * interfaces to a single location thus allowing multicast method call to
     * all objects registered at the adress The proxy can then be passed to
     * other applications in the mesh to use via RMI. It is best practice to
     * unexport the object when it is no longer used.
     * 
     * @param <T>
     *            The type to which to cast the returned stub
     * @param obj
     *            The object to export
     * @param address
     *            The address (e.g. ServiceInterfaceFoo
     * @param interfaces
     *            The interfaces to which to limit the export.
     * @return The proxy that can be used to invoke method calls on the exported
     *         object.
     * @throws Exception
     *             If there is an error exporting
     */
    public <T> T exportMulticast(T obj, String address, Class<?>... interfaces) throws Exception {
        DistributionRef<T> ref = getRef(obj, true, interfaces);
        ref.setMultiCastPrefix(address);
        ref.export();
        return ref.stub;
    }

    /**
     * Gets a proxy object for a multicast export.
     * 
     * @param <T>
     * @param address
     *            The address to which multicast objects are exported.
     * @param interfaces
     *            The interfaces for the proxy.
     * @return The proxy for the multicast address.
     * @throws Exception
     *             If there is an error
     */
    public <T> T getMulticastProxy(String address, Class<?> mainInterface, Class <?> ... extraInterfaces) throws Exception {
        return (T) remoting.getMulticastProxy(address, mainInterface, extraInterfaces);
    }

    /**
     * Unexports an object. If the object's stub was registered it will be
     * unregistered as well.
     * 
     * @param <T>
     * @param object
     *            The object to unexport.
     * @throws Exception
     *             If there is an error unexporting the object.
     */
    public final void unexport(Object object) throws Exception {
        DistributionRef<?> ref = getRef(object, false);
        if (ref != null) {
            ref.unregister();
            synchronized (distributed) {
                distributed.remove(object);
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////
    //Event Related Operations
    ////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Sends an event on the given topic.
     */
    public void sendEvent(MeshEvent event, String topic) throws Exception {
        eventClient.sendEvent(event, topic);
    }

    /**
     * Opens a listener on the given event topic.
     * 
     * @param listener
     *            The listener
     * @param topic
     *            The topic
     * @throws Exception
     *             If there is an error opening the listener
     */
    public void openEventListener(MeshEventListener listener, String topic) throws Exception {
        eventClient.openEventListener(listener, topic);
    }

    /**
     * Stops listening to events on the given topic.
     * 
     * @param listener
     *            The listener The listener
     * @param topic
     *            The topic
     * @throws Exception
     *             If there is an error closing the listener
     */
    public void closeEventListener(MeshEventListener listener, String topic) throws Exception {
        eventClient.closeEventListener(listener, topic);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////
    //Resource Related Operations
    ////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Factory method for creating a resource.
     * 
     * @return An empty resource.
     */
    public MeshArtifact createResource() {
        return resourceManager.createResource();
    }

    /**
     * Called to locate the given resource.
     * 
     * @param resource
     *            The resource to locate.
     * @throws Exception
     *             If there is an error locating the resource.
     */
    public void resolveResource(MeshArtifact resource) throws Exception {
        resourceManager.resolveResource(resource);
    }

    /**
     * @param resource
     * @param data
     * @throws IOException
     */
    public void deployFile(MeshArtifact resource, byte[] data) throws Exception {
        resourceManager.deployFile(resource, data);
    }

    /**
     * 
     * @param resource
     * @param d
     * @throws Exception
     */
    public void deployDirectory(MeshArtifact resource, File d) throws Exception {
        resourceManager.deployDirectory(resource, d);
    }

    /**
     * @return The path to the local resource directory.
     */
    public File getLocalRepoDirectory() {
        return resourceManager.getLocalRepoDirectory();
    }

    /**
     * 
     * @throws IOException
     */
    public void purgeLocalRepo() throws IOException {
        resourceManager.purgeLocalRepo();
    }

    private class DistributionRef<D> implements MeshKeeper.DistributionRef<D> {
        private D object;
        private D stub;
        private String path;
        private String multiCastPrefix;
        private Class<?>[] serviceInterfaces;

        DistributionRef(D object, Class<?>... serviceInterfaces) {
            this.object = object;
        }

        public String getMultiCastPrefix() {
            return multiCastPrefix;
        }

        public void setMultiCastPrefix(String multiCastPrefix) {
            this.multiCastPrefix = multiCastPrefix;
        }

        public D getProxy() {
            return stub;
        }

        public D getTarget() {
            return object;
        }

        public String getRegistryPath() {
            return path;
        }

        private synchronized D export() throws Exception {
            if (stub == null) {
                if (multiCastPrefix != null) {
                    stub = (D) remoting.exportMulticast(object, multiCastPrefix, serviceInterfaces);
                } else {
                    stub = (D) remoting.export(object, serviceInterfaces);
                }
                if (log.isDebugEnabled())
                    log.debug("Exported: " + object + " to " + stub);
            }
            return stub;
        }

        private synchronized String register(String path, boolean sequential) throws Exception {
            if (this.path == null) {
                if (stub == null) {
                    export();
                }
                this.path = registry.addRegistryObject(path, sequential, (Serializable) stub);
            }
            return this.path;
        }

        private synchronized void unexport() throws Exception {
            if (stub != null) {
                remoting.unexport(stub);
                stub = null;
            }

            if (path != null) {
                registry.removeRegistryData(path, true);
            }
        }

        private synchronized void unregister() throws Exception {
            unexport();
        }
    }
}
