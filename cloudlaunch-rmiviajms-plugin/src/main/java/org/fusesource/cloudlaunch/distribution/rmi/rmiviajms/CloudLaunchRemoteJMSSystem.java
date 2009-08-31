/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.cloudlaunch.distribution.rmi.rmiviajms;

import java.net.URI;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;

import org.fusesource.cloudlaunch.control.ControlServer;
import org.fusesource.cloudlaunch.distribution.jms.JMSClientFactory;
import org.fusesource.cloudlaunch.distribution.jms.JMSProvider;
import org.fusesource.cloudlaunch.util.internal.URISupport;
import org.fusesource.rmiviajms.internal.JMSRemoteSystem;

/**
 * ActiveMQRemoteSystem
 * <p>
 * Description:
 * </p>
 * 
 * @author cmacnaug
 * @version 1.0
 */
public class CloudLaunchRemoteJMSSystem extends JMSRemoteSystem {

    public final static String QUEUE_PREFIX = System.getProperty("org.fusesource.rmiviajms.QUEUE_PREFIX", "rmiviajms.");
    static String PROVIDER_URI = ControlServer.DEFAULT_RMI_URI;
    private static JMSProvider provider;

    private static URI CONNECT_URI;

    static void initialize(String providerUri) throws Exception {
        PROVIDER_URI = providerUri;
        provider = new JMSClientFactory().create(PROVIDER_URI);
        CONNECT_URI = URISupport.stripScheme(new URI(PROVIDER_URI));
    }

    @Override
    protected ConnectionFactory createConnectionFactory() {
        try {
            return provider.createConnectionFactory(CONNECT_URI);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error creating rmi connection factory", e);
        }
    }

    @Override
    protected Destination createQueue(String systemId) {
        try {
            return provider.createQueue(QUEUE_PREFIX + systemId);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error creating rmi destination", e);
        }
    }

    public String toString() {
        return "CloudLaunchRemoteSystem at " + PROVIDER_URI;
    }
}
