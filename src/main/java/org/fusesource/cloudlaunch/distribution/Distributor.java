/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.cloudlaunch.distribution;

import java.io.Serializable;
import java.net.URI;
import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.fusesource.cloudlaunch.control.ControlServer;
import org.fusesource.cloudlaunch.distribution.event.EventClient;
import org.fusesource.cloudlaunch.distribution.event.EventClientFactory;
import org.fusesource.cloudlaunch.distribution.registry.Registry;
import org.fusesource.cloudlaunch.distribution.registry.RegistryFactory;
import org.fusesource.cloudlaunch.distribution.rmi.ExporterFactory;
import org.fusesource.cloudlaunch.distribution.rmi.IExporter;
import org.fusesource.cloudlaunch.util.internal.FactoryFinder;

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
    private static final FactoryFinder REGISTRY_FACTORY_FINDER = new FactoryFinder("META-INF/services/org/fusesource/cloudlaunch/distribution/registry/");
    private static final FactoryFinder EXPORTER_FACTORY_FINDER = new FactoryFinder("META-INF/services/org/fusesource/cloudlaunch/distribution/exporter/");
    private static final FactoryFinder EVENT_FACTORY_FINDER = new FactoryFinder("META-INF/services/org/fusesource/cloudlaunch/distribution/event/");

    public static Distributor create(String registryUri) throws Exception {
        URI uri = new URI(registryUri);

        RegistryFactory rf = (RegistryFactory) REGISTRY_FACTORY_FINDER.newInstance(uri.getScheme());
        Registry registry = rf.createRegistry(registryUri);
        registry.start();

        URI exporterUri = new URI((String) registry.getObject(ControlServer.EXPORTER_CONNECT_URI_PATH));
        ExporterFactory ef = (ExporterFactory) EXPORTER_FACTORY_FINDER.newInstance(exporterUri.getScheme());
        IExporter exporter = ef.createExporter(exporterUri.toString());

        URI eventUri = new URI((String) registry.getObject(ControlServer.EVENT_CONNECT_URI_PATH));
        EventClientFactory ecf = (EventClientFactory) EVENT_FACTORY_FINDER.newInstance(eventUri.getScheme());
        EventClient eventClient = ecf.createEventClient(eventUri.toString());

        Distributor ret = new Distributor();
        ret.setDistributorUrl(registryUri);
        ret.setExporter(exporter);
        ret.setRegistry(registry);
        ret.setEventClient(eventClient);

        ret.start();
        ret.log.info("Created: " + ret);
        return ret;
    }

    private IExporter exporter;
    private Registry registry;
    private EventClient eventClient;
    private String distributorUrl;

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
        for (DistributionRef<?> ref : distributed.values()) {
            ref.unregister();
        }
        registry.destroy();
        eventClient.close();
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

    public EventClient getEventClient() {
        return eventClient;
    }

    public void setEventClient(EventClient eventClient) {
        this.eventClient = eventClient;
    }

    public void setDistributorUrl(String distributorUrl) {
        this.distributorUrl = distributorUrl;
    }

    public String getDistributorUrl() {
        return distributorUrl;
    }

    public String toString() {
        return "Distributor [exporter: " + exporter + " registry: " + registry + "]";
    }
}
