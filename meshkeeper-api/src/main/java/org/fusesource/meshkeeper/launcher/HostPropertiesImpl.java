/**
 *  Copyright (C) 2009 Progress Software, Inc. All rights reserved.
 *  http://fusesource.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.fusesource.meshkeeper.launcher;

import java.util.Properties;

import org.fusesource.meshkeeper.HostProperties;


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
    public String directory;
    private Properties systemProperties;
    private String agentId;


    void fillIn(LaunchAgent launcher) throws Exception
    {
        this.agentId = launcher.getAgentId();
        this.systemProperties = System.getProperties();
        this.directory = launcher.getDirectory().getCanonicalPath();
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
    public String getDirectory() {
        return directory;
    }

    public Properties getSystemProperties() {
        return systemProperties;
    }
}
