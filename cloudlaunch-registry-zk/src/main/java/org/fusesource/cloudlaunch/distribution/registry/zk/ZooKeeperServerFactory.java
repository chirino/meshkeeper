/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.cloudlaunch.distribution.registry.zk;

import java.net.URI;
import java.util.Map;

import org.fusesource.cloudlaunch.control.ControlService;
import org.fusesource.cloudlaunch.control.ControlServiceFactory;
import org.fusesource.cloudlaunch.util.internal.IntrospectionSupport;
import org.fusesource.cloudlaunch.util.internal.URISupport;

/** 
 * ZooKeeperServerFactory
 * <p>
 * Description:
 * </p>
 * @author cmacnaug
 * @version 1.0
 */
public class ZooKeeperServerFactory extends ControlServiceFactory {

    @Override
    protected ControlService createControlService(String uri) throws Exception {
        URI connectUri = new URI(URISupport.stripPrefix(uri, "zk:"));

        ZooKeeperServer server = new ZooKeeperServer();
        server.setPort(connectUri.getPort());

        //Use query params to initialize the server:
        Map<String, String> props = URISupport.parseParamters(connectUri);
        if (!props.isEmpty()) {
            IntrospectionSupport.setProperties(server, URISupport.parseQuery(uri));
            connectUri = URISupport.removeQuery(new URI(uri));
            connectUri = URISupport.createRemainingURI(connectUri, props);
        }
        return server;
    }    
}