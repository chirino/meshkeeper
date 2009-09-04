/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
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

import org.fusesource.meshkeeper.Event;
import org.fusesource.meshkeeper.EventListener;
import org.fusesource.meshkeeper.distribution.event.EventClient;
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
public class JMSEventClient implements EventClient {

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

    public synchronized void closeEventListener(EventListener listener, String topic) throws Exception {
        TopicHandler th = listeners.get(topic);
        if (th != null) {
            th.listeners.remove(listener);
            if (th.listeners.isEmpty()) {
                th.close();
            }
        }
    }

    public synchronized void openEventListener(EventListener listener, String topic) throws Exception {
        TopicHandler th = listeners.get(topic);
        if (th == null) {
            th = new TopicHandler(topic);
            listeners.put(topic, th);
        }
        th.addListener(listener);
    }

    public synchronized void sendEvent(Event event, String topic) throws Exception {

        sender.send(sendSession.createTopic(topicPrefix + topic), sendSession.createObjectMessage(event));
    }

    public synchronized void close() throws Exception {
        sendSession.close();
        listenerSession.close();
        listeners.clear();
        provider.releaseConnnection(connection, this);
    }

    private class TopicHandler implements MessageListener {

        private HashSet<EventListener> listeners = new HashSet<EventListener>(1);
        private final MessageConsumer consumer;

        TopicHandler(String topic) throws JMSException {
            consumer = listenerSession.createConsumer(listenerSession.createTopic(topicPrefix + topic));
            consumer.setMessageListener(this);
        }

        public synchronized void addListener(EventListener listener) {
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
            for(EventListener l : listeners)
            {
                Event event;
                try {
                    event = (Event) ((ObjectMessage)msg).getObject();
                    l.onEvent(event);
                } catch (JMSException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }
}
