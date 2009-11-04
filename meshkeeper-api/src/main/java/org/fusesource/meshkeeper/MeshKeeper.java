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

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeoutException;

import org.fusesource.meshkeeper.classloader.ClassLoaderFactory;
import org.fusesource.meshkeeper.classloader.ClassLoaderServer;

/**
 * MeshKeeper
 * <p>
 * {@link MeshKeeper} provides a collection of facilities that assist Java applications to 
 * discover, launch, coordinate, and control remote processes across a grid of computers. 
 * </p>
 * 
 * @author cmacnaug
 * @version 1.0
 */
public interface MeshKeeper {

    /**
     * DistributionRef
     * <p>
     * The distribution ref holds information about objects that 
     * have been exported via the {@link Remoting} and {@link Registry}
     * interfaces as well as the {@link MeshKeeper#distribute(String, boolean, Object, Class...)}
     * helper method.
     * </p>
     * @author cmacnaug
     * @version 1.0
     * @param <D>
     */
    public interface DistributionRef<D> {
        /**
         * @return The path in the {@link Registry} at which the object's proxy is registered (when it has been)
         */
        public String getRegistryPath();

        /**
         * @return The proxy for the distributed object, this proxy can be passed to other objects for rmi.
         */
        public D getProxy();

        /**
         * A pointer to the original object that has been exported. It is possible that the {@link MeshKeeper}
         * or null if the {@link MeshKeeper} implementation has released the reference. 
         * @return The original object that has been remoted. 
         */
        public D getTarget();
    }

    /**
     * Eventing
     * <p>
     * This interface provides access to {@link MeshKeeper}'s eventing facilities. In the highly 
     * distributed environment which {@link MeshKeeper} makes easy to achieve, {@link Eventing} can
     * be quite useful in terms of coordinating activities of distributed objects.  
     * </p>
     * 
     * @author cmacnaug
     * @version 1.0
     */
    public interface Eventing {

        /**
         * Sends an event on the given topic.
         * @param event The event.
         * @param topic The topic.
         */
        public void sendEvent(MeshEvent event, String topic) throws Exception;

        /**
         * Opens a listener on the given event topic.
         * 
         * @param listener
         *            The listener
         * @param topic
         *            The topic
         * @throws Exception
         *             If there is an error opening the listener
         */
        public void openEventListener(MeshEventListener listener, String topic) throws Exception;

        /**
         * Stops listening to events on the given topic.
         * 
         * @param listener
         *            The listener The listener
         * @param topic
         *            The topic
         * @throws Exception
         *             If there is an error closing the listener
         */
        public void closeEventListener(MeshEventListener listener, String topic) throws Exception;
    }

    /**
     * Launcher
     * <p>
     * This interface provides access to {@link MeshKeeper}'s process launching facilities.
     * </p>
     * 
     * @author cmacnaug
     * @version 1.0
     */
    public interface Launcher {

        /**
         * Indicates the path in which Launchers should register themselves.
         */
        public static final String LAUNCHER_REGISTRY_PATH = "/launchclients/";

        /**
         * Launchers request that mesh containers register themselves at this
         * path in a folder under the launcher's name.
         * 
         */
        public static final String MESHCONTAINER_REGISTRY_PATH = "/meshcontainers/";

        /**
         * Requests the specified number of tcp ports from the specified process
         * launcher.
         * 
         * @param agentName
         *            The name of the process launcher
         * @param count
         *            The number of ports.
         * @return The reserved ports
         * @throws Exception
         *             If there is an error reserving the requested number of
         *             ports.
         */
        public List<Integer> reserveTcpPorts(String agentName, int count) throws Exception;

        /**
         * Releases previously reserved ports at the launcher.
         */
        public void releasePorts(String agentName, Collection<Integer> ports) throws Exception;

        /**
         * Releases all ports that have been reserved on the specified launcher.
         */
        public void releaseAllPorts(String agentName) throws Exception;

        /**
         * Waits for an agent to become available timing out of one does not
         * become available after the given timeout.
         * 
         * @param timeout
         *            The timeout
         * 
         * @throws InterruptedException
         *             If the thread is interrupted while waiting
         * @throws TimeoutException
         *             If timed out.
         */
        public void waitForAvailableAgents(long timeout) throws InterruptedException, TimeoutException;

        /**
         * Gets available the properties of available agents.
         * 
         * @return A list of available agents.
         */
        public HostProperties[] getAvailableAgents();

        /**
         * Attempts to bind the given agent. Once an agent is bound it is
         * exclusively available to this mesh keeper until it is released or
         * this keeper is closed.
         * 
         * @param agentName
         *            The agent to bind.
         * @throws Exception
         *             If there is an error binding the agent.
         */
        public void bindAgent(String agentName) throws Exception;

        /**
         * Releases a previously bound agent.
         * 
         * @param agentName
         *            The agent name
         * @throws Exception
         *             If there is an error binding the agent.
         */
        public void releaseAgent(String agentName) throws Exception;

        /**
         * Release all agents bound by this launcher.
         * 
         * @throws Exception
         *             If there is an error releasing agents.
         */
        public void releaseAllAgents() throws Exception;

        /**
         * Launches a process on the specified agent.
         * 
         * @param agentId
         *            The agent
         * @param launch
         *            The launch description.
         * @param listener
         *            A listener for the process's output.
         * @return The launched process.
         * @throws Exception
         *             If there is an error launching the process.
         */
        public MeshProcess launchProcess(String agentId, final LaunchDescription launch, MeshProcessListener listener) throws Exception;

        /**
         * Prints a line to the given process' standard input.
         * 
         * @param process
         *            The process.
         * @param line
         *            The line to print.
         */
        public void println(MeshProcess process, String line);

        /**
         * Creates a JavaLaunch for a {@link MeshContainer}. This can be used in conjunction with
         * {@link #launchMeshContainer(String, JavaLaunch, MeshProcessListener)} in cases where the 
         * caller wishes to add additional classpath elements or arguments to the container launch. 
         * 
         * Callers should not attempt to clear any arguments or overwrite application args or the main class and 
         * an attempt to do so may result in a {@link UnsupportedOperationException}
         * 
         * @return A {@link JavaLaunch} for a MeshContainer. 
         * @throws Exception
         */
        public JavaLaunch createMeshContainerLaunch() throws Exception;
        
        /**
         * Creates a JavaLaunch using this MeshKeeper's bootstrap classloader. The JavaLaunch
         * will have it's classpath configured using {@link #getBootstrapClassLoader()} which
         * is usually the applications {@link ClassLoader} meaning this launch can launch any
         * class in the applications classpath. The returned launch will include the System
         * Properties used to configure a {@link MeshKeeperFactory} so that the launched application
         * can create a {@link MeshKeeper} via {@link MeshKeeperFactory#createMeshKeeper()}.
         * 
         * In many cases the user will want to further configure the launch with additional
         * System Properties, JVM args, working directory etc. 
         * 
         * @param The main class to launch (e.g. that returned by {@link Class#getName()})
         * @return A preconfigured java launch.
         * @throws Exception If there is an error creating the {@link JavaLaunch}
         */
        public JavaLaunch createBootstrapJavaLaunch(String mainClass, String ... args) throws Exception;

        /**
         * Launches a {@link MeshContainer} on the specified agent.
         *  
         * @param agentId The agent. 
         * @return The newly launched container
         * @throws Exception If there is an error launching the container. 
         */
        public MeshContainer launchMeshContainer(String agentId) throws Exception;

        /**
         * Launches a {@link MeshContainer} on the specified agent.
         *  
         * @param agentId The agent. 
         * @param listener The listener for container output. 
         * @return The newly launched container
         * @throws Exception If there is an error launching the container. 
         */
        public MeshContainer launchMeshContainer(String agentId, MeshProcessListener listener) throws Exception;

        /**
         * Launches a {@link MeshContainer} on the specified agent. The provided java launch when non null,
         * is used to take additional jvm args, or classpath elements for the launched container. Usage
         * of a provided java launch is an advanced feature, and is recommended only when needed.  
         *  
         * @param agentId The agent. 
         * @param listener The listener for container output. 
         * @param launch The launch description. 
         * @return The newly launched container
         * @throws Exception If there is an error launching the container. 
         */
        public MeshContainer launchMeshContainer(String agentId, JavaLaunch launch, MeshProcessListener listener) throws Exception;

        /**
         * Sets the classloader that will be used to bootstrap java launches.
         * This classloader will be used for launched {@link MeshContainer}s and
         * launched {@link Runnable}s.
         * 
         * The launcher will internally create {@link ClassLoaderServer} and
         * {@link ClassLoaderFactory} to host the specified classloader. the
         * {@link ClassLoaderServer}'s lifecycle will be tied to that of this
         * {@link Launcher}
         * 
         * If the user explicitly sets a {@link ClassLoaderFactory} via
         * {@link #setBootstrapClassLoaderFactory(ClassLoaderFactory)} that
         * factory will be used instead. Otherwise calls to
         * {@link #getBootstrapClassLoaderFactory()} will return the
         * {@link ClassLoaderFactory} associated witht the specified
         * {@link ClassLoader}.
         * 
         * @param classLoader
         *            The classloader to be used for bootstrapping.
         * @throws Exception
         *             if the classloader can't be used for bootstrapping.
         */
        public void setBootstrapClassLoader(ClassLoader classLoader) throws Exception;

        /**
         * Gets the classloader that will be used to bootstrap java launches.
         * This classloader will be used for launched {@link MeshContainer}s and
         * launched {@link Runnable}s.
         * 
         * @return the current bootstrap {@link ClassLoader}.
         */
        public ClassLoader getBootstrapClassLoader();

        /**
         * Sets the {@link ClassLoaderFactory} that will be used to bootstrap java launches.
         * This classloader will be used for launched {@link MeshContainer}s and
         * launched {@link Runnable}s.
         * 
         * The caller is responsible for managing the lifecycle of the associated {@link ClassLoaderServer}
         * 
         * @param factory The factory stub to use for bootstrapping java launches.
         */
        public void setBootstrapClassLoaderFactory(ClassLoaderFactory factory);

        /**
         * Gets the {@link ClassLoaderFactory} that will be used to bootstrap java launches.
         * This classloader will be used for launched {@link MeshContainer}s and
         * launched {@link Runnable}s. If one hasn't yet been created it will be created
         * on demand using the {@link ClassLoader} returned by {@link #getBootstrapClassLoader()}
         * 
         * @param factory The factory stub to use for bootstrapping java launches.
         */
        public ClassLoaderFactory getBootstrapClassLoaderFactory();

        /**
         * @return The amount of time to allow for binding an agent.
         */
        public long getBindTimeout();

        /**
         * Sets the bind timeout for binding agents
         * 
         * @param bindTimeout
         */
        public void setBindTimeout(long bindTimeout);

        /**
         * @return The amount of time allowed for launching a remote proceess.
         */
        public long getLaunchTimeout();

        /**
         * The amount of time allowed for launching a remote proceess.
         */
        public void setLaunchTimeout(long launchTimeout);

        /**
         * @return The amount of time allowed for killing a remote proceess.
         */
        public long getKillTimeout();

        /**
         * The amount of time allowed for killing a remote proceess.
         */
        public void setKillTimeout(long killTimeout);

    }

    /**
     * Registry
     * <p>
     * This interface provides access to {@link MeshKeeper}'s data/object registry facilities. 
     * </p>
     * 
     * @author cmacnaug
     * @version 1.0
     */
    public interface Registry {

        /**
         * Adds an object to the registry at the given path. If sequential is
         * true then the object will be added at the given location with a
         * unique name. Otherwise the object will be added at the location given
         * by path.
         * <p>
         * Paths must start with a '/' character. In general paths may not end in a
         * "/" character with the exception of "/" root node. One exception to this
         * would be when sequential is true which is legal because an sequential 
         * identifier is appended to the path. 
         * 
         * @param path
         *            The path to add to.
         * @param sequential
         *            When true a unique child node is created at the given path
         * @param o
         *            The object to add.
         * @return The path at which the element was added.
         * @throws Exception
         *             If there is an error adding the node.
         */
        public String addRegistryObject(String path, boolean sequential, Serializable o) throws Exception;

        /**
         * Gets the data at the specified node as an object. The path name
         * must not end with a '/' character. 
         * 
         * @param <T>
         *            The type of the object expected.
         * @param path
         *            The path of the object.
         * @return The object at the given node.
         * @throws Exception
         *             If the object couldn't be retrieved.
         */
        public <T> T getRegistryObject(String path) throws Exception;

        /**
         * Gets the data at the specified node. The path name
         * must not end with a '/' character. 
         * 
         * @param path
         *            The path of the data.
         * @return The data at the given node.
         * @throws Exception
         *             If the object couldn't be retrieved.
         */
        public byte[] getRegistryData(String path) throws Exception;

        /**
         * Removes a node from the registry. The path name
         * must not end with a '/' character. 
         * 
         * @param path
         *            The path to remove.
         * @param recursive
         *            If true then any children will also be removed.
         * @throws Exception
         *             If the path couldn't be removed.
         */
        public void removeRegistryData(String path, boolean recursive) throws Exception;

        /**
         * Adds data to the registry at the given path. If sequential is true
         * then the data will be added at the given location with a unique name.
         * Otherwise the data will be added at the location given by path.
         * <p>
         * Paths must start with a '/' character. In general paths may not end in a
         * "/" character with the exception of "/" root node. One exception to this
         * would be when sequential is true which is legal because an sequential 
         * identifier is appended to the path. 
         * 
         * @param path
         *            The path to add to.
         * @param sequential
         *            When true a unique child node is created at the given path
         * @param data
         *            The data. If null then a 0 byte array will be stored in
         *            the registry
         * @return The path at which the element was added.
         * @throws Exception
         *             If there is an error adding the node.
         */
        public String addRegistryData(String path, boolean sequential, byte[] data) throws Exception;

        /**
         * Adds a listener for changes in a path's child elements. 
         * 
         * @param path
         * @param watcher
         */
        public void addRegistryWatcher(String path, RegistryWatcher watcher) throws Exception;

        /**
         * Removes a previously registered 
         * 
         * @param path
         *            The path on which the listener was listening.
         * @param watcher
         *            The watcher
         */
        public void removeRegistryWatcher(String path, RegistryWatcher watcher) throws Exception;

        /**
         * Convenience method that waits for a minimum number of objects to be
         * registered at the given registry path. 
         * 
         * @param <T>
         * @param path
         *            The path
         * @param min
         *            The minimum number of objects to wait for.
         * @param timeout
         *            The maximum amount of time to wait.
         * @return The objects that were registered.
         * @throws Exception
         */
        public <T> Collection<T> waitForRegistrations(String path, int min, long timeout) throws TimeoutException, Exception;

        /**
         * Convenience method that waits for a registry path to be created.
         * 
         * @param <T>
         * @param path
         *            The path
         * @param timeout
         *            The maximum amount of time to wait.
         * @return The
         * @throws Exception
         */
        public <T> T waitForRegistration(String path, long timeout) throws TimeoutException, Exception;
        
        /**
         * Returns a list of registry nodes at or below the given path that have data associated 
         * with them
         * 
         * @param path The path. 
         * @param recursive If recursive will list all chldren elements under the path as well. 
         * @return A list of nodes at the path or an empty list if there are none. 
         */
        public Collection<String> list(String path, boolean recursive) throws Exception;
    }

    /**
     * Remoting
     * <p>
     * This interface provides access to {@link MeshKeeper}'s rmi facilities. Remoting allows 
     * you to expose a proxy to an object which can be distributed throughout {@link MeshKeeper} objects
     * for remote method invocation. Remoting is frequently used in conjuction with {@link MeshKeeper}'s
     * {@link Registry} facilities to allow discovery of Remoted objects.
     * </p>
     * @author cmacnaug
     * @version 1.0
     */
    public interface Remoting {

        /**
         * Exports a {@link Distributable} object returning an RMI proxy to the
         * Distibutable object. The proxy can then be passed to other
         * applications in the mesh to use via RMI. It is best practice to
         * unexport the object when it is no longer used.
         * 
         * The exported object
         * 
         * @param <T>
         * @param obj
         * @return
         * @throws Exception
         */
        public <T> T export(T obj, Class<?>... interfaces) throws Exception;

        /**
         * Exports a object returning an RMI proxy to it, but to a specific
         * address. This allows users to register multiple objects sharing the
         * same interfaces to a single location thus allowing multicast method
         * call to all objects registered at the adress The proxy can then be
         * passed to other applications in the mesh to use via RMI. It is best
         * practice to unexport the object when it is no longer used.
         * 
         * @param <T>
         *            The type to which to cast the returned stub
         * @param obj
         *            The object to export
         * @param address
         *            The address (e.g. ServiceInterfaceFoo
         * @param interfaces
         *            The interfaces to which to limit the export.
         * @return The proxy that can be used to invoke method calls on the
         *         exported object.
         * @throws Exception
         *             If there is an error exporting
         */
        public <T> T exportMulticast(T obj, String address, Class<?>... interfaces) throws Exception;

        /**
         * Gets a proxy object for a multicast export.
         * 
         * @param <T>
         * @param address
         *            The address to which multicast objects are exported.
         * @param mainIngerface
         *            The interface for the proxy.
         * @param interfaces
         *            Any extra interfaces for the proxy.
         * @return The proxy for the multicast address.
         * @throws Exception
         *             If there is an error
         */
        public <T> T getMulticastProxy(String address, Class<?> mainInterface, Class<?>... extraInterfaces) throws Exception;

        /**
         * Unexports a previously exported object.
         * 
         * @param obj
         *            The object that had previously been exported.
         * @throws Exception
         *             If there is an error unexporting the object.
         */
        public void unexport(Object obj) throws Exception;

    }

    /**
     * Repository
     * <p>
     * This interface provides access to {@link MeshKeeper}'s file / artificat distribution facilities. 
     * This interface is still a work in progress. 
     * </p>
     * 
     * @author cmacnaug
     * @version 1.0
     */
    public interface Repository {

        /**
         * Factory method for creating a resource.
         * 
         * @return An empty resource.
         */
        public MeshArtifact createResource();

        /**
         * Called to locate the given resource.
         * 
         * @param resource
         *            The resource to locate.
         * @throws Exception
         *             If there is an error locating the resource.
         */
        public void resolveResource(MeshArtifact resource) throws Exception;

        /**
         * @param resource
         * @param data
         * @throws IOException
         */
        public void deployFile(MeshArtifact resource, byte[] data) throws Exception;

        /**
         * 
         * @param resource
         * @param d
         * @throws Exception
         */
        public void deployDirectory(MeshArtifact resource, File d) throws Exception;

        /**
         * @return The path to the local resource directory.
         */
        public File getLocalRepoDirectory();

        /**
         * 
         * @throws IOException
         */
        public void purgeLocalRepo() throws IOException;
    }

    /**
     * Gets a uri which can be used to connect to a meshkeeper server
     * 
     * @return
     */
    public String getRegistryConnectUri();

    /**
     * Starts distributor services.
     * 
     * @throws Exception
     */
    public void start() throws Exception;

    /**
     * Closes the distributor cleaning up all distributed references.
     */
    public void destroy() throws Exception;

    /**
     * This is a convenience method to export an Object and register it's proxy, 
     * allowing other components in the mesh to discover it. 
     * 
     * object. This is equivalent to calling: <code>
     * <br> Object proxy = registry().export(distributable, serviceInterfaces);
     * <br> remoting().addRegistryObject(path, true, proxy);
     * </code>
     * <p>
     * It is best practice to call {@link #undistribute(Object))} once the
     * object is no longer needed.
     * 
     * @param path
     *            The path at which to register the exported object.
     * @param sequential
     *            Whether the registry path should be registered as a unique
     *            node at the given path.
     * @param distributable
     *            The object.
     * @return a {@link DistributionRef} for distributed object.
     */
    public <T, S extends T> DistributionRef<T> distribute(String path, boolean sequential, S distributable, Class<?>... serviceInterfaces) throws Exception;

    /**
     * Called to undistribute a previously distributed object. This is
     * equivalent to calling <code>
     * <br>{@link #unexport(Object)};
     * <br>{@link #removeRegistryObject(String, boolean, Serializable)};
     * </code>
     * 
     * @param distributable
     *            The object that previously distributed.
     */
    public void undistribute(Object distributable) throws Exception;

    /**
     * Accesses the MeshKeeper's executor. Tasks run on the executor should not
     * block.
     * 
     * @return The executor service.
     */
    public ScheduledExecutorService getExecutorService();

    /**
     * Sets the user class loader. Setting the user class loader can assist
     * meshkeeper in resolving user's serialized objects in some cases. This is
     * an advanced option and should be used with extreme caution.
     * 
     * @param classLoader
     *            The user classloader.
     */
    public void setUserClassLoader(ClassLoader classLoader);

    /**
     * Gets the user class loader. Setting the user class loader can assist
     * meshkeeper in resolving user's serialized objects in some cases. This is
     * an advanced option and should be used with extreme caution.
     * 
     * @return The user classloader.
     */
    public ClassLoader getUserClassLoader();

    /**
     * Gets the Mesh Registy support interface. Registry support provides a
     * location accessible to all participants in the Mesh where objects and
     * data can be stored, discovered, and retrieved.
     * 
     * @return The registry.
     */
    public Registry registry();

    /**
     * Gets the Remoting support interface. Remoting support allows rmi like
     * export of objects in the mesh.
     * 
     * @return The remoting interface.
     */
    public Remoting remoting();

    /**
     * Gets the Eventing support interface. Eventing support allows users to
     * create and listen for events in the Mesh.
     * 
     * @return The Eventing interface.
     */
    public Eventing eventing();

    /**
     * Gets the Repository support interface. Repository support allows the
     * sharing of artifacts amongst mesh participants. Unlike Registry support,
     * Repository support is intended for large artifacts or directory
     * structures, and also allows referencing resources that are outside the
     * mesh for example in a remote webdav server.
     * 
     * @return Repository Support.
     */
    public Repository repository();

    /**
     * Gets the launcher support interface. Launcher support allows launching of
     * remote processes and Runnables on a Mesh agent.
     * 
     * @return The Launcher support interface
     */
    public Launcher launcher();
}
