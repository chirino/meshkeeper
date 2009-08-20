/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.cloudlaunch;

import org.fusesource.cloudlaunch.launcher.LocalProcess;

/**
 * @author chirino
 */
public interface LaunchTask {
    public void execute(LocalProcess process) throws Exception;
}