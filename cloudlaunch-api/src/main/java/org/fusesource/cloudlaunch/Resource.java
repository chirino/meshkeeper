/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.cloudlaunch;

import java.io.Serializable;

/**
 * Resource
 * <p>
 * Description:
 * </p>
 * 
 * @author cmacnaug
 * @version 1.0
 */
public interface Resource extends Serializable {

    /**
     * Constant indicating that the resource is a single file.
     */
    public static final short FILE = 0;
    
    /**
     * Constant indicating the resrouce is a directory. 
     */
    public static final short DIRECTORY = 1;
    
    /**
     * @return the id
     */
    public String getRepoPath();

    /**
     * @param id
     *            the id to set
     */
    public void setRepoPath(String id);

    /**
     * @return the repoUrl
     */
    public String getRepoUrl();

    /**
     * @param repoUrl
     *            the repoUrl to set
     */
    public void setRepoUrl(String repoUrl);

    /**
     * @return the repoName
     */
    public String getRepoName();

    /**
     * @param repoName
     *            the repoName to set
     */
    public void setRepoName(String repoName);
    
    /**
     * @return the type
     */
    public short getType();

    /**
     * Sets the type of resource. If directory is 
     * specified then the agent will recursively pull
     * down the contents of the directory.
     * 
     * @param type the type to set
     */
    public void setType(short type);

    /**
     * Sets the local path of the resource. 
     * @param resolvedPath
     */
    public void setLocalPath(String resolvedPath);

    /**
     * @return the local path of the resource. 
     */
    public String getLocalPath();
}
