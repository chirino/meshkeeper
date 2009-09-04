/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.meshkeeper;

import java.io.Serializable;
import java.util.Properties;

/** 
 * HostProperties
 * <p>
 * Description:
 * </p>
 * @author cmacnaug
 * @version 1.0
 */
public interface HostProperties extends Serializable {

    
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
    public String getDataDirectory();
    
    public Properties getSystemProperties();
}
