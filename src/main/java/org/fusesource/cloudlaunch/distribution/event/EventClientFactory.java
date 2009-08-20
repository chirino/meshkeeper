/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.cloudlaunch.distribution.event;

import java.net.URI;

import org.fusesource.cloudlaunch.util.internal.FactoryFinder;

/**
 * EventClientFactory
 * <p>
 * Description:
 * </p>
 * 
 * @author cmacnaug
 * @version 1.0
 */
public abstract class EventClientFactory {

    private static final FactoryFinder FACTORY_FINDER = new FactoryFinder("META-INF/services/org/fusesource/cloudlaunch/distribution/event/");

    public static final EventClient create(String uri) throws Exception {
        URI eventUri = new URI(uri);
        EventClientFactory ecf = (EventClientFactory) EventClientFactory.FACTORY_FINDER.newInstance(eventUri.getScheme());
        return ecf.createEventClient(uri);
    }

    protected abstract EventClient createEventClient(String uri) throws Exception;

}
