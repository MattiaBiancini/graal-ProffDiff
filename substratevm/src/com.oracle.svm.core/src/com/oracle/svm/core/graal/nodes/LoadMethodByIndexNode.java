/*
 * Copyright (c) 2025, 2025, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.svm.core.graal.nodes;

import static jdk.graal.compiler.nodeinfo.NodeCycles.CYCLES_2;
import static jdk.graal.compiler.nodeinfo.NodeSize.SIZE_1;

import org.graalvm.nativeimage.c.function.CodePointer;

import jdk.graal.compiler.core.common.type.StampFactory;
import jdk.graal.compiler.graph.NodeClass;
import jdk.graal.compiler.nodeinfo.NodeInfo;
import jdk.graal.compiler.nodes.FixedWithNextNode;
import jdk.graal.compiler.nodes.ValueNode;
import jdk.graal.compiler.nodes.spi.Lowerable;
import jdk.graal.compiler.word.WordTypes;

@NodeInfo(cycles = CYCLES_2, size = SIZE_1)
public final class LoadMethodByIndexNode extends FixedWithNextNode implements Lowerable {
    public static final NodeClass<LoadMethodByIndexNode> TYPE = NodeClass.create(LoadMethodByIndexNode.class);

    @Input protected ValueNode hub;
    @Input protected ValueNode vtableIndex;
    @OptionalInput protected ValueNode interfaceTypeID;

    protected LoadMethodByIndexNode(@InjectedNodeParameter WordTypes wordTypes, ValueNode hub, ValueNode vtableIndex, ValueNode interfaceTypeID) {
        super(TYPE, StampFactory.forKind(wordTypes.getWordKind()));
        this.hub = hub;
        this.vtableIndex = vtableIndex;
        this.interfaceTypeID = interfaceTypeID;
    }

    public ValueNode getHub() {
        return hub;
    }

    public ValueNode getVTableIndex() {
        return vtableIndex;
    }

    public ValueNode getInterfaceTypeID() {
        return interfaceTypeID;
    }

    @NodeIntrinsic
    public static native CodePointer loadMethodByIndex(Object hub, int vtableIndex, int interfaceTypeID);
}
