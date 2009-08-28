/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.cloudlaunch.distribution.rmi.vm;


import org.fusesource.cloudlaunch.distribution.Distributable;
import org.fusesource.cloudlaunch.distribution.rmi.AbstractExporter;

/**
 * VMExporter
 * <p>
 * Description: An in vm exporter.
 * </p>
 * 
 * @author cmacnaug
 * @version 1.0
 */
public class VMExporter extends AbstractExporter {

    /*
     * (non-Javadoc)
     * 
     * @see org.fusesource.cloudlaunch.distribution.rmi.IExporter#destroy()
     */
    public void destroy() throws Exception {
        //No-op
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.fusesource.cloudlaunch.distribution.rmi.IExporter#unexport(org.fusesource
     * .cloudlaunch.distribution.Distributable)
     */
    public void unexport(Distributable obj) throws Exception {
        return;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.fusesource.cloudlaunch.distribution.rmi.AbstractExporter#export(java
     * .lang.Object, java.lang.Class<?>[])
     */
    @Override
    protected <T> T export(Object obj, Class<?>[] interfaces) throws Exception {
        //It is possible that in the future we would actually want to create
        //a proxy here. The reason for doing this would be to better match the 
        //threading model associated with @Oneway method calls which in the distributed
        //version would happen asynchronously. 
        return (T) obj;
    }
}
