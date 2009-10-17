/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.meshkeeper.distribution;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.fusesource.meshkeeper.MeshKeeper;
import org.fusesource.meshkeeper.control.ControlServer;
import org.fusesource.meshkeeper.distribution.event.EventClient;
import org.fusesource.meshkeeper.distribution.event.EventClientFactory;
import org.fusesource.meshkeeper.distribution.registry.RegistryClient;
import org.fusesource.meshkeeper.distribution.registry.RegistryFactory;
import org.fusesource.meshkeeper.distribution.remoting.RemotingClient;
import org.fusesource.meshkeeper.distribution.remoting.RemotingFactory;
import org.fusesource.meshkeeper.distribution.repository.RepositoryClient;
import org.fusesource.meshkeeper.distribution.repository.RepositoryManagerFactory;

/**
 * Distributor
 * <p>
 * Description:
 * </p>
 * 
 * @author cmacnaug
 * @version 1.0
 */
class DefaultDistributor implements MeshKeeper {

    private Log log = LogFactory.getLog(this.getClass());

    private String registryUri;
    private String remotingUri;
    private String eventingUri;
    private String repositoryUri;
    private String workingDirectory;

    private RemotingWrapper remoting;
    private RegistryClient registry;
    private EventClient eventing;
    private RepositoryClient repository;
    private LaunchClient launchClient;
    private ClassLoader userClassLoader;

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
            if (registry != null) {
                registry.setUserClassLoader(classLoader);
            }
            if (remoting != null) {
                remoting.setUserClassLoader(classLoader);
            }
            if (eventing != null) {
                eventing.setUserClassLoader(classLoader);
            }
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

    private synchronized <T extends PluginClient> T createPluginClient(String uri, AbstractPluginFactory<T> factory, String lookupPath, String defaultUri) throws PluginCreationException {
        try {
            //Create Remoting client:
            if (uri == null) {
                if (lookupPath != null) {
                    uri = registry().getRegistryObject(lookupPath);
                }
                if (uri == null) {
                    uri = defaultUri;
                }
            }
            T ret = factory.create(uri);
            if (userClassLoader != null) {
                ret.setUserClassLoader(userClassLoader);
            }
            ret.start();
            return ret;
        } catch (Exception e) {
            PluginCreationException re = new PluginCreationException("Unable to create plugin client from " + factory.getClass().getSimpleName(), e);
            log.error(re.getMessage(), re);
            throw re;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.fusesource.meshkeeper.MeshKeeper#getRegistry()
     */
    public synchronized Registry registry() throws PluginCreationException {
        if (registry == null) {
            synchronized (this) {
                if (registry == null) {
                    registry = createPluginClient(registryUri, new RegistryFactory(), null, ControlServer.DEFAULT_REGISTRY_URI);
                }
            }
        }
        return registry;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.fusesource.meshkeeper.MeshKeeper#getEventing()
     */
    public Eventing eventing() throws PluginCreationException {
        if (eventing == null) {
            synchronized (this) {
                if (eventing == null) {
                    eventing = createPluginClient(eventingUri, new EventClientFactory(), ControlServer.EVENTING_URI_PATH, ControlServer.DEFAULT_EVENT_URI);
                }
            }
        }
        return eventing;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.fusesource.meshkeeper.MeshKeeper#getRemoting()
     */
    public Remoting remoting() throws PluginCreationException {
        if (remoting == null) {
            synchronized (this) {
                if (remoting == null) {
                    remoting = new RemotingWrapper(createPluginClient(remotingUri, new RemotingFactory(), ControlServer.REMOTING_URI_PATH, ControlServer.DEFAULT_REMOTING_URI));

                }
            }
        }
        return remoting;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.fusesource.meshkeeper.MeshKeeper#getRepository()
     */
    public Repository repository() {
        if (repository == null) {
            synchronized (this) {
                //TODO make this work like the other plugins:
                try {
                    //Create ResourceManager:
                    RepositoryClient resourceManager = new RepositoryManagerFactory().create(repositoryUri);
                    String commonRepoUrl = registry.getRegistryObject(ControlServer.REPOSITORY_URI_PATH);
                    if (commonRepoUrl != null) {
                        resourceManager.setCentralRepoUri(commonRepoUrl, null);
                    }
                    resourceManager.setLocalRepoDir(workingDirectory + File.separator + "local-repo");
                    resourceManager.start();
                    repository = resourceManager;
                } catch (Exception e) {
                    RuntimeException re = new RuntimeException("Error creating repository client", e);
                    log.error(re);
                    throw re;
                }
            }
        }

        return repository;
    }

    public Launcher launcher() {

        if (launchClient == null) {
            synchronized (this) {
                if (launchClient == null) {
                    launchClient = new LaunchClient();
                    launchClient.setMeshKeeper(this);
                    try {
                        launchClient.start();
                    } catch (Exception e) {
                        log.warn("Error starting launch client", e);
                        throw new PluginCreationException("Error starting launch client", e);
                    }

                    if (userClassLoader != null) {
                        launchClient.setUserClassLoader(userClassLoader);
                    }
                }
            }
        }
        return launchClient;
    }

    public synchronized void start() throws Exception {
        if (destroyed.get()) {
            throw new IllegalStateException("Can't start destoyed MeshKeeper");
        }

        try {
            //Start up the registry client:
            registry();

            //TODO Consider deferring startup of these?
            remoting();
            eventing();
            repository();
        } catch (PluginCreationException re) {
            if (re.getCause() != null && re.getCause() instanceof Exception) {
                throw (Exception) re.getCause();
            }
            throw re;
        }

        started.set(true);
    }

    public synchronized void destroy() throws Exception {
        if (destroyed.compareAndSet(false, true)) {
            log.info("Shutting down");
            if (launchClient != null) {
                launchClient.destroy();
            }

            if (eventing != null) {
                eventing.destroy();
            }

            for (DistributionRef<?> ref : distributed.values()) {
                ref.unregister();
            }

            if (remoting != null) {
                remoting.destroy();
            }

            if (registry != null) {
                registry.destroy();
            }

            log.info("Shut down");

        }
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
     * @param remotingUri
     *            Set the uri to used to create a remoting client
     */
    void setRemotingUri(String remotingUri) {
        this.remotingUri = remotingUri;
    }

    /**
     * @param eventingUri
     *            Set the uri used to create an eventing client
     */
    public void setEventingUri(String eventingUri) {
        this.eventingUri = eventingUri;
    }

    /**
     * @param repositoryProvider
     *            Set the uri used to create a repository client
     */
    public void setRepositoryUri(String repositoryUri) {
        this.repositoryUri = repositoryUri;
    }

    /**
     * @param directory
     */
    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
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

    /**
     * RemotingWrapper
     * <p>
     * Description: The remoting wrapper intercepts export calls looking for
     * distribution refs.
     * </p>
     * 
     * @author cmacnaug
     * @version 1.0
     */
    private final class RemotingWrapper implements RemotingClient {

        private final RemotingClient delegate;

        RemotingWrapper(RemotingClient delegate) {
            this.delegate = delegate;
        }

        /**
         * Exports an object, returning a proxy that can be used to perform
         * remote method invocation for the object.
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
         * Exports a object returning an RMI proxy to it, but to a specific
         * address. This allows users to register multiple objects sharing the
         * same interfaces to a single location thus allowing multicast method
         * call to all objects registered at the adress The proxy can then be
         * passed to other applications in the mesh to use via RMI. It is best
         * practice to unexport the object when it is no longer used.
         * 
         * @param <T>
         *            The type to which to cast the returned stub
         * @param obj
         *            The object to export
         * @param address
         *            The address (e.g. ServiceInterfaceFoo
         * @param interfaces
         *            The interfaces to which to limit the export.
         * @return The proxy that can be used to invoke method calls on the
         *         exported object.
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
        @SuppressWarnings("unchecked")
        public <T> T getMulticastProxy(String address, Class<?> mainInterface, Class<?>... extraInterfaces) throws Exception {
            return (T) delegate.getMulticastProxy(address, mainInterface, extraInterfaces);
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

        public void destroy() throws Exception {
            delegate.destroy();
        }

        public ClassLoader getUserClassLoader() {
            return delegate.getUserClassLoader();
        }

        public void setUserClassLoader(ClassLoader classLoader) {
            delegate.setUserClassLoader(classLoader);
        }

        public void start() throws Exception {
            delegate.start();
        }

        public Remoting delegate() {
            return delegate;
        }

        public String toString() {
            return delegate.toString();
        }
    }

    private final Remoting remotingDelegate() {
        remoting();
        return remoting.delegate();
    }

    /**
     * PluginCreationException
     * <p>
     * Description: Thrown when there is an error creating a meshkeeper plugin.
     * </p>
     * 
     * @author cmacnaug
     * @version 1.0
     */
    public static class PluginCreationException extends RuntimeException {
        /**
         * @param string
         * @param e
         */
        public PluginCreationException(String string, Exception e) {
            super(string, e);
        }

        private static final long serialVersionUID = 1L;
    }

    private class DistributionRef<D> implements MeshKeeper.DistributionRef<D> {
        private D object;
        private D stub;
        private String path;
        private String multiCastPrefix;
        private Class<?>[] serviceInterfaces;

        DistributionRef(D object, Class<?>... serviceInterfaces) {
            this.object = object;
            this.serviceInterfaces = serviceInterfaces;
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
                    stub = (D) remotingDelegate().exportMulticast(object, multiCastPrefix, serviceInterfaces);
                } else {
                    stub = (D) remotingDelegate().export(object, serviceInterfaces);
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
                this.path = registry().addRegistryObject(path, sequential, (Serializable) stub);
            }
            return this.path;
        }

        private synchronized void unexport() throws Exception {
            if (stub != null) {
                remotingDelegate().unexport(stub);
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
