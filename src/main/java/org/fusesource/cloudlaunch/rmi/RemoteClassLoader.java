package org.fusesource.cloudlaunch.rmi;

import org.apache.activemq.command.ActiveMQDestination;
import org.fusesource.rmiviajms.JMSRemoteObject;

import javax.jms.Destination;
import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

/**
 * @author chirino
 */
public class RemoteClassLoader extends ClassLoader {
    private IClassLoaderServer server;

    public static ClassLoader createRemoteClassLoader(String uri, File cacheDir, int depth, ClassLoader parent) throws IOException {
        Destination queue = ActiveMQDestination.createDestination(uri, ActiveMQDestination.QUEUE_TYPE);
        System.out.println("CL server at "+queue);
        IClassLoaderServer cle = JMSRemoteObject.toProxy(queue, IClassLoaderServer.class);
        return createRemoteClassLoader(cle, cacheDir, depth, parent);
    }
    
    /**
     * Builds a classloader tree to match remote classloader treee
     * exported by the IClassLoaderExporter.
     *
     * @param cle
     * @param cacheDir
     * @param depth
     * @param parent
     * @return
     * @throws RemoteException
     */
    public static ClassLoader createRemoteClassLoader(IClassLoaderServer cle, File cacheDir, int depth, ClassLoader parent) throws IOException {

        if( depth == 0 ) {
            return parent;
        }
        if( cle == null ) {
            return parent;
        }

        parent = createRemoteClassLoader(cle.getParent(), cacheDir, depth-1, parent);
        List<IClassLoaderServer.PathElement> elements = cle.getPathElements();
        if( elements == null ) {
            // That classloader was not URL classloader based, so we could not import it
            // by downloading it's jars.. we will have to use dynamically.
            return new RemoteClassLoader(parent, cle);
        }

        // We can build stadard URLClassLoader by downloading all the
        // jars or using the same URL elements as the original classloader.
        ArrayList<URL> urls = new ArrayList<URL>();
        for (IClassLoaderServer.PathElement element : elements) {

            if( element.jarFileSize==0 ) {
                urls.add(element.url);
            } else {

                String name = new File(element.url.getPath()).getName();
                File jarDirectory = new File(cacheDir, Long.toHexString(element.jarFileChecksum));
                jarDirectory.mkdirs();
                File file = new File(jarDirectory, name);

                if ( !file.exists() ) {
//                    System.out.println("Downloading "+file);
                    // We need to download it...
                    File tmp = null;
                    FileOutputStream out=null;
                    try {
                        tmp = File.createTempFile(name, ".part", jarDirectory);
                        // Yeah this is not ideal.. we should really stream it..
                        byte []data = cle.download(element.url);
                        out = new FileOutputStream(tmp);
                        out.write(data);

                    } finally {
                        try{ out.close(); } catch (Throwable e) {}
                    }
                    if( !tmp.renameTo(file) ) {
                        tmp.delete();
                    }
                }

                // It may be in the cache dir allready...
                if ( file.exists() ) {

                    if( element.jarFileChecksum != checksum(new FileInputStream(file))
                            || element.jarFileSize != file.length() ) {
                        throw new IOException("Checksum missmatch: "+name);
                    }

                    urls.add(file.toURI().toURL());
                } else {
                    throw new IOException("Could not download: "+name);
                }
            }
        }

        URL t[] = new URL[urls.size()];
        urls.toArray(t);
        return new URLClassLoader(t, parent);
    }


    public RemoteClassLoader(ClassLoader parent, IClassLoaderServer server) {
        super(parent);
        this.server = server;
    }

    public Class findClass(String name) throws ClassNotFoundException {
        String path = name.replace('.', '/').concat(".class");
        try {
            byte data[] = server.findResource(path);
            if( data == null ) {
                throw new ClassNotFoundException(name);
            }
            return defineClass(name, data, 0, data.length);
        } catch (RemoteException e) {
            throw new ClassNotFoundException(name);
        }
    }


    public static long checksum(InputStream is) throws IOException {
        Checksum sum = new CRC32();
        try {
            byte buffer[] = new byte[1024*4];
            int c;
            while( (c=is.read(buffer)) > 0 ) {
                sum.update(buffer, 0, c);
            }
            return sum.getValue();
        } finally {
            try {
                is.close();
            } catch (IOException ignore) {
            }
        }
    }
}