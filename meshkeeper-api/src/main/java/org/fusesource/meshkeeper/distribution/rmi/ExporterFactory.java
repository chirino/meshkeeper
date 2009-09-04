/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.meshkeeper.distribution.rmi;


import org.fusesource.meshkeeper.distribution.AbstractPluginFactory;
import org.fusesource.meshkeeper.distribution.FactoryFinder;

/**
 * ExporterFactory
 * <p>
 * Description: Factory interface for creating {@link IExporter}s
 * </p>
 * 
 * @author cmacnaug
 * @version 1.0
 */
public class ExporterFactory extends AbstractPluginFactory<IExporter> {

    private static final FactoryFinder EXPORTER_FACTORY_FINDER = new FactoryFinder("META-INF/services/org/fusesource/meshkeeper/distribution/exporter/");

    @Override
    protected final FactoryFinder getFactoryFinder() {
        return EXPORTER_FACTORY_FINDER;
    }
}
