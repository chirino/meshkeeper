/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
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
 * 
 * @author cmacnaug
 * @version 1.0
 */
public class RmiViaJmsExporterFactory extends ExporterFactory {

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.fusesource.cloudlaunch.distribution.rmi.ExporterFactory#createExporter
     * (java.lang.String)
     */
    @Override
    public IExporter createPlugin(String uri) throws Exception {

        URI connectUri = new URI(URISupport.stripPrefix(uri, "rmiviajms:"));

        RmiViaJmsExporter exporter = new RmiViaJmsExporter();

        Map<String, String> props = URISupport.parseParamters(connectUri);
        if (!props.isEmpty()) {
            IntrospectionSupport.setProperties(exporter, URISupport.parseQuery(uri));
            connectUri = URISupport.removeQuery(new URI(uri));
            //Add back unused query props:
            connectUri = URISupport.createRemainingURI(connectUri, props);
        }
        exporter.setProviderUri(connectUri.toString());
        exporter.start();
        return exporter;
    }

}
