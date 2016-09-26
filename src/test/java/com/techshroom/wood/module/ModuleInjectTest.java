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
