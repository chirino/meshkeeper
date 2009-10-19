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
package org.fusesource.meshkeeper.distribution.remoting;

import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.fusesource.meshkeeper.Distributable;
import org.fusesource.meshkeeper.distribution.AbstractPluginClient;

/**
 * AbstractExporter
 * <p>
 * Description:
 * </p>
 * 
 * @author cmacnaug
 * @version 1.0
 */
public abstract class AbstractRemotingClient extends AbstractPluginClient implements RemotingClient {

    protected static final Log LOG = LogFactory.getLog(AbstractRemotingClient.class);

    /**
     * Subclasses must implement this method. If multicastAddress, is not null,
     * then this implementor must ensure that the object is registered such that
     * multiple registrations at the same address will cause method calls to
     * either returned stub to be invoked on both registered objects.
     * 
     * @param <T>
     * @param obj
     * @param multicastAddress
     * @param interfaces
     * @return
     * @throws Exception
     */
    protected abstract <T> T exportInterfaces(T obj, String multicastAddress, Class<?>[] interfaces) throws Exception;

    public final <T> T export(T obj, Class<?>... serviceInterfaces) throws Exception {
        return exportInternal(obj, null, serviceInterfaces);
    }

    public final <T> T exportMulticast(T obj, String address, Class<?>... serviceInterfaces) throws Exception {
        return exportInternal(obj, address, serviceInterfaces);
    }

    private final <T> T exportInternal(T obj, String multicastAddress, Class<?>... serviceInterfaces) throws Exception {
        LinkedHashSet<Class<?>> interfaces = new LinkedHashSet<Class<?>>();
        if (serviceInterfaces == null || serviceInterfaces.length == 0) {
            collectDistributableInterfaces(obj.getClass(), interfaces);
        } else {
            for (Class<?> serviceInterface : serviceInterfaces) {
                validateInterface(serviceInterface);
                interfaces.add(serviceInterface);
            }
        }

        //If the only interfaces is the Distributable interface itself, then we're
        //just trying to export the class:
        if (interfaces.size() == 0 || (interfaces.size() == 1 && interfaces.contains(Distributable.class))) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Exporting " + obj.getClass() + " with no service interfaces");
            }
            return (T) exportInterfaces(obj, multicastAddress, (Class<?>[]) null);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Exporting " + obj.getClass() + " as: " + interfaces);
        }
        

        Class<?>[] distributable = null;
        //System.out.println("Found distributable interfaces for: " + obj + ": " + interfaces);
        distributable = new Class<?>[interfaces.size()];
        interfaces.toArray(distributable);
        return (T) exportInterfaces(obj, multicastAddress, distributable);
    }

    protected static void validateInterface(Class<?> i) {
        if (!i.isInterface()) {
            throw new IllegalArgumentException("Not an interface: " + i);
        }
    }

    private static void collectDistributableInterfaces(Class<?> clazz, Set<Class<?>> rc) throws Exception {
        for (Class<?> interf : clazz.getInterfaces()) {
            if (Distributable.class.isAssignableFrom(interf)) {
                validateInterface(interf);
                rc.add(interf);
            }
        }

        // Also slowOnewayOperations interfaces in the super classes...
        if (clazz.getSuperclass() != null) {
            collectDistributableInterfaces(clazz.getSuperclass(), rc);
        }
    }
}
