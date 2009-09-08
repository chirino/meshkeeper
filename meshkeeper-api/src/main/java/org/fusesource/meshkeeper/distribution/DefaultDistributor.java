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
import java.util.concurrent.TimeoutException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.fusesource.meshkeeper.Distributable;
import org.fusesource.meshkeeper.MeshKeeper;
import org.fusesource.meshkeeper.MeshEvent;
import org.fusesource.meshkeeper.MeshEventListener;
import org.fusesource.meshkeeper.RegistryWatcher;
import org.fusesource.meshkeeper.MeshKeeper.Remoting;
import org.fusesource.meshkeeper.MeshKeeper.Eventing;
import org.fusesource.meshkeeper.MeshKeeper.Registry;
import org.fusesource.meshkeeper.MeshKeeper.Repository;
import org.fusesource.meshkeeper.MeshKeeper.Launcher;
import org.fusesource.meshkeeper.MeshArtifact;
import org.fusesource.meshkeeper.distribution.event.EventClient;
import org.fusesource.meshkeeper.distribution.registry.RegistryClient;
import org.fusesource.meshkeeper.distribution.registry.RegistryHelper;
import org.fusesource.meshkeeper.distribution.remoting.RemotingClient;
import org.fusesource.meshkeeper.distribution.repository.RepositoryManager;

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
    private RepositoryManager resourceManager;
    private LaunchClient launchClient;

    private String registryUri;
    private final HashMap<Distributable, DistributionRef<?>> distributed = new HashMap<Distributable, DistributionRef<?>>();

    DefaultDistributor() {

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
        if (launchClient == null) {
            launchClient = new LaunchClient();
            launchClient.setMeshKeeper(this);
            try {
                launchClient.start();
            } catch (Exception e) {
                log.warn("Error starting launch client", e);
            }
        }
        return launchClient;
    }

    public void start() {

    }

    public synchronized void destroy() throws Exception {
        log.info("Shutting down");
        if (launchClient != null) {
            launchClient.destroy();
        }

        eventClient.close();
        for (DistributionRef<?> ref : distributed.values()) {
            ref.unregister();
        }
        remoting.destroy();
        registry.destroy();
        log.info("Shut down");
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
    void setResourceManager(RepositoryManager resourceManager) {
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

    private <T extends Distributable> DistributionRef<T> getRef(T object, boolean create) {
        DistributionRef<T> ref = null;
        synchronized (distributed) {

            ref = (DistributionRef<T>) distributed.get(object);
            if (ref == null && create) {
                ref = new DistributionRef<T>(object);
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
    public final <T extends Distributable> DistributionRef<T> distribute(String path, boolean sequential, T object) throws Exception {
        DistributionRef<T> ref = getRef(object, true);
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
    public final void undistribute(Distributable object) throws Exception {
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
        return registry.addObject(path, sequential, o);
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
    public <T> T getRegistryObject(String path) throws Exception {
        return (T) registry.getObject(path);
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
        return registry.getData(path);
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
        registry.remove(path, recursive);
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
        return registry.addData(path, sequential, data);
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
        registry.addRegistryWatcher(path, watcher);
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
        return RegistryHelper.waitForRegistrations(registry, path, min, timeout);
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
    public final <T extends Distributable> T export(T object) throws Exception {
        DistributionRef<T> ref = getRef(object, true);
        ref.export();
        return ref.stub;
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
    public final void unexport(Distributable object) throws Exception {
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
        resourceManager.locateResource(resource);
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

    private class DistributionRef<D extends Distributable> implements MeshKeeper.DistributionRef<D> {
        private D object;
        private D stub;
        private String path;

        DistributionRef(D object) {
            this.object = object;
        }

        @SuppressWarnings("unchecked")
        public D getProxy() {
            return stub;
        }

        @SuppressWarnings("unchecked")
        public D getTarget() {
            return object;
        }

        public String getRegistryPath() {
            return path;
        }

        @SuppressWarnings("unchecked")
        private synchronized D export() throws Exception {
            if (stub == null) {
                stub = (D) remoting.export(object);
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
                this.path = registry.addObject(path, sequential, (Serializable) stub);
            }
            return this.path;
        }

        private synchronized void unexport() throws Exception {
            if (stub != null) {
                remoting.unexport(stub);
                stub = null;
            }

            if (path != null) {
                registry.remove(path, true);
            }
        }

        private synchronized void unregister() throws Exception {
            unexport();
        }
    }

}
