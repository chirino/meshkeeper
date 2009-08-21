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
import org.fusesource.cloudlaunch.control.ControlServiceFactory;
import org.fusesource.cloudlaunch.util.internal.FactoryFinder;
import org.fusesource.cloudlaunch.util.internal.URISupport;

/**
 * JMSServerFactory
 * <p>
 * Description:
 * </p>
 * 
 * @author cmacnaug
 * @version 1.0
 */
public class JMSServerFactory extends ControlServiceFactory {

    private static final FactoryFinder RESOURCE_FACTORY_FINDER = new FactoryFinder("META-INF/services/org/fusesource/cloudlaunch/distribution/jms/server/");

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.fusesource.cloudlaunch.control.ControlServiceFactory#createControlService
     * (java.lang.String)
     */
    @Override
    public final ControlService createControlService(URI providerUri) throws Exception {
        return ((JMSServerFactory) RESOURCE_FACTORY_FINDER.newInstance(providerUri.getScheme())).createJMSControlService(URISupport.stripScheme(providerUri));
    }

    protected ControlService createJMSControlService(URI uri) throws Exception {
        throw new UnsupportedOperationException();
    }
}
