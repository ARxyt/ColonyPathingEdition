package com.arxyt.colonypathingedition.core.message.compatible;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.HiringMode;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.buildings.modules.IAssignsCitizen;
import com.minecolonies.api.colony.buildings.modules.IBuildingModule;
import com.minecolonies.api.colony.buildings.views.IBuildingView;
import com.minecolonies.core.network.messages.server.AbstractBuildingServerMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class CompatibleBuildingHiringModeMessage extends AbstractBuildingServerMessage<IBuilding> {

    /**
     * The Hiring mode to set.
     */
    private HiringMode mode;

    /**
     * The job id.
     */
    private int moduleId;

    /**
     * The job key.
     */
    private String moduleKey;

    /**
     * Empty constructor used when registering the
     */
    public CompatibleBuildingHiringModeMessage()
    {
        super();
    }

    /**
     * Creates object for the hiring mode
     *
     * @param building View of the building to read data from.
     * @param mode     the hiring mode.
     */
    public CompatibleBuildingHiringModeMessage(@NotNull final IBuildingView building, final HiringMode mode, final int moduleId, final String moduleKey)
    {
        super(building);
        this.mode = mode;
        this.moduleId = moduleId;
        this.moduleKey = moduleKey;
    }

    @Override
    public void fromBytesOverride(@NotNull final FriendlyByteBuf buf)
    {
        mode = HiringMode.values()[buf.readInt()];
        moduleId = buf.readInt();
        moduleKey = buf.readUtf();
    }

    @Override
    public void toBytesOverride(@NotNull final FriendlyByteBuf buf)
    {
        buf.writeInt(mode.ordinal());
        buf.writeInt(moduleId);
        buf.writeUtf(moduleKey);
    }

    @Override
    public void onExecute(final NetworkEvent.Context ctxIn, final boolean isLogicalServer, final IColony colony, final IBuilding building)
    {
        IBuildingModule targetModule = building.getModule(moduleId);
        if(!Objects.equals(targetModule.getProducer().key, moduleKey)) {
            targetModule = building.getModule(IAssignsCitizen.class, module -> Objects.equals(module.getProducer().key, moduleKey));
        }
        if (targetModule instanceof IAssignsCitizen module)
        {
            module.setHiringMode(mode);
        }
    }
}
