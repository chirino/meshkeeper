package org.fusesource.cloudlaunch.rmi;

import org.fusesource.cloudlaunch.HostProperties;
import org.fusesource.cloudlaunch.LaunchDescription;

import java.rmi.Remote;

/**
 * @author chirino
 */
public interface IRemoteProcessLauncher extends Remote {

    /**
     * Specifies the registry prefix where IRemoteProcess launchers
     * will be published for discovery. 
     */
    public static final String REGISTRY_PATH = "/launchers";
    
    public void bind(String owner) throws Exception;

    public void unbind(String owner) throws Exception;

    public IRemoteProcess launch(LaunchDescription launchDescription, IRemoteProcessListener handler) throws Exception;

    public HostProperties getHostProperties() throws Exception;
}
