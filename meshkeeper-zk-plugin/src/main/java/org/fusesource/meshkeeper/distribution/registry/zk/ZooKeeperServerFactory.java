/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.meshkeeper.distribution.registry.zk;

import java.net.URI;

import org.fusesource.meshkeeper.control.ControlService;
import org.fusesource.meshkeeper.control.ControlServiceFactory;

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
    protected ControlService createPlugin(String uri) throws Exception {

        URI connectUri = new URI(uri);
        ZooKeeperServer server = new ZooKeeperServer();
        server.setPort(connectUri.getPort());
        server.setPurge(true);
        applyQueryParameters(server, connectUri);
        return server;
    }    
}
