/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.meshkeeper.distribution.registry.zk;

import java.io.File;
import java.io.Serializable;

import org.fusesource.meshkeeper.MeshKeeper.Registry;
import org.fusesource.meshkeeper.control.ControlService;
import org.fusesource.meshkeeper.distribution.registry.RegistryClient;

import junit.framework.TestCase;

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

    RegistryClient client;
    ControlService server;

    protected void setUp() throws Exception {
        String SLASH = File.separator;
        String testDir = System.getProperty("basedir", ".") + SLASH + "target" + SLASH + "test-data" + SLASH + "RegistryTest";
        
        server = new ZooKeeperServerFactory().createPlugin("tcp://localhost:2000");
        server.setDirectory(testDir);
        server.start();

        client = new ZooKeeperFactory().createPlugin("zk:tcp://localhost:2000");
        client.start();
    }
    
    protected void tearDown() throws Exception {
        client.destroy();
        server.destroy();
    }

    public void testAddData() throws Exception
    {
        client.addRegistryObject("/add/foo/1", false, new TestObject());    
        TestObject o = client.getRegistryObject("/add/foo/1");
        assertNotNull(o);
    }
    
    public void testRegistryWatcher() throws Exception
    {
        Runnable r = new Runnable()
        {
            public void run()
            {
                try
                {
                    Thread.currentThread().sleep(5000);
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

    public static class TestObject implements Serializable {
       
    }
}
