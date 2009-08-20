/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.cloudlaunch.distribution.resource;

/**
 * ResourceManagerFactory
 * <p>
 * Description: Interface for creating resource managers
 * </p>
 * 
 * @author cmacnaug
 * @version 1.0
 */
public interface ResourceManagerFactory {

    public ResourceManager createResourceManager() throws Exception;

    public void setCommonRepoUrl(String url);

    public void setLocalRepoDir(String directory);
}
