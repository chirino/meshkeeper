/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.cloudlaunch.distribution.resource.wagon;

import java.io.File;

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
public class WagonResourceManagerFactory implements ResourceManagerFactory {

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
    public ResourceManager createResourceManager() throws Exception {
        WagonResourceManager wrm = new WagonResourceManager();
        wrm.setLocalRepoDir(new File(localRepoDir));
        wrm.setCommonRepo(commonRepoUrl, authInfo);
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
    public void setCommonRepoAuthInfo(String localRepoDir) {
        this.localRepoDir = localRepoDir;
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
