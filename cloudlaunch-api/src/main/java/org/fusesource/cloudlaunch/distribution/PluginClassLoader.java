/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.cloudlaunch.distribution;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.maven.artifact.Artifact;
import org.fusesource.cloudlaunch.classloader.ClassLoaderServer;
import org.fusesource.cloudlaunch.control.ControlService;
import org.fusesource.cloudlaunch.distribution.event.EventClient;
import org.fusesource.cloudlaunch.distribution.jms.JMSProvider;
import org.fusesource.cloudlaunch.distribution.registry.Registry;
import org.fusesource.cloudlaunch.distribution.resource.ResourceManager;
import org.fusesource.cloudlaunch.distribution.rmi.IExporter;
import org.fusesource.mop.MOP;
import org.fusesource.mop.MOPRepository;
import org.fusesource.mop.common.base.Predicate;
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
class PluginClassLoader extends URLClassLoader {

    private static Log LOG = LogFactory.getLog(PluginClassLoader.class);
    
    private final HashSet<String> resolvedPlugins = new HashSet<String>();
    private final HashSet<String> resolvedFiles = new HashSet<String>();

    private static String PLUGIN_VERSION;
    private static final HashSet<String> SPI_PACKAGES = new HashSet<String>();
    private static final HashSet<String> PARENT_FIRST = new HashSet<String>();
    private static final boolean USE_PARENT_FIRST = true;

    private static Predicate<Artifact> ARTIFACT_FILTER = null;

    static {
        SPI_PACKAGES.add(Distributor.class.getPackage().getName());
        SPI_PACKAGES.add(IExporter.class.getPackage().getName());
        SPI_PACKAGES.add(ResourceManager.class.getPackage().getName());
        SPI_PACKAGES.add(Registry.class.getPackage().getName());
        SPI_PACKAGES.add(JMSProvider.class.getPackage().getName());
        SPI_PACKAGES.add(EventClient.class.getPackage().getName());
        SPI_PACKAGES.add(ControlService.class.getPackage().getName());
        SPI_PACKAGES.add(ClassLoaderServer.class.getPackage().getName());

        PARENT_FIRST.add(LogFactory.class.getPackage().getName());
        PARENT_FIRST.add("org.apache.log4j");
        PARENT_FIRST.add("org.apache.maven");
        PARENT_FIRST.add("javax.jms");
    }

    private static final PluginClassLoader DEFAULT_PLUGIN_CLASSLOADER = new PluginClassLoader(Thread.currentThread().getContextClassLoader());

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

    public PluginClassLoader(ClassLoader parent) {
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
            Properties properties = loadProperties(uri);
            if (properties == null) {
                loadPlugin(key);
                properties = loadProperties(uri);
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

    private Properties loadProperties(String uri) throws IOException {
        InputStream in = getResourceAsStream(uri);
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

        MOPRepository repo = new MOPRepository();
        repo.setOnline(true);
        repo.setLocalRepo(new File(System.getProperty("user.home", "."), ".mop" + File.separator + "repository"));
        //repo.setAlwaysCheckUserLocalRepo(true);
        
        ArrayList<ArtifactId> artifact = new ArrayList<ArtifactId>(1);
        artifact.add(ArtifactId.parse(mavenArtifact, getPluginVersion(), MOP.DEFAULT_TYPE));

        List<File> resolved = repo.resolveFiles(artifact, getArtifactFilter());
        for (File f : resolved) {
            if (resolvedFiles.add(f.getCanonicalPath())) {
                addUrl(f.toURL());
            }
        }
        resolvedPlugins.add(mavenArtifact);
    }

    private static Predicate<Artifact> getArtifactFilter() {
        if (ARTIFACT_FILTER == null) {
            MOPRepository repo = new MOPRepository();
            repo.setOnline(true);
            repo.setLocalRepo(new File(System.getProperty("user.home", "."), ".mop" + File.separator + "repository"));
            
            Set<Artifact> deps;
            try {
                deps = repo.resolveArtifacts(new ArtifactId[] {ArtifactId.parse("org.fusesource.cloudlaunch:cloudlaunch-api", getPluginVersion(), MOP.DEFAULT_TYPE)});
            } catch (Exception e) {
                return new Predicate<Artifact>() {
                    public boolean apply(Artifact artifact) {
                        return true;
                    }
                };
            }
            final HashSet<String> filters = new HashSet(deps.size());
            for (Artifact a : deps) {
                filters.add(a.getArtifactId());
            }

            System.out.println("Filters: " + filters);

            ARTIFACT_FILTER = new Predicate<Artifact>() {
                public boolean apply(Artifact artifact) {
                    return !filters.contains(artifact.getArtifactId());
                }
            };
        }
        return ARTIFACT_FILTER;
    }

    private static String getPluginVersion() {
        //Try to find the current cloudlaunch version:
        if (PLUGIN_VERSION == null) {
            String defaultVersion = "LATEST";
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
        return PLUGIN_VERSION;
    }
}
