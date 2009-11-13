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

import org.fusesource.meshkeeper.launcher.MeshContainerService;

/**
 * MeshContainer
 * <p>
 * A MeshContainer is launched on a remote machine and is a convenient way to
 * distribute objects on a remote machine.
 * </p>
 * 
 * @author cmacnaug
 * @version 1.0
 */
public interface MeshContainer extends MeshProcess, MeshContainerService {

    /**
     * Extension of {@link java.util.concurrent.Callable} that extends
     * {@link Serializable}
     * <p>
     * This class enforces the requirement that {@link Callable} object be
     * {@link Serializable}
     * </p>
     * 
     * @author cmacnaug
     * @version 1.0
     * @param <R> The return type of the {@link Callable}
     */
    public interface Callable<R extends Serializable> extends java.util.concurrent.Callable<R>, Serializable {
    };

    /**
     * Extension of {@link java.lang.Runnable} that extends
     * {@link java.lang.Runnable} and {@link Serializable}
     * <p>
     * This class enforces the requirement that {@link Runnable} object be
     * {@link Serializable}
     * </p>
     * 
     * @author cmacnaug
     * @version 1.0
     */
    public interface Runnable extends java.lang.Runnable, Serializable {
    }

    /**
     * Any object that is hosted in a {@link MeshContainer} can optionally
     * implement {@link Hostable} to assist in getting {@link MeshContainer}
     * context.
     * 
     * @author cmacnaug
     * @version 1.0
     */
    public interface Hostable extends Serializable {
        /**
         * Called when a {@link Hostable} object is created in a
         * {@link MeshContainer}
         * 
         * @param context The {@link MeshContainerContext}
         * 
         * @throws Exception if there is an error in the {@link Hostable}'s
         *             initialize call, such an exception will cause the hosting
         *             operation to fail.
         */
        public void initialize(MeshContainerContext context) throws Exception;

        /**
         * Called when a {@link Hostable} object is removed from a
         * {@link MeshContainer}
         * 
         * @param context The {@link MeshContainerContext}
         * @throws Exception If there is an error. 
         */
        public void destroy(MeshContainerContext context) throws Exception;
    }

    /**
     * {@link Hostable} objects are passed a {@link MeshContainerContext} when
     * they are hosted in the container.
     * <p>
     * </p>
     * 
     * @author cmacnaug
     * @version 1.0
     */
    public interface MeshContainerContext {

        /**
         * @return The container's {@link MeshKeeper}
         */
        public MeshKeeper getContainerMeshKeeper();

        /**
         * @return the name given to the {@link MeshContainer}
         */
        public String getContainerName();
    }

}
