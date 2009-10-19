/**
 *  Copyright (C) 2009 Progress Software, Inc. All rights reserved.
 *  http://fusesource.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.fusesource.meshkeeper.distribution.repository.wagon;

import org.fusesource.meshkeeper.MeshArtifact;

/** 
 * WagonResource
 * <p>
 * Description:
 * </p>
 * @author cmacnaug
 * @version 1.0
 */
public class WagonResource implements MeshArtifact{

    private static final long serialVersionUID = 1L;
    public static final short FILE = 0;
    public static final short DIRECTORY = 1;
    
    private short type = DIRECTORY;
    private String repositoryPath;
    private String repositoryUri;
    private String repositoryId = "common";
    
    //Resolved by a resource manager to a local file system
    //repositoryPath after the resource is downloaded.
    private transient String resolvedPath;

    /**
     * @return the path
     */
    public String getRepositoryPath() {
        return repositoryPath;
    }

    /**
     * @param repositoryPath
     *            the id to set
     */
    public void setRepositoryPath(String repositoryPath) {
        this.repositoryPath = repositoryPath;
    }

    /**
     * @return the repoUrl
     */
    public String getRepositoryUri() {
        return repositoryUri;
    }

    /**
     * @param repositoryUri
     *            the repoUrl to set
     */
    public void setRepositoryUri(String repositoryUri) {
        this.repositoryUri = repositoryUri;
    }

    /**
     * @return the repoName
     */
    public String getRepositoryId() {
        return repositoryId;
    }

    /**
     * @param repositoryId
     *            the repoName to set
     */
    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
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
