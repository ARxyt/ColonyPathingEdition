package com.arxyt.colonypathingedition.mixins.minecolonies.entity;

import com.minecolonies.api.entity.citizen.AbstractCivilianEntity;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.api.entity.citizen.citizenhandlers.ICitizenJobHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import static com.arxyt.colonypathingedition.core.costants.AdditionalContants.TAG_UPDATE_INTERVAL;

@Mixin(value = AbstractEntityCitizen.class, remap = false)
public abstract class AbstractEntityCitizenMixin extends AbstractCivilianEntity {
    @Unique private short abstractEntityCitizenIntervalCounter = -1;

    @Shadow(remap = false) public abstract ICitizenJobHandler getCitizenJobHandler();

    public AbstractEntityCitizenMixin(final EntityType<? extends PathfinderMob> type, final Level world)
    {
        super(type, world);
    }

    @Override
    public void tick() {
        super.tick();
        //市民职业信息
        if(++abstractEntityCitizenIntervalCounter <= 0 || abstractEntityCitizenIntervalCounter >= TAG_UPDATE_INTERVAL) {
            abstractEntityCitizenIntervalCounter = 0;
            CompoundTag tag = getPersistentData();
            if (getCitizenJobHandler() != null && getCitizenJobHandler().getColonyJob() != null) {
                tag.putString("citizenJob", getCitizenJobHandler().getColonyJob().getModel().getPath());
            } else {
                tag.putString("citizenJob", "unemployed");
            }
        }
    }
}
