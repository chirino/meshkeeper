/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.meshkeeper.classloader;

import org.fusesource.meshkeeper.distribution.PluginClassLoader;
import org.fusesource.meshkeeper.util.internal.ClassLoadingAwareObjectInputStream;

import java.io.*;

/**
 * Marshalled allows you to marshall an object and send it to another JVM
 * with a class loader factory so that it can be subsequently unmarshalled
 * via remote class loading.
 *  
 * @author chirino
 */
public class Marshalled<T> implements Serializable {

    private static final long serialVersionUID = 1L;
    private final ClassLoaderFactory classLoaderFactory;
    private final byte[] serializedObject;

    public Marshalled(ClassLoaderFactory classLoaderFactory, T value) throws IOException {
        this.classLoaderFactory = classLoaderFactory;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(baos);
        os.writeObject(value);
        os.close();
        this.serializedObject = baos.toByteArray();
    }

    public ClassLoaderFactory getClassLoaderFactory() {
        return classLoaderFactory;
    }

    @SuppressWarnings("unchecked")
    public T get(final ClassLoader cl) throws IOException, ClassNotFoundException {
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(cl);
        try {
            final ClassLoader[]loaders = new ClassLoader[]{cl, PluginClassLoader.getDefaultPluginLoader()};
            ByteArrayInputStream bais = new ByteArrayInputStream(serializedObject);
            ClassLoadingAwareObjectInputStream is = new ClassLoadingAwareObjectInputStream(bais) {
                @Override
                protected ClassLoader[] getClassLoaders() {
                    return loaders;
                }
            };
            return (T) is.readObject();
        } finally {
            Thread.currentThread().setContextClassLoader(original);
        }
    }

}