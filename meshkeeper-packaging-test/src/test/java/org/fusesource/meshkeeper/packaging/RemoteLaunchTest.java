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

import java.io.File;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

import org.fusesource.meshkeeper.Expression;
import org.fusesource.meshkeeper.Expression.FileExpression;
import org.fusesource.meshkeeper.LaunchDescription;
import org.fusesource.meshkeeper.MavenTestSupport;
import org.fusesource.meshkeeper.MeshKeeper;
import org.fusesource.meshkeeper.MeshProcess;
import org.fusesource.meshkeeper.MeshProcessListener;

import junit.framework.TestCase;

/**
 * RemoteLaunchTest
 * <p>
 * Description:
 * </p>
 *
 * @author cmacnaug
 * @version 1.0
 */
public class RemoteLaunchTest extends TestCase {

    MeshKeeper meshKeeper;

    protected void setUp() throws Exception {
        meshKeeper = MavenTestSupport.createMeshKeeper(getClass().getName());
    }

    protected void tearDown() throws Exception {
        if( meshKeeper!=null ) {
            meshKeeper.destroy();
            meshKeeper=null;
        }
    }

    private String getAgent() throws InterruptedException, TimeoutException
    {
        meshKeeper.launcher().waitForAvailableAgents(5000);
        return meshKeeper.launcher().getAvailableAgents()[0].getAgentId();
    }

    public void testDataOutput() throws Exception {
        LaunchDescription ld = new LaunchDescription();
        ld.add("java");
        ld.add("-cp");

        ArrayList<FileExpression> files = new ArrayList<FileExpression>();
        for (String f : System.getProperty("java.class.path").split(File.pathSeparator)) {
            files.add(Expression.file(f));
        }

        ld.add(Expression.path(files));
        ld.add(DataInputTestApplication.class.getName());

        DataOutputTester tester = new DataOutputTester();
        tester.test(meshKeeper.launcher().launchProcess(getAgent(), ld, tester));

    }

    public class DataOutputTester implements MeshProcessListener {

        private final int TEST_OUTPUT = 0;
        private final int TEST_ERROR = 1;
        private final int SUCCESS = 2;
        private final int FAIL = 3;

        private static final String EXPECTED_OUTPUT = "test output";
        private static final String EXPECTED_ERROR = "test error";

        private int state = TEST_OUTPUT;

        private Exception failure;

        public DataOutputTester() throws RemoteException {
        }

        public void test(MeshProcess process) throws Exception {

            try {

                synchronized (this) {
                    while (true) {

                        switch (state) {
                        case TEST_OUTPUT: {
                            System.out.println("Testing output");
                            process.write(MeshProcess.FD_STD_IN, new String("echo:" + EXPECTED_OUTPUT + "\n").getBytes());
                            break;
                        }
                        case TEST_ERROR: {
                            System.out.println("Testing error");
                            process.write(MeshProcess.FD_STD_IN, new String("error:" + EXPECTED_ERROR + "\n").getBytes());
                            break;
                        }
                        case SUCCESS: {
                            if (failure != null) {
                                throw failure;
                            }
                            return;
                        }
                        case FAIL:
                        default: {
                            if (failure == null) {
                                failure = new Exception();
                            }
                            throw failure;
                        }
                        }

                        int oldState = state;
                        wait(10000);
                        if (oldState == state) {
                            throw new Exception("Timed out in state: " + state);
                        }
                    }
                }
            } finally {
                process.kill();
            }
        }

        synchronized public void onProcessOutput(int fd, byte[] data) {
            String output = new String(data);

            if (fd == MeshProcess.FD_STD_OUT) {
                System.out.print("STDOUT: " + output + " [" + output.length() + " bytes]");
                if (state == TEST_OUTPUT && EXPECTED_OUTPUT.equals(output.trim())) {
                    state = TEST_ERROR;
                } else {
                    failure = new Exception("Unexpected system output: " + output);
                    state = FAIL;
                }
                notifyAll();
            } else if (fd == MeshProcess.FD_STD_ERR) {
                System.out.print("STDERR: " + output + " [" + output.length() + " bytes]");
                if (state == TEST_ERROR && EXPECTED_ERROR.equals(output.trim())) {
                    state = SUCCESS;
                } else {
                    failure = new Exception("Unexpected system err: " + output);
                    state = FAIL;
                }
                notifyAll();
            }
        }

        public synchronized void onProcessExit(int exitCode) {
            if (state < SUCCESS) {
                failure = new Exception("Premature process exit");
                state = FAIL;
                notifyAll();
            }
        }

        public synchronized void onProcessError(Throwable thrown) {
            failure = new Exception("Unexpected process error", thrown);
            state = FAIL;
            notifyAll();
        }

        public void onProcessInfo(String message) {
            System.out.println("PROCESS INFO: " + message);
        }
    }

}