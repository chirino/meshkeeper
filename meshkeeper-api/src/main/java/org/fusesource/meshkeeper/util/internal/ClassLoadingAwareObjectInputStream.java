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
package org.fusesource.meshkeeper.util.internal;

import java.io.ObjectInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.ObjectStreamClass;
import java.util.HashMap;
import java.lang.reflect.Proxy;

public class ClassLoadingAwareObjectInputStream extends ObjectInputStream {

    private static final ClassLoader FALLBACK_CLASS_LOADER = ClassLoadingAwareObjectInputStream.class.getClassLoader();

    /**
     * <p>Maps primitive type names to corresponding class objects.</p>
     */
    private static final HashMap<String, Class> primClasses = new HashMap<String, Class>(8, 1.0F);

    public ClassLoadingAwareObjectInputStream(InputStream in) throws IOException {
        super(in);
    }

    protected Class resolveClass(ObjectStreamClass classDesc) throws IOException, ClassNotFoundException {
        String s = classDesc.getName();
        return load(s);
    }

    protected Class resolveProxyClass(String[] interfaces) throws IOException, ClassNotFoundException {
        Class[] cinterfaces = new Class[interfaces.length];
        for (int i = 0; i < interfaces.length; i++) {
            cinterfaces[i] = load(interfaces[i]);
        }
        try {
            return Proxy.getProxyClass(cinterfaces[0].getClassLoader(), cinterfaces);
        } catch (IllegalArgumentException e) {
            throw new ClassNotFoundException(null, e);
        }
    }

    protected ClassLoader[] getClassLoaders() {
        return new ClassLoader[]{Thread.currentThread().getContextClassLoader()};
    }

    private Class load(String s) throws ClassNotFoundException {
        ClassLoader[] cls = getClassLoaders();
        ClassNotFoundException error = null;
        for (ClassLoader cl : cls) {
            try {
                return Class.forName(s, false, cl);
            } catch (ClassNotFoundException e) {
                error = e;
            }
        }
        final Class clazz = (Class) primClasses.get(s);
        if (clazz != null) {
            return clazz;
        } else {
            return Class.forName(s, false, FALLBACK_CLASS_LOADER);
        }
    }

    static {
        primClasses.put("boolean", boolean.class);
        primClasses.put("byte", byte.class);
        primClasses.put("char", char.class);
        primClasses.put("short", short.class);
        primClasses.put("int", int.class);
        primClasses.put("long", long.class);
        primClasses.put("float", float.class);
        primClasses.put("double", double.class);
        primClasses.put("void", void.class);
    }

}
