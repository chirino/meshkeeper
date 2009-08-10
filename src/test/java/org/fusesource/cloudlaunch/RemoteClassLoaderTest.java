/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.cloudlaunch;

import junit.framework.TestCase;
import static org.fusesource.cloudlaunch.Expression.file;
import static org.fusesource.cloudlaunch.Expression.path;

import org.fusesource.cloudlaunch.Expression;
import org.fusesource.cloudlaunch.LaunchDescription;
import org.fusesource.cloudlaunch.Process;
import org.fusesource.cloudlaunch.ProcessListener;
import org.fusesource.cloudlaunch.control.ControlServer;
import org.fusesource.cloudlaunch.control.LaunchClient;
import org.fusesource.cloudlaunch.registry.zk.ZooKeeperFactory;
import org.fusesource.cloudlaunch.local.ProcessLauncher;
import org.fusesource.cloudlaunch.rmi.*;
import org.fusesource.cloudlaunch.rmi.rmiviajms.RmiViaJmsExporter;
import org.fusesource.rmiviajms.JMSRemoteObject;

import java.io.IOException;
import java.io.File;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author chirino
 */
public class RemoteClassLoaderTest extends TestCase {

    ControlServer controlServer;
    ProcessLauncher agent;
    LaunchClient launchClient;

    protected void setUp() throws Exception {

        String dataDir = "target" + File.separator + "remote-classloader-test-data";
        String commonRepo = new File(dataDir + File.separator + "common-repo").toURI().toString();

        try {
            controlServer = new ControlServer();
            controlServer.setDataDirectory(dataDir + File.separator + "control-server");
            controlServer.setJmsConnectUrl("tcp://localhost:61616");
            controlServer.setZooKeeperConnectUrl("tcp://localhost:2012");
            controlServer.start();

            ZooKeeperFactory factory = new ZooKeeperFactory();
            factory.setConnectUrl(controlServer.getZooKeeperConnectUrl());

            RmiViaJmsExporter exporter = new RmiViaJmsExporter();
            exporter.setConnectUrl("tcp://localhost:61616");

            Distributor distributor = new Distributor();
            distributor.setRegistry(factory.getRegistry());
            distributor.setExporter(exporter);

            //Set up a launch agent:

            agent = new ProcessLauncher();
            agent.setDataDirectory(new File(dataDir + File.separator + "testrunner-data"));
            agent.setCommonResourceRepoUrl(commonRepo);
            agent.setDistributor(distributor);
            agent.start();
            agent.purgeResourceRepository();

            launchClient = new LaunchClient();
            launchClient.setBindTimeout(5000);
            launchClient.setLaunchTimeout(10000);
            launchClient.setKillTimeout(5000);
            launchClient.setDistributor(distributor);
            launchClient.start();
            launchClient.waitForAvailableAgents(5000);
            launchClient.bindAgent(agent.getAgentId());
        } catch (Exception e) {
            tearDown();
            throw e;
        }
    }

    protected void tearDown() throws Exception {

        if (launchClient != null) {
            System.out.println("Closing Launch Client");
            launchClient.destroy();
        }

        if (agent != null) {
            System.out.println("Shutting down launch agent");
            try {
                agent.stop();
                agent = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (launchClient != null) {
            launchClient.getDistributor().destroy();
            launchClient = null;
        }

        if (controlServer != null) {
            System.out.println("Shutting down control server");
            controlServer.destroy();
            controlServer = null;
            System.out.println("Shut down control server");
        }
    }

    /**
     * Verify that we can setup a remote launch that does not contain the Main
     * app.
     * 
     * @throws Exception
     */
    public void testClassNotFound() throws Exception {
        LaunchDescription ld = new LaunchDescription();
        ld.add("java");
        ld.add("-cp");
        setClassPath(ld);
        ld.add(DataInputTestApplication.class.getName());

        ExitProcessListener exitListener = new ExitProcessListener();
        Process process = launchClient.launchProcess(agent.getAgentId(), ld, exitListener);
        exitListener.assertExitCode(1);
    }

    public void testInvalidSyntax() throws Exception {
        LaunchDescription ld = new LaunchDescription();
        ld.add("java");
        ld.add("-cp");
        setClassPath(ld);
        ld.add(RemoteLoadingMain.class.getName());
        ld.add(DataInputTestApplication.class.getName());

        ExitProcessListener exitListener = new ExitProcessListener();
        Process process = launchClient.launchProcess(agent.getAgentId(), ld, exitListener);
        exitListener.assertExitCode(2);
        assertTrue(exitListener.getOutAsString().startsWith("Invalid Syntax:"));
    }

    public void testLoadRemoteClass() throws Exception {
        ClassLoaderServer server = new ClassLoaderServer(null, DataInputTestApplication.class.getClassLoader());

        String path = launchClient.getDistributor().register(server, "/test/classloader", true).getPath();
        LaunchDescription ld = new LaunchDescription();
        ld.add("java");
        ld.add("-cp");
        setClassPath(ld);
        ld.add(RemoteLoadingMain.class.getName());
        ld.add("--cache-dir");
        ld.add(file("./classloader-cache"));
        ld.add("--registry-url");
        ld.add(controlServer.getZooKeeperConnectUrl());
        ld.add("--classloader-url");
        ld.add(path);
        ld.add(DataInputTestApplication.class.getName());

        ExitProcessListener exitListener = new ExitProcessListener();
        Process process = launchClient.launchProcess(agent.getAgentId(), ld, exitListener);
        process.write(Process.FD_STD_IN, "exit: 5\n".getBytes());

        try {
            exitListener.assertExitCode(5);
        } finally {
            System.out.println(exitListener.getOutAsString());
            System.out.println(exitListener.getErrAsString());
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

    private static class ExitProcessListener implements ProcessListener {

        private final CountDownLatch exitLatch = new CountDownLatch(1);
        private final AtomicInteger exitCode = new AtomicInteger(0);

        private ByteArrayOutputStream out = new ByteArrayOutputStream();
        private ByteArrayOutputStream err = new ByteArrayOutputStream();

        private ArrayList<String> infos = new ArrayList<String>();
        private ArrayList<Throwable> errors = new ArrayList<Throwable>();

        public void assertExitCode(int value) throws InterruptedException {
            assertTrue("timed out waiting for exit", exitLatch.await(10, TimeUnit.SECONDS));
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
                if (fd == Process.FD_STD_OUT) {
                    out.write(output);
                } else if (fd == Process.FD_STD_ERR) {
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