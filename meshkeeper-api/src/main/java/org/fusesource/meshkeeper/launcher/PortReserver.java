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
package org.fusesource.meshkeeper.launcher;

import java.io.IOException;
import java.lang.reflect.Array;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.util.Collection;
import java.util.HashSet;

import javax.net.ServerSocketFactory;

/**
 * PortReserver
 * <p>
 * A utility class to assist in finding/reserving ports on a machine.
 * </p>
 * 
 * @author cmacnaug
 * @version 1.0
 */
@SuppressWarnings("unchecked")
class PortReserver {

    public static final short TCP = 0;
    public static final short UDP = 1;
    private static final short NUM_PROTOCOLS = 2;

    private static HashSet<Integer>[] reserved;
    private static int[] next = new int[2];
    private static int[] min = new int[2];
    private static int[] max = new int[2];
    private static boolean initialized = false;

    static {
        reserved = (HashSet<Integer>[]) makeArray(HashSet.class, 2);
        for (int i = 0; i < reserved.length; i++) {
            reserved[i] = new HashSet<Integer>();
        }
    }

    public static void setPortRange(short protocol, int lower, int upper) {
        min[protocol] = lower;
        max[protocol] = upper;
        next[protocol] = lower;
    }

    public static synchronized Integer[] reservePorts(short protocol, int count) throws IOException {
        initialize();
        Integer[] ret = new Integer[count];
        int start = next[protocol];
        for (int i = 0; i < count; i++) {

            while (!checkPortFree(protocol, next[protocol])) {
                incrementNext(protocol);

                if (next[protocol] == start) {
                    throw new IOException("No free ports available");
                }
            }

            ret[i] = next[protocol];
            incrementNext(protocol);
        }

        return ret;

    }

    /**
     * @param tcp2
     * @param ports
     */
    public static synchronized void releasePorts(short protocol, Collection<Integer> ports) {
        for (Integer i : ports) {
            reserved[protocol].remove(i);
        }
    }

    private static void incrementNext(short protocol) {
        if (next[protocol] == max[protocol]) {
            next[protocol] = min[protocol];
        } else {
            next[protocol]++;
        }
    }

    private static boolean checkPortFree(short protocol, int port) {
        if (reserved[protocol].contains(new Integer(port))) {
            return false;
        }

        //Try opening the port to make sure that it is available:
        switch (protocol) {
        case TCP: {
            ServerSocket ss = null;
            try {
                ss = ServerSocketFactory.getDefault().createServerSocket(port);
                ss.close();
                return true;

            } catch (IOException e) {

            }
            return false;
        }
        case UDP: {
            DatagramSocket ds = null;
            try {
                ds = new DatagramSocket(port);
                ds.close();
                return true;
            } catch (IOException e) {

            }
            return false;
        }
        default: {
            throw new IllegalArgumentException("Invalid protocol: " + protocol);
        }
        }

    }

    private static <T> T[] makeArray(Class<T> clazz, int size) {
        return (T[]) Array.newInstance(clazz, size);
    }

    private static void initialize() {
        if (!initialized) {
            initialized = true;
            for (short i = 0; i < NUM_PROTOCOLS; i++) {
                //The following ranges are outside the ephemeral port range on most
                //OS's (see: http://www.ncftp.com/ncftpd/doc/misc/ephemeral_ports.html)
                //The intent here is to avoid reserving a port that might be later allocated
                //to a client socket. 
                setPortRange(i, 10000, 32768);
            }
        }
    }

}
