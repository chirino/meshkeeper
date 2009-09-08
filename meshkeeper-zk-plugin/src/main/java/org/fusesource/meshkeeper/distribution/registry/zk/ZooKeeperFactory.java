/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.meshkeeper.distribution.registry.zk;

import org.fusesource.meshkeeper.distribution.registry.RegistryClient;
import org.fusesource.meshkeeper.distribution.registry.RegistryFactory;
import org.fusesource.meshkeeper.util.internal.IntrospectionSupport;
import org.fusesource.meshkeeper.util.internal.URISupport;

import java.net.URI;
import java.util.Map;

/**
 * @author chirino
 */
public class ZooKeeperFactory extends RegistryFactory {

    @Override
    public RegistryClient createPlugin(String uri) throws Exception {
        URI connectUri = new URI(URISupport.stripPrefix(uri, "zk:"));

        ZooKeeperRegistry registry = new ZooKeeperRegistry();

        //Use query params to initialize the factory:
        Map<String, String> props = URISupport.parseParamters(connectUri);
        if (!props.isEmpty()) {
            IntrospectionSupport.setProperties(registry, URISupport.parseQuery(uri));
            connectUri = URISupport.removeQuery(new URI(uri));
            connectUri = URISupport.createRemainingURI(connectUri, props);
        }
        registry.setConnectUrl(connectUri.toString());
        registry.start();
        return registry;

    }

}
