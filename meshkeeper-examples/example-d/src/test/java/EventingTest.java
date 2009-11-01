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
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.fusesource.meshkeeper.HostProperties;
import org.fusesource.meshkeeper.MeshContainer;
import org.fusesource.meshkeeper.MeshEvent;
import org.fusesource.meshkeeper.MeshEventListener;
import org.fusesource.meshkeeper.MeshKeeper;
import org.fusesource.meshkeeper.MeshKeeperFactory;

/**
 * EventingTest
 * <p>
 * Description:
 * </p>
 * 
 * @author cmacnaug
 * @version 1.0
 */
public class EventingTest extends TestCase {

    public static final String EVENT_TOPIC = "examples.eventing";
    private transient Log LOG = LogFactory.getLog(EventingTest.class);
    public static final int ERROR = -11;
    public static final int SAY_HELLO = 0;
    public static final int HELLO = 1;

    @SuppressWarnings("serial")
    public static class Greeter implements Serializable, MeshEventListener {
        private transient Log LOG = LogFactory.getLog(Greeter.class);
        private transient MeshKeeper mk;
        public String name;

        public void startListening() throws Exception {
            mk.eventing().openEventListener(this, EVENT_TOPIC + ".*");
        }

        public void stopListening() throws Exception {
            mk.eventing().closeEventListener(this, EVENT_TOPIC + ".*");
        }

        public void onEvent(MeshEvent event) {
            if (event.getType() == HELLO) {
                LOG.info("Got : " + event.getAttachment() + " from " + event.getSource());
            } else if (event.getType() == SAY_HELLO) {
                try {
                    mk.eventing().sendEvent(new MeshEvent(HELLO, name, "Hello!"), EVENT_TOPIC + "." + name);
                } catch (Exception e) {
                    LOG.error("Error saying hello", e);
                }
            }
        }

        private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
            in.defaultReadObject();
            LOG = LogFactory.getLog(Greeter.class + "." + name);
            //Note that we pick up MeshKeeper from the MeshContainer:
            mk = MeshKeeperFactory.getContainerMeshKeeper();

        }
    }

    public void testEventing() throws Exception {

        MeshKeeper mk = MeshKeeperFactory.createMeshKeeper();
        mk.launcher().waitForAvailableAgents(60000);
        HostProperties[] hosts = mk.launcher().getAvailableAgents();

        MeshContainer c1 = mk.launcher().launchMeshContainer(hosts[0].getAgentId());
        MeshContainer c2 = mk.launcher().launchMeshContainer((hosts.length > 1 ? hosts[1] : hosts[0]).getAgentId());

        Greeter g1 = new Greeter();
        g1.name = "machine1";

        Greeter g2 = new Greeter();
        g2.name = "machine2";

        //Let's host them in their containers,
        //Note that we replace the actual references
        //with their proxy counterparts.
        g1 = c1.host(g1.name, g1);
        g2 = c2.host(g2.name, g2);

        //Set up an event listener to wait for the hellos
        final CountDownLatch hellos = new CountDownLatch(2);
        final ArrayList<Exception> errors = new ArrayList<Exception>();
        MeshEventListener listener = new MeshEventListener() {

            public void onEvent(MeshEvent event) {
                switch (event.getType()) {
                case HELLO: {
                    LOG.info("Got : " + event.getAttachment() + " from " + event.getSource());
                    hellos.countDown();
                    break;
                }
                case SAY_HELLO: {
                    break;
                }

                case ERROR:
                default: {
                    Exception e = event.getAttachment();
                    LOG.error("Error from: " + event.getSource(), e);
                    errors.add(e);

                    while (hellos.getCount() > 0) {
                        hellos.countDown();
                    }
                }
                }
            }
        };
        mk.eventing().openEventListener(listener, EVENT_TOPIC + ".*");

        //Start the
        g1.startListening();
        g2.startListening();
        mk.eventing().sendEvent(new MeshEvent(SAY_HELLO, "controller", null), EVENT_TOPIC + ".control");

        hellos.await(10, TimeUnit.SECONDS);
        Thread.sleep(2000);
        assertTrue("There were errors", errors.isEmpty());
        c1.close();
        c2.close();
        
        mk.eventing().closeEventListener(listener, EVENT_TOPIC + ".*");
    }
}
