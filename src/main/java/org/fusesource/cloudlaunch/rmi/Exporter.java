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
package org.fusesource.cloudlaunch.rmi;

import java.io.Serializable;
import java.rmi.Remote;

import javax.jms.Destination;

import org.fusesource.cloudlaunch.registry.Registry;
import org.fusesource.rmiviajms.JMSRemoteObject;

/**
 * Exporter
 * <p>
 * Description:
 * </p>
 * 
 * @author cmacnaug
 * @version 1.0
 */
public class Exporter {

    Registry registry;
    private Remote source;
    private String path;
    private Destination destination;
    private String actualPath;
    private Remote exportedStub;

    public void export() throws Exception {
        if (exportedStub == null) {
            exportedStub = JMSRemoteObject.exportObject(source, destination);
            actualPath = registry.addObject(path, true, (Serializable) exportedStub);
            System.out.println("Registered as: " + actualPath);
        }
    }

    public void destroy() throws Exception {
        if (exportedStub != null) {
            JMSRemoteObject.unexportObject(exportedStub, true);
            exportedStub = null;
        }
        if (actualPath != null) {
            registry.remove(actualPath, true);
            actualPath = null;
        }
    }
    
    public void setRegistry(Registry registry) {
        this.registry = registry;
    }
    
    public Registry getRegistry() {
        return registry;
    }

    public Remote getSource() {
        return source;
    }

    public void setSource(Remote source) {
        this.source = source;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Destination getDestination() {
        return destination;
    }

    public void setDestination(Destination destination) {
        this.destination = destination;
    }

}
