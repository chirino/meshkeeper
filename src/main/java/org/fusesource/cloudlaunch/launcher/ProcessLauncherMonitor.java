/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.cloudlaunch.launcher;

import java.io.File;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.fusesource.cloudlaunch.util.internal.FileUtils;

/**
 * @version $Revision: 1.1 $
*/
public class ProcessLauncherMonitor implements Runnable {

    Log log = LogFactory.getLog(this.getClass());
    private final ProcessLauncher processLauncher;

    Thread thread;
    private String tempDirectory;
    private boolean cleanupRequested = false;

    public ProcessLauncherMonitor(ProcessLauncher processLauncher) {
        this.processLauncher = processLauncher;
    }

    public void start() {
        tempDirectory = processLauncher.getDataDirectory() + File.separator + processLauncher.getAgentId() + File.separator + "temp";
        thread = new Thread(this, processLauncher.getAgentId() + "-Process Monitor");
        thread.start();
    }

    public void stop() {
        thread.interrupt();
        try {
            thread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void run() {
        while (true) {
            synchronized (this) {
                try {
                    wait(ProcessLauncher.CLEANUP_TIMEOUT);
                } catch (InterruptedException ie) {
                    cleanupRequested = true;
                    return;
                } finally {
                    processLauncher.checkForRogueProcesses(15000);
                    if (cleanupRequested) {
                        cleanUpTempFiles();
                        cleanupRequested = false;
                    }
                }
            }

        }
    }


    public void cleanUpTempFiles() {
        //If we aren't running anything cleanup: temp parts
        Map<Integer, LocalProcess> processes = processLauncher.getProcesses();
        if (processes == null || processes.size() == 0) {
            File tempDir = new File(tempDirectory);
            String[] subDirs = tempDir != null ? tempDir.list() : null;

            log.debug("*************Cleaning up temporary parts*************");
            for (int i = 0; subDirs != null && i < subDirs.length; i++) {
                try {
                    FileUtils.recursiveDelete(tempDir + File.separator + subDirs[i]);
                } catch (Exception e) {
                    log.warn("ERROR cleaning up temporary parts:", e);
                }
            }
        }
    }

    /**
     * Requests cleanup of temporary files
     */
    public synchronized void requestCleanup() {
        cleanupRequested = true;
        notify();
    }

    public String getTempDirectory() {
        return tempDirectory;
    }



    

}
