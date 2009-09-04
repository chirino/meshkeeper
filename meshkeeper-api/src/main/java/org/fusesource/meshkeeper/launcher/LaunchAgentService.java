/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.meshkeeper.launcher;

import java.util.Collection;
import java.util.List;

import org.fusesource.meshkeeper.Distributable;
import org.fusesource.meshkeeper.HostProperties;
import org.fusesource.meshkeeper.LaunchDescription;
import org.fusesource.meshkeeper.Process;
import org.fusesource.meshkeeper.ProcessListener;
import org.fusesource.meshkeeper.classloader.Marshalled;

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
    
    /**
     * Request that the agent reserve the specified number of ports. 
     * 
     * @param count The number of porst
     * @return A list of free ports on the agent. 
     * @throws Exception If the specified number of ports could not be reserved. 
     */
    public List<Integer> reserveTcpPorts(int count) throws Exception;
    
    /**
     * Release a list of previously reserved ports.
     * @param ports The list of ports
     */
    public void releaseTcpPorts(Collection<Integer> ports);

    public Process launch(LaunchDescription launchDescription, ProcessListener handler) throws Exception;


    /**
     * Executes a runnable task in a new JVM. 
     *
     * @param runnable
     * @param handler
     * @return
     * @throws Exception
     */
    public Process launch(Marshalled<Runnable> runnable, ProcessListener handler) throws Exception;

    public HostProperties getHostProperties() throws Exception;
}
