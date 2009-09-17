/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.meshkeeper.distribution.registry;

import org.fusesource.meshkeeper.MeshKeeper;
import org.fusesource.meshkeeper.distribution.PluginClient;

/** 
 * Registry
 * <p>
 * Description:
 * </p>
 * @author cmacnaug
 * @version 1.0
 */
public interface RegistryClient extends MeshKeeper.Registry, PluginClient{

    /**
     * Connects to the registry.
     * @throws Exception
     */
    public void start() throws Exception;
    
    /**
     * Disconnects from the registry and cleans up resources
     * held by the registry. 
     * @throws Exception
     */
    public void destroy() throws Exception;
}
