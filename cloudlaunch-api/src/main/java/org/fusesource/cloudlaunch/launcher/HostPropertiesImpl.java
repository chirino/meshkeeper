/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.cloudlaunch.launcher;

import java.util.Properties;

import org.fusesource.cloudlaunch.HostProperties;


/**
 * HostProperties
 * <p>
 * Description:
 * </p>
 * 
 * @author cmacnaug
 * @version 1.0
 */
class HostPropertiesImpl implements HostProperties {

    private static final long serialVersionUID = 1L;
    public String os;
    public int numProcessors;
    public String defaultHostName;
    public String defaultExternalHostName;
    public String dataDirectory;
    private Properties systemProperties;
    private String agentId;

    
    void fillIn(LaunchAgent launcher) throws Exception
    {
        this.agentId = launcher.getAgentId();
        this.systemProperties = System.getProperties();
        this.dataDirectory = launcher.getDataDirectory().getCanonicalPath();
        defaultHostName = java.net.InetAddress.getLocalHost().getHostName();
        defaultExternalHostName = defaultHostName;
        this.numProcessors = Runtime.getRuntime().availableProcessors();
        this.os = System.getProperty("os.name");
    }
   
    
    public String getAgentId() {
        return agentId;
    }

    /**
     * Gets the os that the host is running.
     * 
     * @return The host's os.
     */
    public String getOS() {
        return os;
    }

    /**
     * @return Gets the number of processors on the host.
     */
    public int getNumProcessors() {
        return numProcessors;
    }

    /**
     * The default hostname
     * 
     * @return The hostname as seen by the host
     */
    public String getDefaultHostName() {
        return defaultHostName;
    }

    /**
     * The hostname externally accessible to by other hosts. If the host isn't
     * behind a firewall this will be the same as the hostname.
     * 
     * @return The hostname externally accessible to by other hosts
     */
    public String getExternalHostName() {
        return defaultExternalHostName;
    }

    /**
     * @return Returns a directory on the host that is free for tests to use.
     */
    public String getDataDirectory() {
        return dataDirectory;
    }

    public Properties getSystemProperties() {
        return systemProperties;
    }
}
