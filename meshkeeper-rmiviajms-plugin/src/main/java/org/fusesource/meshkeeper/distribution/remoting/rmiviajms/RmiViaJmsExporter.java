/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.meshkeeper.distribution.remoting.rmiviajms;

import java.rmi.Remote;

import org.fusesource.meshkeeper.Distributable;
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

    @SuppressWarnings("unchecked")
    protected <T> T exportInterfaces(T obj, Class<?>[] interfaces) throws Exception {
        return (T) JMSRemoteObject.export(obj, interfaces);
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
        System.setProperty("org.fusesource.rmiviajms.REMOTE_SYSTEM_CLASS", MeshKeeperRemoteJMSSystem.class.getName());
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(RmiViaJmsExporter.class.getClassLoader());
        try {
            MeshKeeperRemoteJMSSystem.initialize(providerUri);
            //System.out.println("Initialized remote system: " + JMSRemoteSystem.INSTANCE);
            JMSRemoteObject.addOneWayAnnotation(Oneway.class);
        } finally {
            Thread.currentThread().setContextClassLoader(original);
        }
    }

    @Override
    public void setUserClassLoader(ClassLoader loader) {
        //System.out.println("Setting user classloader: " + loader);
        JMSRemoteSystem.INSTANCE.setUserClassLoader(loader);
    }

    public void destroy() throws InterruptedException, Exception {
        JMSRemoteObject.resetSystem();
    }

    public String toString() {
        return "RmiViaJmsExporter at " + MeshKeeperRemoteJMSSystem.PROVIDER_URI;
    }

}
