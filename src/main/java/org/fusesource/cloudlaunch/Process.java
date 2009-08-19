/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.cloudlaunch;

import java.io.IOException;

import org.fusesource.cloudlaunch.distribution.Distributable;
import org.fusesource.cloudlaunch.distribution.Oneway;

/** 
 * Process
 * <p>
 * Description:
 * </p>
 * @author cmacnaug
 * @version 1.0
 */
public interface Process extends Distributable {
    
    int FD_STD_IN = 0;
    int FD_STD_OUT = 1;
    int FD_STD_ERR = 2;
    
    public boolean isRunning() throws Exception;
    
    public void kill() throws Exception;
    
    public void open(int fd) throws IOException;

    public void write(int fd, byte[] data) throws IOException;

    public void close(int fd) throws IOException;
}
