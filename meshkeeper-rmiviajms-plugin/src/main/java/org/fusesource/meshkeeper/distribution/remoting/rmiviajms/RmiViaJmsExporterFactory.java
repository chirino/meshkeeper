/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.meshkeeper.distribution.remoting.rmiviajms;

import java.net.URI;
import java.util.Map;

import org.fusesource.meshkeeper.distribution.remoting.RemotingFactory;
import org.fusesource.meshkeeper.distribution.remoting.RemotingClient;
import org.fusesource.meshkeeper.util.internal.IntrospectionSupport;
import org.fusesource.meshkeeper.util.internal.URISupport;

/**
 * RMIViaJmsExporterFactory
 * <p>
 * Description:
 * </p>
 * 
 * @author cmacnaug
 * @version 1.0
 */
public class RmiViaJmsExporterFactory extends RemotingFactory {

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.fusesource.meshkeeper.distribution.rmi.ExporterFactory#createExporter
     * (java.lang.String)
     */
    @Override
    public RemotingClient createPlugin(String uri) throws Exception {

        URI connectUri = new URI(URISupport.stripPrefix(uri, "rmiviajms:"));

        RmiViaJmsExporter exporter = new RmiViaJmsExporter();
        applyQueryParameters(exporter, connectUri);
        exporter.setProviderUri(connectUri.toString());
        exporter.start();
        return exporter;
    }

}
