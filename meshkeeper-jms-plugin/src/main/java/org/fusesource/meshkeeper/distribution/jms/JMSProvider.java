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
