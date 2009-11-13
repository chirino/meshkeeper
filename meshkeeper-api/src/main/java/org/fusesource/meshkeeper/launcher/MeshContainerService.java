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

import java.io.Serializable;

import org.fusesource.meshkeeper.Distributable;

/**
 * MeshContainerService
 * <p>
 * Description:
 * </p>
 * 
 * @author cmacnaug
 * @version 1.0
 */
public interface MeshContainerService extends Distributable {



    /**
     * Hosts the given object in the specified container using the given name as
     * an id.
     * 
     * @param <T>
     * @param name
     *            The name at which to host the object.
     * @param object
     *            The object
     * @param interfaces
     *            The interfaces that should be exposed by the object.
     * @return A proxy to the object exported by the container.
     * @throws Exception
     *             If there is an error exporting the object to the container.
     */
    public <T extends Serializable> T host(String name, T object, Class<?>... interfaces) throws Exception;

    /**
     * Unhosts an object previously exported to the container with the given
     * name.
     */
    public void unhost(String name) throws Exception;

    /**
     * Runs the {@link Runnable} in the container. The {@link Runnable} must
     * also implement {@link Serializable}.
     * 
     * @param r
     *            The {@link Runnable}
     * @throws Exception
     */
    public <R extends java.lang.Runnable & Serializable> void run(R r) throws Exception;

    /**
     * Invokes the {@link Callable} in the container. The {@link Callable} must
     * also implement {@link Serializable}.
     * 
     * @param <T>
     * @param c
     *            The {@link Callable}
     * @return The result
     * @throws Exception
     *             If there is an exception
     */
    public <T, C extends java.util.concurrent.Callable<T> & Serializable> T call(C c) throws Exception;

    /**
     * Closes the container.
     */
    public void close();

}
