package com.arxyt.colonypathingedition.core.message.compatible;

import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.buildings.modules.IAssignsCitizen;
import com.minecolonies.api.colony.buildings.modules.IAssignsJob;
import com.minecolonies.api.colony.buildings.modules.IBuildingModule;
import com.minecolonies.api.colony.buildings.views.IBuildingView;
import com.minecolonies.core.network.messages.server.AbstractBuildingServerMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Message class which manages the messages hiring or firing of citizens.
 */
public class CompatibleHireFireMessage extends AbstractBuildingServerMessage<IBuilding>
{
    /**
     * If hiring (true) else firing.
     */
    private boolean hire;

    /**
     * The citizen to hire/fire.
     */
    private int citizenID;

    /**
     * The module id
     */
    private int moduleId;

    /**
     * The module key.
     */
    private String moduleKey;

    /**
     * Empty public constructor.
     */
    public CompatibleHireFireMessage()
    {
        super();
    }

    /**
     * Creates object for the player to hire or fire a citizen.
     *
     * @param building  view of the building to read data from
     * @param hire      hire or fire the citizens
     * @param citizenID the id of the citizen to fill the job.
     */
    public CompatibleHireFireMessage(@NotNull final IBuildingView building, final boolean hire, final int citizenID, final int moduleId, final String moduleKey)
    {
        super(building);
        this.hire = hire;
        this.citizenID = citizenID;
        this.moduleId = moduleId;
        this.moduleKey = moduleKey;
    }

    /**
     * Transformation from a byteStream to the variables.
     *
     * @param buf the used byteBuffer.
     */
    @Override
    public void fromBytesOverride(@NotNull final FriendlyByteBuf buf)
    {
        hire = buf.readBoolean();
        citizenID = buf.readInt();
        moduleId = buf.readInt();
        moduleKey = buf.readUtf();
    }

    /**
     * Transformation to a byteStream.
     *
     * @param buf the used byteBuffer.
     */
    @Override
    public void toBytesOverride(@NotNull final FriendlyByteBuf buf)
    {
        buf.writeBoolean(hire);
        buf.writeInt(citizenID);
        buf.writeInt(moduleId);
        buf.writeUtf(moduleKey);
    }

    @Override
    protected void onExecute(final NetworkEvent.Context ctxIn, final boolean isLogicalServer, final IColony colony, final IBuilding building)
    {
        IBuildingModule targetModule = building.getModule(moduleId);
        if(!Objects.equals(targetModule.getProducer().key, moduleKey)) {
            targetModule = building.getModule(IAssignsJob.class, module -> Objects.equals(module.getProducer().key, moduleKey));
        }
        if (targetModule instanceof IAssignsJob module)
        {
            final ICitizenData citizen = colony.getCitizenManager().getCivilian(citizenID);
            citizen.setPaused(false);
            if (hire)
            {
                module.assignCitizen(citizen);
            }
            else
            {
                module.removeCitizen(citizen);
            }
        }
    }
}
