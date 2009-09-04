/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.meshkeeper.distribution.resource.wagon;

import org.fusesource.meshkeeper.Resource;

/** 
 * WagonResource
 * <p>
 * Description:
 * </p>
 * @author cmacnaug
 * @version 1.0
 */
public class WagonResource implements Resource{

    private static final long serialVersionUID = 1L;
    public static final short FILE = 0;
    public static final short DIRECTORY = 1;
    
    private short type = DIRECTORY;
    private String repoPath;
    private String repoUrl;
    private String repoName = "common";
    
    //Resolved by a resource manager to a local file system
    //path after the resource is downloaded.
    private transient String resolvedPath;

    /**
     * @return the id
     */
    public String getRepoPath() {
        return repoPath;
    }

    /**
     * @param id
     *            the id to set
     */
    public void setRepoPath(String id) {
        this.repoPath = id;
    }

    /**
     * @return the repoUrl
     */
    public String getRepoUrl() {
        return repoUrl;
    }

    /**
     * @param repoUrl
     *            the repoUrl to set
     */
    public void setRepoUrl(String repoUrl) {
        this.repoUrl = repoUrl;
    }

    /**
     * @return the repoName
     */
    public String getRepoName() {
        return repoName;
    }

    /**
     * @param repoName
     *            the repoName to set
     */
    public void setRepoName(String repoName) {
        this.repoName = repoName;
    }
    
    /**
     * @return the type
     */
    public short getType() {
        return type;
    }

    /**
     * Sets the type of resource. If directory is 
     * specified then the agent will recursively pull
     * down the contents of the directory.
     * 
     * @param type the type to set
     */
    public void setType(short type) {
        this.type = type;
    }

    /* (non-Javadoc)
     * @see org.fusesource.meshkeeper.distribution.resource.Resource#getLocalPath()
     */
    public String getLocalPath() {
        return resolvedPath;
    }

    /* (non-Javadoc)
     * @see org.fusesource.meshkeeper.distribution.resource.Resource#setLocalPath(java.lang.String)
     */
    public void setLocalPath(String resolvedPath) {
        this.resolvedPath = resolvedPath;
    }
}
