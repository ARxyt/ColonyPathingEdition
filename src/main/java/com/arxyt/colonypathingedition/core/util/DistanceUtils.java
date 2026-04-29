package com.arxyt.colonypathingedition.core.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public class DistanceUtils {
    /**
     * 曼哈顿距离 (L1)
     */
    public static double manhattanDistance(int x, int y, int z, BlockPos pos) {
        return Math.abs(x - pos.getX()) + Math.abs(y - pos.getY())/5.0 + Math.abs(z - pos.getZ());
    }

    /**
     * 动态曼哈顿距离 (L1)
     */
    public static double manhattanDistanceV(int x, int y, int z, BlockPos pos) {
        int xzDist = Math.abs(x - pos.getX()) + Math.abs(z - pos.getZ());
        double yWeight = 0.2 + 20.0 / (xzDist + 20);
        return xzDist + Math.abs(y - pos.getY()) * yWeight;
    }

    public static double manhattanDistanceV(BlockPos pos1, BlockPos pos2) {
        int xzDist = Math.abs(pos1.getX() - pos2.getX()) + Math.abs(pos1.getZ() - pos2.getZ());
        double yWeight = 0.2 + 20.0 / (xzDist + 20);
        return xzDist + Math.abs(pos1.getY() - pos2.getY()) * yWeight;
    }

    /**
     * 竖切曼哈顿距离 (L1)
     */
    public static double manhattanDistanceVWithYWeight(double x, double y, double z, Vec3 pos, double yWeight) {
        double dx = Math.abs(x - pos.x);
        double dy = Math.abs(y - pos.y);
        double dz = Math.abs(z - pos.z);
        return dx + dy * dy * yWeight + dz;
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
     * 欧氏距离 (L2)
     */
    public static double dist(double x, double y, double z, Vec3 pos) {
        double dx = x - pos.x;
        double dy = y - pos.y;
        double dz = z - pos.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * 2D欧氏距离 (L1)
     */
    public static double dist2D(double x, double z, Vec3 pos) {
        double dx = x - pos.x;
        double dz = z - pos.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    public static double dist2D(BlockPos pos1, BlockPos pos2) {
        double dx = pos1.getX() - pos2.getX();
        double dz = pos1.getZ() - pos2.getZ();
        return Math.sqrt(dx * dx + dz * dz);
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

