/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.meshkeeper.distribution.registry.zk;

import java.io.Serializable;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.fusesource.meshkeeper.MavenTestSupport;
import org.fusesource.meshkeeper.distribution.registry.RegistryClient;

/**
 * RegistryTest
 * <p>
 * Description:
 * </p>
 * 
 * @author cmacnaug
 * @version 1.0
 */
public class RegistryTest extends TestCase {

    Log LOG = LogFactory.getLog(RegistryTest.class);
    
    RegistryClient client;
    ZooKeeperServer server;

    protected void setUp() throws Exception {
        String testDir = MavenTestSupport.getDataDirectory(RegistryTest.class.getSimpleName()).getCanonicalPath();
        
        server = (ZooKeeperServer) new ZooKeeperServerFactory().createPlugin("tcp://localhost:2000");
        server.setDirectory(testDir);
        server.start();

        client = new ZooKeeperFactory().createPlugin(server.getServiceUri());
        client.start();
    }
    
    protected void tearDown() throws Exception {
        client.destroy();
        server.destroy();
    }

    public void testPurgeOnRestart() throws Exception
    {
        LOG.info("Running: testPurgeOnRestart");
        client.addRegistryObject("/add/foo/1", true, new TestObject());    
        
        client.destroy();
        server.destroy();
        
        setUp();
        
        assertNull(client.getRegistryObject("/add/foo/1"));
    }
    
    public void testAddData() throws Exception
    {
        LOG.info("Running: testAddData");
        
        client.addRegistryObject("/add/foo/1", false, new TestObject());    
        TestObject o = client.getRegistryObject("/add/foo/1");
        assertNotNull(o);
    }
    
    public void testRegistryWatcher() throws Exception
    {
        LOG.info("Running: testRegistryWatcher");
        
        Runnable r = new Runnable()
        {
            public void run()
            {
                try
                {
                    Thread.sleep(5000);
                    client.addRegistryObject("/temp/foo/1", false, new TestObject());
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        };
        
        Thread t = new Thread(r);
        t.start();
        
        assertNotNull(client.waitForRegistration("/temp/foo/1", 20000));        
    }
    
    public void testRecursiveDelete() throws Exception
    {
        LOG.info("Running: testRecursiveDelete");
        
        client.addRegistryData("/delete/a/b", true, new byte [100]);
        client.addRegistryData("/delete/d/e", false, new byte [100]);
        client.addRegistryData("/delete/c", true, new byte [100]);
        client.addRegistryData("/delete/f", false, new byte [100]);
        
        client.removeRegistryData("/delete", false);
        assertNull(client.getRegistryData("/delete"));
        client.removeRegistryData("/delete", true);
        
        assertNull(client.getRegistryData("/delete/d/e"));
    }

    public static class TestObject implements Serializable {

        private static final long serialVersionUID = 1L;
       
    }
}
