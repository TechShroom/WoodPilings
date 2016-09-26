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

import static com.google.common.base.Preconditions.checkState;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.techshroom.wood.ModuleDependency;
import com.techshroom.wood.SemVer;

public abstract class AbstractModule implements Module {

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Meta {

        String id();

        String name();

        String version();

        String[] loadAfter() default {};

        String[] loadBefore() default {};

        String[] required() default {};

    }

    private final ModuleMetadata meta;
    {
        try {
            Meta meta = getClass().getDeclaredAnnotation(Meta.class);
            checkState(meta != null);
            String id = meta.id();
            String name = meta.name();
            SemVer version = SemVer.fromString(meta.version());
            Set<ModuleDependency> required =
                    Stream.of(meta.required()).map(ModuleDependency::fromString).collect(Collectors.toSet());
            Set<ModuleDependency> loadAfter =
                    Stream.of(meta.loadAfter()).map(ModuleDependency::fromString).collect(Collectors.toSet());
            Set<ModuleDependency> loadBefore =
                    Stream.of(meta.loadBefore()).map(ModuleDependency::fromString).collect(Collectors.toSet());
            class AnnotBasedMeta implements ModuleMetadata {

                @Override
                public String getId() {
                    return id;
                }

                @Override
                public String getName() {
                    return name;
                }

                @Override
                public SemVer getVersion() {
                    return version;
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

            }
            this.meta = new AnnotBasedMeta();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load @Meta", e);
        }
    }

    @Override
    public ModuleMetadata getMetadata() {
        return this.meta;
    }

}
