package org.fusesource.cloudlaunch.distribution.jms;

import java.net.URI;

import javax.jms.ConnectionFactory;

import org.fusesource.cloudlaunch.util.internal.FactoryFinder;

/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/

/**
 * JMSClientFactory
 * <p>
 * Description:
 * </p>
 * 
 * @author cmacnaug
 * @version 1.0
 */
public abstract class JMSClientFactory {

    private static final FactoryFinder RESOURCE_FACTORY_FINDER = new FactoryFinder("META-INF/services/org/fusesource/cloudlaunch/distribution/jms/client");

    
    public static final ConnectionFactory create(String uri) throws Exception {
        URI providerUri = new URI(uri);
        return ((JMSClientFactory)RESOURCE_FACTORY_FINDER.newInstance(providerUri.getScheme())).createConnectionFactory(uri);
    }
    
    protected abstract ConnectionFactory createConnectionFactory(String uri) throws Exception;
}
