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

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.auto.value.AutoValue;
import com.techshroom.wood.ModuleDependency;
import com.techshroom.wood.SemVer;
import com.techshroom.wood.UTF8Properties;

public final class Modules {

    public static String getBasicRepresentation(Collection<ModuleMetadata> modules) {
        return modules.stream().map(Modules::getBasicRepresentation).collect(Collectors.joining(","));
    }

    public static String getBasicRepresentation(Module module) {
        return getBasicRepresentation(module.getMetadata());
    }

    public static String getBasicRepresentation(ModuleMetadata module) {
        // Class[Name/id]
        return module.getClass().getSimpleName() + "[" + module.getName() + "/" + module.getName() + "]";
    }

    public static ModuleMetadata getModuleMetadata(InputStream stream) throws IOException {
        // Module metadata is kept as a UTF-8 properties file
        UTF8Properties properties = new UTF8Properties().load(stream);
        return ModsModMeta.of(properties.get("id"), properties.get("name"),
                ModuleDependency.fromList(properties.get("loadAfter")).toSet(),
                ModuleDependency.fromList(properties.get("loadBefore")).toSet(),
                ModuleDependency.fromList(properties.get("required")).toSet(),
                SemVer.fromString(properties.get("version")));
    }

    /**
     * AutoValue-based implementation of ModuleMetadata.
     */
    @AutoValue
    static abstract class ModsModMeta implements ModuleMetadata {

        static ModsModMeta
                of(String id, String name, Set<ModuleDependency> loadAfterModules, Set<ModuleDependency> loadBeforeModules, Set<ModuleDependency> requiredModules, SemVer version) {
            return new AutoValue_Modules_ModsModMeta(id, name, version, loadAfterModules, loadBeforeModules,
                    requiredModules);
        }
    }

    private Modules() {
    }
}
