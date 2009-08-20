/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.cloudlaunch.distribution;

import java.io.File;
import java.net.URI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.fusesource.cloudlaunch.control.ControlServer;
import org.fusesource.cloudlaunch.distribution.event.EventClient;
import org.fusesource.cloudlaunch.distribution.event.EventClientFactory;
import org.fusesource.cloudlaunch.distribution.registry.Registry;
import org.fusesource.cloudlaunch.distribution.registry.RegistryFactory;
import org.fusesource.cloudlaunch.distribution.resource.ResourceManager;
import org.fusesource.cloudlaunch.distribution.resource.ResourceManagerFactory;
import org.fusesource.cloudlaunch.distribution.rmi.ExporterFactory;
import org.fusesource.cloudlaunch.distribution.rmi.IExporter;
import org.fusesource.cloudlaunch.util.internal.FactoryFinder;

/**
 * DistributorFactory
 * <p>
 * Description:
 * </p>
 * 
 * @author cmacnaug
 * @version 1.0
 */
public class DistributorFactory {

    private Log log = LogFactory.getLog(DistributorFactory.class);
    private static final FactoryFinder REGISTRY_FACTORY_FINDER = new FactoryFinder("META-INF/services/org/fusesource/cloudlaunch/distribution/registry/");
    private static final FactoryFinder EXPORTER_FACTORY_FINDER = new FactoryFinder("META-INF/services/org/fusesource/cloudlaunch/distribution/exporter/");
    private static final FactoryFinder EVENT_FACTORY_FINDER = new FactoryFinder("META-INF/services/org/fusesource/cloudlaunch/distribution/event/");
    private static final FactoryFinder RESOURCE_FACTORY_FINDER = new FactoryFinder("META-INF/services/org/fusesource/cloudlaunch/distribution/resource/");

    private static String DEFAULT_RESOURCE_MANAGER_PROVIDER = "wagon";
    private static String DEFAULT_DATA_DIR = ".";
    private static String DEFAULT_REGISTRY_URI = ControlServer.DEFAULT_REGISTRY_URI;
    static {
        String repoDir = null;
        try {
            repoDir = System.getProperty("user.home");
        } catch (SecurityException se) {
        }
        DEFAULT_DATA_DIR = repoDir;
    }

    private String resourceManagerProvider = DEFAULT_RESOURCE_MANAGER_PROVIDER;
    private String registryProviderUri = DEFAULT_REGISTRY_URI;
    private String dataDirectory = DEFAULT_DATA_DIR;
    private String eventProviderUri;
    private String rmiProviderUri;
    private String commonRepoUrl;

    /**
     * This convenience method creates a Distributor by connecting to a control
     * server registry and pulls down
     * 
     * @param registryProviderUri
     *            The provider uri for a control server registry.
     * @return
     */
    public static Distributor createDefaultDistributor() throws Exception {
        DistributorFactory df = new DistributorFactory();
        return df.create();
    }

    public static void setDefaultDataDirectory(String dataDirectory) {
        DEFAULT_DATA_DIR = dataDirectory;
    }
    
    public static void setDefaultRegistryUri(String defaultRegistryUri) {
        DEFAULT_REGISTRY_URI = defaultRegistryUri;
    }

    public Distributor create() throws Exception {

        //Create Registry:
        URI registryUri = new URI(registryProviderUri);
        RegistryFactory rf = (RegistryFactory) REGISTRY_FACTORY_FINDER.newInstance(registryUri.getScheme());
        Registry registry = rf.createRegistry(registryUri.toString());
        registry.start();

        //Create Exporter:
        if (rmiProviderUri == null) {
            rmiProviderUri = registry.getObject(ControlServer.EXPORTER_CONNECT_URI_PATH);
            if (rmiProviderUri == null) {
                rmiProviderUri = ControlServer.DEFAULT_RMI_URI;
            }
        }
        URI rmiUri = new URI(rmiProviderUri);
        ExporterFactory ef = (ExporterFactory) EXPORTER_FACTORY_FINDER.newInstance(rmiUri.getScheme());
        IExporter exporter = ef.createExporter(rmiUri.toString());

        //Create Event Client:
        if (eventProviderUri == null) {
            eventProviderUri = registry.getObject(ControlServer.EVENT_CONNECT_URI_PATH);
            if (eventProviderUri == null) {
                eventProviderUri = ControlServer.DEFAULT_EVENT_URI;
            }
        }
        URI eventUri = new URI(eventProviderUri);
        EventClientFactory ecf = (EventClientFactory) EVENT_FACTORY_FINDER.newInstance(eventUri.getScheme());
        EventClient eventClient = ecf.createEventClient(eventUri.toString());

        //Create ResourceManager:
        ResourceManagerFactory rmf = (ResourceManagerFactory) RESOURCE_FACTORY_FINDER.newInstance(resourceManagerProvider);
        String commonRepoUrl = registry.getObject(ControlServer.COMMON_REPO_URL_PATH);
        if (commonRepoUrl != null) {
            rmf.setCommonRepoUrl(commonRepoUrl);
        }
        rmf.setLocalRepoDir(dataDirectory + File.separator + "local-repo");
        ResourceManager resourceManager = rmf.createResourceManager();

        Distributor ret = new Distributor();
        ret.setExporter(exporter);
        ret.setRegistry(registry);
        ret.setEventClient(eventClient);
        ret.setResourceManager(resourceManager);
        ret.setRegistryUri(registryProviderUri);

        ret.start();
        if (log.isTraceEnabled()) {
            log.trace("Created: " + ret);
        }
        return ret;
    }

    public String getResourceManagerProvider() {
        return resourceManagerProvider;
    }

    public void setResourceManagerProvider(String resourceManagerProvider) {
        this.resourceManagerProvider = resourceManagerProvider;
    }

    public String getRegistryProviderUri() {
        return registryProviderUri;
    }

    public void setRegistryProviderUri(String registryProviderUri) {
        this.registryProviderUri = registryProviderUri;
    }

    public String getEventProviderUri() {
        return eventProviderUri;
    }

    public void setEventProviderUri(String eventProviderUri) {
        this.eventProviderUri = eventProviderUri;
    }

    public String getRmiProviderUri() {
        return rmiProviderUri;
    }

    public void setRmiProviderUri(String rmiProviderUri) {
        this.rmiProviderUri = rmiProviderUri;
    }

    public String getCommonRepoUrl() {
        return commonRepoUrl;
    }

    public void setCommonRepoUrl(String commonRepoUrl) {
        this.commonRepoUrl = commonRepoUrl;
    }

    public String getDataDirectory() {
        return dataDirectory;
    }

    public void setDataDirectory(String dataDirectory) {
        this.dataDirectory = dataDirectory;
    }
}
