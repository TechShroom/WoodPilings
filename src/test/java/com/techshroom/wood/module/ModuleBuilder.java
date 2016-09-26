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
