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
package org.openjdk.jmh.logic;

import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

/*
    See the rationale for BlackHoleL1..BlackHoleL4 classes below.
 */

class BlackholeL1 {
    public int p01, p02, p03, p04, p05, p06, p07, p08;
    public int p11, p12, p13, p14, p15, p16, p17, p18;
    public int p21, p22, p23, p24, p25, p26, p27, p28;
    public int p31, p32, p33, p34, p35, p36, p37, p38;
}

class BlackHoleL2 extends BlackholeL1 {
    public byte b1;
    public boolean bool1;
    public char c1;
    public short s1;
    public int i1;
    public long l1;
    public float f1;
    public double d1;
    public Object obj1;
    public Object[] objs1;
    public int tlr = (int)System.nanoTime(); // randomize
    public int tlrMask = 1;
}

class BlackHoleL3 extends BlackHoleL2 {
    public int e01, e02, e03, e04, e05, e06, e07, e08;
    public int e11, e12, e13, e14, e15, e16, e17, e18;
    public int e21, e22, e23, e24, e25, e26, e27, e28;
    public int e31, e32, e33, e34, e35, e36, e37, e38;
}

class BlackHoleL4 extends BlackHoleL3 {
    public int marker;
}

/**
 * Black Hole.
 * <p/>
 * Black hole "consumes" the values, conceiving no information to JIT whether the
 * value is actually used afterwards. This can save from the dead-code elimination
 * of the computations resulting in the given values.
 *
 * @author aleksey.shipilev@oracle.com
 */
@State(Scope.Thread) // Blackholes are always acting like a thread-local state
public class BlackHole extends BlackHoleL4 {

    /**
     * IMPLEMENTATION NOTES:
     * <p/>
     * The major things to dodge with Blackholes are:
     *   a) dead-code elimination: the arguments should be used on every call,
     *      so that compilers are unable to fold them into constants or
     *      otherwise optimize them away along with the computations resulted
     *      in them.
     *   b) false sharing: reading/writing the state may disturb the cache
     *      lines. We need to isolate the critical fields to achieve tolerable
     *      performance.
     *   c) write wall: we need to ease off on writes as much as possible,
     *      since it disturbs the caches, pollutes the write buffers, etc.
     *      This may very well result in hitting the memory wall prematurely.
     *      Reading memory is fine as long as it is cacheable.
     * <p/>
     * To achieve these goals, we are piggybacking on several things in the
     * compilers:
     * <p/>
     * 1. Superclass fields are not reordered with the subclass' fields.
     * No practical VM that we are aware of is doing this. It is unpractical,
     * because if the superclass fields are at the different offsets in two
     * subclasses, the VMs would then need to do the polymorphic access for
     * the superclass fields.
     * <p/>
     * This allows us to "squash" the protected fields in the inheritance
     * hierarchy so that the padding in super- and sub-class are laid out
     * right before and right after the protected fields.
     * <p/>
     * We also pad with "int"-s so that dense layout in superclass does not
     * have the gap where runtime can fit the subclass field.
     * <p/>
     * We fight the dead-code elimination by storing the result on the heap.
     * Since heap writes are expensive (notably for objects which entail
     * store barrier), we are using the inlined PRNG to store only every once
     * in a while. The compilers can eliminate the slow path until it was hit,
     * and move more computation under the branch in consume(), so we need
     * to warm up the slow-path branch. To do that, we gradually increase the
     * range over which we find the collision with zero for the PRNG.
     * <p/>
     * Note that using PRNG still induces the heap writes, but those writes
     * are consistent for every consumed data type. We use also the linear
     * congruential generator with the glibc/gcc constants, which yields
     * enough randomicity in minor bits to tolerate increasing bit masks
     * (which is similar to increasing modulo), and operates on 32-bit values
     * (which decreases the register pressure on 32-bit VMs).
     */
    private static Unsafe U;

    static {
        try {
            Field unsafe = Unsafe.class.getDeclaredField("theUnsafe");
            unsafe.setAccessible(true);
            U = (Unsafe) unsafe.get(null);
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }

        consistencyCheck();
    }

    static void consistencyCheck() {
        // checking the fields are not reordered
        check("b1");
        check("bool1");
        check("c1");
        check("s1");
        check("i1");
        check("l1");
        check("f1");
        check("d1");
        check("obj1");
        check("objs1");
    }

    static void check(String fieldName) {
        final long requiredGap = 128;
        long markerOff = getOffset("marker");
        long off = getOffset(fieldName);
        if (markerOff - off < requiredGap) {
            throw new IllegalStateException("Consistency check failed for " + fieldName + ", off = " + off + ", markerOff = " + markerOff);
        }
    }

    static long getOffset(String fieldName) {
        try {
            Field f = BlackHoleL4.class.getField(fieldName);
            return U.objectFieldOffset(f);
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Consume object. This call provides a side effect preventing JIT to eliminate dependent computations.
     *
     * @param obj object to consume.
     */
    public final void consume(Object obj) {
        // let's play the optimizing compiler, dude!
        int tlr = this.tlr;
        int tlrMask = this.tlrMask;

        this.tlr = (tlr * 1103515245 + 12345) & tlrMask;
        if (tlr == 0) {
            // SHOULD ALMOST NEVER HAPPEN IN MEASUREMENT
            if (tlrMask != 0x7FFFFFFF) {
                this.tlrMask = (tlrMask << 1) + 1;
            }
            this.obj1 = obj;
        }
    }

    /**
     * Consume object. This call provides a side effect preventing JIT to eliminate dependent computations.
     *
     * @param objs objects to consume.
     */
    public final void consume(Object[] objs) {
        int tlr = this.tlr;
        int tlrMask = this.tlrMask;

        this.tlr = (tlr * 1103515245 + 12345) & tlrMask;
        if (tlr == 0) {
            // SHOULD ALMOST NEVER HAPPEN IN MEASUREMENT
            if (tlrMask != 0x7FFFFFFF) {
                this.tlrMask = (tlrMask << 1) + 1;
            }
            this.objs1 = objs;
        }
    }

    /**
     * Consume object. This call provides a side effect preventing JIT to eliminate dependent computations.
     *
     * @param b object to consume.
     */
    public final void consume(byte b) {
        int tlr = this.tlr;
        int tlrMask = this.tlrMask;

        this.tlr = (tlr * 1103515245 + 12345) & tlrMask;
        if (tlr == 0) {
            // SHOULD ALMOST NEVER HAPPEN IN MEASUREMENT
            if (tlrMask != 0x7FFFFFFF) {
                this.tlrMask = (tlrMask << 1) + 1;
            }
            this.b1 = b;
        }
    }

    /**
     * Consume object. This call provides a side effect preventing JIT to eliminate dependent computations.
     *
     * @param bool object to consume.
     */
    public final void consume(boolean bool) {
        int tlr = this.tlr;
        int tlrMask = this.tlrMask;

        this.tlr = (tlr * 1103515245 + 12345) & tlrMask;
        if (tlr == 0) {
            // SHOULD ALMOST NEVER HAPPEN IN MEASUREMENT
            if (tlrMask != 0x7FFFFFFF) {
                this.tlrMask = (tlrMask << 1) + 1;
            }
            this.bool1 = bool;
        }
    }

    /**
     * Consume object. This call provides a side effect preventing JIT to eliminate dependent computations.
     *
     * @param c object to consume.
     */
    public final void consume(char c) {
        int tlr = this.tlr;
        int tlrMask = this.tlrMask;

        this.tlr = (tlr * 1103515245 + 12345) & tlrMask;
        if (tlr == 0) {
            // SHOULD ALMOST NEVER HAPPEN IN MEASUREMENT
            if (tlrMask != 0x7FFFFFFF) {
                this.tlrMask = (tlrMask << 1) + 1;
            }
            this.c1 = c;
        }
    }

    /**
     * Consume object. This call provides a side effect preventing JIT to eliminate dependent computations.
     *
     * @param s object to consume.
     */
    public final void consume(short s) {
        int tlr = this.tlr;
        int tlrMask = this.tlrMask;

        this.tlr = (tlr * 1103515245 + 12345) & tlrMask;
        if (tlr == 0) {
            // SHOULD ALMOST NEVER HAPPEN IN MEASUREMENT
            if (tlrMask != 0x7FFFFFFF) {
                this.tlrMask = (tlrMask << 1) + 1;
            }
            this.s1 = s;
        }
    }

    /**
     * Consume object. This call provides a side effect preventing JIT to eliminate dependent computations.
     *
     * @param i object to consume.
     */
    public final void consume(int i) {
        int tlr = this.tlr;
        int tlrMask = this.tlrMask;

        this.tlr = (tlr * 1103515245 + 12345) & tlrMask;
        if (tlr == 0) {
            // SHOULD ALMOST NEVER HAPPEN IN MEASUREMENT
            if (tlrMask != 0x7FFFFFFF) {
                this.tlrMask = (tlrMask << 1) + 1;
            }
            this.i1 = i;
        }
    }

    /**
     * Consume object. This call provides a side effect preventing JIT to eliminate dependent computations.
     *
     * @param l object to consume.
     */
    public final void consume(long l) {
        int tlr = this.tlr;
        int tlrMask = this.tlrMask;

        this.tlr = (tlr * 1103515245 + 12345) & tlrMask;
        if (tlr == 0) {
            // SHOULD ALMOST NEVER HAPPEN IN MEASUREMENT
            if (tlrMask != 0x7FFFFFFF) {
                this.tlrMask = (tlrMask << 1) + 1;
            }
            this.l1 = l;
        }
    }

    /**
     * Consume object. This call provides a side effect preventing JIT to eliminate dependent computations.
     *
     * @param f object to consume.
     */
    public final void consume(float f) {
        int tlr = this.tlr;
        int tlrMask = this.tlrMask;

        this.tlr = (tlr * 1103515245 + 12345) & tlrMask;
        if (tlr == 0) {
            // SHOULD ALMOST NEVER HAPPEN IN MEASUREMENT
            if (tlrMask != 0x7FFFFFFF) {
                this.tlrMask = (tlrMask << 1) + 1;
            }
            this.f1 = f;
        }
    }

    /**
     * Consume object. This call provides a side effect preventing JIT to eliminate dependent computations.
     *
     * @param d object to consume.
     */
    public final void consume(double d) {
        int tlr = this.tlr;
        int tlrMask = this.tlrMask;

        this.tlr = (tlr * 1103515245 + 12345) & tlrMask;
        if (tlr == 0) {
            // SHOULD ALMOST NEVER HAPPEN IN MEASUREMENT
            if (tlrMask != 0x7FFFFFFF) {
                this.tlrMask = (tlrMask << 1) + 1;
            }
            this.d1 = d;
        }
    }

    public static volatile int consumedCPU = 42;

    /**
     * Consume some amount of time tokens.
     * This method does the CPU work almost linear to the number of tokens.
     * One token is really small, around 3 clocks on 2.0 Ghz i5,
     * see JMH samples for the complete demo.
     *
     * @param tokens CPU tokens to consume
     */
    public static void consumeCPU(long tokens) {
        // randomize start so that JIT could not memoize;
        int t = consumedCPU;

        for (long i = 0; i < tokens; i++) {
            t += (t * 1103515245 + 12345);
        }

        // need to guarantee side-effect on the result,
        // but can't afford contention; make sure we update the shared state
        // only in the unlikely case, so not to do the furious writes,
        // but still dodge DCE.
        if (t == 42) {
            consumedCPU += t;
        }
    }

}
