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

import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.techshroom.wood.ModuleDependency;
import com.techshroom.wood.SemVer;

/**
 * Utility for making Module instances.
 */
class ModuleBuilder {

    private final String id;
    private final String name;
    private Set<ModuleDependency> required = ImmutableSet.of();
    private Set<ModuleDependency> loadAfter = ImmutableSet.of();
    private Set<ModuleDependency> loadBefore = ImmutableSet.of();
    private SemVer version;

    ModuleBuilder(String id, String name, String ver) {
        this.id = id;
        this.name = name;
        this.version = SemVer.fromString(ver);
    }

    public ModuleBuilder setRequired(Set<ModuleDependency> required) {
        this.required = required;
        return this;
    }

    public ModuleBuilder setLoadAfter(Set<ModuleDependency> loadAfter) {
        this.loadAfter = loadAfter;
        return this;
    }

    public ModuleBuilder setLoadBefore(Set<ModuleDependency> loadBefore) {
        this.loadBefore = loadBefore;
        return this;
    }

    ModuleMetadata buildMeta() {

        String id = this.id;
        String name = this.name;
        Set<ModuleDependency> required = this.required;
        Set<ModuleDependency> loadAfter = this.loadAfter;
        Set<ModuleDependency> loadBefore = this.loadBefore;
        SemVer version = this.version;
        class MBSnapshot implements ModuleMetadata {

            @Override
            public String getId() {
                return id;
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public Set<ModuleDependency> getLoadAfterModules() {
                return loadAfter;
            }

            @Override
            public Set<ModuleDependency> getLoadBeforeModules() {
                return loadBefore;
            }

            @Override
            public Set<ModuleDependency> getRequiredModules() {
                return required;
            }

            @Override
            public SemVer getVersion() {
                return version;
            }

        }
        return new MBSnapshot();
    }

    Module build() {
        ModuleMetadata meta = buildMeta();
        class MBModule implements Module {

            @Override
            public ModuleMetadata getMetadata() {
                return meta;
            }
        }
        return new MBModule();

    }

}
