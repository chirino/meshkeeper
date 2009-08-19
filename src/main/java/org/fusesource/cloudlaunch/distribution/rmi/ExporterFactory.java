/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.cloudlaunch.distribution.rmi;

/** 
 * ExporterFactory
 * <p>
 * Description:
 * Factory interface for creating {@link IExporter}s
 * </p>
 * @author cmacnaug
 * @version 1.0
 */
public interface ExporterFactory {

    public IExporter createExporter(String uri) throws Exception;
    
}
