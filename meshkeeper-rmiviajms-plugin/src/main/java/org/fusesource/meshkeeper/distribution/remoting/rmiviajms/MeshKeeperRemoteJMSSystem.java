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
package org.fusesource.meshkeeper.distribution.remoting.rmiviajms;

import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;

import org.fusesource.meshkeeper.control.ControlServer;
import org.fusesource.meshkeeper.distribution.jms.JMSClientFactory;
import org.fusesource.meshkeeper.distribution.jms.JMSProvider;
import org.fusesource.meshkeeper.util.internal.URISupport;
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
public class MeshKeeperRemoteJMSSystem extends JMSRemoteSystem {

    public final static String QUEUE_PREFIX = System.getProperty("org.fusesource.rmiviajms.QUEUE_PREFIX", "rmiviajms.");
    static String PROVIDER_URI = ControlServer.DEFAULT_REMOTING_URI;
    private static JMSProvider provider;

    private static URI CONNECT_URI;

    private static AtomicInteger remoteSysRefCount = new AtomicInteger(0);

    public static void addRef() {
        remoteSysRefCount.incrementAndGet();
    }

    public static void removeRef() {
        if (remoteSysRefCount.decrementAndGet() == 0) {
            try {
                MeshKeeperRemoteJMSSystem.INSTANCE.reset();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

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
    
    @Override
    protected Destination createTopic(String systemId) {
        try {
            return provider.createTopic(QUEUE_PREFIX + systemId);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error creating rmi destination", e);
        }
    }

    public String toString() {
        return "MeshKeeperRemoteSystem at " + PROVIDER_URI;
    }
}
