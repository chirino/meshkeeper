/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.meshkeeper.distribution.jms;

import java.net.URI;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQTopic;
import org.fusesource.meshkeeper.distribution.jms.JMSProvider;

/**
 * ActiveMQProvider
 * <p>
 * Description:
 * </p>
 *
 * @author cmacnaug
 * @version 1.0
 */
public class ActiveMQProvider extends JMSProvider {

    public ConnectionFactory createConnectionFactory(URI uri) {
        return new ActiveMQConnectionFactory(uri);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.fusesource.meshkeeper.distribution.jms.JMSProvider#createQueue(java
     * .lang.String)
     */
    @Override
    public Destination createQueue(String name) {
        return new ActiveMQQueue(name);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.fusesource.meshkeeper.distribution.jms.JMSProvider#createTopic(java
     * .lang.String)
     */
    @Override
    public Destination createTopic(String name) {
        return new ActiveMQTopic(name);
    }

}
