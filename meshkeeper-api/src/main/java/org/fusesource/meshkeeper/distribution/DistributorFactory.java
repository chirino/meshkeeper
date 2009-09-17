/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.meshkeeper.distribution;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.fusesource.meshkeeper.control.ControlServer;
import org.fusesource.meshkeeper.distribution.event.EventClient;
import org.fusesource.meshkeeper.distribution.event.EventClientFactory;
import org.fusesource.meshkeeper.distribution.registry.RegistryClient;
import org.fusesource.meshkeeper.distribution.registry.RegistryFactory;
import org.fusesource.meshkeeper.distribution.remoting.RemotingFactory;
import org.fusesource.meshkeeper.distribution.remoting.RemotingClient;
import org.fusesource.meshkeeper.distribution.repository.RepositoryClient;
import org.fusesource.meshkeeper.distribution.repository.RepositoryManagerFactory;
import org.fusesource.meshkeeper.MeshKeeperFactory;

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

    private static String DEFAULT_REPOSITORY_PROVIDER = "wagon";
    private static String DEFAULT_REGISTRY_URI = ControlServer.DEFAULT_REGISTRY_URI;
    private static final ScheduledExecutorService EXECUTOR;
    private static final AtomicInteger EXECUTOR_COUNT = new AtomicInteger(0);
    static {
        EXECUTOR = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors(), new ThreadFactory() {
            public Thread newThread(Runnable r) {
                return new Thread(r, "MeshKeeperExecutor-" + EXECUTOR_COUNT.incrementAndGet());
            }
        });
    }

    private String repositoryProvider = DEFAULT_REPOSITORY_PROVIDER;
    private String registryUri = DEFAULT_REGISTRY_URI;
    private String directory = MeshKeeperFactory.getDefaultClientDirectory().getPath();
    private String eventingUri;
    private String remotingUri;
    private String repositoryUri;

    /**
     * This convenience method creates a Distributor by connecting to a control
     * server registry and pulls down
     * 
     * @return
     */
    public static DefaultDistributor createDefaultDistributor() throws Exception {
        DistributorFactory df = new DistributorFactory();
        return df.create();
    }

    public static void setDefaultRegistryUri(String defaultRegistryUri) {
        DEFAULT_REGISTRY_URI = defaultRegistryUri;
    }

    public static ScheduledExecutorService getExecutorService() {
        return EXECUTOR;
    }

    public DefaultDistributor create() throws Exception {

        //Create Registry:
        RegistryClient registry = new RegistryFactory().create(registryUri);
        registry.start();

        //Create Exporter:
        if (remotingUri == null) {
            remotingUri = registry.getRegistryObject(ControlServer.REMOTING_URI_PATH);
            if (remotingUri == null) {
                remotingUri = ControlServer.DEFAULT_REMOTING_URI;
            }
        }
        RemotingClient exporter = new RemotingFactory().create(remotingUri);

        //Create Event Client:
        if (eventingUri == null) {
            eventingUri = registry.getRegistryObject(ControlServer.EVENTING_URI_PATH);
            if (eventingUri == null) {
                eventingUri = ControlServer.DEFAULT_EVENT_URI;
            }
        }
        EventClient eventClient = new EventClientFactory().create(eventingUri);

        //Create ResourceManager:
        RepositoryClient resourceManager = new RepositoryManagerFactory().create(repositoryProvider);
        String commonRepoUrl = registry.getRegistryObject(ControlServer.REPOSITORY_URI_PATH);
        if (commonRepoUrl != null) {
            resourceManager.setCentralRepoUri(commonRepoUrl, null);
        }
        resourceManager.setLocalRepoDir(directory + File.separator + "local-repo");

        DefaultDistributor ret = new DefaultDistributor();
        ret.setRemotingClient(exporter);
        ret.setRegistry(registry);
        ret.setEventClient(eventClient);
        ret.setResourceManager(resourceManager);
        ret.setRegistryUri(registryUri);

        ret.start();
        if (log.isTraceEnabled()) {
            log.trace("Created: " + ret);
        }
        return ret;

    }

    public String getRepositoryProvider() {
        return repositoryProvider;
    }

    public void setRepositoryProvider(String repositoryProvider) {
        this.repositoryProvider = repositoryProvider;
    }

    public String getRegistryUri() {
        return registryUri;
    }

    public void setRegistryUri(String registryUri) {
        this.registryUri = registryUri;
    }

    public String getEventingUri() {
        return eventingUri;
    }

    public void setEventingUri(String eventingUri) {
        this.eventingUri = eventingUri;
    }

    public String getRemotingUri() {
        return remotingUri;
    }

    public void setRemotingUri(String remotingUri) {
        this.remotingUri = remotingUri;
    }

    public String getRepositoryUri() {
        return repositoryUri;
    }

    public void setRepositoryUri(String repositoryUri) {
        this.repositoryUri = repositoryUri;
    }

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }
}
