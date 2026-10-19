package com.arxyt.colonypathingedition.core.message.compatible;

import com.ldtteam.common.network.PlayMessageType;
import com.minecolonies.api.advancements.AdvancementTriggers;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.buildings.modules.IBuildingModule;
import com.minecolonies.api.colony.buildings.views.IBuildingView;
import com.minecolonies.api.colony.requestsystem.StandardFactoryController;
import com.minecolonies.api.colony.requestsystem.token.IToken;
import com.minecolonies.api.crafting.IRecipeStorage;
import com.minecolonies.api.util.MessageUtils;
import com.minecolonies.api.util.SoundUtils;
import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.core.colony.buildings.modules.AbstractCraftingBuildingModule;
import com.minecolonies.core.network.messages.server.AbstractBuildingServerMessage;
import com.minecolonies.core.util.AdvancementUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static com.minecolonies.api.util.constant.TranslationConstants.MESSAGE_RECIPE_SAVED;
import static com.minecolonies.api.util.constant.TranslationConstants.UNABLE_TO_ADD_RECIPE_MESSAGE;

public class CompatibleAddRemoveRecipeMessage extends AbstractBuildingServerMessage<IBuilding>
{
    public static final PlayMessageType<?> TYPE = PlayMessageType.forServer(Constants.MOD_ID, "compatible_add_remove_recipe", CompatibleAddRemoveRecipeMessage::new);

    /**
     * Toggle the recipe allocation to remove or add.
     */
    private final boolean remove;

    /**
     * The RecipeStorage to add/remove.
     */
    private final IRecipeStorage storage;

    /**
     * Type of the owning module.
     */
    private final int id;

    /**
     * Type of the owning module.
     */
    private final String moduleKey;

    /**
     * Create a message to add or remove recipes.
     *
     * @param building the building we're executing on.
     * @param remove   true if remove.
     * @param storage  the recipe storage.
     * @param id the unique id of the module.
     */
    public CompatibleAddRemoveRecipeMessage(final IBuildingView building, final boolean remove, final IRecipeStorage storage, final int id, final String moduleKey)
    {
        super(TYPE, building);
        this.remove = remove;
        this.storage = storage;
        this.id = id;
        this.moduleKey = moduleKey;
    }

    /**
     * Transformation from a byteStream.
     *
     * @param buf the used byteBuffer.
     */
    protected CompatibleAddRemoveRecipeMessage(final RegistryFriendlyByteBuf buf, final PlayMessageType<?> type)
    {
        super(buf, type);
        storage = StandardFactoryController.getInstance().deserialize(buf);
        remove = buf.readBoolean();
        this.id = buf.readInt();
        this.moduleKey = buf.readUtf();
    }

    /**
     * Transformation to a byteStream.
     *
     * @param buf the used byteBuffer.
     */
    @Override
    protected void toBytes(@NotNull final RegistryFriendlyByteBuf buf)
    {
        super.toBytes(buf);
        StandardFactoryController.getInstance().serialize(buf, storage);
        buf.writeBoolean(remove);
        buf.writeInt(id);
        buf.writeUtf(moduleKey);
    }

    @Override
    protected void onExecute(final IPayloadContext ctxIn, final ServerPlayer player, final IColony colony, final IBuilding building)
    {
        IBuildingModule moduleToGet = building.getModule(id);
        if (!Objects.equals(moduleToGet.getProducer().key, moduleKey))
        {
            moduleToGet = building.getModule(AbstractCraftingBuildingModule.class, module -> Objects.equals(module.getProducer().key, moduleKey));
        }

        if(!(moduleToGet instanceof AbstractCraftingBuildingModule module)) return;

        if (remove)
        {
            module.removeRecipe(storage.getToken());
            SoundUtils.playSuccessSound(player, player.blockPosition());
        }
        else
        {
            final IToken<?> token = IColonyManager.getInstance().getRecipeManager().checkOrAddRecipe(storage);
            if (!module.addRecipe(token))
            {
                SoundUtils.playErrorSound(player, player.blockPosition());
                MessageUtils.format(UNABLE_TO_ADD_RECIPE_MESSAGE, Component.translatableEscape(building.getBuildingDisplayName())).sendTo(player);
            }
            else
            {
                SoundUtils.playSuccessSound(player, player.blockPosition());
                AdvancementUtils.TriggerAdvancementPlayersForColony(colony, playerMP -> AdvancementTriggers.BUILDING_ADD_RECIPE.get().trigger(playerMP, this.storage));
                MessageUtils.format(MESSAGE_RECIPE_SAVED).sendTo(player);
            }
        }

        building.markDirty();
    }
}
