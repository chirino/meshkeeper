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
package org.fusesource.meshkeeper.distribution.repository;

import org.fusesource.meshkeeper.MeshKeeper;
import org.fusesource.meshkeeper.distribution.PluginClient;

/**
 * RepositoryManager
 * <p>
 * Description:
 * </p>
 * 
 * @author cmacnaug
 * @version 1.0
 */
public interface RepositoryClient extends MeshKeeper.Repository, PluginClient{

    /**
     * Set the location of the central repository
     * @param url The url to central repository. 
     */
    public void setCentralRepoUri(String url, AuthenticationInfo authInfo) throws Exception ;

    /**
     * Set the location of the local repository
     */
    public void setLocalRepoDir(String directory) throws Exception ;
    
}
