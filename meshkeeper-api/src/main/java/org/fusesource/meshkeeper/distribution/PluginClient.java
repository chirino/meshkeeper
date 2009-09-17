/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.meshkeeper.distribution;

/** 
 * PluginClient
 * <p>
 * Description:
 * </p>
 * @author cmacnaug
 * @version 1.0
 */
public interface PluginClient {

    /**
     * Sets the user class loader. Setting the user class loader assists meshkeeper
     * in resolving user's serialized objects. 
     * 
     * @param classLoader The user classloader.
     */
    public void setUserClassLoader(ClassLoader classLoader);
    
    /**
     * Gets the user class loader. Setting the user class loader assists meshkeeper
     * in resolving user's serialized objects. 
     * 
     * @return The user classloader.
     */
    public ClassLoader getUserClassLoader();
    
    /**
     * Starts the plugin. 
     * @throws Exception
     */
    public void start() throws Exception;
    
    /**
     * Destroys the plugin. 
     * @throws Exception
     */
    public void destroy() throws Exception;
}
