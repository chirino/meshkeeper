/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.meshkeeper.distribution.repository;

import org.fusesource.meshkeeper.MeshKeeper;

/**
 * RepositoryManager
 * <p>
 * Description:
 * </p>
 * 
 * @author cmacnaug
 * @version 1.0
 */
public interface RepositoryManager extends MeshKeeper.Repository{

    /**
     * Set the location of the common repo
     * @param url The url to common repo. 
     */
    public void setCommonRepoUrl(String url, AuthenticationInfo authInfo) throws Exception ;

    /**
     * Set the location of the local repository
     */
    public void setLocalRepoDir(String directory) throws Exception ;
    
    /**
     * Closes all repository connections.
     * 
     * @throws Exception
     */
    public void close();
}
