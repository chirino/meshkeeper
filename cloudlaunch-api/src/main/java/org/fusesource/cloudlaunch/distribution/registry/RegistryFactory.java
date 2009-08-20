/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.cloudlaunch.distribution.registry;

import java.net.URI;

import org.fusesource.cloudlaunch.util.internal.FactoryFinder;

/**
 * RegistryFactory
 * <p>
 * Description: Defines the interface for registry factories.
 * </p>
 * 
 * @author cmacnaug
 * @version 1.0
 */
public abstract class RegistryFactory {

    public static final FactoryFinder FACTORY_FINDER = new FactoryFinder("META-INF/services/org/fusesource/cloudlaunch/distribution/registry/");

    public static final Registry create(String uri) throws Exception {
        URI registryUri = new URI(uri);
        RegistryFactory rf = (RegistryFactory) RegistryFactory.FACTORY_FINDER.newInstance(registryUri.getScheme());
        return rf.createRegistry(uri);
    }

    protected abstract Registry createRegistry(String uri) throws Exception;
}
