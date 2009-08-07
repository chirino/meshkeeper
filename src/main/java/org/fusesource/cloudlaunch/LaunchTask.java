package org.fusesource.cloudlaunch;

/**
 * @author chirino
 */
public interface LaunchTask {
    public void execute(LocalProcess process) throws Exception;
}