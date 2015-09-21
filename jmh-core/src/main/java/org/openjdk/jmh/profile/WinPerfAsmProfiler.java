/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmh.profile;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSpec;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.results.BenchmarkResult;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.util.*;

import java.io.*;
import java.util.*;

/**
 * Windows performance profiler based on "xperf" utility.
 * <p>
 * You must install {@code Windows Performance Toolkit}. Once installed, locate directory with {@code xperf.exe}
 * file and either add it to {@code PATH} environment variable, or set it to {@code jmh.perfasm.xperf.dir} system
 * property.
 * <p>
 * This profiler counts only {@code SampledProfile} events. To achieve this, we set {@code xperf} providers to
 * {@code loader+proc_thread+profile}. You may optionally save {@code xperf} binary or parsed outputs using
 * {@code jmh.perfasm.savePerfBin} or {@code jmh.perfasm.savePerf} system properties respectively. If you do so and
 * want to log more events, you can use {@code jmh.perfasm.xperf.providers} system property to override providers.
 * However, you must specify {@code loader}, {@code proc_thread} and {@code profile} providers anyway. Otherwise
 * sample events will not be generated and profiler will show nothing.
 * <p>
 * By default JDK distributive do not have debug symbols. If you want to analyze JVM internals, you must build OpenJDK
 * on your own. Once built, go to {@code bin/server} directory and unpack {@code jvm.diz}. Now you have {@code jvm.pdb}
 * file with JVM debug symbols. Finally, you must set debug symbols directory to {@code jmh.perfasm.symbol.dir} system
 * property.
 * <p>
 * This profiler behaves differently comparing to it's Linux counterpart {@link LinuxPerfAsmProfiler}. Linux profiler
 * employs {@code perf} utility which can be used to profile a single process. Therefore, Linux profiler wraps forked
 * JVM command line. In contrast, {@code xperf} cannot profile only a single process. It have {@code -PidNewProcess}
 * argument, but it's sole purpose is to start profiling before the process is started, so that one can be sure that
 * none events generated by this process are missed. It does not filter events from other processes anyhow. For this
 * reason, this profiler doesn't alter forked JVM startup command. Instead, it starts {@code xperf} recording in
 * {@link #beforeTrial(BenchmarkParams)} method, and stops in {@link ExternalProfiler#afterTrial(org.openjdk.jmh.results.BenchmarkResult, long, java.io.File, java.io.File)}. This
 * leaves possibility to run this profiler in conjunction with some other profiler requiring startup command
 * alteration.
 * <p>
 * For this reason the profiler must know PID of forked JVM process.
 */
public class WinPerfAsmProfiler extends AbstractPerfAsmProfiler {

    private final String xperfProviders;
    private final String symbolDir;
    private final String path;

    /** PID. */
    private volatile String pid;
    private OptionSpec<String> optXperfDir;
    private OptionSpec<String> optXperfProviders;
    private OptionSpec<String> optSymbolDir;

    /**
     * Constructor.
     */
    public WinPerfAsmProfiler(String initLine) throws ProfilerException {
        super(initLine, "SampledProfile");

        try {
            String xperfDir = set.valueOf(optXperfDir);
            xperfProviders = set.valueOf(optXperfProviders);
            symbolDir = set.valueOf(optSymbolDir);
            path = xperfDir != null && !xperfDir.isEmpty() ? xperfDir + File.separatorChar + "xperf" : "xperf";
        } catch (OptionException e) {
            throw new ProfilerException(e.getMessage());
        }

        Collection<String> errs = Utils.tryWith(path);
        if (!errs.isEmpty()) {
            throw new ProfilerException(errs.toString());
        }
    }

    @Override
    protected void addMyOptions(OptionParser parser) {
        optXperfDir = parser.accepts("xperf.dir",
                "Path to \"xperf\" installation directory. Empty by default, so that xperf is expected to be in PATH.")
                .withRequiredArg().ofType(String.class).describedAs("path");
        optXperfProviders = parser.accepts("xperf.providers",
                "xperf providers to use.")
                .withRequiredArg().ofType(String.class).describedAs("string").defaultsTo("loader+proc_thread+profile");
        optSymbolDir = parser.accepts("symbol.dir",
                "Path to a directory with jvm.dll symbols (optional).")
                .withRequiredArg().ofType(String.class).describedAs("string");
    }

    @Override
    public Collection<String> addJVMInvokeOptions(BenchmarkParams params) {
        // "xperf" cannot be started to track particular process as "perf" in Linux does.
        // Therefore we do not alter JVM invoke options anyhow. Instead, profiler will be started
        // during "before-trial" stage.
        return Collections.emptyList();
    }

    @Override
    public void beforeTrial(BenchmarkParams params) {
        // Start profiler before forked JVM is started.insta
        Collection<String> errs = Utils.tryWith(path, "-on", xperfProviders);

        if (!errs.isEmpty())
            throw new IllegalStateException("Failed to start xperf: " + errs);
    }

    @Override
    public Collection<? extends Result> afterTrial(BenchmarkResult br, long pid, File stdOut, File stdErr) {
        if (pid == 0) {
            throw new IllegalStateException("perfasm needs the forked VM PID, but it is not initialized.");
        }
        this.pid = String.valueOf(pid);
        return super.afterTrial(br, pid, stdOut, stdErr);
    }

    @Override
    public String getDescription() {
        return "Windows xperf + PrintAssembly Profiler";
    }

    @Override
    protected void parseEvents() {
        // 1. Stop profiling by calling xperf dumper.
        Collection<String> errs = Utils.tryWith(path, "-d", perfBinData);

        if (!errs.isEmpty())
            throw new IllegalStateException("Failed to stop xperf: " + errs);

        // 2. Convert binary data to text form.
        try {
            ProcessBuilder pb = new ProcessBuilder(path, "-i", perfBinData, "-symbols", "-a", "dumper");
            if (symbolDir != null) {
                pb.environment().put("_NT_SYMBOL_PATH", symbolDir);
            }

            Process p = pb.start();

            FileOutputStream fos = new FileOutputStream(perfParsedData);

            InputStreamDrainer errDrainer = new InputStreamDrainer(p.getErrorStream(), fos);
            InputStreamDrainer outDrainer = new InputStreamDrainer(p.getInputStream(), fos);

            errDrainer.start();
            outDrainer.start();

            p.waitFor();

            errDrainer.join();
            outDrainer.join();
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        } catch (InterruptedException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Override
    protected PerfEvents readEvents(double skipSec) {
        FileReader fr = null;
        try {
            Deduplicator<String> dedup = new Deduplicator<String>();

            fr = new FileReader(perfParsedData);
            BufferedReader reader = new BufferedReader(fr);

            Map<Long, String> methods = new HashMap<Long, String>();
            Map<Long, String> libs = new HashMap<Long, String>();
            Map<String, Multiset<Long>> events = new LinkedHashMap<String, Multiset<Long>>();
            for (String evName : this.events) {
                events.put(evName, new TreeMultiset<Long>());
            }

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                String[] elems = line.split(",");

                String evName = elems[0].trim();

                // We work with only one event type - "SampledProfile".
                if (!this.events.get(0).equals(evName))
                    continue;

                // Check PID.
                String pidStr = elems[2].trim();

                int pidOpenIdx = pidStr.indexOf("(");
                int pidCloseIdx = pidStr.indexOf(")");

                if (pidOpenIdx == -1 || pidCloseIdx == -1 || pidCloseIdx < pidOpenIdx)
                    continue; // Malformed PID, probably this is the header.

                pidStr = pidStr.substring(pidOpenIdx + 1, pidCloseIdx).trim();

                if (!pid.equals(pidStr))
                    continue;

                // Check timestamp
                String timeStr = elems[1].trim();

                double time = Double.valueOf(timeStr) / 1000000;

                if (time < skipSec)
                    continue;

                // Get address.
                String addrStr = elems[4].trim().replace("0x", "");

                // Get lib and function name.
                String libSymStr = elems[7].trim();

                String lib = libSymStr.substring(0, libSymStr.indexOf('!'));
                String symbol = libSymStr.substring(libSymStr.indexOf('!') + 1);

                Multiset<Long> evs = events.get(evName);

                assert evs != null;

                try {
                    Long addr = Long.valueOf(addrStr, 16);
                    evs.add(addr);
                    methods.put(addr, dedup.dedup(symbol));
                    libs.put(addr, dedup.dedup(lib));
                } catch (NumberFormatException e) {
                    // kernel addresses like "ffffffff810c1b00" overflow signed long,
                    // record them as dummy address
                    evs.add(0L);
                }
            }

            methods.put(0L, "<kernel>");

            return new PerfEvents(this.events, events, methods, libs);
        } catch (IOException e) {
            return new PerfEvents(events);
        } finally {
            FileUtils.safelyClose(fr);
        }
    }

    @Override
    protected String perfBinaryExtension() {
        // Files with ".etl" extension can be opened by "Windows Performance Analyzer" right away.
        return ".etl";
    }
}
