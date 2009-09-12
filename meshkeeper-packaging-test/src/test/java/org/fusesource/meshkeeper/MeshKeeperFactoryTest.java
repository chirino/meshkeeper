package org.fusesource.meshkeeper;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.fusesource.meshkeeper.control.ControlServer;
import org.fusesource.meshkeeper.launcher.LaunchAgent;
import junit.framework.TestCase;

import java.io.File;

/**
 * @author chirino
 */
public class MeshKeeperFactoryTest extends TestCase {

    private ControlServer controller;

    @Override
    protected void setUp() throws Exception {
        final String SLASH = File.separator;
        String testDir = System.getProperty("basedir", ".")+ SLASH +"target"+ SLASH +"test-data"+SLASH+ getClass().getName();
        System.setProperty("meshkeeper.base", testDir);

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