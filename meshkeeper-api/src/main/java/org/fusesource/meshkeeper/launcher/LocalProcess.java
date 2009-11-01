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
package org.fusesource.meshkeeper.launcher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.fusesource.meshkeeper.Expression;
import org.fusesource.meshkeeper.LaunchDescription;
import org.fusesource.meshkeeper.LaunchTask;
import org.fusesource.meshkeeper.MeshProcess;
import org.fusesource.meshkeeper.MeshProcessListener;
import org.fusesource.meshkeeper.util.internal.ProcessSupport;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @version $Revision: 1.1 $
 */
public class LocalProcess implements MeshProcess {

    Log log = LogFactory.getLog(this.getClass());
    int FD_STD_IN = 0;
    int FD_STD_OUT = 1;
    int FD_STD_ERR = 2;

    private final Object mutex = new Object();
    private final LaunchDescription ld;
    protected final MeshProcessListener listener;
    private final int pid;

    java.lang.Process process;
    private OutputStream os;

    AtomicBoolean running = new AtomicBoolean();
    private LaunchAgent processLauncher;
    Properties processProperties;

    public LocalProcess(LaunchAgent processLauncher, LaunchDescription ld, MeshProcessListener listener, int pid) {
        this.processLauncher = processLauncher;
        this.ld = ld;
        this.listener = listener;
        this.pid = pid;
        this.processProperties = new Properties(processLauncher.getHostProperties().getSystemProperties());
    }

    public Properties getProcessProperties() {
        return processProperties;
    }

    public LaunchAgent getProcessLauncher() {
        return processLauncher;
    }

    public MeshProcessListener getListener() {
        return listener;
    }

    /**
     * Launches the process.
     */
    public void start() throws Exception {
        if (ld.getCommand().isEmpty()) {
            throw new Exception("LaunchDescription command empty.");
        }

        // Resolve resources (copy them locally:
        for (LaunchTask task : ld.getPreLaunchTasks()) {
            task.execute(this);
        }

        if (log.isDebugEnabled()) {
            log.debug("Evaluating launch command with properties: " + processProperties);
        }

        // Evaluate the command...
        List<String> cmdList = new ArrayList<String>(ld.getCommand().size());
        StringBuilder command_line = new StringBuilder();
        boolean first = true;
        for (Expression expression : ld.getCommand()) {

            String arg = expression.evaluate(processProperties);

            //Skip empty args
            if (arg == null || arg.length() == 0) {
                continue;
            }

            if (!first) {
                command_line.append(" ");
            }
            first = false;

            cmdList.add(arg);

            command_line.append('\'');
            command_line.append(arg);
            command_line.append('\'');
        }
        
        String[] cmdArray = cmdList.toArray(new String [] {});
        
        // Evaluate the enviorment...
        String[] env = null;
        int i = 0;
        if (ld.getEnvironment() != null) {
            env = new String[ld.getEnvironment().size()];
            i = 0;
            for (Map.Entry<String, Expression> entry : ld.getEnvironment().entrySet()) {
                env[i++] = entry.getKey() + "=" + entry.getValue().evaluate();
            }
        }

        File workingDirectory;
        if (ld.getWorkingDirectory() != null) {
            workingDirectory = new File(ld.getWorkingDirectory().evaluate());
        } else {
            workingDirectory = new File(processLauncher.getDirectory(), "pid-" + this.pid);
        }
        workingDirectory.mkdirs();

        //Generate the launch string
        String msg = "Launching as: " + command_line + " [pid = " + pid + "] [workDir = " + workingDirectory + "]";
        log.info(msg);
        if (listener != null) {
            listener.onProcessInfo(msg);
        }

        //Launch:
        synchronized (mutex) {
            process = Runtime.getRuntime().exec(cmdArray, env, workingDirectory);
            if (process == null) {
                throw new Exception("Process launched failed (returned null).");
            }

            running.set(true);
            os = process.getOutputStream();
            ProcessSupport.watch("" + pid, process, new OutputHandler(FD_STD_OUT), new OutputHandler(FD_STD_ERR), new Runnable() {
                public void run() {
                    int exitValue = process.exitValue();
                    onExit(exitValue);
                }
            });

        }

    }

    protected void onExit(int exitValue) {
        running.set(false);
        if (listener != null) {
            listener.onProcessExit(exitValue);
        }
        try {
            processLauncher.getMeshKeeper().remoting().unexport(this);
        } catch (Exception e) {

        }
    }

    public boolean isRunning() {
        synchronized (mutex) {
            return process != null;
        }
    }

    public void kill() throws Exception {
        if (running.compareAndSet(true, false)) {
            try {
                log.info("Killing process " + process + " [pid = " + pid + "]");
                process.destroy();
                process.waitFor();
                log.info("Killed process " + process + " [pid = " + pid + "]");

            } catch (Exception e) {
                log.error("ERROR: destroying process " + process + " [pid = " + pid + "]");
                throw e;
            }
        }
    }

    public void open(int fd) throws IOException {
        if (fd != FD_STD_IN) {
            throw new IOException("Only IRemoteProcessLauncher.FD_STD_IN is supported");
        }
    }

    public void write(int fd, byte[] data) throws IOException {
        if (fd != FD_STD_IN) {
            return;
        }
        os.write(data);
        os.flush();
    }

    public void close(int fd) {
        if (fd != FD_STD_IN) {
            return;
        }
        try {
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // handle output or error data
    private class OutputHandler extends OutputStream {
        private final int fd;
        private final static int MAX_CHUNK_SIZE = 8 * 1024;
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream(MAX_CHUNK_SIZE);

        public OutputHandler(int fd) {
            this.fd = fd;
        }

        @Override
        public void write(int b) throws IOException {
            buffer.write(b);
            if (buffer.size() >= MAX_CHUNK_SIZE) {
                flush();
            }
        }

        @Override
        public void flush() throws IOException {
            if (buffer.size() > 0) {
                if (listener != null) {
                    listener.onProcessOutput(fd, buffer.toByteArray());
                }
                buffer.reset();
            }
        }

        @Override
        public void close() throws IOException {
            flush();
            super.close();
        }
    }
}
