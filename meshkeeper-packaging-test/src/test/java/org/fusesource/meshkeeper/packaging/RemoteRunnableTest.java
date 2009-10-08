/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.meshkeeper.packaging;

import java.io.Serializable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.fusesource.meshkeeper.Distributable;
import org.fusesource.meshkeeper.MavenTestSupport;
import org.fusesource.meshkeeper.MeshKeeper;
import org.fusesource.meshkeeper.util.DefaultProcessListener;

import junit.framework.TestCase;

/**
 * @author chirino
 */
public class RemoteRunnableTest extends TestCase {

    MeshKeeper meshKeeper;

    protected void setUp() throws Exception {
        meshKeeper = MavenTestSupport.createMeshKeeper(getClass().getSimpleName());
    }

    protected void tearDown() throws Exception {
        if( meshKeeper!=null ) {
            meshKeeper.destroy();
            meshKeeper=null;
        }
    }

    public static interface ICallBack extends Distributable {
        public void done();
    }
    
    public static class CallBack implements ICallBack {
        CountDownLatch latch = new CountDownLatch(1);
        public void done() {
            latch.countDown();
        }
    }

    static class RemoteTask implements Serializable, Runnable {
        private final ICallBack callback;

        public RemoteTask(ICallBack callback) {
            this.callback = callback;
        }

        public void run() {
            System.out.println("Doing callback...");
            callback.done();
        }
    }

    public void testRemoteRunnable() throws Exception {
        CallBack cb = new CallBack();
        ICallBack cbp = (ICallBack) meshKeeper.remoting().export(cb);

        meshKeeper.launcher().waitForAvailableAgents(5000);
        String agent = meshKeeper.launcher().getAvailableAgents()[0].getAgentId();

        // Note: the launched JVM will use the class path of this test case.
        meshKeeper.launcher().launch(agent, new RemoteTask(cbp), new DefaultProcessListener(meshKeeper));

        assertTrue(cb.latch.await(30, TimeUnit.SECONDS));
    }
}