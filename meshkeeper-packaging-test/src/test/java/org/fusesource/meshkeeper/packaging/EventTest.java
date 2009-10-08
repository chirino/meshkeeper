/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.meshkeeper.packaging;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.fusesource.meshkeeper.MavenTestSupport;
import org.fusesource.meshkeeper.MeshEvent;
import org.fusesource.meshkeeper.MeshEventListener;
import org.fusesource.meshkeeper.MeshKeeper;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

/**
 * EventTest
 * <p>
 * Description:
 * </p>
 * 
 * @author cmacnaug
 * @version 1.0
 */
public class EventTest extends TestCase {

    MeshKeeper meshKeeper;

    protected void setUp() throws Exception {
        meshKeeper = MavenTestSupport.createMeshKeeper(getClass().getSimpleName());
    }

    protected void tearDown() throws Exception {
        if( meshKeeper!=null ) {
            meshKeeper.destroy();
            meshKeeper=null;
        }
    }

    public void testEvent() throws Exception {
        final CountDownLatch eventRcvd = new CountDownLatch(1);
        final AtomicReference<Throwable> failure = new AtomicReference<Throwable>();
        final MeshEvent event = new MeshEvent();
        event.setAttachment("TestString");
        event.setSource("testSource");
        event.setType(1);

        meshKeeper.eventing().openEventListener(new MeshEventListener() {

            public void onEvent(MeshEvent e) {
                switch (e.getType()) {
                case 1: {
                    try {
                        assertEquals(e.getAttachment(), event.getAttachment());
                        assertEquals(e.getSource(), event.getSource());
                    } catch (AssertionFailedError ae) {
                        failure.set(ae);
                    }
                    break;
                }
                default: {
                    failure.set(new Exception("Unexpected event type: " + e.getType()));
                }
                }

                eventRcvd.countDown();
            }

        }, "test-event");

        meshKeeper.eventing().sendEvent(event, "test-event");
        eventRcvd.await(5, TimeUnit.SECONDS);

        if (failure.get() != null) {
            throw new Exception("Listener failed", failure.get());
        }

    }
}
