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
package org.fusesource.meshkeeper;

import java.io.Serializable;
import java.util.Properties;

import org.fusesource.meshkeeper.MeshKeeper.Launcher;

/** 
 * Holds properties for a given launch agent in the mesh. 
 * <p>
 * This interface returns properties for a launch agent in the mesh, 
 * {@link HostProperties} can be obtained by calling {@link Launcher#getAvailableAgents()}
 * </p>
 * @author cmacnaug
 * @version 1.0
 */
public interface HostProperties extends Serializable {

    /**
     * Gets the agentId used to represent this agent. This is passed
     * to {@link Launcher} when launching a process to identify the 
     * host on which the process should be launched. 
     * @return The id used to indetify the agent. 
     */
    public String getAgentId();

    /**
     * Gets the os that the host is running.
     * 
     * @return The host's os.
     */
    public String getOS();

    /**
     * @return Gets the number of processors on the host.
     */
    public int getNumProcessors();
    /**
     * The default hostname
     * 
     * @return The hostname as seen by the host
     */
    public String getDefaultHostName();

    /**
     * The hostname externally accessible to by other hosts. If the host isn't
     * behind a firewall this will be the same as the hostname.
     * 
     * @return The hostname externally accessible to by other hosts
     */
    public String getExternalHostName();

    /**
     * @return Returns a directory on the host that is free for tests to use.
     */
    public String getDirectory();
    
    /**
     * @return The agent's system properties. 
     */
    public Properties getSystemProperties();
}
