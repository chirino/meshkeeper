/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.cloudlaunch.distribution;

import java.io.File;

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

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

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

    private static String DEFAULT_RESOURCE_MANAGER_PROVIDER_URI = "wagon";
    private static String DEFAULT_DATA_DIR = ".";
    private static String DEFAULT_REGISTRY_URI = ControlServer.DEFAULT_REGISTRY_URI;
    private static final ScheduledExecutorService EXECUTOR;
    private static final AtomicInteger EXECUTOR_COUNT = new AtomicInteger(0);
    static {
        String repoDir = null;
        try {
            repoDir = System.getProperty("user.home");
        } catch (SecurityException se) {
        }
        DEFAULT_DATA_DIR = repoDir;

        EXECUTOR = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors(), new ThreadFactory() {

            public Thread newThread(Runnable r) {
                return new Thread(r, "CloudLaunchExecutor-" + EXECUTOR_COUNT.incrementAndGet());
            }
        });
    }

    private String resourceManagerProvider = DEFAULT_RESOURCE_MANAGER_PROVIDER_URI;
    private String registryProviderUri = DEFAULT_REGISTRY_URI;
    private String dataDirectory = DEFAULT_DATA_DIR;
    private String eventProviderUri;
    private String rmiProviderUri;
    private String commonRepoUrl;

    /**
     * This convenience method creates a Distributor by connecting to a control
     * server registry and pulls down
     * 
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

    public static ScheduledExecutorService getExecutorService() {
        return EXECUTOR;
    }

    public Distributor create() throws Exception {

        //Create Registry:
        Registry registry = new RegistryFactory().create(registryProviderUri);
        registry.start();

        //Create Exporter:
        if (rmiProviderUri == null) {
            rmiProviderUri = registry.getObject(ControlServer.EXPORTER_CONNECT_URI_PATH);
            if (rmiProviderUri == null) {
                rmiProviderUri = ControlServer.DEFAULT_RMI_URI;
            }
        }
        IExporter exporter = new ExporterFactory().create(rmiProviderUri);

        //Create Event Client:
        if (eventProviderUri == null) {
            eventProviderUri = registry.getObject(ControlServer.EVENT_CONNECT_URI_PATH);
            if (eventProviderUri == null) {
                eventProviderUri = ControlServer.DEFAULT_EVENT_URI;
            }
        }
        EventClient eventClient = new EventClientFactory().create(eventProviderUri);

        //Create ResourceManager:
        ResourceManager resourceManager = new ResourceManagerFactory().create(resourceManagerProvider);
        String commonRepoUrl = registry.getObject(ControlServer.COMMON_REPO_URL_PATH);
        if (commonRepoUrl != null) {
            resourceManager.setCommonRepoUrl(commonRepoUrl, null);
        }
        resourceManager.setLocalRepoDir(dataDirectory + File.separator + "local-repo");

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
