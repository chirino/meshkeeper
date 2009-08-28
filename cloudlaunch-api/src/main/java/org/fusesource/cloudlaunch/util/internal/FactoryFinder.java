/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.cloudlaunch.util.internal;

import org.fusesource.mop.MOP;
import org.fusesource.mop.MOPRepository;
import org.fusesource.mop.support.ArtifactId;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.net.URLClassLoader;

public class FactoryFinder {

    private final String path;
    private final ConcurrentHashMap<String, Class> classMap = new ConcurrentHashMap<String, Class>();

    public FactoryFinder(String path) {
        this.path = path;
    }

    /**
     * Creates a new instance of the the class associated with the specified key
     * 
     * @param key is the key to add to the path to find a text file containing the factory class name
     * @return a newly created instance
     */
    public <T> T create(String key) throws IllegalAccessException, InstantiationException, IOException, ClassNotFoundException {
        Class<T> clazz = find(key);
        return clazz.newInstance();
    }

    /**
     * Creates a new instance of the the class associated with the specified key
     * 
     * @param key is the key to add to the path to find a text file containing the factory class name
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

    private Class loadClass(String key) throws ClassNotFoundException, IOException {

        ClassLoader childCL=null;
        ClassLoader systemCL = Thread.currentThread().getContextClassLoader();
        if ( systemCL == null ) {
            systemCL = FactoryFinder.class.getClassLoader();
        }

        String uri = path + key;

        Properties properties = loadProperties(uri, systemCL);
        if( properties == null ) {
            // Not found in the system class loaders.. lets try to dynamically
            // load the module via mop.
            childCL = getPluginClassLoader(systemCL, key);
            if( childCL!=null ) {
                properties = loadProperties(uri, childCL);
            }
        }

        if( properties==null ) {
            throw new IOException("Could not find factory properties: " + uri);
        }

        String className = properties.getProperty("class");
        if (className == null) {
            throw new IOException("Expected property is missing: class");
        }
        
        // First try to load it from the system classloader....
        try {
            return systemCL.loadClass(className);
        } catch (ClassNotFoundException e) {
        }
        
        // Looks like we need to try to use a dynamic one...
        
        // The properties file can point us to the artifact to use...
        String mavenArtifact = properties.getProperty("maven.artifact");
        if( mavenArtifact!=null ) {
            childCL = getArtifactClassLoader(systemCL, mavenArtifact);
        }

        if ( childCL == null ) {
            throw new ClassNotFoundException(className);
        }

        // Now  try to load it from the child classloader....
        return childCL.loadClass(className);
    }

    private Properties loadProperties(String uri, ClassLoader loaders) throws IOException {
        InputStream in = loaders.getResourceAsStream(uri);
        if (in == null) {
            return null;
        }

        // lets load the file
        BufferedInputStream reader = new BufferedInputStream(in);
        try {
            Properties properties = new Properties();
            properties.load(reader);
            return properties;
        } finally {
            try {
                reader.close();
            } catch (Exception e) {
            }
        }
    }

    private ClassLoader getPluginClassLoader(ClassLoader parent, String key) {
        return getArtifactClassLoader(parent, "org.fusesource.cloudlaunch:cloudlaunch-" + key + "-plugin");
    }

    // Use a WeakHashMap so that the classes can get GCed..
    private static WeakHashMap<ClassLoader, String> CLASSLOADER_CACHE = new WeakHashMap<ClassLoader, String>();

    static synchronized private ClassLoader getArtifactClassLoader(ClassLoader parent, String mavenArtifact) {
        try {
            // Perhaps we already have class loader for it...
            for (Map.Entry<ClassLoader, String> entry : CLASSLOADER_CACHE.entrySet()) {
                // Yeah to bad the weak reference is the key.. not the value.
                if( mavenArtifact.equals(entry.getValue()) ) {
                    return entry.getKey();
                }
            }

            MOPRepository repo = new MOPRepository();
            repo.setOnline(true);
            URLClassLoader cl = repo.createArtifactClassLoader(parent, ArtifactId.parse(mavenArtifact));

            CLASSLOADER_CACHE.put(cl, mavenArtifact);
            return cl;
        } catch (Exception e) {
            return null;
        }
    }


}
