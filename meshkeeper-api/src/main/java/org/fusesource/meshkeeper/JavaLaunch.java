/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.meshkeeper;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

import static org.fusesource.meshkeeper.Expression.*;

/**
 * JavaLaunch
 * <p>
 * Description:
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
    String classLoaderFactoryBootstrap;

    public Expression getJvm() {
        return jvm;
    }

    public void setJvm(String jvm) {
        this.jvm = string(jvm);
    }

    public void setJvm(Expression jvm) {
        this.jvm = jvm;
    }

    public List<Expression> getJvmArgs() {
        return jvmArgs;
    }

    public JavaLaunch addJvmArgs(String... args) {
        return addJvmArgs(string(args));
    }

    public JavaLaunch addJvmArgs(Expression... args) {
        return addJvmArgs(Arrays.asList(args));
    }

    public JavaLaunch addJvmArgs(List<Expression> args) {
        this.jvmArgs.addAll(args);
        return this;
    }

    public JavaLaunch addSystemProperty(String key, String value) {
        systemProperties.add(string("-D" + key + "=" + value));
        return this;
    }

    public JavaLaunch addSystemProperty(Expression key, Expression value) {
        systemProperties.add(append(string("-D"), key, string("="), value));
        return this;
    }

    public JavaLaunch propageSystemProperties(String... names) {
        for (String name : names) {
            if (System.getProperty(name) != null) {
                addSystemProperty(name, System.getProperty(name));
            }
        }
        return this;
    }

    public Expression getClasspath() {
        return classpath;
    }

    public void setClassLoaderFactoryBootstrap(String classLoaderFactoryPath) {
        classLoaderFactoryBootstrap = classLoaderFactoryPath;
    }

    public void setClasspath(Expression classpath) {
        this.classpath = classpath;
    }

    public void setClasspath(FileExpression... classpath) {
        this.classpath = path(classpath);
    }

    public void setClasspath(String... classpath) {
        this.classpath = path(file(classpath));
    }

    public FileExpression getWorkingDir() {
        return workingDir;
    }

    public void setWorkingDir(String workingDir) {
        this.workingDir = file(workingDir);
    }

    public void setWorkingDir(FileExpression workingDir) {
        this.workingDir = workingDir;
    }

    public Expression getMainClass() {
        return mainClass;
    }

    public void setMainClass(String mainClass) {
        this.mainClass = string(mainClass);
    }

    public void setMainClass(Expression mainClass) {
        this.mainClass = mainClass;
    }

    public ArrayList<Expression> args() {
        return args;
    }

    public JavaLaunch addArgs(String... args) {
        return addArgs(string(args));
    }

    public JavaLaunch addArgs(Expression... args) {
        return addArgs(Arrays.asList(args));
    }

    public JavaLaunch addArgs(List<Expression> args) {
        this.args.addAll(args);
        return this;
    }

    public LaunchDescription toLaunchDescription() {
        LaunchDescription ld = new LaunchDescription();
        ld.setWorkingDirectory(workingDir);
        ld.add(jvm);
        ld.add(jvmArgs);
        if (classpath != null || classLoaderFactoryBootstrap != null) {
            ld.add(string("-cp"));
            Expression launchClasspath = null;
            if (classLoaderFactoryBootstrap != null) {
                ld.addPreLaunchTask(new LaunchDescription.BootstrapClassPathTask(classLoaderFactoryBootstrap));
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
        ld.add(systemProperties);
        ld.add(mainClass);
        ld.add(args);
        return ld;
    }
}
