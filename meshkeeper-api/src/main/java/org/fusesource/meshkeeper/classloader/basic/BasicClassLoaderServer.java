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
import org.fusesource.meshkeeper.Distributable;
import org.fusesource.meshkeeper.MeshKeeper;
import org.fusesource.meshkeeper.classloader.ClassLoaderFactory;
import org.fusesource.meshkeeper.classloader.ClassLoaderServer;
import static org.fusesource.meshkeeper.util.internal.FileSupport.jar;
import static org.fusesource.meshkeeper.util.internal.FileSupport.read;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author chirino
 */
public class BasicClassLoaderServer implements ClassLoaderServer {

    private static final Log LOG = LogFactory.getLog(BasicClassLoaderServer.class);

    static final long ROUNDUP_MILLIS = 1999;
    private final MeshKeeper meshKeeper;

    public static class PathElement implements Serializable {
        private static final long serialVersionUID = 1L;
        public long id;
        public byte[] fingerprint;
        public long length;
        public URL url;
    }

    public interface IServer extends Distributable {
        List<PathElement> getPathElements(long classLoaderId) throws Exception;

        byte[] download(long fileId, int pos, int length) throws IOException;
    }

    public class Server implements IServer {

        public List<PathElement> getPathElements(long classLoaderId) throws Exception {
            LOG.debug("Client is downloading the classpath list for " + classLoaderId);
            ArrayList<ExportedFile> files = exportedClassLoaders.get(classLoaderId);
            if (files == null) {
                throw new IllegalArgumentException("Requested class loader not found.");
            }
            ArrayList<PathElement> rc = new ArrayList<PathElement>(files.size());
            for (ExportedFile file : files) {
                //LOG.debug("CP [ " + classLoaderId + " ]" + file.file);
                rc.add(file.element);
            }
            return rc;
        }

        public byte[] download(long fileId, int pos, int length) throws IOException {
            ExportedFile exportedFile = exportedFiles.get(fileId);
            if (exportedFile == null) {
                throw new IllegalArgumentException("Requested file not found: " + fileId);
            }
            File file = exportedFile.jared == null ? exportedFile.file : exportedFile.jared;
            LOG.debug("Client downloading from: " + file + " starting at " + pos);
            return read(file, pos, length);
        }
    }

    static class ExportedFile {
        final public PathElement element = new PathElement();;
        public File file;
        public File jared;
    }

    private final ConcurrentHashMap<ClassLoader, ClassLoaderFactory> factories = new ConcurrentHashMap<ClassLoader, ClassLoaderFactory>();
    private final static AtomicLong ids = new AtomicLong();
    private final ConcurrentHashMap<Long, ArrayList<ExportedFile>> exportedClassLoaders = new ConcurrentHashMap<Long, ArrayList<ExportedFile>>();
    private final ConcurrentHashMap<Long, ExportedFile> exportedFiles = new ConcurrentHashMap<Long, ExportedFile>();
    private final Server server = new Server();
    private IServer proxy;

    public BasicClassLoaderServer(MeshKeeper meshKeeper) {
        this.meshKeeper = meshKeeper;
    }

    synchronized public void start() throws Exception {
        if (proxy == null) {
            proxy = (IServer) meshKeeper.remoting().export(server);
        }
    }

    synchronized public void stop() throws Exception {
        if (proxy != null) {
            meshKeeper.remoting().unexport(proxy);
            proxy = null;
        }
    }

    public ClassLoaderFactory export(List<File> classPath) throws IOException {
        ArrayList<ExportedFile> exports = new ArrayList<ExportedFile>();
        for (File file : classPath) {
            addExportedFile(exports, file);
        }
        long id = ids.incrementAndGet();
        exportedClassLoaders.put(id, exports);
        for (ExportedFile export : exports) {
            exportedFiles.put(export.element.id, export);
        }
        return new BasicClassLoaderFactory(proxy, id);
    }

    public ClassLoaderFactory export(ClassLoader classLoader, int maxExportDepth) throws IOException {
        ClassLoaderFactory factory = factories.get(classLoader);
        if (factory == null) {

            ArrayList<ExportedFile> exports = new ArrayList<ExportedFile>();
            addExportedFiles(classLoader, maxExportDepth, exports);
            long id = ids.incrementAndGet();
            exportedClassLoaders.put(id, exports);
            for (ExportedFile export : exports) {
                exportedFiles.put(export.element.id, export);
            }
            factory = new BasicClassLoaderFactory(proxy, id);
            factories.put(classLoader, factory);

        }
        return factory;
    }

    private static void addExportedFiles(ClassLoader classLoader, int maxExportDepth, ArrayList<ExportedFile> elements) throws IOException {
        if (maxExportDepth > 0) {
            if (classLoader.getParent() != null) {
                addExportedFiles(classLoader.getParent(), maxExportDepth - 1, elements);
            }
        }
        addExportedFiles(classLoader, elements);
    }

    private static void addExportedFiles(ClassLoader classLoader, ArrayList<ExportedFile> elements) throws IOException {
        if (!(classLoader instanceof URLClassLoader)) {
            throw new IOException("Encountered a non URLClassLoader classloader.");
        }

        URLClassLoader ucl = (URLClassLoader) classLoader;
        URL[] urls = ucl.getURLs();

        for (URL url : urls) {

            if ("file".equals(url.getProtocol())) {
                File file;
                try {
                    file = new File(url.toURI());
                } catch (URISyntaxException e) {
                    IOException ioe = new IOException(e.getMessage());
                    ioe.initCause(e);
                    throw ioe;
                }
                addExportedFile(elements, file);
            } else {
                ExportedFile exportedFile = new ExportedFile();
                exportedFile.element.id = ids.incrementAndGet();
                exportedFile.element.url = url;
                elements.add(exportedFile);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Exporting: " + url.toString());
                }
            }
        }
    }

    private static void addExportedFile(ArrayList<ExportedFile> elements, File file) throws IOException {
        ExportedFile exportedFile = new ExportedFile();
        exportedFile.file = file;

        // No need to add if it does not exist.
        if (!file.exists()) {
            return;
        }
        // No need to add if it's in the list allready..
        for (ExportedFile element : elements) {
            if (file.equals(element.file)) {
                LOG.debug("duplicate file :" + file + " on classpath");
                return;
            }
        }

        // if it's a directory, then jar it up..
        if (file.isDirectory()) {
            if (file.list().length <= 0) {
                return;
            }
            File jar = exportedFile.jared = jar(file);
            LOG.debug("Jared: " + file + " as: " + jar);
            file = jar;
        } else {
            // if it's a file then it needs to be eaither a zip or jar file.
            String name = file.getName();
            if (!(name.endsWith(".jar") || name.endsWith(".zip"))) {
                LOG.debug("Not a jar.. ommititng from the classpath: " + file);
                return;
            }
        }

        exportedFile.element.id = ids.incrementAndGet();
        exportedFile.element.length = file.length();
        exportedFile.element.fingerprint = BasicClassLoaderFactory.fingerprint(new FileInputStream(file));
        elements.add(exportedFile);
    }

}