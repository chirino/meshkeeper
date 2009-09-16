/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.meshkeeper;

import org.fusesource.meshkeeper.control.ControlServer;
import org.fusesource.meshkeeper.distribution.DistributorFactory;
import org.fusesource.meshkeeper.launcher.LaunchAgent;
import org.fusesource.meshkeeper.util.internal.MeshKeeperWrapper;
import org.fusesource.meshkeeper.util.internal.EmbeddedServer;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Factory class that can creates the MeshKeeper clients,
 * agents or control sever instances.
 *
 * @author chirino
 */
public class MeshKeeperFactory {

    public static final String MESHKEEPER_REGISTRY_PROPERTY = "meshkeeper.registry";
    public static final String MESHKEEPER_BASE_PROPERTY = "meshkeeper.base";
    public static final EmbeddedServer EMBEDDED_SERVER = new EmbeddedServer();

    public static File getDefaultBaseDirectory() {
        return new File(System.getProperty(MESHKEEPER_BASE_PROPERTY,"./data"));
    }

    public static File getDefaultClientDirectory() {


        return new File(getDefaultBaseDirectory(), "client");
    }
    public static File getDefaultAgentDirectory() {
        return new File(getDefaultBaseDirectory(), "agent");
    }
    public static File getDefaultServerDirectory() {
        return new File(getDefaultBaseDirectory(), "server");
    }

    /**
     * Creates a MeshKeeper object.  If the "meshkeeper.registry" system property
     * is not set or if it is set to "embedded", then this method will connect the
     * returned MeshKeeper object to embedded control server and agent which it will
     * start up and shutdown automatically on demand.
     *
     * Uses the meshkeeper.repository system property to control if a
     */
    public static MeshKeeper createMeshKeeper() throws Exception {

        String url = System.getProperty(MESHKEEPER_REGISTRY_PROPERTY, "embedded");
        if( "embedded".equals(url) ) {

            // We wrap it so we know when we can stop the embedded registiry.
            return new MeshKeeperWrapper(createMeshKeeper(EMBEDDED_SERVER.aquireEmbeddedRegistry())) {
                AtomicBoolean destroyed = new AtomicBoolean(false);
                public void destroy() throws Exception {
                    next.destroy();
                    if( destroyed.compareAndSet(false, true) ) {
                        EMBEDDED_SERVER.releaseEmbeddedRegistry();
                    }
                }
            };

        }
        return createMeshKeeper(url);
    }


    static public MeshKeeper createMeshKeeper(String registry) throws Exception {
        return createMeshKeeper(registry, getDefaultClientDirectory());
    }

    static public MeshKeeper createMeshKeeper(String registry, File dataDir) throws Exception {
        DistributorFactory df = new DistributorFactory();
        df.setRegistryUri(registry);
        df.setDirectory(dataDir.getCanonicalPath());
        MeshKeeper mk = df.create();
        mk.start();
        return mk;
    }

    static public LaunchAgent createAgent(MeshKeeper keeper) throws Exception {
        return createAgent(keeper, getDefaultAgentDirectory());
    }

    static public LaunchAgent createAgent(MeshKeeper keeper, File dataDir) throws Exception {
        LaunchAgent agent = new LaunchAgent();
        agent.setMeshKeeper(keeper);
        agent.setDirectory(dataDir);
        agent.start();
        return agent;
    }

    static public ControlServer createControlServer(String registry) throws Exception {
        return createControlServer(registry, getDefaultServerDirectory());
    }

    static public ControlServer createControlServer(String registry, File dataDir) throws Exception {
        ControlServer rc = new ControlServer();
        rc.setRegistryUri(registry);
        rc.setJmsUri("jms:activemq:tcp://localhost:2100");
        rc.setDirectory(dataDir.getCanonicalPath());
        rc.setRepositoryUri(System.getProperty("common.repo.url"));
        rc.start();
        return rc;
    }


}