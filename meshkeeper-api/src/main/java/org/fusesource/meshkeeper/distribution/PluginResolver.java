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


    static final String PROJECT_GROUP_ID = "org.fusesource.meshkeeper";
    static final String PROJECT_ARTIFACT_ID = "meshkeeper-api";

    /**
     * Finds plugin classpath resources, and returns them as URLs.
     *
     * @param artifactId The maven artifact id for the plugin
     * @return The artifactId's resolved resources.
     */
    public List<File> resolvePlugin(String ... artifactId) throws Exception;

    /**
     * Resolves a classpath for the given maven artifact id.
     * @param artifact The maven artifact id.
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
