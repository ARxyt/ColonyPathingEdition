package com.arxyt.colonypathingedition.mixins.minecraft;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(DoorBlock.class)
public class DoorBlockMixin extends Block {

    public DoorBlockMixin(Properties pProperties, BlockSetType pType) {
        super(pProperties.sound(pType.soundType()));
    }

    @Override
    public @NotNull VoxelShape getCollisionShape(@NotNull BlockState pState, @NotNull BlockGetter pLevel, @NotNull BlockPos pPos, @NotNull CollisionContext pContext) {
        if (!(pContext instanceof EntityCollisionContext ecc && ecc.getEntity() instanceof Animal animal)) return super.getCollisionShape(pState, pLevel, pPos, pContext);
        if (animal.getPersistentData().getBoolean("ColonyPathingEdition_Penned")) return Shapes.block();
        return super.getCollisionShape(pState, pLevel, pPos, pContext);
    }
}
