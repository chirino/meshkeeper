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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.fusesource.meshkeeper.Expression;
import static org.fusesource.meshkeeper.Expression.*;

import org.fusesource.meshkeeper.HostProperties;
import org.fusesource.meshkeeper.LaunchDescription;
import org.fusesource.meshkeeper.MavenTestSupport;
import org.fusesource.meshkeeper.MeshKeeper;
import org.fusesource.meshkeeper.MeshProcess;
import org.fusesource.meshkeeper.MeshProcessListener;
import org.fusesource.meshkeeper.classloader.ClassLoaderFactory;
import org.fusesource.meshkeeper.classloader.ClassLoaderServer;
import org.fusesource.meshkeeper.classloader.ClassLoaderServerFactory;
import org.fusesource.meshkeeper.launcher.LaunchAgent;
import org.fusesource.meshkeeper.launcher.RemoteBootstrap;

import junit.framework.TestCase;

/**
 * @author chirino
 */
public class RemoteClassLoaderTest extends TestCase {

    MeshKeeper meshKeeper;

    protected void setUp() throws Exception {
        meshKeeper = MavenTestSupport.createMeshKeeper("RemoteClassLoaderTest");
    }

    protected void tearDown() throws Exception {
        if( meshKeeper!=null ) {
            meshKeeper.destroy();
            meshKeeper=null;
        }
    }

    private HostProperties getAgent() throws InterruptedException, TimeoutException
    {
        meshKeeper.launcher().waitForAvailableAgents(5000);
        return meshKeeper.launcher().getAvailableAgents()[0];
    }

    /**
     * Verify that we can setup a remote launch that does not contain the Main
     * app.
     *
     * @throws Exception
     */
    public void testClassNotFound() throws Exception {
        HostProperties hostProps = getAgent();
        LaunchDescription ld = new LaunchDescription();
        ld.add("java");
        ld.add("-cp");
        setClassPath(ld);
        ld.propagateSystemProperties(hostProps.getSystemProperties(), LaunchAgent.PROPAGATED_SYSTEM_PROPERTIES);
        ld.add(DataInputTestApplication.class.getName());

        ExitProcessListener exitListener = new ExitProcessListener();
        meshKeeper.launcher().launchProcess(hostProps.getAgentId(), ld, exitListener);
        exitListener.assertExitCode(1);
    }

    public void testInvalidSyntax() throws Exception {
        HostProperties hostProps = getAgent();
        LaunchDescription ld = new LaunchDescription();
        ld.add("java");
        ld.add("-cp");
        setClassPath(ld);
        ld.propagateSystemProperties(hostProps.getSystemProperties(), LaunchAgent.PROPAGATED_SYSTEM_PROPERTIES);
        ld.add(RemoteBootstrap.class.getName());
        ld.add(DataInputTestApplication.class.getName());

        ExitProcessListener exitListener = new ExitProcessListener();
        meshKeeper.launcher().launchProcess(hostProps.getAgentId(), ld, exitListener);
        exitListener.assertExitCode(2);
        try
        {
            assertTrue(exitListener.getOutAsString().contains("Invalid Syntax:"));
        }
        finally
        {
            System.out.println("==== client output ====");
            System.out.println(exitListener.getOutAsString());
            System.out.println(exitListener.getErrAsString());
        }
    }

    public void testLoadRemoteClass() throws Exception {
        ClassLoaderServer server = ClassLoaderServerFactory.create("basic:", meshKeeper);
        server.start();

        ClassLoaderFactory stub = server.export(DataInputTestApplication.class.getClassLoader(), "/test/classloader", 1);
        
        HostProperties hostProps = getAgent();
        
        LaunchDescription ld = new LaunchDescription();
        ld.add("java");
        ld.add("-cp");
        setClassPath(ld);
        ld.propagateSystemProperties(hostProps.getSystemProperties(), LaunchAgent.PROPAGATED_SYSTEM_PROPERTIES);
        ld.add(RemoteBootstrap.class.getName());
        ld.add("--cache");
        ld.add(file("./classloader-cache"));
        ld.add("--distributor");
        ld.add(meshKeeper.getRegistryConnectUri());
        ld.add("--classloader");
        ld.add(stub.getRegistryPath());
        ld.add(DataInputTestApplication.class.getName());

        ExitProcessListener exitListener = new ExitProcessListener();
        MeshProcess process = meshKeeper.launcher().launchProcess(hostProps.getAgentId(), ld, exitListener);
        process.write(MeshProcess.FD_STD_IN, "exit: 5\n".getBytes());
        try {
            exitListener.assertExitCode(5);
        } finally {
            System.out.println("==== client output ====");
            System.out.println(exitListener.getOutAsString());
            System.out.println(exitListener.getErrAsString());
            server.stop();
        }
    }

    private void setClassPath(LaunchDescription ld) throws IOException {
        File testClassesLocation = codeLocation(DataInputTestApplication.class);
        ArrayList<Expression.FileExpression> files = new ArrayList<Expression.FileExpression>();
        for (String file : System.getProperty("java.class.path").split(File.pathSeparator)) {
            File t = new File(file).getCanonicalFile();
            // We want to skip the test classes directory...
            if (!t.equals(testClassesLocation)) {
                files.add(file(file));
            }
        }
        ld.add(path(files));
    }

    private static File codeLocation(Class<?> clazz) throws IOException {
        return new File(clazz.getProtectionDomain().getCodeSource().getLocation().getPath()).getCanonicalFile();
    }

    private static class ExitProcessListener implements MeshProcessListener {

        private final CountDownLatch exitLatch = new CountDownLatch(1);
        private final AtomicInteger exitCode = new AtomicInteger(0);

        private ByteArrayOutputStream out = new ByteArrayOutputStream();
        private ByteArrayOutputStream err = new ByteArrayOutputStream();

        private ArrayList<String> infos = new ArrayList<String>();
        private ArrayList<Throwable> errors = new ArrayList<Throwable>();

        public void assertExitCode(int value) throws InterruptedException {
            assertTrue("timed out waiting for exit", exitLatch.await(60, TimeUnit.SECONDS));
            assertEquals(value, exitCode.get());
        }

        public void onProcessExit(int value) {
            this.exitCode.set(value);
            this.exitLatch.countDown();
        }

        synchronized public void onProcessError(Throwable thrown) {
            errors.add(thrown);
        }

        synchronized public void onProcessInfo(String message) {
            infos.add(message);
        }

        synchronized public void onProcessOutput(int fd, byte[] output) {
            try {
                if (fd == MeshProcess.FD_STD_OUT) {
                    out.write(output);
                } else if (fd == MeshProcess.FD_STD_ERR) {
                    err.write(output);
                }
            } catch (IOException e) {
            }
        }

        synchronized public String getOutAsString() {
            return new String(out.toByteArray());
        }

        synchronized public String getErrAsString() {
            return new String(err.toByteArray());
        }
    }
}