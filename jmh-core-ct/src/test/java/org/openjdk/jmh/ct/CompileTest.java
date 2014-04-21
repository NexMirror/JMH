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
package org.openjdk.jmh.ct;

import junit.framework.Assert;
import org.openjdk.jmh.generators.core.BenchmarkGenerator;
import org.openjdk.jmh.generators.core.GeneratorDestination;
import org.openjdk.jmh.generators.core.MetadataInfo;
import org.openjdk.jmh.generators.reflective.RFGeneratorSource;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CompileTest {

    public static void assertFail(Class<?> klass) {
        TestGeneratorDestination destination = doTest(klass);
        if (!destination.hasErrors()) {
            Assert.fail("Should have failed.");
        }
    }

    public static void assertOK(Class<?> klass) {
        TestGeneratorDestination destination = doTest(klass);

        if (destination.hasErrors()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Failed with:\n");
            for (String e : destination.getErrors()) {
                sb.append(e).append("\n");
            }
            Assert.fail(sb.toString());
        }

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();

        JavaCompiler.CompilationTask task = ToolProvider.getSystemJavaCompiler().getTask(
                null, null, diagnostics, null, null,
                Arrays.asList(new JavaSourceFromString(destination.className, destination.getContents())));

        boolean success = task.call();

        if (!success) {
            System.out.println(destination.getContents());
            for (Diagnostic diagnostic : diagnostics.getDiagnostics()) {
                System.out.println(diagnostic.getKind() + " at line " + diagnostic.getLineNumber() + ": " + diagnostic.getMessage(null));
            }
            Assert.fail("Unable to compile the generated code");
        }
    }

    public static class JavaSourceFromString extends SimpleJavaFileObject {
        final String code;

        JavaSourceFromString(String name, String code) {
            super(URI.create("string:///" + name.replace('.', '/') + JavaFileObject.Kind.SOURCE.extension), JavaFileObject.Kind.SOURCE);
            this.code = code;
        }

        @Override
        public CharSequence getCharContent(boolean iee) {
            return code;
        }
    }

    private static TestGeneratorDestination doTest(Class<?> klass) {
        RFGeneratorSource source = new RFGeneratorSource();
        TestGeneratorDestination destination = new TestGeneratorDestination();

        source.processClasses(klass);

        BenchmarkGenerator gen = new BenchmarkGenerator();
        gen.generate(source, destination);
        gen.complete(source, destination);
        return destination;
    }

    public static class TestGeneratorDestination implements GeneratorDestination {

        List<String> errors = new ArrayList<String>();

        String className;
        StringWriter sw = new StringWriter();

        @Override
        public Writer newResource(String resourcePath) throws IOException {
            return new PrintWriter(System.out, true);
        }

        @Override
        public Writer newClass(String className) throws IOException {
            if (this.className != null) {
                throw new IllegalStateException("Already writing the class");
            }
            this.className = className;
            return new PrintWriter(sw, true);
        }

        public String getContents() {
            return sw.toString();
        }

        @Override
        public void printError(String message) {
            errors.add(message);
        }

        @Override
        public void printError(String message, MetadataInfo element) {
            errors.add(message);
        }

        @Override
        public void printError(String message, Throwable throwable) {
            errors.add(message);
        }

        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        public List<String> getErrors() {
            return errors;
        }
    }


}
