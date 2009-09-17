/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.meshkeeper;

/** 
 * MeshContainer
 * <p>
 * Description:
 * </p>
 * @author cmacnaug
 * @version 1.0
 */
public interface MeshContainer extends MeshProcess {

    public Distributable host(String name, Distributable object) throws Exception;
    
    public void unhost(String name);
    
    public void run(Runnable r) throws Exception;
}
