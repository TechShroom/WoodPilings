package com.techshroom.wood.module;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate fields with this to get a dependency injected. This dependency MUST
 * be listed in the {@link ModuleMetadata#getLoadAfterModules() loadAfter} or
 * {@link ModuleMetadata#getRequiredModules() required} field of ModuleMetadata.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Dependency {

    String value();
}
