/*
 * Copyright (c) 2005, 2014, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmh.generators.asm;

import org.objectweb.asm.ClassReader;
import org.openjdk.jmh.generators.core.ClassInfo;
import org.openjdk.jmh.generators.core.GeneratorSource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

public class ASMGeneratorSource implements GeneratorSource {

    private final ClassInfoRepo classInfos;

    public ASMGeneratorSource() {
        this.classInfos = new ClassInfoRepo();
    }

    public void processClasses(Collection<File> classFiles)  throws IOException {
        for (File f : classFiles) {
            processClass(f);
        }
    }

    public void processClass(File classFile) throws IOException {
        try (FileInputStream fis = new FileInputStream(classFile)){
            processClass(fis);
        }
    }

    public void processClass(InputStream stream) throws IOException {
        final ASMClassInfo ci = new ASMClassInfo(classInfos);
        ClassReader reader = new ClassReader(stream);
        reader.accept(ci, 0);
        classInfos.put(ci.getIdName(), ci);
    }

    @Override
    public Collection<ClassInfo> getClasses() {
        return classInfos.getInfos();
    }

    @Override
    public ClassInfo resolveClass(String className) {
        return classInfos.get(className);
    }

}
