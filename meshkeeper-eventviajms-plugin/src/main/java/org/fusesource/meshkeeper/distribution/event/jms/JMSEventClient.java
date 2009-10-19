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
import java.util.HashMap;
import java.util.HashSet;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;

import org.fusesource.meshkeeper.MeshEvent;
import org.fusesource.meshkeeper.MeshEventListener;
import org.fusesource.meshkeeper.distribution.event.AbstractEventClient;
import org.fusesource.meshkeeper.distribution.jms.JMSProvider;

/**
 * JMSEventClient
 * <p>
 * Description:
 * </p>
 * 
 * @author cmacnaug
 * @version 1.0
 */
public class JMSEventClient extends AbstractEventClient {

    private static final String topicPrefix = "clevent.";

    private final JMSProvider provider;
    private final Connection connection;
    private final Session sendSession;
    private final MessageProducer sender;
    private final Session listenerSession;

    private final HashMap<String, TopicHandler> listeners = new HashMap<String, TopicHandler>();

    JMSEventClient(JMSProvider provider, URI uri) throws JMSException {
        this.provider = provider;
        this.connection = provider.getConnection(this, uri);
        this.sendSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        this.sender = sendSession.createProducer(null);
        sender.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
        sender.setTimeToLive(120000);
        this.listenerSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
    }

    public synchronized void closeEventListener(MeshEventListener listener, String topic) throws Exception {
        TopicHandler th = listeners.get(topic);
        if (th != null) {
            th.listeners.remove(listener);
            if (th.listeners.isEmpty()) {
                th.close();
            }
        }
    }

    public synchronized void openEventListener(MeshEventListener listener, String topic) throws Exception {
        TopicHandler th = listeners.get(topic);
        if (th == null) {
            th = new TopicHandler(topic);
            listeners.put(topic, th);
        }
        th.addListener(listener);
    }

    public synchronized void sendEvent(MeshEvent event, String topic) throws Exception {

        sender.send(sendSession.createTopic(topicPrefix + topic), sendSession.createObjectMessage(event));
    }

    public void start() {
        //No-Op
    }

    public synchronized void destroy() throws Exception {
        sendSession.close();
        listenerSession.close();
        listeners.clear();
        provider.releaseConnnection(connection, this);
    }

    private class TopicHandler implements MessageListener {

        private HashSet<MeshEventListener> listeners = new HashSet<MeshEventListener>(1);
        private final MessageConsumer consumer;

        TopicHandler(String topic) throws JMSException {
            consumer = listenerSession.createConsumer(listenerSession.createTopic(topicPrefix + topic));
            consumer.setMessageListener(this);
        }

        public synchronized void addListener(MeshEventListener listener) {
            listeners.add(listener);
        }

        public synchronized void close() throws JMSException {
            consumer.close();
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.jms.MessageListener#onMessage(javax.jms.Message)
         */
        public synchronized void onMessage(Message msg) {
            for (MeshEventListener l : listeners) {
                MeshEvent event;
                try {
                    event = (MeshEvent) ((ObjectMessage) msg).getObject();
                    l.onEvent(event);
                } catch (JMSException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }
}
