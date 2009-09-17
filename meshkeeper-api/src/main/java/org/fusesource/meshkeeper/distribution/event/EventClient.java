/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.meshkeeper.distribution.event;

import org.fusesource.meshkeeper.MeshKeeper;

/** 
 * EventClient
 * <p>
 * Description:
 * </p>
 * @author cmacnaug
 * @version 1.0
 */
public interface EventClient extends MeshKeeper.Eventing{
    
    public void close() throws Exception;
}
