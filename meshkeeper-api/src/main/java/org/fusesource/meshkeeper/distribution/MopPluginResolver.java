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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.maven.artifact.Artifact;
import org.fusesource.mop.MOP;
import org.fusesource.mop.MOPRepository;
import org.fusesource.mop.common.base.Predicate;
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
class MopPluginResolver implements PluginResolver {
    private static Predicate<Artifact> ARTIFACT_FILTER = null;
    private static final Log LOG = LogFactory.getLog(PluginClassLoader.class);
    private static MOPRepository MOP_REPO;
    private String defaultPluginVersion = "LATEST";
    private String baseDir = System.getProperty(PluginResolver.BASE_DIR, System.getProperty("user.home", "."));

    public synchronized List<File> resolvePlugin(String mavenArtifact) throws Exception {

        ArrayList<ArtifactId> artifactIds = new ArrayList<ArtifactId>(1);
        artifactIds.add(ArtifactId.parse(mavenArtifact));

        return getMopRepository().resolveFiles(artifactIds, getArtifactFilter());

    }

    public String resolveClassPath(String mavenArtifact) throws Exception
    {
        ArrayList<ArtifactId> artifactIds = new ArrayList<ArtifactId>(1);
        artifactIds.add(ArtifactId.parse(mavenArtifact));
        return getMopRepository().classpath(artifactIds);
    }
    
    private Predicate<Artifact> getArtifactFilter() {
        if (ARTIFACT_FILTER == null) {

            Set<Artifact> deps;
            try {
                deps = getMopRepository().resolveArtifacts(new ArtifactId[] { ArtifactId.parse(PROJECT_GROUP_ID + ":" + PROJECT_ARTIFACT_ID, defaultPluginVersion, MOP.DEFAULT_TYPE) });
            } catch (Exception e) {
                return new Predicate<Artifact>() {
                    public boolean apply(Artifact artifact) {
                        return true;
                    }
                };
            }
            final HashSet<String> filters = new HashSet<String>(deps.size());
            for (Artifact a : deps) {
                filters.add(a.getArtifactId());
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug("Filters: " + filters);
            }

            ARTIFACT_FILTER = new Predicate<Artifact>() {
                public boolean apply(Artifact artifact) {
                    return !filters.contains(artifact.getArtifactId());
                }
            };
        }
        return ARTIFACT_FILTER;
    }

    private synchronized MOPRepository getMopRepository() {
        if (MOP_REPO == null) {
            MOP_REPO = new MOPRepository();
            MOP_REPO.setOnline(true);
            MOP_REPO.setLocalRepo(new File(baseDir + File.separator + ".mop"));
            //repo.setAlwaysCheckUserLocalRepo(true);
        }
        return MOP_REPO;
    }

    /**
     * Sets the base in which the resolver can store resolved plugins.
     * 
     * @param dir
     *            the base in which the resolver can store resolved plugins.
     */
    public void setBaseDir(String baseDir) {
        this.baseDir = baseDir;
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
