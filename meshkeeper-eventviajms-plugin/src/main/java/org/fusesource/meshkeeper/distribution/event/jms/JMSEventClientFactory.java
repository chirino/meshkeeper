/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
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