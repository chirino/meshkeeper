/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.cloudlaunch.distribution.resource;

import java.io.File;
import java.io.IOException;

/**
 * RepositoryManager
 * <p>
 * Description:
 * </p>
 * 
 * @author cmacnaug
 * @version 1.0
 */
public interface ResourceManager {

    /**
     * Factory method for creating a resource.
     * @return An empty resource. 
     */
    public Resource createResource();
   
    /**
     * Called to locate the given resource. 
     * @param resource The resource to locate.
     * @throws Exception If there is an error locating the resource.
     */
    public void locateResource(Resource resource) throws Exception;

    /**
     * @param resource
     * @param data
     * @throws IOException
     */
    public void deployFile(Resource resource, byte[] data) throws Exception;

    /**
     * 
     * @param resource
     * @param d
     * @throws Exception
     */
    public void deployDirectory(Resource resource, File d) throws Exception;

    /**
     * 
     * @throws IOException
     */
    public void purgeLocalRepo() throws IOException;

    /**
     * Closes all repository connections.
     * 
     * @throws Exception
     */
    public void close();

    /**
     * @return The path to the local resource directory.
     */
    public File getLocalRepoDirectory();

}
