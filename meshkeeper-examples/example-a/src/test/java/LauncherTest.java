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
import java.util.concurrent.TimeoutException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.fusesource.meshkeeper.Expression.*;

import org.fusesource.meshkeeper.Expression;
import org.fusesource.meshkeeper.HostProperties;
import org.fusesource.meshkeeper.LaunchDescription;
import org.fusesource.meshkeeper.MavenTestSupport;
import org.fusesource.meshkeeper.MeshKeeper;
import org.fusesource.meshkeeper.MeshKeeper.Launcher;
import org.fusesource.meshkeeper.MeshKeeperFactory;
import org.fusesource.meshkeeper.MeshProcess;
import org.fusesource.meshkeeper.util.DefaultProcessListener;

import junit.framework.TestCase;

/**
 * This TestCase shows you how you can use MeshKeeper to launch remote processes
 * in your test case.
 * 
 */
public class LauncherTest extends TestCase {

    MeshKeeper meshKeeper;

    protected void setUp() throws Exception {
        //Use MavenTestSupport to create MeshKeeper under target directory:
        meshKeeper = MavenTestSupport.createMeshKeeper("LauncherTest");
    }

    protected void tearDown() throws Exception {
        if (meshKeeper != null) {
            meshKeeper.destroy();
            meshKeeper = null;
        }
    }

    private HostProperties getAgent() throws InterruptedException, TimeoutException {
        meshKeeper.launcher().waitForAvailableAgents(5000);
        return meshKeeper.launcher().getAvailableAgents()[0];
    }

    public void testDataOutput() throws Exception {

        // Process launching is main focused around the Launcher interface
        Launcher launcher = meshKeeper.launcher();

        HostProperties host = getAgent();

        System.out.println("Launching on: " + host.getExternalHostName());

        boolean windows = host.getSystemProperties().get("os.name").toString().toLowerCase().contains("windows");

        // Setup a simple remote command to execute.. we are just going to do a simple echo 
        // of Hello World from the host machines timezone.
        LaunchDescription ld = new LaunchDescription();
        //For windows start cmd shell:
        if (windows) {
            ld.add("cmd");
            ld.add("/c");
        }
        ld.add("echo");
        //Note the use of Expressions' append', 'string' and 'property' used to build the command.
        //Here the 'property' is evaluated from the host's launcher:
        ld.add(append(string("Hello World From: "), property("user.timezone")));

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
