/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.cloudlaunch.registry;

import java.io.Serializable;

/** 
 * Registry
 * <p>
 * Description:
 * </p>
 * @author cmacnaug
 * @version 1.0
 */
public interface Registry {

    /**
     * Connects to the registry.
     * @throws Exception
     */
    public void connect() throws Exception;
    
    /**
     * Disconnects from the registry and cleans up resources
     * held by the registry. 
     * @throws Exception
     */
    public void close() throws Exception;
    
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
    public String addObject(String path, boolean sequential, Serializable o) throws Exception;

    /**
     * Gets the data at the specified node as an object. 
     * @param <T> The type of the object expected. 
     * @param path The path of the object. 
     * @return The object at the given node. 
     * @throws Exception If the object couldn't be retrieved. 
     */
    public <T> T getObject(String path) throws Exception;
    
    /**
     * Removes a node from the registry.
     * 
     * @param path The path to remove.
     * @param recursive If true then any children will also be removed.
     * @throws Exception If the path couldn't be removed. 
     */
    public void remove(String path, boolean recursive) throws Exception;
    
    
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
    public String addData(String path, boolean sequential, byte[] data) throws Exception ;

    
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
    public void removeRegistryWatcher(String path, RegistryWatcher watcher);
}
