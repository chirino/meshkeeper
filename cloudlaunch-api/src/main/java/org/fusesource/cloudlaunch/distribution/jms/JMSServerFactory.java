/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.cloudlaunch.distribution.jms;

import java.net.URI;

import org.fusesource.cloudlaunch.control.ControlService;
import org.fusesource.cloudlaunch.util.internal.FactoryFinder;

/** 
 * JMSServerFactory
 * <p>
 * Description:
 * </p>
 * @author cmacnaug
 * @version 1.0
 */
public abstract class JMSServerFactory {

    private static final FactoryFinder RESOURCE_FACTORY_FINDER = new FactoryFinder("META-INF/services/org/fusesource/cloudlaunch/distribution/jms/server/");

    
    public static final ControlService create(String uri) throws Exception {
        URI providerUri = new URI(uri);
        return ((JMSServerFactory)RESOURCE_FACTORY_FINDER.newInstance(providerUri.getScheme())).createConnectionFactory(uri);
    }
    
    protected abstract ControlService createConnectionFactory(String uri) throws Exception;
}
