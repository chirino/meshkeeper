/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.cloudlaunch.distribution.rmi;

import java.net.URI;

import org.fusesource.cloudlaunch.util.internal.FactoryFinder;

/** 
 * ExporterFactory
 * <p>
 * Description:
 * Factory interface for creating {@link IExporter}s
 * </p>
 * @author cmacnaug
 * @version 1.0
 */
public abstract class ExporterFactory {

    private static final FactoryFinder EXPORTER_FACTORY_FINDER = new FactoryFinder("META-INF/services/org/fusesource/cloudlaunch/distribution/exporter/");
    
    public static final IExporter create(String uri) throws Exception
    {
        ClassLoader originalLoader = Thread.currentThread().getContextClassLoader();
        try
        {
            ClassLoader cl = originalLoader;
            Thread.currentThread().setContextClassLoader(cl);
            URI rmiUri = new URI(uri);
            ExporterFactory ef = (ExporterFactory) EXPORTER_FACTORY_FINDER.newInstance(rmiUri.getScheme());
            return ef.createExporter(rmiUri.toString());
        }
        finally
        {
            Thread.currentThread().setContextClassLoader(originalLoader);
        }
    }
    
    protected abstract IExporter createExporter(String uri) throws Exception;
    
}
