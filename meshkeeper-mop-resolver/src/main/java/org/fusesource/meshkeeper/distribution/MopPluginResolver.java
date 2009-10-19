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
package org.fusesource.meshkeeper.distribution;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.LinkedHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.fusesource.mop.MOP;
import org.fusesource.mop.MOPRepository;
import org.fusesource.mop.org.apache.maven.artifact.Artifact;
import org.fusesource.mop.org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.fusesource.mop.org.apache.maven.repository.RepositorySystem;
import org.fusesource.mop.support.ArtifactId;

/**
 * MopPluginResolver
 * <p>
 * Description:
 * </p>
 * 
 * @author cmacnaug
 * @version 1.0
 */
public class MopPluginResolver implements PluginResolver {
    private static ArtifactFilter ARTIFACT_FILTER = null;
    private static final Log LOG = LogFactory.getLog(MopPluginResolver.class);
    private static MOPRepository MOP_REPO;
    private String defaultPluginVersion = "LATEST";

    public synchronized List<File> resolvePlugin(String... mavenArtifacts) throws Exception {

        ArrayList<ArtifactId> artifactIds = new ArrayList<ArtifactId>(mavenArtifacts.length);
        for (String artifact : mavenArtifacts) {
            artifactIds.add(ArtifactId.parse(artifact));
        }

        return getMopRepository().resolveFiles(getArtifactFilter(), artifactIds);

    }

    public String resolveClassPath(String mavenArtifact) throws Exception {
        ArrayList<ArtifactId> artifactIds = new ArrayList<ArtifactId>(1);
        artifactIds.add(ArtifactId.parse(mavenArtifact));
        return getMopRepository().classpath(artifactIds);
    }

    private ArtifactFilter getArtifactFilter() {
        if (ARTIFACT_FILTER == null) {

            Set<Artifact> deps;
            try {
                deps = getMopRepository().resolveArtifacts(new ArtifactId[] { ArtifactId.parse(PROJECT_GROUP_ID + ":" + PROJECT_ARTIFACT_ID, defaultPluginVersion, MOP.DEFAULT_TYPE) });
            } catch (Exception e) {
                deps = Collections.emptySet();
            }

            final HashSet<String> filters = new HashSet<String>(deps.size());
            for (Artifact a : deps) {
                filters.add(a.getArtifactId());
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug("Filters: " + filters);
            }

            ARTIFACT_FILTER = new ArtifactFilter() {
                public boolean include(Artifact artifact) {
                    return !filters.contains(artifact.getArtifactId());
                }
            };
        }
        return ARTIFACT_FILTER;
    }

    private synchronized MOPRepository getMopRepository() {
        if (MOP_REPO == null) {
            MOP_REPO = new MOPRepository();

            if (System.getProperty(MOPRepository.MOP_BASE) == null && System.getProperty(MOPRepository.MOP_REPO_CONFIG_PROP) == null) {
                LOG.warn("Neither: " + MOPRepository.MOP_BASE + " or " + MOPRepository.MOP_REPO_CONFIG_PROP + " are set. Will use default repos");
            }
            // The plexus container is created on demand /w the context classloader.
            // Lets load it now, so we can properly set it's classloader.
            ClassLoader original = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(MOPRepository.class.getClassLoader());
                MOP_REPO.getContainer();
            } finally {
                Thread.currentThread().setContextClassLoader(original);
            }

            LinkedHashMap<String, String> repositories = MOP_REPO.getRemoteRepositories();
            repositories.clear();
            //Add in configured repos (which may have auth to get to meshkeeper:
            repositories.putAll(MOP_REPO.getConfiguredRepositories());
            //We could add our meshkeeper repos.. but they require authorization.. so there is no point.
            //repositories.put("meshkeeper.release", "http://meshkeeper.fusesource.org/repo/release");
            //repositories.put("meshkeeper.snapshot", "http://meshkeeper.fusesource.org/repo/snapshot");

        }
        return MOP_REPO;
    }

    /**
     * @param defaultPluginVersion
     *            the defaultPluginVersion to set
     */
    public void setDefaultPluginVersion(String defaultPluginVersion) {
        this.defaultPluginVersion = defaultPluginVersion;
    }

    /**
     * @return the defaultPluginVersion
     */
    public String getDefaultPluginVersion() {
        return defaultPluginVersion;
    }
}
