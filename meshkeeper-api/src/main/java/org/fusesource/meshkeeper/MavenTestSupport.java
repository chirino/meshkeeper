/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.meshkeeper;

import java.io.File;

import org.fusesource.meshkeeper.util.internal.FileSupport;

/**
 * Helper methods for working with MeshKeeper in a Test case in a maven project <br/>
 * The created meshkeeper instance will be configured to store it's data under
 * ${basedir}/target/test-data <br/>
 * Maven should be configured so that the build base directory of the project
 * gets passed to the test case via a system property. For example: <br/>
 * <code><![CDATA[
 * <plugin>
 *   <artifactId>maven-surefire-plugin</artifactId>
 *   <configuration>
 *     <systemProperties>
 *      <property>
 *         <name>basedir</name>
 *         <value>${basedir}</value>
 *       </property>
 *     </systemProperties>
 *   </configuration>
 * </plugin>
 * ]]></code> <br/>
 * You may also want to set the "meshkeeper.registry.uri" system property in the
 * maven-surefire-plugin configuration. Since the
 * {@link MeshKeeperFactory#createMeshKeeper()} method is used to create the
 * MeshKeeper object, the "meshkeeper.registry.uri" will control if the test
 * runs against a local control server or against a remote one.
 * 
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class MavenTestSupport {

    public static final String SLASH = File.separator;

    public static MeshKeeper createMeshKeeper() throws Exception {
        return createMeshKeeper(null);
    }

    public static MeshKeeper createMeshKeeper(String testName) throws Exception {
        File dataDirectory = getDataDirectory(testName);
        //Set up the meshkeeper base dir (private to the test):
        System.setProperty("meshkeeper.base", dataDirectory.getCanonicalPath());
        //Set up mop.base (shared between tests)
        System.setProperty("mop.base", new File(getDataDirectory(null), "mop").getCanonicalPath());
        return MeshKeeperFactory.createMeshKeeper();
    }

    public static File getBaseDirectory() {
        return new File(System.getProperty("basedir", "."));
    }
    
    public static File getDataDirectory() {
        return getDataDirectory(null);
    }

    public static File getDataDirectory(String testName) {
        return new File(getBaseDirectory(), "target" + SLASH + getRelativeDataDirectory(testName));
    }

    public static String getRelativeDataDirectory(String testName) {
        if (testName == null) {
            testName = "meshkeeper";
        }
        return "test-data" + SLASH + testName;
    }

    public static boolean deleteDataDirectory() {
        return deleteDataDirectory(null);
    }

    public static boolean deleteDataDirectory(String testName) {
        return FileSupport.recursiveDelete(getDataDirectory((testName)));
    }

}