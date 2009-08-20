/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.cloudlaunch.util;

import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


/**
 * @author chirino
*/
public class ClassLoaderServer implements IClassLoaderServer {

    private final IClassLoaderServer parent;
    private final ClassLoader classLoader;
    List<PathElement> elements;
    static final long ROUNDUP_MILLIS = 1999;

    public ClassLoaderServer(IClassLoaderServer parent, ClassLoader classLoader) throws IOException {
        this.parent = parent;
        this.classLoader = classLoader;
        elements = createElements(classLoader);
    }

    public IClassLoaderServer getParent() {
        return parent;
    }

    public List<PathElement> getPathElements() {
        return elements;
    }

    public byte[] download(URL url) throws IOException {
        for (PathElement element : elements) {
            if( element.url.equals(url) ) {
                return read(url.openStream());
            }
        }
        return null;
    }

    public byte[] findResource(String name) {
        InputStream is = classLoader.getResourceAsStream(name);
        if( is ==null )
            return null;
        return read(is);
    }
    static List<PathElement> createElements(ClassLoader classLoader) throws IOException {
        if( !(classLoader instanceof URLClassLoader) ) {
            return null;
        }
        URLClassLoader ucl = (URLClassLoader) classLoader;
        URL[] urls = ucl.getURLs();

        List<PathElement> rc = new ArrayList<PathElement>();
        for (URL url : urls) {
            PathElement element = new PathElement();
            element.url = url;
            if( "file".equals(url.getProtocol()) ) {
                File file = new File(url.getFile());

                if( !file.exists() ) {
                    continue;
                }

                // We have to jar up dirs..
                if( file.isDirectory() ) {
                    file = jar(file);
                    file.deleteOnExit();
                    element.url = file.toURI().toURL();
                }

                element.jarFileSize = file.length();
                element.jarFileChecksum = RemoteClassLoader.checksum(new FileInputStream(file));
            }
            rc.add(element);
        }
        return rc;
    }

    private static File jar(File source) throws IOException {
        File tempJar = File.createTempFile("temp", ".jar");
        ZipOutputStream os = new ZipOutputStream(new FileOutputStream(tempJar));
        os.setMethod(ZipOutputStream.DEFLATED);
        os.setLevel(5);
        recusiveAdd(os, source, null);
        os.close();
        return tempJar;
    }

    private static void recusiveAdd(ZipOutputStream os, File source, String jarpath) throws IOException {
        String prefix = "";
        if( jarpath!=null ) {
            ZipEntry entry = new ZipEntry(jarpath);
            entry.setTime(source.lastModified()+ROUNDUP_MILLIS);
            os.putNextEntry(entry);
            prefix = jarpath+"/";
        }

        if ( source.isDirectory() ) {
            for (File file : source.listFiles()) {
                recusiveAdd(os, file, prefix+file.getName());
            }
        } else {
            FileInputStream is = new FileInputStream(source);
            try {
                copy(is, os);
            } finally {
                try {
                    is.close();
                } catch (Throwable e) {
                }
            }
        }
    }

    private static byte[] read(InputStream is) {
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            copy(is, os);
            return os.toByteArray();
        } catch (IOException e) {
            return null;
        } finally {
            try {
                is.close();
            } catch (IOException ignore) {
            }
        }
    }

    private static void copy(InputStream is, OutputStream os) throws IOException {
        byte buffer[] = new byte[1024*4];
        int c;
        while( (c=is.read(buffer)) > 0 ) {
            os.write(buffer, 0, c);
        }
    }
}