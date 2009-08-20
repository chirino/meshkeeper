/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.cloudlaunch.distribution.event.jms;

import java.net.URI;

import org.fusesource.cloudlaunch.distribution.event.EventClient;
import org.fusesource.cloudlaunch.distribution.event.EventClientFactory;
import org.fusesource.cloudlaunch.util.internal.URISupport;

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

    public static final String JMS_PROVIDER_CLASS = System.getProperty("org.fusesource.distribution.event.jms.PROVIDER_CLASS", "org.fusesource.cloudlaunch.distribution.event.jms.ActiveMQProvider");

    private static JMSProvider provider;

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.fusesource.cloudlaunch.distribution.rmi.ExporterFactory#createExporter
     * (java.lang.String)
     */
    protected EventClient createEventClient(String uri) throws Exception {
        URI connectUri = new URI(URISupport.stripPrefix(uri, "jms:"));
        return new JMSEventClient(getJMSProvider(), connectUri);
    }

    private static final JMSProvider getJMSProvider() throws Exception {
        if (provider == null) {
            provider = (JMSProvider) JMSProvider.class.getClassLoader().loadClass(JMS_PROVIDER_CLASS).newInstance();
        }
        return provider;
    }
}
