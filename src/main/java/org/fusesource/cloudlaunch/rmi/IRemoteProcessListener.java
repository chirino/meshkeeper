package org.fusesource.cloudlaunch.rmi;

import org.fusesource.cloudlaunch.ProcessListener;
import org.fusesource.rmiviajms.Oneway;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * @author chirino
*/
public interface IRemoteProcessListener extends Remote {
    @Oneway
    public void onExit(int exitCode) throws RemoteException;

    @Oneway
    public void onError(Throwable thrown) throws RemoteException;

    @Oneway
    public void onInfoLogging(String message) throws RemoteException;
    
    @Oneway
    public void onStreamOutput(int fd, byte [] b) throws RemoteException;

    public void ping() throws RemoteException;
}