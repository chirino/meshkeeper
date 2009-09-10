/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.meshkeeper.distribution.remoting;

import org.fusesource.meshkeeper.Distributable;

/** 
 * IExporter
 * <p>
 * Description:
 * </p>
 * @author cmacnaug
 * @version 1.0
 */
public interface RemotingClient {

    public <T extends Distributable> T export(Distributable obj) throws Exception;
    
    public void unexport(Distributable obj) throws Exception;
    
    public void destroy() throws Exception;
    
}