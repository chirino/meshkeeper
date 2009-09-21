/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.meshkeeper.packaging;

import java.io.File;
import java.io.Serializable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

import org.fusesource.meshkeeper.Distributable;
import org.fusesource.meshkeeper.JavaLaunch;
import org.fusesource.meshkeeper.MeshContainer;
import org.fusesource.meshkeeper.MeshKeeper;
import org.fusesource.meshkeeper.MavenTestSupport;
import org.fusesource.meshkeeper.util.DefaultProcessListener;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * MeshContainerTest
 * <p>
 * Description:
 * </p>
 * 
 * @author cmacnaug
 * @version 1.0
 */
public class MeshContainerTest extends TestCase {

    MeshKeeper meshKeeper;

    protected void setUp() throws Exception {
        meshKeeper = MavenTestSupport.createMeshKeeper("MeshContainerTest");
    }

    protected void tearDown() throws Exception {
        if (meshKeeper != null) {
            meshKeeper.destroy();
            meshKeeper = null;
        }
    }

    public static interface ICallBack extends Distributable, Serializable {
        public void done();
    }

    public static class CallBack implements ICallBack {
        CountDownLatch latch = new CountDownLatch(1);

        public void done() {
            latch.countDown();
        }
    }

    public void testMeshContainer() throws Exception {
        //Create default JavaLaunch:
        JavaLaunch jl = new JavaLaunch();
        meshKeeper.launcher().waitForAvailableAgents(5000);
        String agentId = meshKeeper.launcher().getAvailableAgents()[0].getAgentId();
        MeshContainer container = meshKeeper.launcher().launchMeshContainer(agentId, jl, new DefaultProcessListener("TestContainer"));

        try {
            CallBack callback = new CallBack();
            MeshContainerTestObject proxy = (MeshContainerTestObject) container.host("testObject", new MeshContainerTestObject(meshKeeper.remoting().export(callback)));
            proxy.start();

            System.out.println("Waiting for callback to fire");
            assertTrue(callback.latch.await(30, TimeUnit.SECONDS));
            System.out.println("Callback fired!");
        } catch (Exception thrown) {
            thrown.printStackTrace();
            throw thrown;
        }
    }

    public static class MeshContainerTestObject implements Distributable, Serializable {
        ICallBack callback;

        public MeshContainerTestObject() {

        }

        MeshContainerTestObject(ICallBack callback) {
            this.callback = callback;
        }

        public void start() {
            System.out.println("Firing callback");
            callback.done();
        }
    }
}
