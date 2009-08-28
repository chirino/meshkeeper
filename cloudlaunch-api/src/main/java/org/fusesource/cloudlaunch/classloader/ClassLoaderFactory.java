/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.cloudlaunch.classloader;

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
    
}