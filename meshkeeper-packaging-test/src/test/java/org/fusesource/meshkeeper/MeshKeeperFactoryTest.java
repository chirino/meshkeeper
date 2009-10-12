/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.meshkeeper;

import junit.framework.TestCase;

import org.fusesource.meshkeeper.control.ControlServer;
import org.fusesource.meshkeeper.launcher.LaunchAgent;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class MeshKeeperFactoryTest extends TestCase {

    private ControlServer controller;

    @Override
    protected void setUp() throws Exception {
        System.setProperty("meshkeeper.base", MavenTestSupport.getDataDirectory(MeshKeeperFactoryTest.class.getSimpleName()).getCanonicalPath());
        // This shows how to start an embedded server /w java
        controller = MeshKeeperFactory.createControlServer("zk:tcp://localhost:2101");
    }

    @Override
    protected void tearDown() throws Exception {
        controller.destroy();
    }

    public void testFactories() throws Exception {

        // Test out the spring factory..
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("factory-test.xml");
        MeshKeeper mk = (MeshKeeper) context.getBean("mk");
        assertNotNull(mk.registry().getRegistryData(ControlServer.REMOTING_URI_PATH));
        context.destroy();

        // Test out the java factories.
        mk = MeshKeeperFactory.createMeshKeeper("zk:tcp://localhost:2101");
        assertNotNull(mk.registry().getRegistryData(ControlServer.REMOTING_URI_PATH));
        mk.destroy();

        // Test out starting a launcher agent
        mk = MeshKeeperFactory.createMeshKeeper("zk:tcp://localhost:2101");
        LaunchAgent agent = MeshKeeperFactory.createAgent(mk);
        assertNotNull( agent.getAgentId() );
        agent.stop();
    }

}