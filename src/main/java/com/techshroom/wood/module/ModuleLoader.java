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
