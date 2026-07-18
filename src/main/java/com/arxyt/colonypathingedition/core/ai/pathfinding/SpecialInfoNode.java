package com.arxyt.colonypathingedition.core.ai.pathfinding;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.pathfinder.Node;
import org.jetbrains.annotations.NotNull;

public class SpecialInfoNode extends Node {

    /**
     * Ladder params.
     */
    private boolean onLadder = false;
    private boolean ladderEntry = false;
    private boolean ladderExit = false;
    private Direction ladderFacing = null;
    private Direction ladderNext = null;
    /**
     * Rails params.
     */
    private boolean onRails;
    private boolean railsEntry;
    private boolean railsExit;

    /**
     * Water params.
     */
    private boolean waterEntry;

    /**
     * Instantiates the pathPoint with a position.
     *
     * @param pos the position.
     */
    public SpecialInfoNode(@NotNull final BlockPos pos)
    {
        super(pos.getX(), pos.getY(), pos.getZ());
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
     * Set the ladder entry.
     */
    public void setLadderEntry()
    {
        this.ladderEntry = true;
    }
    public boolean isLadderEntry()
    {
        return ladderEntry;
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
     * Set the rail's entry.
     */
    public void setRailsEntry()
    {
        this.railsEntry = true;
    }
    public boolean isRailsEntry()
    {
        return railsEntry;
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
     * Set the water entry.
     */
    public void setWaterEntry()
    {
        this.waterEntry = true;
    }
    public boolean isWaterEntry()
    {
        return waterEntry;
    }
}
