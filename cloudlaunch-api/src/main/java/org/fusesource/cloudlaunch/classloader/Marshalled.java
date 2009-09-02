package org.fusesource.cloudlaunch.classloader;

import org.fusesource.cloudlaunch.classloader.ClassLoaderFactory;
import org.fusesource.cloudlaunch.util.internal.ClassLoadingAwareObjectInputStream;

import java.io.*;

/**
 * Marshalled allows you to marshall an object and send it to another JVM
 * with a class loader factory so that it can be subsequently unmarshalled
 * via remote class loading.
 *  
 * @author chirino
 */
public class Marshalled<T> implements Serializable {

    private final ClassLoaderFactory classLoaderFactory;
    private final byte[] serializedObject;

    public Marshalled(ClassLoaderFactory classLoaderFactory, T value) throws IOException {
        this.classLoaderFactory = classLoaderFactory;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(baos);
        os.writeObject(value);
        os.close();
        this.serializedObject = baos.toByteArray();
    }

    public ClassLoaderFactory getClassLoaderFactory() {
        return classLoaderFactory;
    }

    public T get(final ClassLoader cl) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bais = new ByteArrayInputStream(serializedObject);
        ClassLoadingAwareObjectInputStream is = new ClassLoadingAwareObjectInputStream(bais) {
            @Override
            protected ClassLoader getClassLoader() {
                return cl;
            }
        };
        return (T) is.readObject();
    }

}