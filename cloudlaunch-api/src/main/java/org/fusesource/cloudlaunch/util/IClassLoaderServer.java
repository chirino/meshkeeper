/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.cloudlaunch.util;

import java.util.List;
import java.net.URL;
import java.io.IOException;
import java.io.Serializable;

import org.fusesource.cloudlaunch.distribution.Distributable;

/**
 * @author chirino
*/
public interface IClassLoaderServer extends Distributable {
    public static class PathElement implements Serializable {
        private static final long serialVersionUID = 1L;
        URL url;
        long jarFileChecksum;
        long jarFileSize;
    }

    IClassLoaderServer getParent() throws Exception;
    List<PathElement> getPathElements() throws Exception;
    byte[] download(URL url) throws IOException;
    byte[] findResource(String name) throws Exception;

}