/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.cloudlaunch.launcher;

import org.fusesource.cloudlaunch.HostProperties;
import org.fusesource.cloudlaunch.LaunchDescription;
import org.fusesource.cloudlaunch.Process;
import org.fusesource.cloudlaunch.ProcessListener;
import org.fusesource.cloudlaunch.distribution.Distributable;

/** 
 * ProcessLauncher
 * <p>
 * Description:
 * </p>
 * @author cmacnaug
 * @version 1.0
 */
public interface LaunchAgentService extends Distributable {

    /**
     * Specifies the registry prefix where IRemoteProcess launchers
     * will be published for discovery. 
     */
    public static final String REGISTRY_PATH = "/launchers";
    
    public void bind(String owner) throws Exception;

    public void unbind(String owner) throws Exception;

    public Process launch(LaunchDescription launchDescription, ProcessListener handler) throws Exception;

    public HostProperties getHostProperties() throws Exception;
}
