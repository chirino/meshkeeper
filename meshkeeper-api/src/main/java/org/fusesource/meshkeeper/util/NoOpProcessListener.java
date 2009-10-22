/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.meshkeeper.util;

import org.fusesource.meshkeeper.MeshProcessListener;

/** 
 * NoOpProcessListener
 * <p>
 * A No-Op process listener which oes nothing with process output/info. This class 
 * can be useful for subclassing.
 * </p>
 * @author cmacnaug
 * @version 1.0
 */
public class NoOpProcessListener implements MeshProcessListener{

    /* (non-Javadoc)
     * @see org.fusesource.meshkeeper.MeshProcessListener#onProcessError(java.lang.Throwable)
     */
    public void onProcessError(Throwable thrown) {
        // NOOP!
    }

    /* (non-Javadoc)
     * @see org.fusesource.meshkeeper.MeshProcessListener#onProcessExit(int)
     */
    public void onProcessExit(int exitCode) {
     // NOOP!
    }

    /* (non-Javadoc)
     * @see org.fusesource.meshkeeper.MeshProcessListener#onProcessInfo(java.lang.String)
     */
    public void onProcessInfo(String message) {
     // NOOP!
    }

    /* (non-Javadoc)
     * @see org.fusesource.meshkeeper.MeshProcessListener#onProcessOutput(int, byte[])
     */
    public void onProcessOutput(int fd, byte[] output) {
        // NOOP!
    }

}
