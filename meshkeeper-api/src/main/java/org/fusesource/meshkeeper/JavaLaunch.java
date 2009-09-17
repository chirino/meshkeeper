/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.meshkeeper;

import static org.fusesource.meshkeeper.Expression.*;

/** 
 * JavaLaunch
 * <p>
 * Description:
 * </p>
 * @author cmacnaug
 * @version 1.0
 */
public class JavaLaunch {

    private Expression jvm = string("java");
    Expression jvmArgs = string("");
    Expression classpath = string("");
    FileExpression workingDir;
    Expression mainClass;
    Expression args;
    
    public Expression getJvm() {
        return jvm;
    }
    
    public void setJvm(Expression jvm) {
        this.jvm = jvm;
    }
    
    public void setJvm(String jvm) {
        this.jvm = string(jvm);
    }
    
    public Expression getJvmArgs() {
        return jvmArgs;
    }
    
    public void setJvmArgs(Expression jvmArgs) {
        this.jvmArgs = jvmArgs;
    }
    
    public void setJvmArgs(String jvmArgs) {
        this.jvmArgs = string(jvmArgs);
    }
    
    public Expression getClasspath() {
        return classpath;
    }
    
    public void setClasspath(Expression classpath) {
        this.classpath = classpath;
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
    
    public void setMainClass(Expression mainClass) {
        this.mainClass = mainClass;
    }
    
    public Expression args() {
        return args;
    }
    
    public void setArgs(Expression args) {
        this.args = args;
    }
    
    public LaunchDescription toLaunchDescription()
    {
        LaunchDescription ld = new LaunchDescription();
        ld.add(jvm);
        ld.add(jvmArgs);
        ld.add(string("-cp"));
        ld.add(classpath);
        ld.add(mainClass);
        ld.add(args);
        ld.setWorkingDirectory(workingDir);
        return ld;
    }
   
}
