/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/package org.fusesource.meshkeeper;

import org.fusesource.meshkeeper.Expression;
import org.fusesource.meshkeeper.launcher.LocalProcess;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

/**
 * @author chirino
*/
public class LaunchDescription implements Serializable {
    ArrayList<Expression> command = new ArrayList<Expression>();
    HashMap<String, Expression> environment;
    Expression.FileExpression workingDirectory;
    ArrayList<LaunchTask> preLaunchTasks = new ArrayList<LaunchTask>();
    

    public LaunchDescription add(String value) {
        return add(Expression.string(value));
    }

    public LaunchDescription add(Expression value) {
        command.add(value);
        return this;
    }

    public LaunchDescription setEnv(String key, String value) {
        return setEnv(key, Expression.string(value));
    }

    public LaunchDescription setEnv(String key, Expression value) {
        if( environment == null ) {
            environment = new HashMap<String, Expression>();
        }
        environment.put(key, value);
        return this;
    }

    public void propageSystemProperties(String... names) {
        for (String name : names) {
            if( System.getProperty(name)!=null ) {
                add("-D" + name + "=" +System.getProperty(name));
            }
        }
    }

    private static class InstallLaunchResourceTask implements Serializable, LaunchTask {
        private final MeshArtifact resource;

        public InstallLaunchResourceTask(MeshArtifact resource) {
            this.resource = resource;
        }

        public void execute(LocalProcess process) throws Exception {
            process.getProcessLauncher().getMeshKeeper().repository().resolveResource(resource);
        }
    }


    private static class SubLaunchTask implements Serializable, LaunchTask, MeshProcessListener {
        private final LaunchDescription launch;

        transient private CountDownLatch done = new CountDownLatch(1);
        transient private LocalProcess process;
        transient private Exception error;

        public SubLaunchTask(LaunchDescription launch) {
            this.launch = launch;
        }

        public void execute(LocalProcess process) throws Exception {
            this.process = process;
            process.getProcessLauncher().launch(launch, this);
            done.await();
            if( error!=null ) {
                throw error;
            }
        }

        public void onProcessExit(int exitCode) {
            if( exitCode!=0 ) {
                error = new Exception("Sub process failed. exit code:"+exitCode);
            }
            done.countDown();
        }
        public void onProcessError(Throwable thrown) {
            error = new Exception("Sub process failed", thrown);
        }
        public void onProcessInfo(String message) {
            MeshProcessListener listener = process.getListener();
            if (listener!=null) {
                listener.onProcessInfo(message);
            }

        }
        public void onProcessOutput(int fd, byte[] output) {
            MeshProcessListener listener = process.getListener();
            if (listener!=null) {
                listener.onProcessOutput(fd, output);
            }
        }
    }


    /**
     * Adds a resource to the launch description. The receiving
     * agent will resolve it, copying it to it's local resource
     * cache. 
     * 
     * To refer to the Resource on the command line call {@link #add(Expression)}
     * with {@link Expression#resource(MeshArtifact)} e.g.
     * 
     * <code>
     * LaunchDescription ld = new LaunchDescription();
     * LaunchResource lr = new LauncResource();
     * ... 
     * ld.addResource(lr);
     * ld.add(Expression.resource(lr);
     * </code>
     * 
     * 
     * @param resource
     * @see Expression#resource(MeshArtifact)
     */
    public void installResource(MeshArtifact resource) {
        preLaunchTasks.add(new InstallLaunchResourceTask(resource));
    }

    /**
     * Executes a process before launching this process.  Usually used to setup/
     * @param launch
     */
    public void setup(LaunchDescription launch) {
        preLaunchTasks.add(new SubLaunchTask(launch));
    }

    public Expression.FileExpression getWorkingDirectory() {
        return workingDirectory;
    }

    public void setWorkingDirectory(Expression.FileExpression workingDirectory) {
        this.workingDirectory = workingDirectory;
    }


    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory =  Expression.file(workingDirectory);
    }

    public ArrayList<Expression> getCommand() {
        return command;
    }

    public HashMap<String, Expression> getEnvironment() {
        return environment;
    }

    public ArrayList<LaunchTask> getPreLaunchTasks() {
        return preLaunchTasks;
    }
}