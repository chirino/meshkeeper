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
package org.fusesource.meshkeeper.distribution.event.jms;

import java.net.URI;

import org.fusesource.meshkeeper.distribution.event.EventClient;
import org.fusesource.meshkeeper.distribution.event.EventClientFactory;
import org.fusesource.meshkeeper.distribution.jms.JMSClientFactory;
import org.fusesource.meshkeeper.distribution.jms.JMSProvider;
import org.fusesource.meshkeeper.util.internal.URISupport;

/**
 * JMSEventFactory
 * <p>
 * Description:
 * </p>
 *
 * @author cmacnaug
 * @version 1.0
 */
public class JMSEventClientFactory extends EventClientFactory {

    private static JMSProvider provider;

    /*
     * (non-Javadoc)
     *
     * @see
     * org.fusesource.meshkeeper.distribution.rmi.ExporterFactory#createExporter
     * (java.lang.String)
     */
    @Override
    protected EventClient createPlugin(String uri) throws Exception {
        URI providerUri = new URI(uri);
        getJMSProvider(providerUri);
        URI connectUri = URISupport.stripScheme(providerUri);
        return new JMSEventClient(provider, connectUri);
    }

    private static final JMSProvider getJMSProvider(URI providerUri) throws Exception {
        if (provider == null) {
            provider = new JMSClientFactory().create(providerUri.toString());
            ;
        }
        return provider;
    }
}
