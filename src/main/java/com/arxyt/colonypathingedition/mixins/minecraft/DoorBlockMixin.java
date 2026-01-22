package com.arxyt.colonypathingedition.mixins.minecraft;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(DoorBlock.class)
public class DoorBlockMixin extends Block {

    public DoorBlockMixin(BlockBehaviour.Properties pProperties, BlockSetType pType) {
        super(pProperties.sound(pType.soundType()));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        if (!(pContext instanceof EntityCollisionContext ecc && ecc.getEntity() instanceof Animal animal && !animal.isLeashed())) return super.getCollisionShape(pState, pLevel, pPos, pContext);
        if (animal.getPersistentData().getBoolean("ColonyPathingEdition_Penned")) return Shapes.block();
        return super.getCollisionShape(pState, pLevel, pPos, pContext);
    }
}
