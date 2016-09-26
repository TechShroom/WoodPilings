package com.techshroom.wood.module;

import static com.google.common.base.Preconditions.checkState;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.Throwables;
import com.google.common.collect.Sets;
import com.techshroom.wood.ModuleDependency;

final class ModuleDependencyInjector {

    private static final Field MODS;
    static {
        try {
            MODS = Field.class.getDeclaredField("modifiers");
            MODS.setAccessible(true);
        } catch (Exception e) {
            throw new Error("unable to aquire modifiers field", e);
        }
    }

    private ModuleDependencyInjector() {
    }

    public static void inject(Module m, Map<String, Module> moduleMap) {
        try {
            doInject(m, moduleMap);
        } catch (Exception e) {
            Throwables.throwIfUnchecked(e);
            throw new RuntimeException(e);
        }
    }

    private static void doInject(Module m, Map<String, Module> moduleMap) throws Exception {
        Set<String> allowedDeps =
                Sets.union(m.getMetadata().getLoadAfterModules(), m.getMetadata().getRequiredModules()).stream()
                        .map(ModuleDependency::getId).collect(Collectors.toSet());
        Collection<Field> injectFields = Stream.of(m.getClass().getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Dependency.class)).collect(Collectors.toSet());
        for (Field f : injectFields) {
            String id = f.getAnnotation(Dependency.class).value();
            checkState(allowedDeps.contains(id), "id %s is not a declared dependency of module %s", id,
                    m.getMetadata().getId());
            if (moduleMap.containsKey(id)) {
                f.setAccessible(true);
                MODS.setInt(f, MODS.getInt(f) & ~(Modifier.FINAL));
                f.set(m, moduleMap.get(id));
            }
        }
    }
}
