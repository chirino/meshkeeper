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
package org.fusesource.meshkeeper.packaging;

import java.io.Serializable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

import org.fusesource.meshkeeper.Distributable;
import org.fusesource.meshkeeper.MavenTestSupport;
import org.fusesource.meshkeeper.MeshContainer;
import org.fusesource.meshkeeper.MeshKeeper;
import org.fusesource.meshkeeper.MeshContainer.Callable;
import org.fusesource.meshkeeper.MeshContainer.Runnable;
import org.fusesource.meshkeeper.util.DefaultProcessListener;

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
        private static final long serialVersionUID = 1L;
        CountDownLatch latch = new CountDownLatch(1);

        public void done() {
            latch.countDown();
        }
    }

    public static class MeshContainerTestObject implements Distributable, Serializable {
        private static final long serialVersionUID = 1L;
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

    static class RemoteTask implements Runnable {
        private static final long serialVersionUID = 1L;
        private final ICallBack callback;

        public RemoteTask(ICallBack callback) {
            this.callback = callback;
        }

        public void run() {
            System.out.println("Doing callback...");
            callback.done();
        }
    }
    
    static class RemoteCallable implements Callable<String> {
        private static final long serialVersionUID = 1L;
        private static final String RET = "CALLED";
        
        public String call() {
            System.out.println("Returning: " + RET);
            return RET;
        }
    }
    
    public void testMeshContainer() throws Exception {
        //Create default JavaLaunch:
        meshKeeper.launcher().waitForAvailableAgents(5000);
        String agentId = meshKeeper.launcher().getAvailableAgents()[0].getAgentId();
        MeshContainer container = meshKeeper.launcher().launchMeshContainer(agentId, new DefaultProcessListener("TestContainer"));

        try {
            CallBack callback = new CallBack();
            MeshContainerTestObject proxy = (MeshContainerTestObject) container.host("testObject", new MeshContainerTestObject(meshKeeper.remoting().export(callback)));
            proxy.start();

            System.out.println("Waiting for callback to fire");
            assertTrue(callback.latch.await(30, TimeUnit.SECONDS));
            System.out.println("Callback fired!");

            container.unhost("testObject");

            //Unhost and assert we can't get to it:
            Exception expected = null;
            try {
                proxy.start();
            } catch (Exception e) {
                expected = e;
            }
            assertNotNull(expected);

            System.out.println("Dumping registry:");
            for(String path : meshKeeper.registry().list("*", true))
            {
                System.out.println(path);
            }
            
            container.close();
            
            System.out.println("Dumping registry:");
            for(String path : meshKeeper.registry().list("*", true))
            {
                System.out.println(path);
            }

        } catch (Exception thrown) {
            thrown.printStackTrace();
            throw thrown;
        }
    }

    public void testRemoteRunnable() throws Exception {

        meshKeeper.launcher().waitForAvailableAgents(5000);
        String agentId = meshKeeper.launcher().getAvailableAgents()[0].getAgentId();
        MeshContainer container = meshKeeper.launcher().launchMeshContainer(agentId, new DefaultProcessListener("TestContainer"));

        try {
            CallBack cb = new CallBack();
            ICallBack cbp = (ICallBack) meshKeeper.remoting().export(cb);

            // Note: the launched JVM will use the class path of this test case.
            container.run(new RemoteTask(cbp));

            assertTrue(cb.latch.await(30, TimeUnit.SECONDS));
        } finally {
            container.close();
        }
    }
    
    public void testRemoteCallable() throws Exception {

        meshKeeper.launcher().waitForAvailableAgents(5000);
        String agentId = meshKeeper.launcher().getAvailableAgents()[0].getAgentId();
        MeshContainer container = meshKeeper.launcher().launchMeshContainer(agentId, new DefaultProcessListener("TestContainer"));

        try {
            // Note: the launched JVM will use the class path of this test case.
            assertEquals(container.call(new RemoteCallable()), RemoteCallable.RET);

        } finally {
            container.close();
        }
    }

}
