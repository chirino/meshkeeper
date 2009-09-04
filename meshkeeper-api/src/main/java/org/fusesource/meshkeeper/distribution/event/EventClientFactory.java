/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.meshkeeper.distribution.event;

import org.fusesource.meshkeeper.distribution.AbstractPluginFactory;
import org.fusesource.meshkeeper.distribution.FactoryFinder;

/**
 * EventClientFactory
 * <p>
 * Description:
 * </p>
 * 
 * @author cmacnaug
 * @version 1.0
 */
public class EventClientFactory extends AbstractPluginFactory<EventClient> {

    private static final FactoryFinder FACTORY_FINDER = new FactoryFinder("META-INF/services/org/fusesource/meshkeeper/distribution/event/");

    @Override
    protected final FactoryFinder getFactoryFinder() {
        return FACTORY_FINDER;
    }
}
