/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.meshkeeper.distribution.remoting;

import java.util.LinkedHashSet;
import java.util.Set;

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
            return (T) exportInterfaces(obj, multicastAddress, (Class<?>[]) null);
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
