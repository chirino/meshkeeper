/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.meshkeeper.distribution;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import org.fusesource.meshkeeper.util.internal.IntrospectionSupport;
import org.fusesource.meshkeeper.util.internal.URISupport;

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

    @SuppressWarnings("unchecked")
    public final P create(String uri) throws Exception {
        URI providerUri = new URI(uri);
        String factoryName = providerUri.getScheme();
        String remaining = providerUri.getSchemeSpecificPart();
        if (factoryName == null) {
            factoryName = remaining;
            remaining = "";
        }
        
        AbstractPluginFactory<P> f = (AbstractPluginFactory<P>) getFactoryFinder().create(factoryName);
        
        //Create the plugin using PluginClassLoader
        ClassLoader orig = Thread.currentThread().getContextClassLoader();
        PluginClassLoader pcl = PluginClassLoader.getContextPluginLoader();
        Thread.currentThread().setContextClassLoader(pcl);
        try {
            return f.createPlugin(remaining);
        } finally {
            Thread.currentThread().setContextClassLoader(orig);
        }
    }

    protected abstract FactoryFinder getFactoryFinder();

    protected P createPlugin(String uri) throws Exception {
        throw new UnsupportedOperationException("Factory class must override createPlugin to return the plugin");
    }
    
    /**
     * Parses query parameters and attempts to apply them to specified object. Applied
     * parameters are removed from the returned uri.
     * 
     * @param target The target object to apply parameters to.
     * @param connectUri The connect uris from which to parse parameters.
     * @return The original uri with paratemeters removed.
     * @throws URISyntaxException 
     */
    protected static URI applyQueryParameters(Object target, URI connectUri) throws URISyntaxException
    {
        Map<String, String> props = URISupport.parseParamters(connectUri);
        if (!props.isEmpty()) {
            IntrospectionSupport.setProperties(target, props);
            connectUri = URISupport.removeQuery(connectUri);
            connectUri = URISupport.createRemainingURI(connectUri, props);
        }
        return connectUri;
    }
}
