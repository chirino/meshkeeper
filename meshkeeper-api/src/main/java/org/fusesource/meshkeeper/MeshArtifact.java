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
package org.fusesource.meshkeeper;

import java.io.Serializable;

import org.fusesource.meshkeeper.MeshKeeper.Repository;

/**
 * Encapsulates a Distributable test artifact for use with the
 * {@link Repository} api.
 * 
 * @author cmacnaug
 * @version 1.0
 */
public interface MeshArtifact extends Serializable {

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
    public String getRepositoryPath();

    /**
     * @param id
     *            the id to set
     */
    public void setRepositoryPath(String id);

    /**
     * @return the repoUrl
     */
    public String getRepositoryUri();

    /**
     * @param repoUrl
     *            the repoUrl to set
     */
    public void setRepositoryUri(String repoUrl);

    /**
     * @return the repoName
     */
    public String getRepositoryId();

    /**
     * @param repoName
     *            the repoName to set
     */
    public void setRepositoryId(String repoName);

    /**
     * @return the type
     */
    public short getType();

    /**
     * Sets the type of resource. If directory is specified then the agent will
     * recursively pull down the contents of the directory.
     * 
     * @param type
     *            the type to set
     */
    public void setType(short type);

    /**
     * Sets the local path of the resource.
     * 
     * @param resolvedPath
     */
    public void setLocalPath(String resolvedPath);

    /**
     * @return the local path of the resource.
     */
    public String getLocalPath();
}
