/**
 *  Copyright (C) 2009 Progress Software, Inc. All rights reserved.
 *  http://fusesource.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.fusesource.meshkeeper.util;

import org.fusesource.meshkeeper.MeshProcessListener;

/** 
 * NoOpProcessListener
 * <p>
 * A No-Op process listener which oes nothing with process output/info. This class 
 * can be useful for subclassing.
 * </p>
 * @author cmacnaug
 * @version 1.0
 */
public class NoOpProcessListener implements MeshProcessListener{

    /* (non-Javadoc)
     * @see org.fusesource.meshkeeper.MeshProcessListener#onProcessError(java.lang.Throwable)
     */
    public void onProcessError(Throwable thrown) {
        // NOOP!
    }

    /* (non-Javadoc)
     * @see org.fusesource.meshkeeper.MeshProcessListener#onProcessExit(int)
     */
    public void onProcessExit(int exitCode) {
     // NOOP!
    }

    /* (non-Javadoc)
     * @see org.fusesource.meshkeeper.MeshProcessListener#onProcessInfo(java.lang.String)
     */
    public void onProcessInfo(String message) {
     // NOOP!
    }

    /* (non-Javadoc)
     * @see org.fusesource.meshkeeper.MeshProcessListener#onProcessOutput(int, byte[])
     */
    public void onProcessOutput(int fd, byte[] output) {
        // NOOP!
    }

}
