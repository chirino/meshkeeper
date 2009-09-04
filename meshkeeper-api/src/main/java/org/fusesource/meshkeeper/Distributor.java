/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.meshkeeper;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.concurrent.TimeoutException;



/**
 * Distributor
 * <p>
 * A distributor provides access to meshkeeper distribution services.
 * </p>
 * @author cmacnaug
 * @version 1.0
 */
public interface Distributor {

    public interface DistributionRef<D extends Distributable>
    {
        public String getRegistryPath();
        public D getProxy();
        public D getTarget();
    }

    /**
     * Gets a uri which can be used to connect to a meshkeeper server
     * @return
     */
    public String getDistributorUri();

    /**
     * Starts distributor services.
     * @throws Exception
     */
    public void start() throws Exception;

    /**
     * Closes the distributor cleaning up all distributed references.
     */
    public void destroy() throws Exception;

    /**
     * This is a convenience method to register and export a Distributable object. This is equivalent
     * to calling:
     * <code>
     * <br>{@link #export(Distributable)};
     * <br>{@link #addRegistryObject(String, boolean, Serializable)};
     * </code>
     *
     * It is best practice to call {@link #undistribute(Distributable)} once the object is no longer needed.
     *
     *
     * @param path The path at which to register the exported object.
     * @param sequential Whether the registry path should be registered as a unique node at the given path.
     * @param distributable The {@link Distributable} object.
     * @return a {@link DistributionRef} to the distributed object.
     */
    public <T extends Distributable> DistributionRef<T> distribute(String path, boolean sequential, T distributable) throws Exception;

    /**
     * Called to undistribute a previously distributed object. This is equivalent to calling
     * <code>
     * <br>{@link #unexport(Distributable)};
     * <br>{@link #removeRegistryObject(String, boolean, Serializable)};
     * </code>
     * @param distributable The object that previously distributed.
     */
    public void undistribute(Distributable distributable) throws Exception;

    ////////////////////////////////////////////////////////////////////////////////////////////
    //Registry Related Operations
    ////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Adds an object to the registry at the given path. If sequential is
     * true then the object will be added at the given location with a unique
     * name. Otherwise the object will be added at the location given by path.
     *
     * @param path The path to add to.
     * @param sequential When true a unique child node is created at the given path
     * @param o The object to add.
     * @return The path at which the element was added.
     * @throws Exception If there is an error adding the node.
     */
    public String addRegistryObject(String path, boolean sequential, Serializable o) throws Exception;

    /**
     * Gets the data at the specified node as an object.
     * @param <T> The type of the object expected.
     * @param path The path of the object.
     * @return The object at the given node.
     * @throws Exception If the object couldn't be retrieved.
     */
    public <T> T getRegistryObject(String path) throws Exception;

    /**
     * Gets the data at the specified node.
     * @param path The path of the data.
     * @return The data at the given node.
     * @throws Exception If the object couldn't be retrieved.
     */
    public byte [] getRegistryData(String path) throws Exception;

    /**
     * Removes a node from the registry.
     *
     * @param path The path to remove.
     * @param recursive If true then any children will also be removed.
     * @throws Exception If the path couldn't be removed.
     */
    public void removeRegistryData(String path, boolean recursive) throws Exception;


    /**
     * Adds data to the registry at the given path. If sequential is
     * true then the data will be added at the given location with a unique
     * name. Otherwise the data will be added at the location given by path.
     *
     * @param path The path to add to.
     * @param sequential When true a unique child node is created at the given path
     * @param data The data. If null then a 0 byte array will be stored in the registry
     * @return The path at which the element was added.
     * @throws Exception If there is an error adding the node.
     */
    public String addRegistryData(String path, boolean sequential, byte[] data) throws Exception ;


    /**
     * Adds a listener for changes in a path's child elements.
     * @param path
     * @param watcher
     */
    public void addRegistryWatcher(String path, RegistryWatcher watcher) throws Exception;

    /**
     * Removes a previously registered
     * @param path The path on which the listener was listening.
     * @param watcher The watcher
     */
    public void removeRegistryWatcher(String path, RegistryWatcher watcher) throws Exception;


    /**
     * Convenience method that waits for a minimum number of objects to be registered at the given
     * registry path.
     *
     * @param <T>
     * @param path The path
     * @param min The minimum number of objects to wait for.
     * @param timeout The maximum amount of time to wait.
     * @return The objects that were registered.
     * @throws Exception
     */
    public <T> Collection<T> waitForRegistrations(String path, int min, long timeout) throws TimeoutException, Exception;

    ////////////////////////////////////////////////////////////////////////////////////////////
    //RMI Related Operations
    ////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Exports a {@link Distributable} object returning an RMI proxy to the Distibutable object. The proxy
     * can then be passed to other applications in the mesh to use via RMI. It is best practice
     * to unexport the object when it is no longer used.
     *
     * The exported object
     *
     * @param <T>
     * @param obj
     * @return
     * @throws Exception
     */
    public <T extends Distributable> T export(T obj) throws Exception;

    /**
     * Unexports a previously exported object.
     *
     * @param obj The object that had previously been exported.
     * @throws Exception If there is an error unexporting the object.
     */
    public void unexport(Distributable obj) throws Exception;

    ////////////////////////////////////////////////////////////////////////////////////////////
    //Event Related Operations
    ////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Sends an event on the given topic.
     */
    public void sendEvent(Event event, String topic) throws Exception;

    /**
     * Opens a listener on the given event topic.
     *
     * @param listener The listener
     * @param topic The topic
     * @throws Exception If there is an error opening the listener
     */
    public void openEventListener(EventListener listener, String topic) throws Exception;

    /**
     * Stops listening to events on the given topic.
     *
     * @param listener The listener The listener
     * @param topic The topic
     * @throws Exception If there is an error closing the listener
     */
    public void closeEventListener(EventListener listener, String topic) throws Exception;

    ////////////////////////////////////////////////////////////////////////////////////////////
    //Resource Related Operations
    ////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Factory method for creating a resource.
     * @return An empty resource.
     */
    public Resource createResource();

    /**
     * Called to locate the given resource.
     * @param resource The resource to locate.
     * @throws Exception If there is an error locating the resource.
     */
    public void resolveResource(Resource resource) throws Exception;

    /**
     * @param resource
     * @param data
     * @throws IOException
     */
    public void deployFile(Resource resource, byte[] data) throws Exception;

    /**
     *
     * @param resource
     * @param d
     * @throws Exception
     */
    public void deployDirectory(Resource resource, File d) throws Exception;

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
