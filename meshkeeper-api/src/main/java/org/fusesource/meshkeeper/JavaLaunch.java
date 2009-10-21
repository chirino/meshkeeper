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
package org.fusesource.meshkeeper;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Properties;

import org.fusesource.meshkeeper.MeshKeeper.Registry;
import org.fusesource.meshkeeper.classloader.ClassLoaderFactory;
import org.fusesource.meshkeeper.classloader.ClassLoaderServer;
import org.fusesource.meshkeeper.launcher.LaunchAgent;

import static org.fusesource.meshkeeper.Expression.*;

/**
 * JavaLaunch
 * <p>
 * This is a helper class used to construct {@link LaunchDescription}s for
 * Java processes.
 * </p>
 * 
 * @author cmacnaug
 * @version 1.0
 */
public class JavaLaunch {

    private Expression jvm = string("java");
    Expression classpath;
    FileExpression workingDir;
    Expression mainClass;
    ArrayList<Expression> jvmArgs = new ArrayList<Expression>();
    ArrayList<Expression> args = new ArrayList<Expression>();
    ArrayList<Expression> systemProperties = new ArrayList<Expression>();
    String bootStrapClassLoaderFactoryPath;


    /**
     * @return The expression representing the current jvm for the launch.
     */
    public Expression getJvm() {
        return jvm;
    }

    /**
     * Sets the java executable to use for the launch. By default
     * this is simply "java".
     * @param jvm The java executable for the launch.
     */
    public void setJvm(String jvm) {
        this.jvm = string(jvm);
    }

    /**
     * Sets the java executable to use for the launch. By default
     * this is simply "java".
     * @param jvm The java executable for the launch.
     */
    public void setJvm(Expression jvm) {
        this.jvm = jvm;
    }

    /**
     * @return The expression representing the current jvm args. 
     */
    public List<Expression> getJvmArgs() {
        return jvmArgs;
    }

    /**
     * Appends jvm args for the launched process.
     * @param args The arg
     * @return This {@link JavaLaunch}
     */
    public JavaLaunch addJvmArgs(String... args) {
        return addJvmArgs(string(args));
    }

    /**
     * Appends jvm args for the launched process.
     * @param args The arg
     * @return This {@link JavaLaunch}
     */
    public JavaLaunch addJvmArgs(Expression... args) {
        return addJvmArgs(Arrays.asList(args));
    }

    /**
     * Appends jvm args for the launched process.
     * @param args The arg
     * @return This {@link JavaLaunch}
     */
    public JavaLaunch addJvmArgs(List<Expression> args) {
        this.jvmArgs.addAll(args);
        return this;
    }

    /**
     * Appends the key and value as system properties for the 
     * launched process. 
     * @param key The key
     * @param value The value
     * @return This {@link JavaLaunch}
     */
    public JavaLaunch addSystemProperty(String key, String value) {
        systemProperties.add(string("-D" + key + "=" + value));
        return this;
    }

    /**
     * Appends the key and value expression as system properties for the 
     * launched class. 
     * @param key The key
     * @param value The value
     * @return This {@link JavaLaunch}
     */
    public JavaLaunch addSystemProperty(Expression key, Expression value) {
        systemProperties.add(append(string("-D"), key, string("="), value));
        return this;
    }

    /**
     * Propagates the specified property names from the source propreties as System
     * properties for the java launch.
     * 
     * @param sourceProps The source properties. 
     * @param names The property names to propagate if present.
     * @return This {@link JavaLaunch}
     */
    public JavaLaunch propagateSystemProperties(Properties sourceProps, String... names) {
        for (String name : names) {
            if (sourceProps.getProperty(name) != null) {
                addSystemProperty(name, sourceProps.getProperty(name));
            }
        }
        return this;
    }

    /**
     * @return the current classpath for the java launch
     */
    public Expression getClasspath() {
        return classpath;
    }

    /**
     * Sets the classloader factory path that will be resolved by the {@link LaunchAgent} prior
     * to launching the class. The specified <code>bootStrapClassLoaderFactoryPath</code> should
     * already be exported into the {@link Registry} via a {@link ClassLoaderServer} export method
     * prior to launching the class.
     * 
     * @param bootStrapClassLoaderFactoryPath A path to a {@link ClassLoaderFactory} in the {@link Registry}
     */
    public void setBootstrapClassLoaderFactory(String bootStrapClassLoaderFactoryPath) {
        this.bootStrapClassLoaderFactoryPath = bootStrapClassLoaderFactoryPath;
    }

    /**
     * Sets the classpath for the {@link JavaLaunch}. The classpath when
     * set is appended to the bootstrap classpath (when present). 
     * 
     * @param classpath An expression that can be resolved to a classpath
     * @return The working directory for the launch.
     */
    public void setClasspath(Expression classpath) {
        this.classpath = classpath;
    }

    /**
     * Sets the classpath for the  {@link JavaLaunch}. The classpath when
     * set is appended to the bootstrap classpath (when present). 
     * 
     * @param classpath A list of file expressions. 
     * @return The working directory for the launch.
     */
    public void setClasspath(FileExpression... classpath) {
        this.classpath = path(classpath);
    }

    /**
     * Sets the classpath for the  {@link JavaLaunch}. The classpath when
     * set is appended to the bootstrap classpath (when present). 
     * 
     * @param classpath A list of Strings that are interpreted as file expressions. 
     * @return The working directory for the launch.
     */
    public void setClasspath(String... classpath) {
        this.classpath = path(file(classpath));
    }

    /**
     * @return The working directory for the launch.
     */
    public FileExpression getWorkingDir() {
        return workingDir;
    }

    /**
     * Sets the working directory for the java launch. In general it
     * is good practice to choose a subdirectoy unrderneath the agent's
     * working directory which can be found by examing the corresponding
     * agent's {@link HostProperties#getDirectory()
     * @param workingDir the working directory for the launch.
     * @see HostProperties#getDirectory()
     */
    public void setWorkingDir(String workingDir) {
        this.workingDir = file(workingDir);
    }

    /**
     * Sets the working directory for the java launch. 
     * @param workingDir the working directory for the launch.
     * @see HostProperties#getDirectory()
     */
    public void setWorkingDir(FileExpression workingDir) {
        this.workingDir = workingDir;
    }

    /**
     * @return the main class to launch
     */
    public Expression getMainClass() {
        return mainClass;
    }

    /**
     * Sets the java main class to launch. 
     * @param mainClass The main class
     */
    public void setMainClass(String mainClass) {
        this.mainClass = string(mainClass);
    }

    /**
     * Sets the java main class to launch. 
     * @param mainClass The main class
     */
    public void setMainClass(Expression mainClass) {
        this.mainClass = mainClass;
    }

    /**
     * @return The arguments as a array of {@link Expression}s
     */
    public ArrayList<Expression> args() {
        return args;
    }

    /**
     * Adds program arguments to the launch. 
     * @param args The program arguments. 
     * @return The JavaLaunch.
     */
    public JavaLaunch addArgs(String... args) {
        return addArgs(string(args));
    }

    /**
     * Adds program arguments to the launch. 
     * @param args The program arguments. 
     * @return The JavaLaunch.
     */
    public JavaLaunch addArgs(Expression... args) {
        return addArgs(Arrays.asList(args));
    }

    /**
     * Adds program arguments to the launch. 
     * @param args The program arguments. 
     * @return The JavaLaunch.
     */
    public JavaLaunch addArgs(List<Expression> args) {
        this.args.addAll(args);
        return this;
    }

    /**
     * Coverts this {@link JavaLaunch} into a {@link LaunchDescription}. 
     * @return A {@link LaunchDescription} for this {@link JavaLaunch}
     */
    public LaunchDescription toLaunchDescription() {
        LaunchDescription ld = new LaunchDescription();
        ld.setWorkingDirectory(workingDir);
        ld.add(jvm);
        ld.add(jvmArgs);
        ld.add(systemProperties);
        if (classpath != null || bootStrapClassLoaderFactoryPath != null) {
            ld.add(string("-cp"));
            Expression launchClasspath = null;
            if (bootStrapClassLoaderFactoryPath != null) {
                ld.addPreLaunchTask(new LaunchDescription.BootstrapClassPathTask(bootStrapClassLoaderFactoryPath));
                launchClasspath = file(property(LaunchDescription.BootstrapClassPathTask.BOOTSTRAP_CP_PROPERTY, string("")));
            }
            if (classpath != null) {
                if (launchClasspath == null) {
                    launchClasspath = classpath;
                } else {
                    launchClasspath = path(file(launchClasspath), file(classpath));
                }
            }
            ld.add(launchClasspath);
        }
        
        ld.add(mainClass);
        ld.add(args);
        return ld;
    }
}
