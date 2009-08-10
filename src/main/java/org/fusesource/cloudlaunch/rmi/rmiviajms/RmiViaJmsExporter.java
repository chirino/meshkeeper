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
package org.fusesource.cloudlaunch.rmi.rmiviajms;

import java.rmi.Remote;

import org.fusesource.cloudlaunch.rmi.AbstractExporter;
import org.fusesource.cloudlaunch.rmi.Distributable;
import org.fusesource.cloudlaunch.rmi.IExporter;
import org.fusesource.cloudlaunch.rmi.Oneway;
import org.fusesource.rmiviajms.JMSRemoteObject;
import org.fusesource.rmiviajms.internal.ActiveMQRemoteSystem;

/**
 * RmiViaJmsExporter
 * <p>
 * Description: Exports Objects via RMIviaJMS
 * </p>
 * 
 * @author cmacnaug
 * @version 1.0
 */
public class RmiViaJmsExporter extends AbstractExporter {

    String connectUrl = ActiveMQRemoteSystem.CONNECT_URL;

    //Add our version of the oneway annotation. 
    static {
        JMSRemoteObject.addOneWayAnnotation(Oneway.class);
    }

    protected <T> T export(Object obj, Class<?>[] interfaces) throws Exception {
        return (T) JMSRemoteObject.exportNonRemote(obj, interfaces);
    }

    public void unexport(Distributable obj) throws Exception {
        if (obj instanceof Remote) {
            JMSRemoteObject.unexportObject((Remote) obj, true);
        }
    }

    public void setConnectUrl(String connectUrl) {
        this.connectUrl = connectUrl;
        System.setProperty("org.fusesource.rmiviajms.CONNECT_URL", connectUrl);
    }

    public void destroy() throws InterruptedException, Exception {
        JMSRemoteObject.resetSystem();
    }
    
    public String toString()
    {
        return "RmiViaJmsExporter at " + ActiveMQRemoteSystem.CONNECT_URL;
    }

}
