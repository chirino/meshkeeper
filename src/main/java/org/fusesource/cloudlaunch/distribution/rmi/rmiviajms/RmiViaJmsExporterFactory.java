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
package org.fusesource.cloudlaunch.distribution.rmi.rmiviajms;

import java.net.URI;
import java.util.Map;

import org.fusesource.cloudlaunch.distribution.rmi.ExporterFactory;
import org.fusesource.cloudlaunch.distribution.rmi.IExporter;
import org.fusesource.cloudlaunch.util.internal.IntrospectionSupport;
import org.fusesource.cloudlaunch.util.internal.URISupport;

/** 
 * RMIViaJmsExporterFactory
 * <p>
 * Description:
 * </p>
 * @author cmacnaug
 * @version 1.0
 */
public class RmiViaJmsExporterFactory implements ExporterFactory {

    /* (non-Javadoc)
     * @see org.fusesource.cloudlaunch.distribution.rmi.ExporterFactory#createExporter(java.lang.String)
     */
    public IExporter createExporter(String uri) throws Exception {
        URI connectUri = new URI(URISupport.stripPrefix(uri, "rmiviajms:"));

        RmiViaJmsExporter exporter = new RmiViaJmsExporter();

        Map<String, String> props = URISupport.parseParamters(connectUri);
        if (!props.isEmpty()) {
            IntrospectionSupport.setProperties(exporter, URISupport.parseQuery(uri));
            connectUri = URISupport.removeQuery(new URI(uri));
            //Add back unused query props:
            connectUri = URISupport.createRemainingURI(connectUri, props);
        }
        exporter.setConnectUrl(connectUri.toString());
        return exporter;
    }

}
