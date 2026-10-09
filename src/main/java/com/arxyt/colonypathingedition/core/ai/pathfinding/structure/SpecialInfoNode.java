package com.arxyt.colonypathingedition.core.ai.pathfinding.structure;

import com.minecolonies.core.entity.pathfinding.MNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.pathfinder.Node;
import org.jetbrains.annotations.NotNull;

// These nodes contain information for navigator.
public class SpecialInfoNode extends Node {

    public double offsetX = 0D;
    public double offsetY = 0D;
    public double offsetZ = 0D;

    public long hashLong;

    public short visitTimes;

    public enum NodeState {

    }

    /**
     * Ladder params.
     */
    public boolean onLadder = false;
    public boolean ladderEntrance = false;
    public boolean ladderExit = false;
    public Direction ladderFacing = null;
    public Direction ladderNext = null;

    /**
     * Rails params.
     */
    public boolean onRails;
    public boolean railsEntrance;
    public boolean railsExit;

    /**
     * Water params.
     */
    public boolean waterEntrance;
    public boolean swimming;
    public boolean diving;

    /**
     * Danger params.
     */
    public boolean inDanger;

    /**
     * Instantiates the pathPoint with a position.
     *
     * @param pos the position.
     */
    public SpecialInfoNode(@NotNull final BlockPos pos)
    {
        super(pos.getX(), pos.getY(), pos.getZ());
        hashLong = pos.asLong();
    }

    public long hashCodeLong() {
        return this.hashLong;
    }

    /**
     * On ladder settings.
     */
    public boolean isOnLadder()
    {
        return onLadder;
    }
    public void setOnLadder(final boolean onLadder)
    {
        this.onLadder = onLadder;
    }

    /**
     * Set the ladder entrance.
     */
    public void setLadderEntrance()
    {
        this.ladderEntrance = true;
    }
    public boolean isLadderEntrance()
    {
        return ladderEntrance;
    }

    /**
     * Set the ladder exit.
     */
    public void setLadderExit()
    {
        this.ladderExit = true;
    }
    public boolean isLadderExit()
    {
        return ladderExit;
    }

    /**
     * Ladder Facing Settings
     */
    public Direction getLadderFacing()
    {
        return ladderFacing;
    }
    public void setLadderFacing(final Direction ladderFacing)
    {
        this.ladderFacing = ladderFacing;
    }

    /**
     * Next Ladder Direction.
     */
    public Direction getNextLadder()
    {
        return ladderNext;
    }
    public void setNextLadder(final Direction ladderNext)
    {
        this.ladderNext = ladderNext;
    }

    /**
     * Set if it is on rails.
     *
     * @param isOnRails if on rails.
     */
    public void setOnRails(final boolean isOnRails)
    {
        this.onRails = isOnRails;
    }
    public boolean isOnRails()
    {
        return onRails;
    }

    /**
     * Set the rail's entrance.
     */
    public void setRailsEntrance()
    {
        this.railsEntrance = true;
    }
    public boolean isRailsEntrance()
    {
        return railsEntrance;
    }

    /**
     * Set the rail's exit.
     */
    public void setRailsExit()
    {
        this.railsExit = true;
    }
    public boolean isRailsExit()
    {
        return railsExit;
    }

    /**
     * Set the water entrance.
     */
    public void setWaterEntrance()
    {
        this.waterEntrance = true;
    }
    public boolean isWaterEntrance()
    {
        return waterEntrance;
    }

    public boolean isSwimming() {
        return swimming || diving;
    }
}
