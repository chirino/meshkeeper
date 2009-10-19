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
package org.fusesource.meshkeeper.distribution.remoting.vm;

import org.fusesource.meshkeeper.distribution.remoting.AbstractRemotingClient;

/**
 * VMExporter
 * <p>
 * Description: An in vm exporter.
 * </p>
 * 
 * @author cmacnaug
 * @version 1.0
 */
public class VMExporter extends AbstractRemotingClient {

    public void start() {
        //No-Op
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.fusesource.meshkeeper.distribution.rmi.IExporter#destroy()
     */
    public void destroy() throws Exception {
        //No-op
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.fusesource.meshkeeper.distribution.rmi.IExporter#unexport(org.fusesource
     * .meshkeeper.distribution.Distributable)
     */
    public void unexport(Object obj) throws Exception {
        return;
    }
  
    public <T> T getMulticastProxy(String address, Class<?> mainInterface,  Class<?>... interfaces) throws Exception {
        throw new UnsupportedOperationException("multicast not implemented");
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see
     * org.fusesource.meshkeeper.distribution.rmi.AbstractExporter#export(java
     * .lang.Object, java.lang.Class<?>[])
     */
    protected <T> T exportInterfaces(T obj, String multicastAddress, Class<?>[] interfaces) throws Exception {
        if (multicastAddress != null) {
            throw new UnsupportedOperationException("multicast not implemented");
        }
        //It is possible that in the future we would actually want to create
        //a proxy here. The reason for doing this would be to better match the 
        //threading model associated with @Oneway method calls which in the distributed
        //version would happen asynchronously. 
        return (T) obj;
    }
}
