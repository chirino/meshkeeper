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

import org.fusesource.meshkeeper.distribution.registry.RegistryClient;
import org.fusesource.meshkeeper.distribution.registry.RegistryFactory;
import org.fusesource.meshkeeper.util.internal.URISupport;

/**
 * @author chirino
 */
public class ZooKeeperFactory extends RegistryFactory {

    @Override
    public RegistryClient createPlugin(String uri) throws Exception {
        URI connectUri = new URI(URISupport.stripPrefix(uri, "zk:"));

        ZooKeeperRegistry registry = new ZooKeeperRegistry();
        applyQueryParameters(registry, connectUri);
        
        registry.setConnectUrl(connectUri.toString());
        registry.start();
        return registry;

    }

}
