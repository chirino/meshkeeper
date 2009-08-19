/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.cloudlaunch.distribution.rmi;

import java.util.LinkedHashSet;
import java.util.Set;

import org.fusesource.cloudlaunch.distribution.Distributable;

/**
 * AbstractExporter
 * <p>
 * Description:
 * </p>
 * 
 * @author cmacnaug
 * @version 1.0
 */
public abstract class AbstractExporter implements IExporter {

    protected abstract <T> T export(Object obj, Class<?>[] interfaces) throws Exception;

    @SuppressWarnings("unchecked")
    public <T extends Distributable> T export(Distributable obj) throws Exception {
        LinkedHashSet<Class<?>> interfaces = new LinkedHashSet<Class<?>>();
        collectDistributableInterfaces(obj.getClass(), interfaces);
        if(interfaces.size() == 0 || (interfaces.size() == 1 && interfaces.contains(Distributable.class)))
        {
            return (T) export(obj, null);
        }
                
        Class<?>[] distributable = null;
        if (interfaces.size() > 0) {
            //System.out.println("Found distributable interfaces for: " + obj + ": " + interfaces);
            distributable = new Class<?>[interfaces.size()];
            interfaces.toArray(distributable);
        }
        return (T) export(obj, distributable);
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
