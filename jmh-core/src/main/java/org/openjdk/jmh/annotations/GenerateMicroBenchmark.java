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
package org.openjdk.jmh.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>GenerateMicroBenchmark annotates the benchmark method.</p>
 *
 * <p>JMH will produce the generated benchmark code for this method during compilation,
 * register this method as the benchmark in the benchmark list, read out the default
 * values from the annotations, and generally prepare the environment for the benchmark
 * to run.</p>
 *
 * <p>Benchmarks may use annotations to control different things in their operations.
 * See {@link org.openjdk.jmh.annotations} package for available annotations, or
 * look through
 * <a href="http://hg.openjdk.java.net/code-tools/jmh/file/tip/jmh-samples/src/main/java/org/openjdk/jmh/samples/">
 * JMH samples</a> for their canonical uses. As the rule of thumb, most annotations
 * may be placed either at the {@link GenerateMicroBenchmark} method, or at enclosing
 * class.</p>
 *
 * <p>{@link org.openjdk.jmh.annotations.GenerateMicroBenchmark} demarcates the benchmark payload,
 * and JMH treats it specifically as the wrapper which contains the benchmark code. In order to
 * run the benchmark reliably, JMH enforces a few stringent properties for these wrapper methods,
 * including, but not limited to:</p>
 * <ul>
 *     <li>Method should be public</li>
 *     <li>Arguments may only include either {@link org.openjdk.jmh.annotations.State} classes, which
 *     JMH will inject while calling the method (see {@link org.openjdk.jmh.annotations.State} for
 *     more details), or JMH infrastructure classes, like {@link org.openjdk.jmh.logic.Control},
 *     or {@link org.openjdk.jmh.logic.BlackHole}</li>
 *     <li>Method should not be synchronized</li>
 *     <li>Method should not be static</li>
 * </ul>
 * <p>If you want to benchmark methods which break these properties, you have to write them
 * out specifically as the benchmark payload and call them from {@link org.openjdk.jmh.annotations.GenerateMicroBenchmark}
 * method.</p>
 *
 * <p>Benchmark method may declare Exceptions and Throwables to throw. Any exception actually
 * raised and thrown will be treated as benchmark failure.</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface GenerateMicroBenchmark {

}
