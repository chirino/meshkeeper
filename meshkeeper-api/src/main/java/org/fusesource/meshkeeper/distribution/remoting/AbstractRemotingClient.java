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

    protected abstract <T> T exportInterfaces(T obj, Class<?>[] interfaces) throws Exception;
    
    public final <T> T export(T obj, Class<?>... serviceInterfaces) throws Exception {
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
            return (T) exportInterfaces(obj, (Class<?> []) null);
        }

        Class<?>[] distributable = null;
        //System.out.println("Found distributable interfaces for: " + obj + ": " + interfaces);
        distributable = new Class<?>[interfaces.size()];
        interfaces.toArray(distributable);
        return (T) exportInterfaces(obj, distributable);
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
