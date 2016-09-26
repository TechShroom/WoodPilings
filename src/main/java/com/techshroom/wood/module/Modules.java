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
