package com.arxyt.colonypathingedition.core.util;

import net.minecraft.core.BlockPos;

public class DistanceUtils {
    /**
     * 曼哈顿距离 (L1)
     */
    public static double manhattanDistance(int x, int y, int z, BlockPos pos) {
        return Math.abs(x - pos.getX()) + Math.abs(y - pos.getY())/5.0 + Math.abs(z - pos.getZ());
    }

    /**
     * 欧氏距离 (L2)
     */
    public static double dist(int x, int y, int z, BlockPos pos) {
        double dx = x - pos.getX();
        double dy = (y - pos.getY())/5.0;
        double dz = z - pos.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public static double dist(BlockPos pos1, BlockPos pos2) {
        double dx = pos1.getX() - pos2.getX();
        double dy = (pos1.getY() - pos2.getY())/5.0;
        double dz = pos1.getZ() - pos2.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * 欧氏距离的平方 (L2)
     */
    public static double dist2(BlockPos pos, BlockPos pos2) {
        double dx = pos2.getX() - pos.getX();
        double dy = (pos2.getY() - pos.getY())/5.0;
        double dz = pos2.getZ() - pos.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    public static double dist2(int x, int y, int z, BlockPos pos) {
        double dx = x - pos.getX();
        double dy = (y - pos.getY())/5.0;
        double dz = z - pos.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * 柱形 (L2)
     */
    public static double cylinderDistance(int x, int y, int z, BlockPos pos) {
        double dx = x - pos.getX();
        double dy = (y - pos.getY())/5.0;
        double dz = z - pos.getZ();
        return Math.max(Math.sqrt(dx * dx + dz * dz) , dy);
    }

    /**
     * 切比雪夫距离 (L∞)
     */
    public static double chebyshevDistance(int x, int y, int z, BlockPos pos) {
        return Math.max(Math.max(Math.abs(x - pos.getX()), Math.abs(y - pos.getY())/5.0), Math.abs(z - pos.getZ()));
    }

}

