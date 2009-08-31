/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.cloudlaunch.util.internal;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.fusesource.cloudlaunch.distribution.DistributorFactory;
import org.fusesource.mop.MOP;
import org.fusesource.mop.MOPRepository;
import org.fusesource.mop.support.ArtifactId;

/**
 * PluginClassLoader
 * <p>
 * Description:
 * </p>
 * 
 * @author cmacnaug
 * @version 1.0
 */
public class PluginClassLoader extends ClassLoader {

    private static Log LOG = LogFactory.getLog(DistributorFactory.class);

    private final PluginCL delegate;
    private final HashSet<String> resolvedPlugins = new HashSet<String>();
    private final HashSet<String> resolvedFiles = new HashSet<String>();

    private static String PLUGIN_VERSION;

    public PluginClassLoader(ClassLoader parent) {
        super(parent);
        delegate = new PluginCL(this.getParent());
    }

    public Class<?> loadClass(String className) throws ClassNotFoundException {
        return findClass(className);
    }

    public Class<?> findClass(String className) throws ClassNotFoundException {
        return delegate.loadClass(className);
    }

    /**
     * @param key
     */
    public Class<?> loadPlugin(String path, String key) throws IOException, ClassNotFoundException {

        String uri = path + key;

        try {
            Properties properties = loadProperties(uri, delegate);
            if (properties == null) {
                loadPlugin(key);
                properties = loadProperties(uri, delegate);
            }

            if (properties == null) {
                throw new IOException("Could not find factory properties: " + uri);
            }

            String className = properties.getProperty("class");
            if (className == null) {
                throw new IOException("Expected property is missing: class");
            }

            // The properties file can point us to the artifact to use...
            String mavenArtifact = properties.getProperty("maven.artifact");
            if (mavenArtifact != null) {
                loadArtifact(mavenArtifact);
            }

            //See if we can load it (either from system or the recently
            //loaded plugin classloader:
            return loadClass(className);

        } catch (IOException ioe) {
            throw ioe;
        } catch (ClassNotFoundException cnfe) {
            throw cnfe;
        } catch (Exception e) {
            IOException ioe = new IOException();
            ioe.initCause(e);
            throw ioe;
        }

    }

    private static Properties loadProperties(String uri, ClassLoader loaders) throws IOException {
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

    private void loadPlugin(String key) throws IOException, Exception {
        loadArtifact("org.fusesource.cloudlaunch:cloudlaunch-" + key + "-plugin");
    }

    synchronized private void loadArtifact(String mavenArtifact) throws Exception {

        if (resolvedPlugins.contains(mavenArtifact)) {
            return;
        }

        //Try to find the current cloudlaunch version:
        if (PLUGIN_VERSION == null) {
            String defaultVersion = "1.0-SNAPSHOT";
            Properties p = new Properties();
            try {
                p.load(FactoryFinder.class.getResourceAsStream("META-INF/maven/org.fusesource.cloudlaunch/cloudlaunch-api/pom.properties"));
                PLUGIN_VERSION = p.getProperty("version", defaultVersion);
            } catch (Exception e) {
                LOG.warn("Unable to locate cloudlaunch-api/pom.properties to determine plugin versions using default: " + defaultVersion);
                PLUGIN_VERSION = defaultVersion;
            }

            LOG.trace("Plugin version is: " + PLUGIN_VERSION);
        }

        MOPRepository repo = new MOPRepository();
        repo.setOnline(true);
        repo.setLocalRepo(new File(System.getProperty("user.home", "."), ".mop" + File.separator + "repository"));
        repo.setAlwaysCheckUserLocalRepo(true);
        List<File> artifacts = repo.resolveFiles(ArtifactId.parse(mavenArtifact, PLUGIN_VERSION, MOP.DEFAULT_TYPE));
        for (File f : artifacts) {
            if (!resolvedFiles.contains(f.getCanonicalPath())) {
                delegate.addUrl(f.toURL());
            }
        }
        resolvedPlugins.add(mavenArtifact);
    }

    private class PluginCL extends URLClassLoader {

        /**
         * @param urls
         */
        public PluginCL(ClassLoader parent) {
            super(new URL[] {}, parent);
        }

        public Class<?> findClass(String name) throws ClassNotFoundException {
            Class<?> c = super.findClass(name);
            if (PluginClassLoader.this.LOG.isDebugEnabled()) {
                PluginClassLoader.this.LOG.debug("PluginCL-" + this.hashCode() + " Found class: " + name);
            }
            return c;
        }

        public void addUrl(URL url) {
            super.addURL(url);
        }
    }
}
