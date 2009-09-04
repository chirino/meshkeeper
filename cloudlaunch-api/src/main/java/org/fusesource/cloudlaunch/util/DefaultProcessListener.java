/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.cloudlaunch.util;

import org.fusesource.cloudlaunch.Distributable;
import org.fusesource.cloudlaunch.ProcessListener;
import org.fusesource.cloudlaunch.Process;
import org.fusesource.cloudlaunch.Distributor;

import java.io.Serializable;
import java.io.ObjectStreamException;

/**
 * DefaultProcessListener
 * <p>
 * A default process listener. Applications can subclass this to customize
 * behavior
 * </p>
 * 
 * @author cmacnaug
 * @version 1.0
 */
public class DefaultProcessListener implements ProcessListener, Serializable {

    protected String name = "";
    private Distributable proxy;

    public DefaultProcessListener(Distributor distributor) throws Exception {
        proxy = distributor.export(this);
    }

    // When this object is serialized, we serialize the remote proxy.
    protected Object writeReplace() throws ObjectStreamException {
        return proxy;
    }

    public DefaultProcessListener(String name) {
        this.name = name;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.fusesource.cloudlaunch.ProcessListener#onProcessError(java.lang.Throwable
     * )
     */
    public void onProcessError(Throwable thrown) {
        System.err.println(format("ERROR: " + thrown.getStackTrace()));
        thrown.printStackTrace();

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.fusesource.cloudlaunch.ProcessListener#onProcessExit(int)
     */
    public void onProcessExit(int exitCode) {
        System.out.println(format("exited with " + exitCode));

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.fusesource.cloudlaunch.ProcessListener#onProcessInfo(java.lang.String
     * )
     */
    public void onProcessInfo(String message) {
        System.out.println(format(message));
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.fusesource.cloudlaunch.ProcessListener#onProcessOutput(int,
     * byte[])
     */
    public void onProcessOutput(int fd, byte[] output) {
        if (fd == Process.FD_STD_ERR) {
            System.err.println(format(new String(output)));
        } else {
            System.out.println(format(new String(output)));
        }
    }

    private String format(String s) {
        if (s.endsWith("\r\n")) {
            s = s.substring(0, s.length() - 2);
        } else if (s.endsWith("\n")) {
            s = s.substring(0, s.length() - 1);
        }

        if (name == null) {
            return s;
        } else {
            return name + ": " + s;
        }
    }

    public String toString() {
        return name;
    }

}
