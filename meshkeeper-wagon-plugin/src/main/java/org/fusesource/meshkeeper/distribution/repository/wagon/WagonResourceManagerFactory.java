/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
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
            wrm.setCommonRepoUrl(commonRepoUrl, authInfo);
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
