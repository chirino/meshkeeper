package org.fusesource.cloudlaunch;

import org.fusesource.cloudlaunch.rmi.Distributable;
import org.fusesource.cloudlaunch.rmi.Oneway;

/**
 * @author chirino
 */
public interface ProcessListener extends Distributable{

    @Oneway
    public void onProcessExit(int exitCode);

    @Oneway
    public void onProcessError(Throwable thrown);

    @Oneway
    public void onProcessInfo(String message);
    
    @Oneway
    public void onProcessOutput(int fd, byte [] output);

}
