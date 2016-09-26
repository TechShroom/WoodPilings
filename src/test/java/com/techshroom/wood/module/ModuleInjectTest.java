/*
 * This file is part of WoodPilings, licensed under the MIT License (MIT).
 *
 * Copyright (c) TechShroom Studios <https://techshroom.com>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.techshroom.wood.module;

import static org.junit.Assert.*;

import org.junit.Test;

import com.google.auto.service.AutoService;
import com.google.common.annotations.VisibleForTesting;
import com.techshroom.wood.module.AbstractModule.Meta;

public class ModuleInjectTest extends TestBase {

    @AutoService(Module.class)
    @VisibleForTesting
    @Meta(id = "decl", name = "Decl", version = "1.0.0")
    public static final class DeclDep extends AbstractModule {

    }

    @AutoService(Module.class)
    @VisibleForTesting
    @Meta(id = "target", name = "Target", version = "1.0.0", required = "decl")
    public static final class Target extends AbstractModule {

        @Dependency("decl")
        private final DeclDep decl = null;

    }

    @Test
    public void injectDependecy() throws Exception {
        ModuleLoader.load();
        assertNotNull(ModuleLoader.getAllModules().get("target"));
        assertNotNull(((Target) ModuleLoader.getAllModules().get("target")).decl);
    }

}
