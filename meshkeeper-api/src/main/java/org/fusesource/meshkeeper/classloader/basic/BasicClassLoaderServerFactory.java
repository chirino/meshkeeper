/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.meshkeeper.classloader.basic;

import org.fusesource.meshkeeper.MeshKeeper;
import org.fusesource.meshkeeper.classloader.ClassLoaderServer;
import org.fusesource.meshkeeper.classloader.ClassLoaderServerFactory;

/**
 * @author chirino
 */
public class BasicClassLoaderServerFactory extends ClassLoaderServerFactory {
    protected ClassLoaderServer createClassLoaderManager(String uri, MeshKeeper meshKeeper) throws Exception {
        return new BasicClassLoaderServer(meshKeeper);
    }
}