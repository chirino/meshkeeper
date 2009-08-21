/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.cloudlaunch.control;

import java.net.URI;

import org.fusesource.cloudlaunch.util.internal.FactoryFinder;

/**
 * ControlServiceFactory
 * <p>
 * Description:
 * </p>
 * 
 * @author cmacnaug
 * @version 1.0
 */
public abstract class ControlServiceFactory {

    private static final FactoryFinder RESOURCE_FACTORY_FINDER = new FactoryFinder("META-INF/services/org/fusesource/cloudlaunch/distribution/control/");

    public static final ControlService create(String uri) throws Exception {
        URI providerUri = new URI(uri);
        return ((ControlServiceFactory)RESOURCE_FACTORY_FINDER.newInstance(providerUri.getScheme())).createControlService(uri);
    }

    protected abstract ControlService createControlService(String uri) throws Exception;
}
