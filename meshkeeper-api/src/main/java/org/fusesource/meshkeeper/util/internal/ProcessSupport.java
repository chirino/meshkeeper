/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.meshkeeper.util.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.CountDownLatch;

/**
 * A helper class for handling Process objects.
 * 
 * @version $Revision: 1.1 $
 */
public class ProcessSupport {

    static final private AtomicLong ID_GENERATOR = new AtomicLong(0);

    static private String getNextAnonymousId() {
        return "anonymous:"+ID_GENERATOR.incrementAndGet();
    }

    static public String caputure(final Process process) throws InterruptedException {
        return caputure(getNextAnonymousId(), process);
    }

    /**
     * @param id
     * @param process
     * @return null if the process exits with a non zero return code
     * @throws InterruptedException
     */
    static  public String caputure(String id, final Process process) throws InterruptedException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
        if( system(id, process, out, null) != 0 ) {
            return null;
        }
        return out.toString();
    }

    static public int system(final Process process) throws InterruptedException {
        return system(getNextAnonymousId(), process);
    }

    static public int system(String id, final Process process) throws InterruptedException {
        return system(id, process, System.out, System.err);
    }

    static public int system(String id, final Process process, OutputStream sout, OutputStream serr) throws InterruptedException {
        final CountDownLatch done = new CountDownLatch(1);
        watch(id, process, sout, serr, new Runnable() {
            public void run() {
                done.countDown();
            }
        });
        done.await();
        return process.exitValue();
    }

    static public void watch(String id, final Process process, OutputStream out, OutputStream err, final Runnable onExit) {

        final StreamPump errorHandler = new StreamPump("Error Pump for Process: " + id, process.getErrorStream(), err);
        errorHandler.start();
        final StreamPump outputHandler = new StreamPump("Output Pump for Process: " + id, process.getInputStream(), out);
        outputHandler.start();

        if( onExit!=null ) {
            new Thread("Exit Watcher for Process: " + id) {
                @Override
                public void run() {
                    try {
                        process.waitFor();
                        // Prior to sending exit event, join the output
                        // handler threads to make sure that all
                        // data is sent has sent.
                        errorHandler.join();
                        outputHandler.join();
                        onExit.run();
                    } catch (InterruptedException e) {
                    }
                }
            }.start();
        }
    }

    static private class StreamPump extends Thread {
        private final InputStream in;
        private OutputStream out;

        public StreamPump(String name, InputStream in, OutputStream out) {
            super(name);
            this.in = in;
            this.out = out;
        }

        public void run() {
            try {
                int count;
                byte buffer[] = new byte[1204];
                while ( (count=in.read(buffer,0,buffer.length)) >= 0) {
                    if( out!=null ) {
                        out.write(buffer, 0, count);
                        if( count < buffer.length ) {
                            out.flush();
                        }
                    }
                }
            } catch (IOException expected) {
            } finally {
                try {
                    in.close();
                } catch (Throwable e) {
                }
                try {
                    out.close();
                } catch (Throwable e) {
                }
            }
        }
    }

}
