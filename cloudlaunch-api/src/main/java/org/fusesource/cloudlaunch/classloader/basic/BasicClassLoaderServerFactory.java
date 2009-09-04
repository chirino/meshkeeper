/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.cloudlaunch.classloader.basic;

import org.fusesource.cloudlaunch.classloader.ClassLoaderServerFactory;
import org.fusesource.cloudlaunch.classloader.ClassLoaderServer;
import org.fusesource.cloudlaunch.Distributor;

/**
 * @author chirino
 */
public class BasicClassLoaderServerFactory extends ClassLoaderServerFactory {
    protected ClassLoaderServer createClassLoaderManager(String uri, Distributor distributor) throws Exception {
        return new BasicClassLoaderServer(distributor);
    }
}