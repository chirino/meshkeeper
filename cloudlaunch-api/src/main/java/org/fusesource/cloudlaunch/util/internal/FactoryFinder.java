/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 * 
 * Based on org.apache.activemq.util.FactoryFinder with below license
 **************************************************************************************/

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
package org.fusesource.cloudlaunch.util.internal;


import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public class FactoryFinder {
    private static final PluginClassLoader DEFAULT_PLUGIN_CLASSLOADER = new PluginClassLoader(Thread.currentThread().getContextClassLoader());
    private final String path;
    private final ConcurrentHashMap<String, Class<?>> classMap = new ConcurrentHashMap<String, Class<?>>();

    public FactoryFinder(String path) {
        this.path = path;
    }

    /**
     * Creates a new instance of the the class associated with the specified key
     * 
     * @param key
     *            is the key to add to the path to find a text file containing
     *            the factory class name
     * @param pLoader
     *            TODO
     * @return a newly created instance
     */
    public <T> T create(String key) throws IllegalAccessException, InstantiationException, IOException, ClassNotFoundException {
        Class<T> clazz = find(key);
        return clazz.newInstance();
    }

    /**
     * Creates a new instance of the the class associated with the specified key
     * 
     * @param key
     *            is the key to add to the path to find a text file containing
     *            the factory class name
     * @return a newly created instance
     */
    public <T> Class<T> find(String key) throws ClassNotFoundException, IOException {
        Class clazz = classMap.get(key);
        if (clazz == null) {
            clazz = loadClass(key);
            classMap.put(key, clazz);
        }
        return clazz;
    }

    private Class<?> loadClass(String key) throws ClassNotFoundException, IOException {

        PluginClassLoader pcl = DEFAULT_PLUGIN_CLASSLOADER;

        ClassLoader systemCL = Thread.currentThread().getContextClassLoader();
        if (systemCL == null) {
            systemCL = FactoryFinder.class.getClassLoader();
        }

        if (systemCL instanceof PluginClassLoader) {
            pcl = (PluginClassLoader) pcl;
        }

        return pcl.loadPlugin(path, key);
        
    }

    
//    private Class loadClass(String key) throws ClassNotFoundException, IOException {
//
//        ClassLoader childCL=null;
//        ClassLoader systemCL = Thread.currentThread().getContextClassLoader();
//        if ( systemCL == null ) {
//            systemCL = FactoryFinder.class.getClassLoader();
//        }
//
//        String uri = path + key;
//
//        Properties properties = loadProperties(uri, systemCL);
//        if( properties == null ) {
//            // Not found in the system class loaders.. lets try to dynamically
//            // load the module via mop.
//            childCL = getPluginClassLoader(systemCL, key);
//            if( childCL!=null ) {
//                properties = loadProperties(uri, childCL);
//            }
//        }
//
//        if( properties==null ) {
//            throw new IOException("Could not find factory properties: " + uri);
//        }
//
//        String className = properties.getProperty("class");
//        if (className == null) {
//            throw new IOException("Expected property is missing: class");
//        }
//        
//        // First try to load it from the system classloader....
//        try {
//            return systemCL.loadClass(className);
//        } catch (ClassNotFoundException e) {
//        }
//        
//        // Looks like we need to try to use a dynamic one...
//        
//        // The properties file can point us to the artifact to use...
//        String mavenArtifact = properties.getProperty("maven.artifact");
//        if( mavenArtifact!=null ) {
//            childCL = getArtifactClassLoader(systemCL, mavenArtifact);
//        }
//
//        if ( childCL == null ) {
//            throw new ClassNotFoundException(className);
//        }
//
//        // Now  try to load it from the child classloader....
//        return childCL.loadClass(className);
//    }

//    private Properties loadProperties(String uri, ClassLoader loaders) throws IOException {
//        InputStream in = loaders.getResourceAsStream(uri);
//        if (in == null) {
//            return null;
//        }
//
//        // lets load the file
//        BufferedInputStream reader = new BufferedInputStream(in);
//        try {
//            Properties properties = new Properties();
//            properties.load(reader);
//            return properties;
//        } finally {
//            try {
//                reader.close();
//            } catch (Exception e) {
//            }
//        }
//    }
//
//    private ClassLoader getPluginClassLoader(ClassLoader parent, String key) {
//        return getArtifactClassLoader(parent, "org.fusesource.cloudlaunch:cloudlaunch-" + key + "-plugin");
//    }
//
//    // Use a WeakHashMap so that the classes can get GCed..
//    private static WeakHashMap<ClassLoader, String> CLASSLOADER_CACHE = new WeakHashMap<ClassLoader, String>();
//
//    static synchronized private ClassLoader getArtifactClassLoader(ClassLoader parent, String mavenArtifact) {
//        try {
//            // Perhaps we already have class loader for it...
//            for (Map.Entry<ClassLoader, String> entry : CLASSLOADER_CACHE.entrySet()) {
//                // Yeah to bad the weak reference is the key.. not the value.
//                if( mavenArtifact.equals(entry.getValue()) ) {
//                    return entry.getKey();
//                }
//            }
//
//            MOPRepository repo = new MOPRepository();
//            repo.setOnline(true);
//            URLClassLoader cl = repo.createArtifactClassLoader(parent, ArtifactId.parse(mavenArtifact));
//
//            CLASSLOADER_CACHE.put(cl, mavenArtifact);
//            return cl;
//        } catch (Exception e) {
//            LOG.warn("Error loading artifact class loader", e);
//            return null;
//        }
//    }


}
