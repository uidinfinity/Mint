package net.melbourne.pathfinding;

import lombok.Getter;
import net.minecraft.util.math.BlockPos;

public class PathNode implements Comparable<PathNode> {
    public final BlockPos pos;
    public final double g;
    public final double h;
    @Getter
    public final double f;
    public final PathNode parent;

    public PathNode(BlockPos pos, PathNode parent, double g, double h) {
        this.pos = pos;
        this.parent = parent;
        this.g = g;
        this.h = h;
        this.f = g + h;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PathNode pathNode = (PathNode) o;
        return pos.equals(pathNode.pos);
    }

    @Override
    public int hashCode() {
        return pos.hashCode();
    }

    @Override
    public int compareTo(PathNode other) {
        return Double.compare(this.f, other.f);
    }
}