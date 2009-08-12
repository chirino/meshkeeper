/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.cloudlaunch.util;

import org.fusesource.cloudlaunch.ProcessListener;
import org.fusesource.cloudlaunch.Process;

/**
 * DefaultProcessListener
 * <p>
 * A default process listener. Applications can subclass this to customize
 * behavior
 * </p>
 * 
 * @author cmacnaug
 * @version 1.0
 */
public class DefaultProcessListener implements ProcessListener {

    protected String name = "";

    public DefaultProcessListener() {

    }

    public DefaultProcessListener(String name) {
        this.name = name;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.fusesource.cloudlaunch.ProcessListener#onProcessError(java.lang.Throwable
     * )
     */
    public void onProcessError(Throwable thrown) {
        System.err.println(format("ERROR: " + thrown.getStackTrace()));
        thrown.printStackTrace();

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.fusesource.cloudlaunch.ProcessListener#onProcessExit(int)
     */
    public void onProcessExit(int exitCode) {
        System.out.println(format("exited with " + exitCode));

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.fusesource.cloudlaunch.ProcessListener#onProcessInfo(java.lang.String
     * )
     */
    public void onProcessInfo(String message) {
        System.out.println(format(message));
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.fusesource.cloudlaunch.ProcessListener#onProcessOutput(int,
     * byte[])
     */
    public void onProcessOutput(int fd, byte[] output) {
        if (fd == Process.FD_STD_ERR) {
            System.err.println(format(new String(output)));
        } else {
            System.out.println(format(new String(output)));
        }
    }

    private String format(String s) {
        if (s.endsWith("\r\n")) {
            s = s.substring(0, s.length() - 2);
        } else if (s.endsWith("\n")) {
            s = s.substring(0, s.length() - 1);
        }

        if (name == null) {
            return s;
        } else {
            return name + ": " + s;
        }
    }

    public String toString() {
        return name;
    }

}
