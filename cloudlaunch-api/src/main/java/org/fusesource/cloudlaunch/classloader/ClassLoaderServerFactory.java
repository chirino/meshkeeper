/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.cloudlaunch.classloader;

import org.fusesource.cloudlaunch.util.internal.URISupport;
import org.fusesource.cloudlaunch.distribution.Distributor;
import org.fusesource.cloudlaunch.distribution.DistributorFactory;
import org.fusesource.cloudlaunch.distribution.FactoryFinder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;

/**
 * ClassLoaderServerFactory provies an extenisble way to create custom ClassLoaderServer implementations.
 *
 * @author chirino
 * @version 1.0
 */
public abstract class ClassLoaderServerFactory {

    private static final Log LOG = LogFactory.getLog(ClassLoaderServerFactory.class);
    private static final FactoryFinder FACTORY_FINDER = new FactoryFinder("META-INF/services/org/fusesource/cloudlaunch/classloader/");

    public static final ClassLoaderServer create(String uri, Distributor distributor) throws Exception {
        String parts[] = URISupport.extractScheme(uri);
        ClassLoaderServerFactory factory = FACTORY_FINDER.create(parts[0]);
        return factory.createClassLoaderManager(parts[1], distributor);
    }

    protected abstract ClassLoaderServer createClassLoaderManager(String uri, Distributor distributor) throws Exception;

    /**
     * Creates a ClassLoader which loads classes from a remote location.  It accesses
     * the remote ClassLoaderServer by looking up one of it's ClassLoaderFactory objects in
     * the distributor registry which is addressed by <code>distributorUri</code> and <code>path</code>.
     * <br/>
     * Remote class files will be cahced in the specified cache directory.  The created classloader will
     * have the specified parent class loader.
     *
     * @param distributorUri
     * @param path
     * @param cacheDir
     * @param parent
     * @return
     * @throws Exception
     */
    public static ClassLoader createClassLoader(String distributorUri, String path, File cacheDir, ClassLoader parent) throws Exception {
        LOG.debug("Looking up classloader: "+distributorUri+" @ "+path);
        DistributorFactory.setDefaultRegistryUri(distributorUri);
        Distributor d = DistributorFactory.createDefaultDistributor();
        ClassLoaderFactory clf = d.getRegistry().getObject(path);
        return clf.createClassLoader(parent, cacheDir);
    }

}