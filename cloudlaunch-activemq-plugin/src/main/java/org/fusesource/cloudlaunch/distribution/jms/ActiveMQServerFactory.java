/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.cloudlaunch.distribution.jms;

import java.net.URI;
import java.net.URISyntaxException;

import org.fusesource.cloudlaunch.control.ControlService;


/** 
 * ActiveMQServerFactory
 * <p>
 * Description:
 * </p>
 * @author cmacnaug
 * @version 1.0
 */
public class ActiveMQServerFactory extends JMSServerFactory {

    /* (non-Javadoc)
     * @see org.fusesource.cloudlaunch.distribution.jms.JMSServerFactory#createJMSServerControlService(java.lang.String)
     */
    @Override
    public ControlService createPlugin(String providerUri) throws Exception {
        return ActiveMQControlService.create(new URI(providerUri));
    }
}
