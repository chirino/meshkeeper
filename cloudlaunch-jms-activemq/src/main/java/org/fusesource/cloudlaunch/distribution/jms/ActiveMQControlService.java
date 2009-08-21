/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.cloudlaunch.distribution.jms;

import java.net.URI;

import org.apache.activemq.broker.BrokerFactory;
import org.apache.activemq.broker.BrokerService;
import org.fusesource.cloudlaunch.control.ControlService;
import org.fusesource.cloudlaunch.util.internal.URISupport;

/**
 * ActiveMQControlService
 * <p>
 * Description:
 * </p>
 * 
 * @author cmacnaug
 * @version 1.0
 */
public class ActiveMQControlService implements ControlService {

    BrokerService controlBroker;
    String serviceUrl;

    /*
     * (non-Javadoc)
     * 
     * @see org.fusesource.cloudlaunch.control.ControlService#start()
     */
    public void start() throws Exception {

        controlBroker.start();
        controlBroker.getTransportConnectorURIs();
        serviceUrl = "activemq:" + controlBroker.getTransportConnectorURIs()[0];
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.fusesource.cloudlaunch.control.ControlService#destroy()
     */
    public void destroy() throws Exception {
        if (controlBroker != null) {
            controlBroker.stop();
            controlBroker.waitUntilStopped();
            controlBroker = null;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.fusesource.cloudlaunch.control.ControlService#getName()
     */
    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.fusesource.cloudlaunch.control.ControlService#getServiceUri()
     */
    public String getServiceUri() {
        return serviceUrl;
    }

    static ActiveMQControlService create(String uri) throws Exception {
        URI connectUri = new URI(URISupport.stripPrefix(uri, "activemq:"));
        ActiveMQControlService rc = new ActiveMQControlService();
        rc.controlBroker = BrokerFactory.createBroker(connectUri);
        return rc;
    }
}
