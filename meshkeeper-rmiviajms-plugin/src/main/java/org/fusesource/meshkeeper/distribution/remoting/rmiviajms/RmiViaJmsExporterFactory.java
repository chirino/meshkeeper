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
package org.fusesource.meshkeeper.distribution.remoting.rmiviajms;

import java.net.URI;
import java.util.Map;

import org.fusesource.meshkeeper.distribution.remoting.RemotingFactory;
import org.fusesource.meshkeeper.distribution.remoting.RemotingClient;
import org.fusesource.meshkeeper.util.internal.IntrospectionSupport;
import org.fusesource.meshkeeper.util.internal.URISupport;

/**
 * RMIViaJmsExporterFactory
 * <p>
 * Description:
 * </p>
 * 
 * @author cmacnaug
 * @version 1.0
 */
public class RmiViaJmsExporterFactory extends RemotingFactory {

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.fusesource.meshkeeper.distribution.rmi.ExporterFactory#createExporter
     * (java.lang.String)
     */
    @Override
    public RemotingClient createPlugin(String uri) throws Exception {

        URI connectUri = new URI(URISupport.stripPrefix(uri, "rmiviajms:"));

        RmiViaJmsExporter exporter = new RmiViaJmsExporter();
        applyQueryParameters(exporter, connectUri);
        exporter.setProviderUri(connectUri.toString());
        exporter.start();
        return exporter;
    }

}
