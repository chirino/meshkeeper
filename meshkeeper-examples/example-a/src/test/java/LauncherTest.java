/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/

import java.util.concurrent.TimeoutException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.fusesource.meshkeeper.Expression.*;
import org.fusesource.meshkeeper.HostProperties;
import org.fusesource.meshkeeper.LaunchDescription;
import org.fusesource.meshkeeper.MeshKeeper;
import org.fusesource.meshkeeper.MeshKeeper.Launcher;
import org.fusesource.meshkeeper.MeshKeeperFactory;
import org.fusesource.meshkeeper.MeshProcess;
import org.fusesource.meshkeeper.util.DefaultProcessListener;

import junit.framework.TestCase;

/**
 * This TestCase shows you how you can use MeshKeeper to launch remote
 * processes in your test case.
 *
 */
public class LauncherTest extends TestCase {

    MeshKeeper meshKeeper;

    protected void setUp() throws Exception {
        meshKeeper = MeshKeeperFactory.createMeshKeeper();
    }

    protected void tearDown() throws Exception {
        if (meshKeeper != null) {
            meshKeeper.destroy();
            meshKeeper = null;
        }
    }

    private String getAgent() throws InterruptedException, TimeoutException
    {
        meshKeeper.launcher().waitForAvailableAgents(5000);
        return meshKeeper.launcher().getAvailableAgents()[0].getAgentId();
    }

    public void testDataOutput() throws Exception {

        // Process launching is main focused around the Launcher interface
        Launcher launcher = meshKeeper.launcher();

        // Lets get an agent to launch on...
        launcher.waitForAvailableAgents(5000);
        HostProperties host = launcher.getAvailableAgents()[0];

        System.out.println("Launching on: "+host.getExternalHostName());

        // Setup a simple remote command to execute.. we are just going to do an echo of the OS
        LaunchDescription ld = new LaunchDescription();
        ld.add("echo");
        ld.add("The remote OS is a: ");
        ld.add(property("os.name"));

        final CountDownLatch done = new CountDownLatch(1);
        final AtomicInteger exitCode = new AtomicInteger();

        // All process output will be delivered to this listener..
        DefaultProcessListener listener = new DefaultProcessListener("remote process") {
            @Override
            public void onProcessExit(int code) {
                exitCode.set(code);
                done.countDown();
            }
        };

        MeshProcess process = launcher.launchProcess(host.getAgentId(), ld, listener);

        assertTrue(done.await(10, TimeUnit.SECONDS));
        assertEquals(0, exitCode.get());

    }


}