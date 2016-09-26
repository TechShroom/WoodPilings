package com.techshroom.wood.module;

import java.util.Set;

import com.techshroom.wood.ModuleDependency;
import com.techshroom.wood.SemVer;

public interface ModuleMetadata {

    String getId();

    String getName();

    SemVer getVersion();

    Set<ModuleDependency> getLoadAfterModules();

    Set<ModuleDependency> getLoadBeforeModules();

    Set<ModuleDependency> getRequiredModules();

}
