package com.elfmcys.yesstevemodel.resource.models;

import java.util.Objects;

public final class MainModelInfo {
    private final int bones;
    private final int cubes;
    private final int faces;

    public MainModelInfo(int bones, int cubes, int faces) {
        this.bones = bones;
        this.cubes = cubes;
        this.faces = faces;
    }

    public int bones() {
        return this.bones;
    }

    public int cubes() {
        return this.cubes;
    }

    public int faces() {
        return this.faces;
    }

    public int getBones() {
        return this.bones;
    }

    public int getCubes() {
        return this.cubes;
    }

    public int getFaces() {
        return this.faces;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MainModelInfo)) {
            return false;
        }
        MainModelInfo other = (MainModelInfo) obj;
        return this.bones == other.bones && this.cubes == other.cubes && this.faces == other.faces;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.bones, this.cubes, this.faces);
    }

    @Override
    public String toString() {
        return "MainModelInfo[bones=" + this.bones + ", cubes=" + this.cubes + ", faces=" + this.faces + "]";
    }
}
