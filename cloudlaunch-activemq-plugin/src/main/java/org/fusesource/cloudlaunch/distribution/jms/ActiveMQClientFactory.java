/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.cloudlaunch.distribution.jms;

/** 
 * ActiveMQClientFactory
 * <p>
 * Description:
 * </p>
 * @author cmacnaug
 * @version 1.0
 */
public class ActiveMQClientFactory extends JMSClientFactory{

    /* (non-Javadoc)
     * @see org.fusesource.cloudlaunch.distribution.jms.JMSClientFactory#createConnectionFactory(java.lang.String)
     */
    @Override
    protected JMSProvider createPlugin(String uri) throws Exception {
        return new ActiveMQProvider();
    }

}
