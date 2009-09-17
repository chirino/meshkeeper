/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.meshkeeper;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.fusesource.meshkeeper.MeshArtifact;
import org.fusesource.meshkeeper.distribution.repository.AuthenticationInfo;
import org.fusesource.meshkeeper.distribution.repository.wagon.WagonResourceManager;

import junit.framework.TestCase;

/**
 * ResourceTest
 * <p>
 * Description:
 * </p>
 * 
 * @author cmacnaug
 * @version 1.0
 */
public class ResourceTest extends TestCase {

    Log log = LogFactory.getLog(ResourceTest.class);

    public void testWebDavResourceManager() throws Exception {

        WagonResourceManager rm = new WagonResourceManager();
        File localDir = new File("target" + File.separator + "test-repo");
        rm.setLocalRepoDir(localDir.getCanonicalPath());
        log.info("Deleting local resource directory: " + localDir);
        rm.purgeLocalRepo();
        log.info("Deleted local resource directory: " + localDir);

        String remoteRepo = "dav://fusesource.com/forge/dav/fusemqptest/test-file-repo";
        AuthenticationInfo authInfo = new AuthenticationInfo();
        authInfo.setUserName("fusemqtest");
        authInfo.setPassword("fusemqtestpw");
        rm.setCentralRepoUri(remoteRepo, authInfo);

        MeshArtifact resource = rm.createResource();
        resource.setRepositoryId("common");
        resource.setRepositoryPath("testfolder");
        resource.setType(MeshArtifact.DIRECTORY);

        rm.resolveResource(resource);

        assertEquals(new File("test-file-repo", resource.getRepositoryPath()), new File(resource.getLocalPath()));

        rm.destroy();

    }

    public void testFileResourceManager() throws Exception {
        WagonResourceManager rm = new WagonResourceManager();
        File localDir = new File("target" + File.separator + "test-repo");
        rm.setLocalRepoDir(localDir.getCanonicalPath());
        log.info("Deleting local resource directory: " + localDir);
        rm.purgeLocalRepo();
        log.info("Deleted local resource directory: " + localDir);

        File remoteDir = new File("test-file-repo");
        rm.setCentralRepoUri(remoteDir.toURI().toString(), null);

        String resourcePath = "testfolder";
        MeshArtifact resource = rm.createResource();
        resource.setRepositoryId("common");
        resource.setRepositoryPath(resourcePath);
        resource.setType(MeshArtifact.DIRECTORY);

        rm.resolveResource(resource);

        assertEquals(new File("test-file-repo", resource.getRepositoryPath()), new File(resource.getLocalPath()));

        rm.destroy();
    }

    /**
     * @param source
     * @param copy
     */
    private void assertEquals(File source, File copy) {
        assertTrue(copy.exists());

        if (ignoreFile(source.getName())) {
            return;
        }

        if (source.isFile() && copy.isFile()) {
            return;
        } else if (source.isFile() != copy.isFile()) {
            fail(source + " is not equal to " + copy);
        }

        //Compare directory contents:
        ArrayList<String> copies = new ArrayList<String>(Arrays.asList(copy.list()));
        List<String> sources = Arrays.asList(source.list());
        for (String sf : sources) {
            if (!copies.remove(sf)) {
                
                if(!ignoreFile(sf))
                fail(sf + " not found in " + copy);
            }
            else
            {
                assertEquals(new File(source, sf), new File(copy, sf));
            }

        }

        if (!copies.isEmpty()) {
            fail("Extra files in copy: " + copies);
        }
    }

    private static final boolean ignoreFile(String name) {
        if (name.equals(".svn")) {
            return true;
        }
        return false;
    }
}
