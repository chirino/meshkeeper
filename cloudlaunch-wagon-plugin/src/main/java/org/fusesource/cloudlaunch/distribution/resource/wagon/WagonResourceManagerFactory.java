/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.cloudlaunch.distribution.resource.wagon;

import org.fusesource.cloudlaunch.distribution.resource.AuthenticationInfo;
import org.fusesource.cloudlaunch.distribution.resource.ResourceManager;
import org.fusesource.cloudlaunch.distribution.resource.ResourceManagerFactory;

/**
 * WagonResourceManagerFactory
 * <p>
 * Description:
 * </p>
 * 
 * @author cmacnaug
 * @version 1.0
 */
public class WagonResourceManagerFactory extends ResourceManagerFactory {

    String localRepoDir;
    String commonRepoUrl;
    AuthenticationInfo authInfo;

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.fusesource.cloudlaunch.distribution.resource.ResourceManagerFactory
     * #createResourceManager()
     */
    @Override
    public ResourceManager createPlugin(String uri) throws Exception {
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
     * org.fusesource.cloudlaunch.distribution.resource.ResourceManagerFactory
     * #setCommonRepoUrl(java.lang.String)
     */
    public void setCommonRepoUrl(String commonRepoUrl) {
        this.commonRepoUrl = commonRepoUrl;

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.fusesource.cloudlaunch.distribution.resource.ResourceManagerFactory
     * #setLocalRepoDir(java.lang.String)
     */
    public void setCommonRepoAuthInfo(AuthenticationInfo authInfo) {
        this.authInfo = authInfo;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.fusesource.cloudlaunch.distribution.resource.ResourceManagerFactory
     * #setLocalRepoDir(java.lang.String)
     */
    public void setLocalRepoDir(String localRepoDir) {
        this.localRepoDir = localRepoDir;
    }

}
