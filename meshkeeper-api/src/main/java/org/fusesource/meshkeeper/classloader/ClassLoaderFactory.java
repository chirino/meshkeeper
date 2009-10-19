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

import java.io.IOException;
import java.io.Serializable;
import java.io.File;

/**
 * The ClassLoaderFactory implementations are typically serialized and stored in the distributor registry
 * so that remote JVM can locate it and use it create the ClassLoader instance which will download
 * classes from the ClassLoaderServer.
 *
 * @author chirino
 */
public interface ClassLoaderFactory extends Serializable {

    /**
     * Creates a clasloader.  Typically this method blocks until all the remote class files
     * are downloaded locally.  The remote class files may be store in the specified cache directory.  
     *
     * @param parent
     * @param cacheDir
     * @return
     * @throws IOException
     * @throws Exception
     */
    public ClassLoader createClassLoader(ClassLoader parent, File cacheDir) throws IOException, Exception;
    
    /**
     * Returns the path at which this {@link ClassLoaderFactory} is registered.
     * @return the path at which this {@link ClassLoaderFactory} is registered.
     */
    public String getRegistryPath();
    
}