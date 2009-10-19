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
package org.fusesource.meshkeeper.distribution.remoting.rmiviajms;

import java.rmi.Remote;
import java.util.concurrent.atomic.AtomicBoolean;

import org.fusesource.meshkeeper.Oneway;
import org.fusesource.meshkeeper.distribution.remoting.AbstractRemotingClient;
import org.fusesource.rmiviajms.JMSRemoteObject;
import org.fusesource.rmiviajms.internal.JMSRemoteSystem;

/**
 * RmiViaJmsExporter
 * <p>
 * Description: Exports Objects via RMIviaJMS
 * </p>
 * 
 * @author cmacnaug
 * @version 1.0
 */
class RmiViaJmsExporter extends AbstractRemotingClient {

    String providerUri;
    private AtomicBoolean started = new AtomicBoolean(false);

    //TODO we should keep track of what gets exported and unexport it during destroy.

    @SuppressWarnings("unchecked")
    protected <T> T exportInterfaces(T obj, String multiCastAddress, Class<?>[] interfaces) throws Exception {
        if (multiCastAddress != null) {
            return (T) JMSRemoteObject.export(obj, JMSRemoteObject.MULTICAST_PREFIX + multiCastAddress, interfaces);
        } else {
            return (T) JMSRemoteObject.export(obj, interfaces);
        }
    }

    /* (non-Javadoc)
     * @see org.fusesource.meshkeeper.MeshKeeper.Remoting#getMulticastProxy(java.lang.String, java.lang.Class<?>[])
     */
    @SuppressWarnings("unchecked")
    public <T> T getMulticastProxy(String address, Class<?> mainInterface, Class<?>... interfaces) throws Exception {
        return (T) JMSRemoteObject.toProxy(JMSRemoteObject.MULTICAST_PREFIX + address, mainInterface, interfaces);
    }
    
    public void unexport(Object obj) throws Exception {
        if (obj instanceof Remote) {
            JMSRemoteObject.unexportObject((Remote) obj, true);
        }
    }

    public void setProviderUri(String providerUri) {
        this.providerUri = providerUri;
    }

    public void start() throws Exception {
        if (started.compareAndSet(false, true)) {
            System.setProperty("org.fusesource.rmiviajms.REMOTE_SYSTEM_CLASS", MeshKeeperRemoteJMSSystem.class.getName());
            ClassLoader original = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(RmiViaJmsExporter.class.getClassLoader());
            try {
                MeshKeeperRemoteJMSSystem.addRef();
                MeshKeeperRemoteJMSSystem.initialize(providerUri);
                //System.out.println("Initialized remote system: " + JMSRemoteSystem.INSTANCE);
                JMSRemoteObject.addOneWayAnnotation(Oneway.class);
            } finally {
                Thread.currentThread().setContextClassLoader(original);
            }
        }
    }

    @Override
    public void setUserClassLoader(ClassLoader loader) {
        //System.out.println("Setting user classloader: " + loader);
        JMSRemoteSystem.INSTANCE.setUserClassLoader(loader);
    }

    public void destroy() throws InterruptedException, Exception {
        if (started.compareAndSet(true, false)) {
            //System.out.println("Destroying RmiViaJMSExporter " + this);
            MeshKeeperRemoteJMSSystem.removeRef();
        }
    }

    public String toString() {
        return "RmiViaJmsExporter at " + MeshKeeperRemoteJMSSystem.PROVIDER_URI;
    }
}
