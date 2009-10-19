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

import org.fusesource.meshkeeper.distribution.repository.AuthenticationInfo;
import org.fusesource.meshkeeper.distribution.repository.RepositoryClient;
import org.fusesource.meshkeeper.distribution.repository.RepositoryManagerFactory;

/**
 * WagonResourceManagerFactory
 * <p>
 * Description:
 * </p>
 * 
 * @author cmacnaug
 * @version 1.0
 */
public class WagonResourceManagerFactory extends RepositoryManagerFactory {

    String localRepoDir;
    String commonRepoUrl;
    AuthenticationInfo authInfo;

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.fusesource.meshkeeper.distribution.resource.ResourceManagerFactory
     * #createResourceManager()
     */
    @Override
    public RepositoryClient createPlugin(String uri) throws Exception {
        WagonResourceManager wrm = new WagonResourceManager();
        if (localRepoDir != null) {
            wrm.setLocalRepoDir(localRepoDir);
        }
        if (commonRepoUrl != null) {
            wrm.setCentralRepoUri(commonRepoUrl, authInfo);
        }
        return wrm;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.fusesource.meshkeeper.distribution.resource.ResourceManagerFactory
     * #setRepositoryUri(java.lang.String)
     */
    public void setCommonRepoUrl(String commonRepoUrl) {
        this.commonRepoUrl = commonRepoUrl;

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.fusesource.meshkeeper.distribution.resource.ResourceManagerFactory
     * #setLocalRepoDir(java.lang.String)
     */
    public void setCommonRepoAuthInfo(AuthenticationInfo authInfo) {
        this.authInfo = authInfo;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.fusesource.meshkeeper.distribution.resource.ResourceManagerFactory
     * #setLocalRepoDir(java.lang.String)
     */
    public void setLocalRepoDir(String localRepoDir) {
        this.localRepoDir = localRepoDir;
    }

}
