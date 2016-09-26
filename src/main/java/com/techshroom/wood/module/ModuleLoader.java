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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads {@link Module Modules} using {@link ServiceLoader}.
 * <p>
 * Note: Module discovery DOES NOT OCCUR until {@link #load()} has been called.
 * </p>
 */
public final class ModuleLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModuleLoader.class);

    private static final Object LOAD_LOCK = new Object();
    private static boolean initialized;
    private static final Map<String, Module> moduleMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private static final Map<String, Module> unmodifiableModuleMap = Collections.unmodifiableMap(moduleMap);
    private static List<Module> dependencyOrder;

    public static Map<String, Module> getAllModules() {
        return unmodifiableModuleMap;
    }

    public static void load() {
        synchronized (LOAD_LOCK) {
            if (initialized) {
                return;
            }
            initialized = true;
            LOGGER.info("Initializing module system...");
            new LoadManager().doLoad();
            LOGGER.info("Injecting dependencies");
            dependencyOrder.forEach(m -> {
                ModuleDependencyInjector.inject(m, moduleMap);
            });
            LOGGER.info("Firing pre-init");
            dependencyOrder.forEach(m -> {
                try {
                    m.onPreInit();
                } catch (Exception e) {
                    LOGGER.error("Error in preInit for module " + m.getMetadata().getId(), e);
                }
            });
            LOGGER.info("Firing init");
            dependencyOrder.forEach(m -> {
                try {
                    m.onInit();
                } catch (Exception e) {
                    LOGGER.error("Error in init for module " + m.getMetadata().getId(), e);
                }
            });
        }
    }

    /**
     * Loading logic is encapsulated in this class.
     */
    private static final class LoadManager {

        private static final Logger LOGGER = LoggerFactory.getLogger(LoadManager.class);

        private final ServiceLoader<Module> loader = ServiceLoader.load(Module.class);

        private void doLoad() {
            moduleMap.clear();
            try {
                for (Module info : this.loader) {
                    ModuleMetadata metadata = info.getMetadata();
                    Module old = moduleMap.put(metadata.getId(), info);
                    // this is efficient because we expect to not have
                    // duplicates
                    // so we'll only do one put in most cases
                    if (old != null) {
                        moduleMap.put(metadata.getId(), old);
                        if (LOGGER.isWarnEnabled()) {
                            LOGGER.warn(String.format("%s tried to override id %s, but it is already used by %s.",
                                    Modules.getBasicRepresentation(info), metadata.getId(),
                                    Modules.getBasicRepresentation(old)));
                        }
                    }
                }
            } catch (Exception | ServiceConfigurationError t) {
                LOGGER.error("Error creating modules", t);
            }
            try {
                dependencyOrder = new ModuleDependencySolver(moduleMap).computeDependencyOrder();
            } catch (Exception e) {
                LOGGER.info("Error while calculating depdency graph", e);
            }
        }

    }

}
