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

import java.util.Optional;
import java.util.function.ToIntBiFunction;

import javax.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ComparisonChain;

@AutoValue
public abstract class SemVer implements Comparable<SemVer> {

    public static SemVer fromString(String version) {
        String preRelease = null;
        String buildMetadata = null;

        String[] preRelSplit = version.split("-", 2);
        if (preRelSplit.length == 2) {
            version = preRelSplit[0];
            preRelease = preRelSplit[1];
        }

        String[] buildSplit = version.split("\\+", 2);
        if (buildSplit.length == 2) {
            version = buildSplit[0];
            buildMetadata = buildSplit[1];
        }

        ToIntBiFunction<String, String> intParse = (s, field) -> {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("The " + field + " part must be an integer, not " + s);
            }
        };
        String[] versionSplit = version.split("\\.", 3);
        if (versionSplit.length != 3) {
            throw new IllegalArgumentException("Version must have 3 parts.");
        }
        int major = intParse.applyAsInt(versionSplit[0], "major");
        int minor = intParse.applyAsInt(versionSplit[1], "minor");
        int patch = intParse.applyAsInt(versionSplit[2], "patch");
        return fromFields(major, minor, patch, preRelease, buildMetadata);
    }

    public static SemVer
            fromFields(int major, int minor, int patch, @Nullable String preReleaseInfo, @Nullable String buildMetadata) {
        return fromFieldsRaw(major, minor, patch, Optional.ofNullable(preReleaseInfo),
                Optional.ofNullable(buildMetadata));
    }

    private static SemVer
            fromFieldsRaw(int major, int minor, int patch, Optional<String> preReleaseInfo, Optional<String> buildMetadata) {
        return new AutoValue_SemVer(major, minor, patch, preReleaseInfo, buildMetadata);
    }

    SemVer() {
    }

    public abstract int getMajor();

    public final SemVer incrementMajor() {
        return fromFieldsRaw(getMajor() + 1, getMinor(), getPatch(), getPreReleaseInfo(), getBuildMetadata());
    }

    public abstract int getMinor();

    public final SemVer incrementMinor() {
        return fromFieldsRaw(getMajor(), getMinor() + 1, getPatch(), getPreReleaseInfo(), getBuildMetadata());
    }

    public abstract int getPatch();

    public final SemVer incrementPatch() {
        return fromFieldsRaw(getMajor(), getMinor(), getPatch() + 1, getPreReleaseInfo(), getBuildMetadata());
    }

    public abstract Optional<String> getPreReleaseInfo();

    public final SemVer withPreReleaseInfo(@Nullable String preReleaseInfo) {
        return fromFieldsRaw(getMajor(), getMinor(), getPatch(), Optional.ofNullable(preReleaseInfo),
                getBuildMetadata());
    }

    public abstract Optional<String> getBuildMetadata();

    public final SemVer withBuildMetadata(@Nullable String buildMetadata) {
        return fromFieldsRaw(getMajor(), getMinor(), getPatch(), getPreReleaseInfo(),
                Optional.ofNullable(buildMetadata));
    }

    @Override
    public int compareTo(SemVer o) {
        return ComparisonChain.start().compare(getMajor(), o.getMajor()).compare(getMinor(), o.getMinor())
                .compare(getPatch(), o.getPatch())
                .compareFalseFirst(getPreReleaseInfo().isPresent(), o.getPreReleaseInfo().isPresent()).result();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(getMajor()).append('.').append(getMinor()).append('.').append(getPatch());
        getPreReleaseInfo().ifPresent(pr -> builder.append('-').append(pr));
        getBuildMetadata().ifPresent(pr -> builder.append('+').append(pr));
        return builder.toString();
    }

}
