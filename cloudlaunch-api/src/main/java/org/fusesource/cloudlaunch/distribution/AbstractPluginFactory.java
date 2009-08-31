/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.cloudlaunch.distribution;

import java.net.URI;

import org.fusesource.cloudlaunch.util.internal.FactoryFinder;

/**
 * AbstractPluginFactory
 * <p>
 * Description:
 * </p>
 * 
 * @author cmacnaug
 * @version 1.0
 */
public abstract class AbstractPluginFactory<P> {

    public final P create(String uri) throws Exception {
        URI providerUri = new URI(uri);
        String factoryName = providerUri.getScheme();
        String remaining = providerUri.getSchemeSpecificPart();
        if (factoryName == null) {
            factoryName = remaining;
            remaining = "";
        }
        AbstractPluginFactory<P> f = (AbstractPluginFactory<P>) getFactoryFinder().create(factoryName);
        return f.createPlugin(remaining);
    }

    protected abstract FactoryFinder getFactoryFinder();

    protected P createPlugin(String uri) throws Exception {
        throw new UnsupportedOperationException("Factory class must override createPlugin to return the plugin");
    }
}
