/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.meshkeeper.util;

import java.util.LinkedList;
import java.util.regex.Pattern;

import org.fusesource.meshkeeper.MeshProcess;
import org.fusesource.meshkeeper.MeshProcessListener;

/**
 * A {@link MeshProcessListener} implementation which perform {@link Pattern} mathcing on a
 * {@link MeshProcess}'s output. 
 * <p>
 * This class is intended to be subclasses to allow searching for output in a process's 
 * output streams. This is useful, for example, in searching for Exceptions or Errors
 * printed in a process' output. 
 * </p>
 * When a match is found the {@link #onMatch(String)} method is called with the output that
 * triggered the match.
 * 
 * @author cmacnaug
 * @version 1.0
 */
public abstract class ProcessOutputMatcher extends NoOpProcessListener {

    private final String name;
    private LinkedList<Pattern> triggers = new LinkedList<Pattern>();
    private LinkedList<Pattern> filters = new LinkedList<Pattern>();

    public ProcessOutputMatcher(String name) {
        this.name = name;
    }

    @Override
    public final void onProcessOutput(int fd, byte[] output) {
        String line = new String(output);
        if (matches(line)) {
            onMatch(line);
        }

    }

    /**
     * Subclasses should override this handle matched output. 
     * @param output
     */
    protected abstract void onMatch(String output);

    /**
     * @return the name used to create this {@link ProcessOutputMatcher}
     */
    public String getName() {
        return name;
    }

    /**
     * Adds a {@link Pattern} to match against. 
     * @param regex an expression to match against in a {@link MeshProcess}' output.
     * 
     */
    public final void addTriggerPattern(String regex) {
        triggers.add(Pattern.compile(regex, Pattern.MULTILINE));
    }

    /**
     * Adds a {@link Pattern} against which to filter matches.
     * @param regex If output matches this pattern, then {@link #onMatch(String)} will not be triggered.
     */
    public final void addFilterPattern(String regex) {
        filters.add(Pattern.compile(regex, Pattern.MULTILINE));
    }

    private boolean matches(String line) {

        for (Pattern p : triggers) {
            if (p.matcher(line).find()) {
                //Check if filtered:
                for (Pattern f : filters) {
                    if (f.matcher(line).find()) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

}
