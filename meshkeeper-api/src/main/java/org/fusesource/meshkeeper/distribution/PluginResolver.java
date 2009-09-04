/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.meshkeeper.distribution;

import java.io.File;
import java.util.List;

/** 
 * PluginResolver
 * <p>
 * Description:
 * </p>
 * @author cmacnaug
 * @version 1.0
 */
public interface PluginResolver {

    // Allows folks to set a per plugin version.. for example: meshkeeper.plugin.version.jms=1.2
    public static final String KEY_PLUGIN_VERSION_PREFIX = "meshkeeper.plugin.version.";
    // Sets the default plugin version.. for example: meshkeeper.plugin.version.default=1.0
    public static final String KEY_DEFAULT_PLUGINS_VERSION = KEY_PLUGIN_VERSION_PREFIX +"default";
    // Allows setting of the plugin resolver directory via system property:
    public static final String BASE_DIR = "meshkeeper.plugin.basedir";
    
    
    static final String PROJECT_GROUP_ID = "org.fusesource.meshkeeper";
    static final String PROJECT_ARTIFACT_ID = "meshkeeper-api";
    
    /**
     * Sets the base in which the resolver can store resolved plugins.
     * @param dir the base in which the resolver can store resolved plugins.
     */
    public void setBaseDir(String dir);
    
    /**
     * Finds plugin classpath resources, and returns them as URLs.
     * 
     * @param artifactId The maven artifact id for the plugin
     * @return The artifactId's resolved resources.
     */
    public List<File> resolvePlugin(String artifactId) throws Exception;
    
    /**
     * Resolves a classpath for the given maven artifact id.
     * @param artifactId The maven artifact id.
     * @return A locally resolved classpath. 
     * @throws Exception If there is an error resolving the classpath
     */
    public String resolveClassPath(String artifact) throws Exception;
    
    /**
     * Sets the default version to use when searching for artifacts (in the case that one isn't supplied)
     * @param defaultVersion The default version to use when searching for artifacts
     */
    public void setDefaultPluginVersion(String defaultVersion);
}
