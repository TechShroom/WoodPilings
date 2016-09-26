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

import static com.google.common.base.Preconditions.checkState;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import com.google.common.collect.Sets;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import com.techshroom.wood.ModuleDependency;
import com.techshroom.wood.SemVer;

@VisibleForTesting
class ModuleDependencySolver {

    private final Map<String, Module> moduleMap;

    ModuleDependencySolver(Map<String, Module> moduleMap) {
        this.moduleMap = moduleMap;
    }

    ImmutableList<Module> computeDependencyOrder() {
        // Fast-track no module case
        if (this.moduleMap.isEmpty()) {
            return ImmutableList.of();
        }
        // If a node goes from A->B, A must be loaded AFTER B
        MutableGraph<ModuleMetadata> depGraph =
                GraphBuilder.directed().allowsSelfLoops(false).expectedNodeCount(this.moduleMap.size()).build();
        // Insert all nodes before connecting
        this.moduleMap.values().stream().map(Module::getMetadata).forEach(depGraph::addNode);
        for (Module factory : this.moduleMap.values()) {
            ModuleMetadata data = factory.getMetadata();
            data.getLoadAfterModules().forEach(dep -> {
                Range<SemVer> acceptable = dep.getVersionRange();
                // Here, we must load data after meta, put data->meta
                depGraph.nodes().stream().filter(meta -> acceptable.contains(meta.getVersion())).findAny()
                        .ifPresent(meta -> {
                            // Do a check for existing edges going the other
                            // way
                            if (depGraph.edges().contains(EndpointPair.ordered(meta, data))) {
                                throw new IllegalStateException("Cannot have a two-way dependency. Found between "
                                        + Modules.getBasicRepresentation(data) + " and "
                                        + Modules.getBasicRepresentation(meta));
                            }
                            depGraph.putEdge(data, meta);
                        });
            });
            data.getLoadBeforeModules().forEach(dep -> {
                Range<SemVer> acceptable = dep.getVersionRange();
                // Here, we must load data before meta, put meta->data
                depGraph.nodes().stream().filter(meta -> acceptable.contains(meta.getVersion())).findAny()
                        .ifPresent(meta -> {
                            // Do a check for existing edges going the other
                            // way
                            if (depGraph.edges().contains(EndpointPair.ordered(data, meta))) {
                                throw new IllegalStateException("Cannot have a two-way dependency. Found between "
                                        + Modules.getBasicRepresentation(data) + " and "
                                        + Modules.getBasicRepresentation(meta));
                            }
                            depGraph.putEdge(meta, data);
                        });
            });
            data.getRequiredModules().forEach(dep -> {
                Range<SemVer> acceptable = dep.getVersionRange();
                // Here, we must load data after meta, put data->meta
                ModuleMetadata result = depGraph.nodes().stream().filter(meta -> acceptable.contains(meta.getVersion()))
                        .findAny().orElseThrow(() -> {
                            return new IllegalStateException("Missing required dependency " + dep);
                        });
                // Do a check for existing edges going the other
                // way
                if (depGraph.edges().contains(EndpointPair.ordered(result, data))) {
                    throw new IllegalStateException("Cannot have a two-way dependency. Found between "
                            + Modules.getBasicRepresentation(data) + " and " + Modules.getBasicRepresentation(result));
                }
                depGraph.putEdge(data, result);
            });
        }
        // Modules in dependency-loading order
        List<ModuleMetadata> dependencyOrder = new LinkedList<>();
        // The outDegree is the number of dependencies
        Set<ModuleMetadata> noDeps =
                depGraph.nodes().stream().filter(m -> depGraph.outDegree(m) == 0).collect(Collectors.toSet());
        checkState(!noDeps.isEmpty(), "There must be at least one module with no dependencies.");
        // this set tracks encountered modules (i.e. child nodes)
        // that have not been known as satisfied by things in depedencyOrder
        Set<ModuleMetadata> encounteredNotSatisfied = new HashSet<>();
        // this set tracks satisfied modules
        // (but not yet added to dependencyOrder)
        // that have not been processed to find other modules
        Set<ModuleMetadata> satisfiedNotProcessed = new HashSet<>(noDeps);
        // Snapshots the last round hashcode for checks
        int lastDepOrderSize = 0;
        while (!satisfiedNotProcessed.isEmpty()
                || lastDepOrderSize != Objects.hash(dependencyOrder, encounteredNotSatisfied, satisfiedNotProcessed)) {
            lastDepOrderSize = Objects.hash(dependencyOrder, encounteredNotSatisfied, satisfiedNotProcessed);
            // Process satisfied modules
            for (ModuleMetadata node : satisfiedNotProcessed) {
                dependencyOrder.add(node);
                // Load modules that depend on `node`
                // insert them into encountered
                depGraph.predecessors(node).forEach(dependent -> {
                    encounteredNotSatisfied.add(dependent);
                });
            }
            // Clear satisfiedNotProcessed, after processing
            satisfiedNotProcessed.clear();
            // Process encountered nodes
            for (ModuleMetadata node : encounteredNotSatisfied) {
                // Calculate the load-after deps that might be satisfiable
                // Basically does a ID check against the available
                // dependencies.
                Set<ModuleDependency> satisfiableLoadAfters =
                        getSatisfiableLoadAfters(depGraph.nodes(), node.getLoadAfterModules());
                Set<ModuleDependency> deps = Sets.union(satisfiableLoadAfters, node.getRequiredModules());
                if (allDependenciesSatisified(dependencyOrder, deps)) {
                    satisfiedNotProcessed.add(node);
                }
            }
            // Remove all satisfied
            encounteredNotSatisfied.removeAll(satisfiedNotProcessed);
        }
        if (encounteredNotSatisfied.size() > 0) {
            throw new IllegalStateException("Unsatisfied dependencies: " + encounteredNotSatisfied);
        }
        return FluentIterable.from(dependencyOrder).transform(ModuleMetadata::getId).transform(this.moduleMap::get)
                .toList();
    }

    private Set<ModuleDependency>
            getSatisfiableLoadAfters(Collection<ModuleMetadata> available, Set<ModuleDependency> loadAfters) {
        Set<String> decomposedDeps = available.stream().map(ModuleMetadata::getId).collect(Collectors.toSet());
        return loadAfters.stream().filter(dep -> decomposedDeps.contains(dep.getId())).collect(Collectors.toSet());
    }

    private boolean allDependenciesSatisified(Collection<ModuleMetadata> available, Set<ModuleDependency> required) {
        Map<String, SemVer> decomposedDeps =
                available.stream().collect(Collectors.toMap(ModuleMetadata::getId, ModuleMetadata::getVersion));
        return required.stream().allMatch(dep -> {
            SemVer ver = decomposedDeps.get(dep.getId());
            return ver != null && dep.getVersionRange().contains(ver);
        });
    }

}
