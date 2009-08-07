package org.fusesource.cloudlaunch.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.net.URL;
import java.io.IOException;
import java.io.Serializable;

/**
 * @author chirino
*/
public interface IClassLoaderServer extends Remote {
    public static class PathElement implements Serializable {
        URL url;
        long jarFileChecksum;
        long jarFileSize;
    }

    IClassLoaderServer getParent() throws RemoteException;
    List<PathElement> getPathElements() throws RemoteException;
    byte[] download(URL url) throws RemoteException, IOException;
    byte[] findResource(String name) throws RemoteException;

}