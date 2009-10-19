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
