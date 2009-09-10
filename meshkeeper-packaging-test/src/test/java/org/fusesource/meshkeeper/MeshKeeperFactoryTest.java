package org.fusesource.meshkeeper;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.fusesource.meshkeeper.control.ControlServer;
import org.fusesource.meshkeeper.launcher.LaunchAgentService;
import org.fusesource.meshkeeper.launcher.LaunchAgent;
import junit.framework.TestCase;

import java.io.File;

import sun.management.resources.agent;

/**
 * @author chirino
 */
public class MeshKeeperFactoryTest extends TestCase {

    public static String basedir = System.getProperty("basedir", "target/test-data");
    private ControlServer controller;

    @Override
    protected void setUp() throws Exception {
        // This shows how to start an embedded server /w java
        controller = MeshKeeperFactory.createControlServer("zk:tcp://localhost:2101", new File(basedir+"/meshkeeper-controller"));
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
        mk = MeshKeeperFactory.createMeshKeeper("zk:tcp://localhost:2101", new File(basedir+"/client"));
        assertNotNull(mk.registry().getRegistryData(ControlServer.REMOTING_URI_PATH));
        mk.destroy();


        // Test out starting a launcher agent
        mk = MeshKeeperFactory.createMeshKeeper("zk:tcp://localhost:2101", new File(basedir+"/client"));
        LaunchAgent agent = MeshKeeperFactory.createAgent(mk, new File(basedir+"/launcher"));
        assertNotNull( agent.getAgentId() );
        agent.stop();
    }

}