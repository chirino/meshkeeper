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
package org.fusesource.cloudlaunch.rmi;

import java.io.Serializable;
import java.util.HashMap;

import org.fusesource.cloudlaunch.registry.Registry;

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

    private IExporter exporter;
    private Registry registry;

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

    public final <T extends Distributable> DistributionRef<T> export(Distributable object) throws Exception {
        DistributionRef<T> ref = getRef(object, true);
        ref.export();
        return ref;
    }

    public final void unexport(Distributable object) throws Exception {
        DistributionRef<?> ref = getRef(object, false);
        if (ref != null) {
            ref.unregister();
            synchronized (distributed) {
                distributed.remove(object);
            }
        }
    }

    public final <T extends Distributable> DistributionRef<T> register(Distributable object, String path, boolean sequential) throws Exception {
        DistributionRef<T> ref = getRef(object, true);
        ref.register(path, sequential);
        return ref;
    }

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
        for(DistributionRef<?> ref : distributed.values())
        {
            ref.unregister();
        }
        registry.close();
    }

    public synchronized void setExporter(IExporter exporter) {
        if (this.exporter == null) {
            this.exporter = exporter;
        }
    }

    public IExporter getExporter() {
        return exporter;
    }

    public synchronized void setRegistry(Registry registry) {
        if (this.registry == null) {
            this.registry = registry;
        }
    }

    public synchronized Registry getRegistry() {
        return registry;
    }
    
    public String toString()
    {
        return "Distributor [exporter: " + exporter + " registry: " + registry + "]";
    }
}
