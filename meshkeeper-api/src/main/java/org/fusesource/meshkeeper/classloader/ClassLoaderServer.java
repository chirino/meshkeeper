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
package org.fusesource.meshkeeper.classloader;

import java.io.File;
import java.util.List;

import org.fusesource.meshkeeper.Distributable;

/**
 * A ClassLoaderServer allows to create ClassLoaderFactory objects which will
 * can be used by remote JVMs to download the remote classes from this server.
 *
 * @author chirino
 */
public interface ClassLoaderServer extends Distributable {

    /**
     * Exposes the specified classloader so it can be downloaded remotely.
     *
     * @param classLoader
     * @param registryPath the path at which to register the factory.
     * @param maxExportDepth controls how many parent class loaders are also exported.
     * @return
     */
    ClassLoaderFactory export(ClassLoader classLoader, String registryPath, int maxExportDepth) throws Exception;

    /**
     * Exports the file list as the classpath used by a ClassLoaderFactory.
     *
     * 
     * @param classPath
     * @param registryPath the path at which to register the factory.
     * @return
     */
    ClassLoaderFactory export(List<File> classPath, String registryPath) throws Exception;

    /**
     * Must be called before remote clients can access remotely exposed
     * class loaders.
     *
     * @throws Exception
     */
    public void start() throws Exception;

    /**
     * Must be called to stop exposing the remote class loaders.
     *
     * @throws Exception
     */
    public void stop() throws Exception;

}