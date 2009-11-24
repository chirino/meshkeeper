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

        if (!uri.toString().contains("failover")) {
            return new ActiveMQConnectionFactory("failover:(" + uri.toString() + ")");
        } else {
            return new ActiveMQConnectionFactory(uri);
        }

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
