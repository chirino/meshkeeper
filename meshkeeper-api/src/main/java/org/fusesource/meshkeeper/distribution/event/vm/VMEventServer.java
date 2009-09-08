/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.meshkeeper.distribution.event.vm;

import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.fusesource.meshkeeper.MeshEvent;
import org.fusesource.meshkeeper.MeshEventListener;
import org.fusesource.meshkeeper.control.ControlService;
import org.fusesource.meshkeeper.distribution.DistributorFactory;

/**
 * VMEventServer
 * <p>
 * Description: An in memory event server. This class queues and dispatches
 * events, on a separate thread.
 * </p>
 * 
 * @author cmacnaug
 * @version 1.0
 */
public class VMEventServer implements ControlService{

    private final HashMap<String, EventQueue> EVENT_QUEUES = new HashMap<String, EventQueue>();
    private final ExecutorService EXECUTOR = DistributorFactory.getExecutorService();
    /*
     * (non-Javadoc)
     * 
     * @see
     * org.fusesource.meshkeeper.distribution.event.EventClient#closeEventListener
     * (org.fusesource.meshkeeper.distribution.event.EventListener,
     * java.lang.String)
     */
    public synchronized void closeEventListener(MeshEventListener listener, String topic) throws Exception {
        EventQueue queue = EVENT_QUEUES.get(topic);
        if (queue != null) {
            queue.removeListener(listener);
            if (queue.canBeRemoved()) {
                EVENT_QUEUES.remove(topic);
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.fusesource.meshkeeper.distribution.event.EventClient#openEventListener
     * (org.fusesource.meshkeeper.distribution.event.EventListener,
     * java.lang.String)
     */
    public synchronized void openEventListener(MeshEventListener listener, String topic) throws Exception {
        EventQueue queue = EVENT_QUEUES.get(topic);
        if (queue == null) {
            queue = new EventQueue(topic);
            EVENT_QUEUES.put(topic, queue);
        }

        queue.addListener(listener);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.fusesource.meshkeeper.distribution.event.EventClient#sendEvent(org
     * .fusesource.meshkeeper.distribution.event.Event, java.lang.String)
     */
    public synchronized void sendEvent(final MeshEvent event, String topic) throws Exception {
        EventQueue queue = EVENT_QUEUES.get(topic);

        if (queue != null) {
            queue.add(event);
        }
    }

    private class EventQueue implements Runnable {
        final HashSet<MeshEventListener> listeners = new HashSet<MeshEventListener>(1);
        final LinkedBlockingQueue<Runnable> eventQueue = new LinkedBlockingQueue<Runnable>();
        final AtomicBoolean scheduled = new AtomicBoolean(false);
        final String topic;

        EventQueue(String topic) {
            this.topic = topic;
        }

        public synchronized boolean canBeRemoved() {
            return listeners.isEmpty() && eventQueue.isEmpty();
        }

        public synchronized void addListener(MeshEventListener listener) {
            listeners.add(listener);
        }

        public synchronized void removeListener(MeshEventListener listener) {
            listeners.remove(listener);
        }

        public void add(final MeshEvent event) {
            synchronized (this) {
                if (listeners.isEmpty()) {
                    return;
                }

                final MeshEventListener[] targets = (MeshEventListener[]) listeners.toArray();
                eventQueue.add(new Runnable() {

                    public void run() {
                        for (MeshEventListener t : targets) {
                            t.onEvent(event);
                        }
                    }
                });
            }
            schedule();
        }

        public void run() {
            Runnable r = eventQueue.poll();
            if (r != null) {
                r.run();
            }
            scheduled.set(false);

            if (canBeRemoved()) {
                synchronized (VMEventServer.this) {
                    if (canBeRemoved()) {
                        EVENT_QUEUES.remove(topic);
                    }
                }
            } else {
                //Reschedule if the queue isn't empty:
                schedule();
            }
        }

        private void schedule() {
            if (!eventQueue.isEmpty() || listeners.isEmpty()) {
                if (!scheduled.compareAndSet(false, true)) {
                    EXECUTOR.execute(this);
                }
            }
        }
    }

    //////////////////////////////////////////////////////////////////////
    //Control Service Implementation
    //////////////////////////////////////////////////////////////////////
    
    /* (non-Javadoc)
     * @see org.fusesource.meshkeeper.control.ControlService#start()
     */
    public void start() throws Exception {
        // TODO Auto-generated method stub
        
    }
    
    /* (non-Javadoc)
     * @see org.fusesource.meshkeeper.control.ControlService#destroy()
     */
    public void destroy() throws Exception {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see org.fusesource.meshkeeper.control.ControlService#getName()
     */
    public String getName() {
        // TODO Auto-generated method stub
        return "VMEventServer";
    }

    /* (non-Javadoc)
     * @see org.fusesource.meshkeeper.control.ControlService#getServiceUri()
     */
    public String getServiceUri() {
        // TODO Auto-generated method stub
        return "vm:" + getName();
    }

    /* (non-Javadoc)
     * @see org.fusesource.meshkeeper.control.ControlService#setDataDirectory(java.lang.String)
     */
    public void setDataDirectory(String directory) {
        //Noop
    }


}
