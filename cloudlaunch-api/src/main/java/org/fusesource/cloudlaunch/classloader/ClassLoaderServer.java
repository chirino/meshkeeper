/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.cloudlaunch.classloader;

import java.io.IOException;
import java.io.File;
import java.util.List;

/**
 * A ClassLoaderServer allows to create ClassLoaderFactory objects which will
 * can be used by remote JVMs to download the remote classes from this server.
 *
 * @author chirino
 */
public interface ClassLoaderServer {

    /**
     * Exposes the specified classloader so it can be downloaded remotely.
     *
     * @param classLoader
     * @param maxExportDepth controls how many parent class loaders are also exported.
     * @return
     */
    ClassLoaderFactory export(ClassLoader classLoader, int maxExportDepth) throws IOException;

    /**
     * Exports the file list as the classpath used by a ClassLoaderFactory.
     *
     *
     * @param classPath
     * @return
     */
    ClassLoaderFactory export(List<File> classPath) throws IOException;

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