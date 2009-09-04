/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.meshkeeper.distribution.rmi.vm;

import org.fusesource.meshkeeper.distribution.rmi.ExporterFactory;
import org.fusesource.meshkeeper.distribution.rmi.IExporter;

/** 
 * VMExporterFactory
 * <p>
 * Description:
 * </p>
 * @author cmacnaug
 * @version 1.0
 */
public class VMExporterFactory extends ExporterFactory{

    /* (non-Javadoc)
     * @see org.fusesource.meshkeeper.distribution.rmi.ExporterFactory#createExporter(java.lang.String)
     */
    @Override
    protected IExporter createPlugin(String uri) throws Exception {
        
        return new VMExporter();
    }

}
