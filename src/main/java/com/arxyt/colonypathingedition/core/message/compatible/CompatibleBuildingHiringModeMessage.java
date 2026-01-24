package com.arxyt.colonypathingedition.core.message.compatible;

import com.ldtteam.common.network.PlayMessageType;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.HiringMode;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.buildings.modules.IAssignsCitizen;
import com.minecolonies.api.colony.buildings.modules.IBuildingModule;
import com.minecolonies.api.colony.buildings.views.IBuildingView;
import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.core.network.messages.server.AbstractBuildingServerMessage;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class CompatibleBuildingHiringModeMessage extends AbstractBuildingServerMessage<IBuilding> {

    public static final PlayMessageType<?> TYPE = PlayMessageType.forServer(Constants.MOD_ID, "compatible_building_hiring_mode", CompatibleBuildingHiringModeMessage::new);

    /**
     * The Hiring mode to set.
     */
    private final HiringMode    mode;

    /**
     * The job id.
     */
    private final int moduleId;

    /**
     * The job id.
     */
    private final String moduleKey;

    /**
     * Creates object for the hiring mode
     *
     * @param building View of the building to read data from.
     * @param mode     the hiring mode.
     */
    public CompatibleBuildingHiringModeMessage(@NotNull final IBuildingView building, final HiringMode mode, final int moduleId, final String moduleKey)
    {
        super(TYPE, building);
        this.mode = mode;
        this.moduleId = moduleId;
        this.moduleKey = moduleKey;
    }

    protected CompatibleBuildingHiringModeMessage(final RegistryFriendlyByteBuf buf, final PlayMessageType<?> type)
    {
        super(buf, type);
        mode = HiringMode.values()[buf.readInt()];
        moduleId = buf.readInt();
        moduleKey = buf.readUtf();
    }

    @Override
    protected void toBytes(@NotNull final RegistryFriendlyByteBuf buf)
    {
        super.toBytes(buf);
        buf.writeInt(mode.ordinal());
        buf.writeInt(moduleId);
        buf.writeUtf(moduleKey);
    }

    @Override
    protected void onExecute(final IPayloadContext ctxIn, final ServerPlayer player, final IColony colony, final IBuilding building)
    {
        IBuildingModule targetModule = building.getModule(moduleId);
        if(targetModule == null || !Objects.equals(targetModule.getProducer().key, moduleKey)) {
            targetModule = building.getModule(IAssignsCitizen.class, module -> Objects.equals(module.getProducer().key, moduleKey));
        }
        if (targetModule instanceof IAssignsCitizen module)
        {
            module.setHiringMode(mode);
        }
    }
}
