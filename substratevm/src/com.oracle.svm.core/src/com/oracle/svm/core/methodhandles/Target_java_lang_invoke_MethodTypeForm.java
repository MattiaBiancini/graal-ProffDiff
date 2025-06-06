/*
 * Copyright (c) 2020, 2024, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.svm.core.methodhandles;

import java.lang.ref.SoftReference;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.RecomputeFieldValue.Kind;
import com.oracle.svm.core.annotate.TargetClass;
import com.oracle.svm.core.fieldvaluetransformer.NewEmptyArrayFieldValueTransformer;
import com.oracle.svm.core.invoke.Target_java_lang_invoke_MemberName;

@TargetClass(className = "java.lang.invoke.MethodTypeForm")
final class Target_java_lang_invoke_MethodTypeForm {

    /*
     * The following two fields are arrays that are filled lazily. Resetting them brings them into a
     * consistent state, to avoid problems when the lazily initialization happens during image heap
     * writing.
     */
    @Alias @RecomputeFieldValue(kind = Kind.Custom, declClass = NewEmptyArrayFieldValueTransformer.class, isFinal = true) //
    private SoftReference<?>[] methodHandles;
    @Alias @RecomputeFieldValue(kind = Kind.Custom, declClass = NewEmptyArrayFieldValueTransformer.class, isFinal = true) //
    private SoftReference<?>[] lambdaForms;

    @Alias @RecomputeFieldValue(kind = Kind.Reset) //
    private SoftReference<Target_java_lang_invoke_MemberName> interpretEntry;
}
