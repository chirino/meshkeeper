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

import java.io.File;

/**
 * Factory class that can creates the MeshKeeper clients,
 * agents or control sever instances.
 *
 * @author chirino
 */
public class MeshKeeperFactory {

    static public MeshKeeper createMeshKeeper(String registry, File dataDir) throws Exception {
        DistributorFactory df = new DistributorFactory();
        df.setRegistryUri(registry);
        df.setDirectory(dataDir.getCanonicalPath());
        MeshKeeper mk = df.create();
        mk.start();
        return mk;
    }

    static public LaunchAgent createAgent(MeshKeeper keeper, File dataDir) throws Exception {
        LaunchAgent agent = new LaunchAgent();
        agent.setMeshKeeper(keeper);
        agent.setDirectory(dataDir);
        agent.start();
        return agent;
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