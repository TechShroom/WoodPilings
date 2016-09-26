package com.techshroom.wood.module;

public interface Module {

    ModuleMetadata getMetadata();

    /**
     * Called after any dependent mods have been loaded & pre-init'd
     */
    default void onPreInit() {
    }

    /**
     * Called after all mods have been loaded & pre-init'd, and after any
     * dependent mods have be init'd.
     */
    default void onInit() {
    }

}
