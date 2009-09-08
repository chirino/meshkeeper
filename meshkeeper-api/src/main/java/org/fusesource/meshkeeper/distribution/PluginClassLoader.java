/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.meshkeeper.distribution;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.fusesource.meshkeeper.classloader.ClassLoaderServer;
import org.fusesource.meshkeeper.control.ControlService;
import org.fusesource.meshkeeper.distribution.event.EventClient;
import org.fusesource.meshkeeper.distribution.registry.RegistryClient;
import org.fusesource.meshkeeper.distribution.remoting.RemotingClient;
import org.fusesource.meshkeeper.distribution.resource.ResourceManager;

/**
 * PluginClassLoader
 * <p>
 * Description:
 * </p>
 * 
 * @author cmacnaug
 * @version 1.0
 */
public class PluginClassLoader extends URLClassLoader {

    private static final Log LOG = LogFactory.getLog(PluginClassLoader.class);

    private static final HashSet<String> SPI_PACKAGES = new HashSet<String>();
    private static final HashSet<String> PARENT_FIRST = new HashSet<String>();
    private final String DEFAULT_PLUGIN_VERSION = getDefaultPluginVersion();

    private static final boolean USE_PARENT_FIRST = false;
    private static PluginResolver PLUGIN_RESOLVER;

    static {
        SPI_PACKAGES.add(DefaultDistributor.class.getPackage().getName());
        SPI_PACKAGES.add(RemotingClient.class.getPackage().getName());
        SPI_PACKAGES.add(ResourceManager.class.getPackage().getName());
        SPI_PACKAGES.add(RegistryClient.class.getPackage().getName());
        SPI_PACKAGES.add(EventClient.class.getPackage().getName());
        SPI_PACKAGES.add(ControlService.class.getPackage().getName());
        SPI_PACKAGES.add(ClassLoaderServer.class.getPackage().getName());

        PARENT_FIRST.add(LogFactory.class.getPackage().getName());
        PARENT_FIRST.add("org.apache.log4j");
        PARENT_FIRST.add("org.apache.maven");
        PARENT_FIRST.add("javax.jms");
    }

    private static final PluginClassLoader DEFAULT_PLUGIN_CLASSLOADER = new PluginClassLoader(Thread.currentThread().getContextClassLoader());

    private final HashSet<String> resolvedPlugins = new HashSet<String>();
    private final HashSet<String> resolvedFiles = new HashSet<String>();

    /**
     * @return Returns the default plugin classloader.
     */
    public static final PluginClassLoader getDefaultPluginLoader() {
        return DEFAULT_PLUGIN_CLASSLOADER;
    }

    /**
     * If the current context class loader is a plugin class loader it is
     * returned otherwise the default plugin classloader is returned
     * 
     * @return
     */
    public static final PluginClassLoader getContextPluginLoader() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl instanceof PluginClassLoader) {
            return (PluginClassLoader) cl;
        } else {
            return getDefaultPluginLoader();
        }
    }

    PluginClassLoader(ClassLoader parent) {
        super(new URL[] {}, parent);
        //        delegate = new PluginCL(this.getParent());
    }

    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class<?> c = findLoadedClass(name);
        if (c == null) {

            int pDelim = name.lastIndexOf(".");
            if (USE_PARENT_FIRST || (pDelim > 0 && SPI_PACKAGES.contains(name.substring(0, pDelim)))) {
                c = super.loadClass(name, resolve);
            } else {
                for (String prefix : PARENT_FIRST) {
                    if (name.startsWith(prefix)) {
                        try {
                            c = super.loadClass(name, resolve);
                        } catch (ClassNotFoundException cnfe) {
                        }
                    }
                }

                if (c == null) {
                    try {
                        c = findClass(name);
                    } catch (ClassNotFoundException cnfe) {
                        c = super.loadClass(name, resolve);
                    }
                }
            }
        }

        if (c == null) {
            throw new ClassNotFoundException(name);
        }

        if (resolve) {
            resolveClass(c);
        }
        return c;
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {

        Class<?> c = super.findClass(name);
        if (LOG.isDebugEnabled()) {
            LOG.debug("PluginCL-" + this.hashCode() + " Found class: " + name);
            //" from: " + c.getProtectionDomain().getCodeSource().getLocation());
        }
        return c;
    }

    @Override
    public URL getResource(String name) {
        URL url = findResource(name);

        // if local search failed, delegate to parent
        if (url == null) {
            url = getParent().getResource(name);
        }
        return url;
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        Enumeration<URL> results = null;
        try {
            results = super.findResources(name);
        } catch (IOException ioe) {
        }

        // if local search failed, delegate to parent
        if (results == null || !results.hasMoreElements()) {
            results = getParent().getResources(name);
        }
        return results;
    }

    /**
     * Finds the resource with the specified name on the URL search path.
     * 
     * @param name
     *            the name of the resource
     * @return a <code>URL</code> for the resource, or <code>null</code> if the
     *         resource could not be found.
     */
    @Override
    public URL findResource(final String name) {
        URL url = super.findResource(name);
        if (LOG.isDebugEnabled()) {

            if (url == null) {
                LOG.debug("Couldn't find resource " + name + " in path: " + Arrays.toString(super.getURLs()));
            } else {
                LOG.debug("Looking for resource: " + name + " found: " + url);
            }
        }

        return url;
    }

    public void addUrl(URL url) {
        super.addURL(url);
    }

    /**
     * @param key
     */
    public Class<?> loadPlugin(String path, String key) throws IOException, ClassNotFoundException {

        String uri = path + key;

        try {
            //Possible that the plugin properties are already on the classpath
            //try loading there first: 
            Properties properties = loadProperties(this, uri);
            if (properties == null) {
                loadPlugin(key);
                properties = loadProperties(this, uri);
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

    private static Properties loadProperties(ClassLoader cl, String uri) throws IOException {
        InputStream in = cl.getResourceAsStream(uri);
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

    public void loadArtifact(String artifactId) throws IOException, Exception {
        if (resolvedPlugins.contains(artifactId)) {
            return;
        }

        List<File> resolved = getPluginResolver().resolvePlugin(artifactId);

        for (File f : resolved) {
            if (resolvedFiles.add(f.getCanonicalPath())) {
                LOG.debug("Adding plugin dependency: " + f.getCanonicalPath());
                addUrl(f.toURL());
            }
        }
        LOG.info("Resolved plugin: " + artifactId);
        resolvedPlugins.add(artifactId);
    }

    private void loadPlugin(String key) throws IOException, Exception {
        // The plugin version can be configured via a system prop
        String version = System.getProperty(PluginResolver.KEY_PLUGIN_VERSION_PREFIX + key, DEFAULT_PLUGIN_VERSION);
        loadArtifact(PluginResolver.PROJECT_GROUP_ID + ":meshkeeper-" + key + "-plugin:" + version);
    }

    private static String getDefaultPluginVersion() {
        String rc = System.getProperty(PluginResolver.KEY_DEFAULT_PLUGINS_VERSION);
        if (rc != null) {
            return rc;
        }

        return getModuleVersion();
    }

    static public String getModuleVersion() {
        String pomProps = "META-INF/maven/" + PluginResolver.PROJECT_GROUP_ID + "/" + PluginResolver.PROJECT_ARTIFACT_ID + "/pom.properties";

        final String DEFAULT_VERSION = "LATEST";
        try {
            Properties p = loadProperties(PluginClassLoader.class.getClassLoader(), pomProps);
            if (p != null) {
                return p.getProperty("version", DEFAULT_VERSION);
            }
        } catch (Exception e) {
        }
        LOG.warn("Unable to locate '" + pomProps + "' to determine plugin versions using default: " + DEFAULT_VERSION);
        return DEFAULT_VERSION;
    }

    public static synchronized PluginResolver getPluginResolver() {

        if (PLUGIN_RESOLVER == null) {
            PLUGIN_RESOLVER = new MopPluginResolver();
            PLUGIN_RESOLVER.setDefaultPluginVersion(getDefaultPluginVersion());
        }

//        if (PLUGIN_RESOLVER == null) {
//            ClassLoader loader = PluginClassLoader.DEFAULT_PLUGIN_CLASSLOADER;
//            try {
//                URL url = PluginClassLoader.class.getClassLoader().getResource("mop-core-1.0-SNAPSHOT.jar");
//                if (url != null) {
//
//                    if (url.getProtocol().equals("jar")) {
//                        InputStream jaris = PluginClassLoader.class.getClassLoader().getResourceAsStream("mop-core-1.0-SNAPSHOT.jar");
//                        loader = new JarClassLoader(jaris, ClassLoader.getSystemClassLoader());
//                    } else {
//                        loader = new URLClassLoader(new URL[] { url });
//                    }
//                } else {
//                    LOG.warn("mop-core-1.0-SNAPSHOT.jar was not found on the classpath");
//                }
//
//                PLUGIN_RESOLVER = (PluginResolver) loader.loadClass("org.fusesource.meshkeeper.distribution.MopPluginResolver").newInstance();
//
//            } catch (Throwable thrown) {
//                LOG.error("Error loading plugin resolver:" + thrown.getMessage(), thrown);
//                throw new RuntimeException(thrown);
//            }
//
//        }
        return PLUGIN_RESOLVER;
    }

    private static class JarClassLoader extends ClassLoader {
        HashMap<String, byte[]> classBytes = new HashMap<String, byte[]>();
        private final ReferenceQueue<URL> cleanupQueue = new ReferenceQueue<URL>();
        private final AtomicInteger cleanupUrls = new AtomicInteger();

        JarClassLoader(InputStream jaris, ClassLoader parent) throws IOException {
            super(parent);
            JarInputStream jis = new JarInputStream(jaris);
            JarEntry je = jis.getNextJarEntry();
            while (je != null) {
                
                if (!je.isDirectory()) {
                    String name = je.getName();
                    if (je.getName().endsWith(".class")) {
                        name = name.substring(0, name.length() - 6);
                        name = name.replaceAll("\\/", ".");
                    }
                    
                    byte[] bytes = new byte [] {};
                    if (je.getSize() > 0) {
                        bytes = new byte[(int) je.getSize()];
                        int i = 0;
                        while (i < bytes.length) {
                            int read = jis.read(bytes, i, bytes.length - 1);
                            if (read > 0) {
                                i += read;
                            }
                        }
                    }
                    //Unknown length;
                    else if (je.getSize() < 0)
                    {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
                        while(jis.available() > 0)
                        {
                            byte [] chunk = new byte [1024];
                            int read = jis.read(chunk);
                            if(read > 0)
                            {
                                baos.write(chunk, 0, read);
                            }
                        }
                        bytes = baos.toByteArray();
                    }

                    //LOG.debug("Read jar entry for " + name + " size " + bytes.length);
                    
                    classBytes.put(name, bytes);
                }
                jis.closeEntry();
                je = jis.getNextJarEntry();
            }

            jis.close();
        }

        protected Class<?> findClass(String name) throws ClassNotFoundException {
            
            LOG.debug("Looking for class " + name);
            byte[] b = classBytes.get(name);
            if (b == null) {
                throw new ClassNotFoundException(name);
            }
            LOG.debug("Defining class " + name);
            return defineClass(name, b, 0, b.length);
        }

        protected URL findResource(String name) {

            byte[] b = classBytes.get(name);
            if (b == null) {
                return null;
            }
            try {
                File f = File.createTempFile(UUID.randomUUID().toString(), name);

                f.deleteOnExit();
                FileOutputStream fos = new FileOutputStream(f);
                fos.write(b);
                fos.flush();
                fos.close();

                URL ret = f.toURL();
                final String cleanupPath = f.getCanonicalPath();
                new PhantomReference<URL>(ret, cleanupQueue) {
                    String toDelete = cleanupPath;

                    public void clear() {
                        super.clear();
                        try {
                            LOG.info("DELETING: " + toDelete);
                            new File(toDelete).delete();
                        } catch (Throwable thrown) {
                        }
                    }
                };

                if (cleanupUrls.incrementAndGet() == 1) {
                    scheduleCleanup();
                }

                return ret;

            } catch (IOException ioe) {
                LOG.warn("Error creating resource temp file for" + name, ioe);
                return null;
            }

        }

        public void cleanupTempFiles() {
            Reference<?> ref = cleanupQueue.poll();
            while (ref != null) {
                ref.clear();
                cleanupUrls.decrementAndGet();
            }

            if (cleanupUrls.get() > 0) {
                scheduleCleanup();
            }
        }

        private void scheduleCleanup() {
            DistributorFactory.getExecutorService().schedule(new Runnable() {
                public void run() {
                    cleanupTempFiles();
                }
            }, 1, TimeUnit.SECONDS);
        }

        public Enumeration<URL> findResources(String name) {
            URL u = findResource(name);
            if (u == null) {
                return null;
            }
            Vector<URL> r = new Vector<URL>(1);
            r.add(u);
            return r.elements();
        }

    }

}
