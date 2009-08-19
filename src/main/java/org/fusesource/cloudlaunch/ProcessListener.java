/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.cloudlaunch;

import org.fusesource.cloudlaunch.distribution.Distributable;
import org.fusesource.cloudlaunch.distribution.Oneway;

/**
 * @author chirino
 */
public interface ProcessListener extends Distributable{

    @Oneway
    public void onProcessExit(int exitCode);

    @Oneway
    public void onProcessError(Throwable thrown);

    @Oneway
    public void onProcessInfo(String message);
    
    @Oneway
    public void onProcessOutput(int fd, byte [] output);

}
