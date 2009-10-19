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