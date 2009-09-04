/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.meshkeeper.distribution.jms;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;

/**
 * JMSProvider
 * <p>
 * Description:
 * </p>
 * 
 * @author cmacnaug
 * @version 1.0
 */
public abstract class JMSProvider {

    private HashMap<String, SharedConnection> connectionByHostPort = new HashMap<String, SharedConnection>();
    private HashMap<Connection, SharedConnection> connections = new HashMap<Connection, SharedConnection>();

    public abstract ConnectionFactory createConnectionFactory(URI uri);

    public abstract Destination createQueue(String name);
    
    public abstract Destination createTopic(String name);
    
    public synchronized Connection getConnection(Object ref, URI uri) throws JMSException {
        String hostPort = uri.getHost() + ":" + uri.getPort();

        SharedConnection sc = connectionByHostPort.get(hostPort);
        if (sc == null) {
            sc = new SharedConnection();
            sc.hostPort = hostPort;
            sc.connection = createConnectionFactory(uri).createConnection();
            sc.connection.start();
            connectionByHostPort.put(hostPort, sc);
            connections.put(sc.connection, sc);
        }

        return sc.connection;
    }

    public synchronized void releaseConnnection(Connection c, Object ref) {

        SharedConnection sc = connections.get(c);
        if (sc == null) {
            return;
        }

        sc.users.remove(ref);
        if (sc.users.isEmpty()) {
            connections.remove(c);
            connectionByHostPort.remove(sc.hostPort);
        }

        try {
            sc.connection.close();
        } catch (Exception e) {

        }

    }

    private class SharedConnection {
        String hostPort;
        Connection connection;
        HashSet<Object> users = new HashSet<Object>(1);
    }

}
