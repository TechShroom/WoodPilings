package com.techshroom.wood.module;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.Range;
import com.techshroom.wood.ModuleDependency;

abstract class TestBase {

    static Set<ModuleDependency> require(Module... modules) {
        return Stream.of(modules).map(Module::getMetadata)
                .map(m -> ModuleDependency.fromFields(m.getId(), Range.singleton(m.getVersion())))
                .collect(Collectors.toSet());
    }

    static Set<ModuleDependency> require(String... modules) {
        return Stream.of(modules).map(m -> {
            return ModuleDependency.fromString(m);
        }).collect(Collectors.toSet());
    }

}
