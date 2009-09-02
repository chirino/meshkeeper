/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.cloudlaunch.launcher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.fusesource.cloudlaunch.Expression;
import org.fusesource.cloudlaunch.LaunchDescription;
import org.fusesource.cloudlaunch.LaunchTask;
import org.fusesource.cloudlaunch.Process;
import org.fusesource.cloudlaunch.ProcessListener;
import org.fusesource.cloudlaunch.util.internal.ProcessSupport;

import java.io.*;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @version $Revision: 1.1 $
 */
public class LocalProcess implements Process {

    Log log = LogFactory.getLog(this.getClass());
    int FD_STD_IN = 0;
    int FD_STD_OUT = 1;
    int FD_STD_ERR = 2;

    private final Object mutex = new Object();
    private final LaunchDescription ld;
    protected final ProcessListener listener;
    private final int pid;

    java.lang.Process process;
    private OutputStream os;

    AtomicBoolean running = new AtomicBoolean();
    private LaunchAgent processLauncher;

    public LocalProcess(LaunchAgent processLauncher, LaunchDescription ld, ProcessListener listener, int pid) {
        this.processLauncher = processLauncher;
        this.ld = ld;
        this.listener = listener;
        this.pid = pid;
    }

    public LaunchAgent getProcessLauncher() {
        return processLauncher;
    }

    public ProcessListener getListener() {
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

        // Evaluate the command...
        String[] cmd = new String[ld.getCommand().size()];
        StringBuilder command_line = new StringBuilder();
        boolean first = true;
        int i = 0;
        for (Expression expression : ld.getCommand()) {
            if (!first) {
                command_line.append(" ");
            }
            first = false;

            String arg = expression.evaluate(processLauncher.getHostProperties().getSystemProperties());
            cmd[i++] = arg;

            command_line.append('\'');
            command_line.append(arg);
            command_line.append('\'');
        }

        // Evaluate the enviorment...
        String[] env = null;
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
            workingDirectory = new File(processLauncher.getDataDirectory(), "pid-" + this.pid);
        }
        workingDirectory.mkdirs();

        //Generate the launch string
        String msg = "Launching as: " + command_line + " [pid = " + pid + "] [workDir = " + workingDirectory + "]";
        log.info(msg);
        listener.onProcessInfo(msg);

        //Launch:
        synchronized (mutex) {
            process = Runtime.getRuntime().exec(cmd, env, workingDirectory);
            if (process == null) {
                throw new Exception("Process launched failed (returned null).");
            }

            running.set(true);
            os = process.getOutputStream();
            ProcessSupport.watch(""+pid, process, new OutputHandler(FD_STD_OUT), new OutputHandler(FD_STD_ERR), new Runnable() {
                public void run() {
                    int exitValue = process.exitValue();
                    onExit(exitValue);
                }
            });
            
        }

    }

    protected void onExit(int exitValue) {
        running.set(false);
        listener.onProcessExit(exitValue);
        try {
            processLauncher.getDistributor().unexport(this);
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
            if( buffer.size() >= MAX_CHUNK_SIZE) {
                flush();
            }
        }

        @Override
        public void flush() throws IOException {
            if( buffer.size() > 0 ) {
                listener.onProcessOutput(fd, buffer.toByteArray());
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
