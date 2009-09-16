/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.meshkeeper.distribution;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.fusesource.mop.MOP;
import org.fusesource.mop.MOPRepository;
import org.fusesource.mop.org.apache.commons.logging.Log;
import org.fusesource.mop.org.apache.commons.logging.LogFactory;
import org.fusesource.mop.org.apache.maven.artifact.Artifact;
import org.fusesource.mop.org.apache.maven.artifact.resolver.filter.ArtifactFilter;
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

    public synchronized List<File> resolvePlugin(String ... mavenArtifacts) throws Exception {

        ArrayList<ArtifactId> artifactIds = new ArrayList<ArtifactId>(mavenArtifacts.length);
        for(String artifact : mavenArtifacts)
        {
            artifactIds.add(ArtifactId.parse(artifact));
        }

        return getMopRepository().resolveFiles(getArtifactFilter(), artifactIds);

    }

    public String resolveClassPath(String mavenArtifact) throws Exception
    {
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
                deps = Collections.EMPTY_SET;
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

            // The plexus container is created on demand /w the context classloader.
            // Lets load it now, so we can properly set it's classloader.
            ClassLoader original = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(MOPRepository.class.getClassLoader());
                MOP_REPO.getContainer();
            } finally {
                Thread.currentThread().setContextClassLoader(original);
            }

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