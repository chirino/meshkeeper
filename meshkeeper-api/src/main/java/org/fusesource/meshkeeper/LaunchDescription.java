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

import org.fusesource.meshkeeper.Expression;
import org.fusesource.meshkeeper.MeshKeeper.Launcher;
import org.fusesource.meshkeeper.classloader.ClassLoaderFactory;
import org.fusesource.meshkeeper.launcher.LocalProcess;

import java.io.File;
import java.io.Serializable;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import static org.fusesource.meshkeeper.Expression.*;

/**
 * A builder used to build a command line for launching a process via A {@link Launcher}
 * <p>
 * It is recommended that you create {@link LaunchDescription} via the {@link Launcher#createLaunchDescription()}
 * method, so that the {@link Launcher} can perform an necessary pre-initialization. 
 * @author chirino
 */
public class LaunchDescription implements Serializable {

    private static final long serialVersionUID = 1L;
    ArrayList<Expression> command = new ArrayList<Expression>();
    HashMap<String, Expression> environment;
    Expression.FileExpression workingDirectory;
    ArrayList<LaunchTask> preLaunchTasks = new ArrayList<LaunchTask>();

    public LaunchDescription add(String... values) {
        return add(string(values));
    }

    public LaunchDescription add(Expression... value) {
        return add(Arrays.asList(value));
    }

    public LaunchDescription add(List<Expression> value) {
        command.addAll(value);
        return this;
    }

    public LaunchDescription setEnv(String key, String value) {
        return setEnv(key, string(value));
    }

    public LaunchDescription setEnv(String key, Expression value) {
        if (environment == null) {
            environment = new HashMap<String, Expression>();
        }
        environment.put(key, value);
        return this;
    }

    public void propagateSystemProperties(Properties sourceProps, String... names) {
        for (String name : names) {
            if (sourceProps.getProperty(name) != null) {
                add("-D" + name + "=" + sourceProps.getProperty(name));
            }
        }
    }

    private static class InstallLaunchResourceTask implements Serializable, LaunchTask {

        private static final long serialVersionUID = 1L;
        private final MeshArtifact resource;

        public InstallLaunchResourceTask(MeshArtifact resource) {
            this.resource = resource;
        }

        public void execute(LocalProcess process) throws Exception {
            process.getProcessLauncher().getMeshKeeper().repository().resolveResource(resource);
        }
    }

    static class BootstrapClassPathTask implements Serializable, LaunchTask {
        public static final String BOOTSTRAP_CP_PROPERTY = "bootstrap.classpath";
        private static final long serialVersionUID = 1L;
        private final String classLoaderPath;

        public BootstrapClassPathTask(String classLoaderPath) {
            this.classLoaderPath = classLoaderPath;
        }

        public void execute(LocalProcess process) throws Exception {
            ClassLoaderFactory clf = process.getProcessLauncher().getMeshKeeper().registry().getRegistryObject(this.classLoaderPath);

            process.getListener().onProcessInfo("Setting up bootstrap classpath from: " + classLoaderPath);
            URLClassLoader classloader = (URLClassLoader) clf.createClassLoader(getClass().getClassLoader(), new File(process.getProcessLauncher().getHostProperties().getDirectory() + File.separator
                    + "classloaer-cache"));
            Expression classpath = null;
            for (URL url : classloader.getURLs()) {
                if ("file".equals(url.getProtocol())) {
                    if (classpath == null) {
                        classpath = file(new File(url.toURI()).getAbsolutePath());
                    }
                    classpath = path(file(classpath), file(new File(url.toURI()).getAbsolutePath()));
                } else {
                    throw new Exception("Can't bootstrap url classpath elements: " + url);
                }
            }

            if (classpath != null) {
                //process.getListener().onProcessInfo("Set up bootstrap classpath from: " + classpath.evaluate());
                process.getProcessProperties().put(BOOTSTRAP_CP_PROPERTY, classpath.evaluate());
            } else {
                process.getListener().onProcessInfo("No urls found to bootstrap for: " + classLoaderPath);
            }

        }
    }

    private static class SubLaunchTask implements Serializable, LaunchTask, MeshProcessListener {

        private static final long serialVersionUID = 1L;

        private final LaunchDescription launch;

        transient private CountDownLatch done = new CountDownLatch(1);
        transient private LocalProcess process;
        transient private Exception error;

        public SubLaunchTask(LaunchDescription launch) {
            this.launch = launch;
        }

        public void execute(LocalProcess process) throws Exception {
            this.process = process;
            process.getProcessLauncher().launch(launch, process.getOwnerRegistryPath(), this);
            done.await();
            if (error != null) {
                throw error;
            }
        }

        public void onProcessExit(int exitCode) {
            if (exitCode != 0) {
                error = new Exception("Sub process failed. exit code:" + exitCode);
            }
            done.countDown();
        }

        public void onProcessError(Throwable thrown) {
            error = new Exception("Sub process failed", thrown);
        }

        public void onProcessInfo(String message) {
            MeshProcessListener listener = process.getListener();
            if (listener != null) {
                listener.onProcessInfo(message);
            }

        }

        public void onProcessOutput(int fd, byte[] output) {
            MeshProcessListener listener = process.getListener();
            if (listener != null) {
                listener.onProcessOutput(fd, output);
            }
        }
    }

    /**
     * Adds a prelaunch taks to the launch description:
     * 
     * @param task
     *            The task to add.
     */
    void addPreLaunchTask(LaunchTask task) {
        preLaunchTasks.add(task);
    }

    /**
     * Adds a resource to the launch description. The receiving agent will
     * resolve it, copying it to it's local resource cache.
     * 
     * To refer to the Resource on the command line call
     * {@link #add(Expression[])} )} with
     * {@link Expression#resource(MeshArtifact)} e.g.
     * 
     * <code>
     * LaunchDescription ld = new LaunchDescription();
     * LaunchResource lr = new LauncResource();
     * ... 
     * ld.addResource(lr);
     * ld.add(resource(lr);
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
     * Executes a process before launching this process. Usually used to setup/
     * 
     * @param launch
     */
    public void setup(LaunchDescription launch) {
        preLaunchTasks.add(new SubLaunchTask(launch));
    }

    public FileExpression getWorkingDirectory() {
        return workingDirectory;
    }

    public void setWorkingDirectory(FileExpression workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = file(workingDirectory);
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