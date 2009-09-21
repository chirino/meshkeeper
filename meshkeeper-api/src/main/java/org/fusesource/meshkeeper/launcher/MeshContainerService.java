/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.meshkeeper.launcher;

import org.fusesource.meshkeeper.Distributable;

/** 
 * MeshContainerService
 * <p>
 * Description:
 * </p>
 * @author cmacnaug
 * @version 1.0
 */
public interface MeshContainerService extends Distributable {

    public <T> T host(String name, T object, Class<?> ... interfaces) throws Exception ;

    public void unhost(String name) throws Exception ;

    public void run(Runnable r) throws Exception ;

    public void close() ;
    
}
