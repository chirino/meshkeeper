/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.meshkeeper.distribution.registry;

import java.util.Collection;
import java.util.concurrent.TimeoutException;

import org.fusesource.meshkeeper.distribution.AbstractPluginClient;

/** 
 * AbstractRegistryClient
 * <p>
 * Description:
 * </p>
 * @author cmacnaug
 * @version 1.0
 */
public abstract class AbstractRegistryClient extends AbstractPluginClient implements RegistryClient{

    
    /**
     * Convenience method that waits for a minimum number of objects to be registered at the given
     * registry path.
     *
     * @param <T>
     * @param path The path
     * @param min The minimum number of objects to wait for.
     * @param timeout The maximum amount of time to wait.
     * @return The objects that were registered.
     * @throws Exception
     */
    public <T> Collection<T> waitForRegistrations(String path, int min, long timeout) throws TimeoutException, Exception
    {
        return RegistryHelper.waitForRegistrations(this, path, min, timeout);
    }
    
    /**
     * Convenience method that waits for a registry path to be created.
     *
     * @param <T>
     * @param path The path
     * @param timeout The maximum amount of time to wait.
     * @return The 
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public <T> T waitForRegistration(String path, long timeout) throws TimeoutException, Exception
    {
        return (T) RegistryHelper.waitForRegistration(this, path, timeout);
    }
}
