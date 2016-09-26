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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class DepdencySolverTest extends TestBase {

    private static ImmutableList<Module> solve(Module... mods) {
        Map<String, Module> map =
                Stream.of(mods).collect(Collectors.toMap(m -> m.getMetadata().getId(), Function.identity()));
        return new ModuleDependencySolver(map).computeDependencyOrder();
    }

    private static void assertAfter(List<Module> res, Module before, Module... afters) {
        int beforeI = res.indexOf(before);
        int[] afterI = Stream.of(afters).mapToInt(res::indexOf).toArray();
        assertNotEquals(Modules.getBasicRepresentation(before) + " not in list", -1, beforeI);
        for (int i = 0; i < afters.length; i++) {
            Module after = afters[i];
            assertNotEquals(Modules.getBasicRepresentation(after) + " not in list", -1, afterI[i]);
            assertTrue(
                    Modules.getBasicRepresentation(before) + " occurs after " + Modules.getBasicRepresentation(after),
                    beforeI < afterI[i]);
        }
    }

    @Test
    public void solveNoModules() throws Exception {
        assertEquals(ImmutableList.of(), solve());
    }

    @Test
    public void solveOneModule() throws Exception {
        Module module = new ModuleBuilder("id", "id", "1.0.0").build();
        assertEquals(ImmutableList.of(module), solve(module));
    }

    @Test
    public void solveTwoIndependentModule() throws Exception {
        Module moduleA = new ModuleBuilder("a", "A", "1.0.0").build();
        Module moduleB = new ModuleBuilder("b", "B", "1.0.0").build();
        ImmutableList<Module> solve = solve(moduleA, moduleB);
        assertTrue(solve.containsAll(ImmutableList.of(moduleA, moduleB)));
    }

    @Test
    public void solveBDependsA() throws Exception {
        Module moduleA = new ModuleBuilder("a", "A", "1.0.0").build();
        Module moduleB = new ModuleBuilder("b", "B", "1.0.0").setRequired(require(moduleA)).build();
        ImmutableList<Module> solve = solve(moduleA, moduleB);
        assertAfter(solve, moduleA, moduleB);
    }

    @Test
    public void solveComplexTree() throws Exception {
        /*
         * Large dependency tree. 1 is the root. 2 & 3 depend on 1, 3 depends on
         * 2, 4 depends on 3, B depends on all of them. C only depends on 2.
         */
        Module dep1 = new ModuleBuilder("a1", "A1", "1.0.0").build();
        Module dep2 = new ModuleBuilder("a2", "A2", "2.0.0").setRequired(require(dep1)).build();
        Module dep3 = new ModuleBuilder("a3", "A3", "3.0.0").setRequired(require(dep1, dep2)).build();
        Module dep4 = new ModuleBuilder("a4", "A4", "4.0.0").setRequired(require(dep3)).build();
        Module depB = new ModuleBuilder("b", "B", "5.0.0").setRequired(require(dep1, dep2, dep3, dep4)).build();
        Module depC = new ModuleBuilder("c", "C", "6.0.0").setRequired(require(dep2)).build();
        ImmutableList<Module> solve = solve(dep2, dep4, dep3, depC, dep1, depB);
        assertAfter(solve, dep1, dep2, dep3, dep4, depB);
        assertAfter(solve, dep2, dep3, dep4, depB, depC);
        assertAfter(solve, dep3, dep4, depB);
        assertAfter(solve, dep4, depB);
    }

}
