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
package org.fusesource.meshkeeper.packaging;

import junit.framework.TestCase;
import static org.fusesource.meshkeeper.Expression.file;
import static org.fusesource.meshkeeper.Expression.path;

import org.fusesource.meshkeeper.Distributor;
import org.fusesource.meshkeeper.Expression;
import org.fusesource.meshkeeper.LaunchClient;
import org.fusesource.meshkeeper.LaunchDescription;
import org.fusesource.meshkeeper.Process;
import org.fusesource.meshkeeper.ProcessListener;
import org.fusesource.meshkeeper.classloader.ClassLoaderFactory;
import org.fusesource.meshkeeper.classloader.ClassLoaderServer;
import org.fusesource.meshkeeper.classloader.ClassLoaderServerFactory;
import org.fusesource.meshkeeper.launcher.RemoteBootstrap;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author chirino
 */
public class RemoteClassLoaderTest extends TestCase {

    ClassPathXmlApplicationContext context;
    LaunchClient client;

    protected void setUp() throws Exception {
        String dataDir = "target" + File.separator + "remote-classloader-test";
        String commonRepo = new File(dataDir + File.separator + "common-repo").toURI().toString();

        System.setProperty("basedir", dataDir);
        System.setProperty("common.repo.url", commonRepo);

        context = new ClassPathXmlApplicationContext("meshkeeper-all-spring.xml");
        client = (LaunchClient) context.getBean("launch-client");

    }

    protected void tearDown() throws Exception {

        context.destroy();
        client = null;
    }

    private String getAgent() throws InterruptedException, TimeoutException
    {
        client.waitForAvailableAgents(5000);
        return client.getAvailableAgents()[0].getAgentId();
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
        Process process = client.launchProcess(getAgent(), ld, exitListener);
        exitListener.assertExitCode(1);
    }

    public void testInvalidSyntax() throws Exception {
        LaunchDescription ld = new LaunchDescription();
        ld.add("java");
        ld.add("-cp");
        setClassPath(ld);
        ld.add(RemoteBootstrap.class.getName());
        ld.add(DataInputTestApplication.class.getName());

        ExitProcessListener exitListener = new ExitProcessListener();
        Process process = client.launchProcess(getAgent(), ld, exitListener);
        exitListener.assertExitCode(2);
        assertTrue(exitListener.getOutAsString().startsWith("Invalid Syntax:"));
    }

    public void testLoadRemoteClass() throws Exception {
        Distributor distributor = client.getDistributor();
        ClassLoaderServer server = ClassLoaderServerFactory.create("basic:", distributor);
        server.start();

        ClassLoaderFactory stub = server.export(DataInputTestApplication.class.getClassLoader(), 1);
        String path = distributor.addRegistryObject("/test/classloader", true, stub);

        LaunchDescription ld = new LaunchDescription();
        ld.add("java");
        ld.add("-cp");
        setClassPath(ld);
        ld.add(RemoteBootstrap.class.getName());
        ld.add("--cache");
        ld.add(file("./classloader-cache"));
        ld.add("--distributor");
        ld.add(client.getDistributor().getDistributorUri());
        ld.add("--classloader");
        ld.add(path);
        ld.add(DataInputTestApplication.class.getName());

        ExitProcessListener exitListener = new ExitProcessListener();
        Process process = client.launchProcess(getAgent(), ld, exitListener);
        process.write(Process.FD_STD_IN, "exit: 5\n".getBytes());
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

    private static class ExitProcessListener implements ProcessListener {

        private final CountDownLatch exitLatch = new CountDownLatch(1);
        private final AtomicInteger exitCode = new AtomicInteger(0);

        private ByteArrayOutputStream out = new ByteArrayOutputStream();
        private ByteArrayOutputStream err = new ByteArrayOutputStream();

        private ArrayList<String> infos = new ArrayList<String>();
        private ArrayList<Throwable> errors = new ArrayList<Throwable>();

        public void assertExitCode(int value) throws InterruptedException {
            assertTrue("timed out waiting for exit", exitLatch.await(30, TimeUnit.SECONDS));
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