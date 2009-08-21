/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.cloudlaunch.distribution.resource;

import java.net.URI;

import org.fusesource.cloudlaunch.util.internal.FactoryFinder;

/**
 * ResourceManagerFactory
 * <p>
 * Description: Interface for creating resource managers
 * </p>
 * 
 * @author cmacnaug
 * @version 1.0
 */
public abstract class ResourceManagerFactory {

    private static final FactoryFinder RESOURCE_FACTORY_FINDER = new FactoryFinder("META-INF/services/org/fusesource/cloudlaunch/distribution/resource/");

    public static final ResourceManager create(String uri) throws Exception {
        URI providerUri = new URI(uri);
        String scheme = providerUri.getScheme();
        if (scheme == null) {
            scheme = providerUri.getSchemeSpecificPart();
        }
        return ((ResourceManagerFactory) RESOURCE_FACTORY_FINDER.newInstance(scheme)).createResourceManager(uri);
    }

    protected abstract ResourceManager createResourceManager(String uri) throws Exception;

}
