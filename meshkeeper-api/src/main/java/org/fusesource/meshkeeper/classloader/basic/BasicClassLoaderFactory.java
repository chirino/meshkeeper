/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/

package org.fusesource.meshkeeper.classloader.basic;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.fusesource.meshkeeper.classloader.ClassLoaderFactory;
import org.fusesource.meshkeeper.util.internal.HexSupport;

import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author chirino
 */
public class BasicClassLoaderFactory implements ClassLoaderFactory {
    private static final long serialVersionUID = 1L;
    private static final int CHUNK_SIZE = 1024*64;
    private static final Log LOG = LogFactory.getLog(BasicClassLoaderFactory.class);

    private BasicClassLoaderServer.IServer server;
    private final long id;

    public BasicClassLoaderFactory(BasicClassLoaderServer.IServer proxy, long id) {
        this.server = proxy;
        this.id = id;
    }

//    TODO: We may need to implement a custom classloader to stream resources directly
//    if we want to support non URLClassLoaders...
//
//    public class RemoteClassLoader extends ClassLoader {
//
//        @SuppressWarnings("unchecked")
//        public Class findClass(String name) throws ClassNotFoundException {
//            String path = name.replace('.', '/').concat(".class");
//            try {
//                byte data[] = server.findResource(path);
//                if (data == null) {
//                    throw new ClassNotFoundException(name);
//                }
//                return defineClass(name, data, 0, data.length);
//            } catch (Exception e) {
//                throw new ClassNotFoundException(name);
//            }
//        }
//    }


    public ClassLoader createClassLoader(ClassLoader parent, File cacheDir) throws Exception {

//        parent = createRemoteClassLoader(server.getParent(), cacheDir, depth - 1, parent);
        List<BasicClassLoaderServer.PathElement> elements = server.getPathElements(id);
        if (elements == null) {
            // That classloader was not URL classloader based, so we could not import it
            // by downloading it's jars.. we will have to use dynamically.
            // return new RemoteClassLoader(parent, server);
            throw new IOException("Unexpected Remote Response");
        }

        // We can build stadard URLClassLoader by downloading all the
        // jars or using the same URL elements as the original classloader.
        ArrayList<URL> urls = new ArrayList<URL>();
        for (BasicClassLoaderServer.PathElement element : elements) {

            if (element.url!=null) {
                urls.add(element.url);
            } else {

                cacheDir.mkdirs();
                String name = HexSupport.toHexFromBytes(element.fingerprint)+".jar";
                File file = new File(cacheDir, name);

                if (!file.exists()) {
                    LOG.debug("Downloading: "+file);
                    // We need to download it...
                    File tmp = null;
                    FileOutputStream out = null;
                    try {
                        tmp = File.createTempFile(name, ".part", cacheDir);
                        out = new FileOutputStream(tmp);
                        int pos = 0;
                        while( true ) {
                            byte[] data = server.download(element.id, pos, CHUNK_SIZE);
                            out.write(data);
                            if( data.length < CHUNK_SIZE ) {
                                break;
                            }
                            pos += CHUNK_SIZE;
                        }
                    } finally {
                        try {
                            out.close();
                        } catch (Throwable e) {
                        }
                    }
                    if (!tmp.renameTo(file)) {
                        tmp.delete();
                    }
                }

                // It may be in the cache dir already...
                if (file.exists()) {
                    if ( !Arrays.equals(element.fingerprint, fingerprint(new FileInputStream(file)) )
                            || element.length != file.length()) {
                        throw new IOException("fingerprint missmatch: " + name);
                    }

                    urls.add(file.toURI().toURL());
                } else {
                    throw new IOException("Could not download: " + name);
                }
            }
        }

        URL t[] = new URL[urls.size()];
        urls.toArray(t);
        //System.out.println("Created URL class loader with: " + urls);
        return new URLClassLoader(t, parent);
    }


    static byte[] fingerprint(InputStream is) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte buffer[] = new byte[1024 * 4];
            int c;
            while ((c = is.read(buffer)) > 0) {
                md.update(buffer, 0, c);
            }
            return md.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(e.toString());
        } finally {
            try {
                is.close();
            } catch (IOException ignore) {
            }
        }
    }
}