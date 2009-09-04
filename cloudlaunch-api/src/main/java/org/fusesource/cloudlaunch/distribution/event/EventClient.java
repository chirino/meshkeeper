/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.cloudlaunch.distribution.event;

import org.fusesource.cloudlaunch.Event;
import org.fusesource.cloudlaunch.EventListener;

/** 
 * EventClient
 * <p>
 * Description:
 * </p>
 * @author cmacnaug
 * @version 1.0
 */
public interface EventClient {
    
    public void sendEvent(Event event, String topic) throws Exception;
    
    public void openEventListener(EventListener listener, String topic) throws Exception;
    
    public void closeEventListener(EventListener listener, String topic) throws Exception;
    
    public void close() throws Exception;
}
