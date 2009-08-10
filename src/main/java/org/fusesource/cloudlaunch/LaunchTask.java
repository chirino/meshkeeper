package org.fusesource.cloudlaunch;

import org.fusesource.cloudlaunch.local.LocalProcess;

/**
 * @author chirino
 */
public interface LaunchTask {
    public void execute(LocalProcess process) throws Exception;
}