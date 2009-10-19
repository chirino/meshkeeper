/**
 *  Copyright (C) 2009 Progress Software, Inc. All rights reserved.
 *  http://fusesource.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
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