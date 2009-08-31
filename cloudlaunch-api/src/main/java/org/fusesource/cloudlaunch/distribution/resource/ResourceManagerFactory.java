/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.cloudlaunch.distribution.resource;

import org.fusesource.cloudlaunch.distribution.AbstractPluginFactory;
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
public class ResourceManagerFactory extends AbstractPluginFactory<ResourceManager> {

    private static final FactoryFinder RESOURCE_FACTORY_FINDER = new FactoryFinder("META-INF/services/org/fusesource/cloudlaunch/distribution/resource/");

    @Override
    protected final FactoryFinder getFactoryFinder() {
        return RESOURCE_FACTORY_FINDER;
    }

}
