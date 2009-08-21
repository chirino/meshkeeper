/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.cloudlaunch.distribution;

import java.io.Serializable;
import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.fusesource.cloudlaunch.distribution.event.EventClient;
import org.fusesource.cloudlaunch.distribution.registry.Registry;
import org.fusesource.cloudlaunch.distribution.resource.ResourceManager;
import org.fusesource.cloudlaunch.distribution.rmi.IExporter;

/**
 * Distributor
 * <p>
 * Description:
 * </p>
 * 
 * @author cmacnaug
 * @version 1.0
 */
public class Distributor {

    private Log log = LogFactory.getLog(this.getClass());

    private IExporter exporter;
    private Registry registry;
    private EventClient eventClient;
    private ResourceManager resourceManager;
    private String registryUri;
    private final HashMap<Distributable, DistributionRef<?>> distributed = new HashMap<Distributable, DistributionRef<?>>();

    Distributor() {

    }

    private <T extends Distributable> DistributionRef<T> getRef(Distributable object, boolean create) {
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
     * method invocation for the object.
     * 
     * @param <T>
     * @param object
     *            The object to export.
     * @return A reference to the distributed object.
     * @throws Exception
     *             If there is an error distributing the object.
     */
    public final <T extends Distributable> DistributionRef<T> export(Distributable object) throws Exception {
        DistributionRef<T> ref = getRef(object, true);
        ref.export();
        return ref;
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
    public final <T extends Distributable> DistributionRef<T> register(Distributable object, String path, boolean sequential) throws Exception {
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
    public final void unregister(Distributable object) throws Exception {
        DistributionRef<?> ref = getRef(object, false);
        if (ref != null) {
            ref.unregister();
            synchronized (distributed) {
                distributed.remove(object);
            }
        }
    }

    public void start() {
        
    }

    public void destroy() throws Exception {
        log.info("Shutting down");
        eventClient.close();
        for (DistributionRef<?> ref : distributed.values()) {
            ref.unregister();
        }
        exporter.destroy();
        registry.destroy();
        log.info("Shut down");
    }

    synchronized void setExporter(IExporter exporter) {
        if (this.exporter == null) {
            this.exporter = exporter;
        }
    }

    public IExporter getExporter() {
        return exporter;
    }

    void setRegistry(Registry registry) {
        if (this.registry == null) {
            this.registry = registry;
        }
    }

    public synchronized Registry getRegistry() {
        return registry;
    }

    /**
     * @param resourceManager
     *            the resourceManager to set
     */
    void setResourceManager(ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    /**
     * @return the resourceManager
     */
    public ResourceManager getResourceManager() {
        return resourceManager;
    }

    public EventClient getEventClient() {
        return eventClient;
    }

    void setEventClient(EventClient eventClient) {
        this.eventClient = eventClient;
    }

    public String toString() {
        return "Distributor [exporter: " + exporter + " registry: " + registry + "]";
    }
    
    /**
     * @param registryUri the registryUri to set
     */
    void setRegistryUri(String registryUri) {
        this.registryUri = registryUri;
    }

    /**
     * Returns the URI of the cloudlaunch registry to which this distributor is 
     * connected.
     * 
     * @return the registryUri
     */
    public String getRegistryUri() {
        return registryUri;
    }

    public class DistributionRef<D extends Distributable> {
        private Distributable object;
        private D stub;
        private String path;

        DistributionRef(Distributable object) {
            this.object = object;
        }

        public Distributable getStub() {
            return stub;
        }

        public String getPath() {
            return path;
        }

        @SuppressWarnings("unchecked")
        private synchronized D export() throws Exception {
            if (stub == null) {
                stub = (D) exporter.export(object);
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
                exporter.unexport(stub);
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
