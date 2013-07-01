/*
 * Copyright (c) 2005, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.openjdk.jmh.runner.parameters;


import java.util.List;

/**
 * @author sergey.kuksenko@oracle.com
 */
public interface MicroBenchmarkParameters {

    /**
     * Will the harness continue to execute work in all threads until all thread have returned a Result from their
     * measured part of the run?
     *
     * @return true if they will
     */
    public boolean shouldSynchIterations();

    public IterationParams getWarmup();

    public IterationParams getIteration();

    public List<Integer> getThreadCounts();

    public boolean shouldScale();

    /**
     * Getter for max amount of threads to use.
     *
     * @return amount of threads
     */
    public int getMaxThreads();

    public List<ThreadIterationParams> getThreadIterationSequence();

    /**
     * returns MicroBenchmarkParameters where warmup parameters pushed to iteration
     * parameters. Used by bulk warmup mode.
     * @return
     */
    public MicroBenchmarkParameters warmupToIteration();
}
