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
package com.techshroom.wood;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.auto.value.AutoValue;
import com.google.common.base.Splitter;
import com.google.common.collect.BoundType;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Range;

@AutoValue
public abstract class ModuleDependency {

    private static final Pattern VERSION_RANGE_PATTERN;
    static {
        // Raw regex: (\d\.\d\.\d(?:\-[^+]+?)?
        // Matches semver specifications
        String versionPattern = "(\\d\\.\\d\\.\\d(?:\\-[^+]+?)?(?:\\+.+?)?)?";
        // Start and end match open/close range markers
        // ^(\(|\[) + versionPattern + ',' + versionPattern + (\)|\])$
        String regex = "^(\\(|\\[)" + versionPattern + "," + versionPattern + "(\\)|\\])$";
        VERSION_RANGE_PATTERN = Pattern.compile(regex);
    }
    private static final SemVer ZERO = SemVer.fromFields(0, 0, 0, null, null);

    public static FluentIterable<ModuleDependency> fromList(String list) {
        Iterable<String> splits = Splitter.on(';').split(list);
        return FluentIterable.from(splits).transform(ModuleDependency::fromString);
    }

    public static ModuleDependency fromString(String dependency) {
        // Dependency format: '<id>:<version>'
        // List format: '<dep>;<dep>;<dep>'
        // Version range format:
        // Any version: [0.0.0,) or * or omitted
        // Any version above 1.0.0: [1.0.0,)
        // Any version with major 1: [1.0.0,2.0.0)
        // Any version with major.minor 1.0: [1.0.0,1.1.0)
        // 1.0.0-1.2.0: [1.0.0,1.2.0]
        // etc. Basically works like Range.
        String id;
        Range<SemVer> range;
        String[] idSplit = dependency.split(":", 2);
        id = idSplit[0];
        if (idSplit.length == 2 && !idSplit[1].equals("*")) {
            String r = idSplit[1];
            Matcher matcher = VERSION_RANGE_PATTERN.matcher(r);

            checkArgument(matcher.matches(), "'%s' is not a valid range", r);

            BoundType lowBound = matcher.group(1).equals("(") ? BoundType.OPEN : BoundType.CLOSED;
            BoundType hiBound = matcher.group(4).equals(")") ? BoundType.OPEN : BoundType.CLOSED;
            String lowVersion = matcher.group(2);
            String hiVersion = matcher.group(3);
            checkArgument(lowVersion != null || hiVersion != null,
                    "A bound must have at least one version. Use \"[0.0.0,)\" or '*' for any.");
            if (lowVersion == null) {
                checkArgument(lowBound == BoundType.OPEN, "must use '(' with no lower bound");
                range = Range.upTo(SemVer.fromString(hiVersion), hiBound);
            } else if (hiVersion == null) {
                checkArgument(hiBound == BoundType.OPEN, "must use ')' with no upper bound");
                range = Range.downTo(SemVer.fromString(lowVersion), lowBound);
            } else {
                range = Range.range(SemVer.fromString(lowVersion), lowBound, SemVer.fromString(hiVersion), hiBound);
            }
        } else {
            range = Range.downTo(ZERO, BoundType.CLOSED);
        }
        return fromFields(id, range);
    }

    public static ModuleDependency fromFields(String id, Range<SemVer> versionRange) {
        return new AutoValue_ModuleDependency(id, versionRange);
    }

    ModuleDependency() {
    }

    public abstract String getId();

    public abstract Range<SemVer> getVersionRange();

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(getId()).append(':');
        Range<SemVer> r = getVersionRange();
        if (r.hasLowerBound()) {
            builder.append(r.lowerBoundType() == BoundType.OPEN ? '(' : '[').append(r.lowerEndpoint());
        } else {
            builder.append('(');
        }
        builder.append(',');
        if (r.hasUpperBound()) {
            builder.append(r.upperEndpoint()).append(r.upperBoundType() == BoundType.OPEN ? ')' : ']');
        } else {
            builder.append(')');
        }
        return builder.toString();
    }

}
