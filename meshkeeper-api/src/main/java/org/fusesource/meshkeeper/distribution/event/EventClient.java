/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.meshkeeper.distribution.event;

import org.fusesource.meshkeeper.MeshEvent;
import org.fusesource.meshkeeper.MeshEventListener;

/** 
 * EventClient
 * <p>
 * Description:
 * </p>
 * @author cmacnaug
 * @version 1.0
 */
public interface EventClient {
    
    public void sendEvent(MeshEvent event, String topic) throws Exception;
    
    public void openEventListener(MeshEventListener listener, String topic) throws Exception;
    
    public void closeEventListener(MeshEventListener listener, String topic) throws Exception;
    
    public void close() throws Exception;
}
