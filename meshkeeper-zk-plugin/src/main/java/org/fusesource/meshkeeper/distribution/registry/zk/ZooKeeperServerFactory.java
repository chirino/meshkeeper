/**
 *  Copyright (C) 2009 Progress Software, Inc. All rights reserved.
 *  http://fusesource.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
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
