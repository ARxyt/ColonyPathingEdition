package com.arxyt.colonypathingedition.core.ai.worker;

import com.arxyt.colonypathingedition.core.data.farmlandmap.SpecialSeedManager;
import com.arxyt.colonypathingedition.core.data.tag.ModTag;
import com.google.common.reflect.TypeToken;
import com.minecolonies.api.advancements.AdvancementTriggers;
import com.minecolonies.api.colony.buildingextensions.IBuildingExtension;
import com.minecolonies.api.colony.interactionhandling.ChatPriority;
import com.minecolonies.api.colony.requestsystem.requestable.StackList;
import com.minecolonies.api.entity.ai.JobStatus;
import com.minecolonies.api.entity.ai.statemachine.AITarget;
import com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.api.entity.citizen.VisibleCitizenStatus;
import com.minecolonies.api.equipment.ModEquipmentTypes;
import com.minecolonies.api.items.ModItems;
import com.minecolonies.api.util.*;
import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.api.util.constant.translation.RequestSystemTranslationConstants;
import com.minecolonies.core.blocks.BlockScarecrow;
import com.minecolonies.core.blocks.MinecoloniesCropBlock;
import com.minecolonies.core.blocks.MinecoloniesFarmland;
import com.minecolonies.core.colony.buildingextensions.FarmField;
import com.minecolonies.core.colony.buildings.modules.BuildingExtensionsModule;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingFarmer;
import com.minecolonies.core.colony.interactionhandling.StandardInteraction;
import com.minecolonies.core.colony.jobs.JobFarmer;
import com.minecolonies.core.entity.ai.workers.crafting.AbstractEntityAICrafting;
import com.minecolonies.core.items.ItemCrop;
import com.minecolonies.core.network.messages.client.CompostParticleMessage;
import com.minecolonies.core.util.AdvancementUtils;
import com.minecolonies.core.util.citizenutils.CitizenItemUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static com.arxyt.colonypathingedition.core.costants.AdditionalContants.PRECISE_FARMING;
import static com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState.*;
import static com.minecolonies.api.research.util.ResearchConstants.FARMING;
import static com.minecolonies.api.util.constant.CitizenConstants.BLOCK_BREAK_SOUND_RANGE;
import static com.minecolonies.api.util.constant.Constants.STACKSIZE;
import static com.minecolonies.api.util.constant.Constants.TICKS_SECOND;
import static com.minecolonies.api.util.constant.EquipmentLevelConstants.TOOL_LEVEL_WOOD_OR_GOLD;
import static com.minecolonies.api.util.constant.StatisticsConstants.*;
import static com.minecolonies.api.util.constant.TranslationConstants.NO_FREE_FIELDS;
import static com.minecolonies.core.colony.buildings.modules.BuildingModules.FARMER_FIELDS;
import static com.minecolonies.core.colony.buildings.modules.BuildingModules.STATS_MODULE;
import static net.minecraft.core.Direction.*;

public class NewEntityAIWorkFarmer extends AbstractEntityAICrafting<JobFarmer, BuildingFarmer> {
    /**
     * Return to chest after this amount of stacks.
     */
    private static final int MAX_BLOCKS_MINED = 64;

    /**
     * The maximum depth to search for a surface
     */
    private static final int MAX_DEPTH = 5;

    /**
     * Farming icon
     */
    private static final VisibleCitizenStatus FARMING_ICON =
            new VisibleCitizenStatus(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "textures/icons/work/farmer.png"), "com.minecolonies.gui.visiblestatus.farmer");

    /**
     * Changed after finished harvesting in order to dump the inventory.
     */
    private boolean shouldDumpInventory = false;

    /**
     * If the farmer needed recheck the field.
     */
    private boolean needRecheck = false;

    /**
     * If the special farmland block is missing.
     */
    private boolean isMissingFarmland = false;

    /**
     * If the seed is missing during farming.
     */
    private boolean isMissingSeed = false;

    /**
     * If we repeat work on a field for a long time.
     */
    private int repeatTime = 0;

    /**
     * If the farmland is normal.
     */
    private final Predicate<BlockState> normal =
            blockState -> blockState.getBlock() instanceof MinecoloniesFarmland || blockState.is(Blocks.FARMLAND);

    /**
     * Constructor for the Farmer. Defines the tasks the Farmer executes.
     *
     * @param job a farmer job to use.
     */
    public NewEntityAIWorkFarmer(@NotNull final JobFarmer job)
    {
        super(job);
        super.registerTargets(
                new AITarget<IAIState>(PREPARING, this::prepareForFarming, TICKS_SECOND),
                new AITarget<IAIState>(FARMER_HOE, this::workAtField, 5),
                new AITarget<IAIState>(FARMER_PLANT, this::workAtField, 5),
                new AITarget<IAIState>(FARMER_HARVEST, this::workAtField, 5)
        );
        worker.setCanPickUpLoot(true);
    }

    @Override
    protected IAIState decide()
    {
        IAIState state = super.decide();

        if (state == IDLE)
        {
            return PREPARING;
        }
        return state;
    }

    @Override
    public IAIState afterDump(){
        IAIState state = super.afterDump();
        if (state == IDLE) {
            return PREPARING;
        }
        return state;
    }

    /**
     * Prepares the farmer for farming. Also requests the tools, the compost (if needed) and checks if the farmer has sufficient fields.
     *
     * @return the next IAIState
     */
    @NotNull
    private IAIState prepareForFarming()
    {
        worker.getCitizenData().setJobStatus(JobStatus.IDLE);
        if (building == null || building.getBuildingLevel() < 1)
        {
            worker.getCitizenData().setJobStatus(JobStatus.STUCK);
            return PREPARING;
        }

        final BuildingExtensionsModule module = building.getModule(BuildingExtensionsModule.class);

        if(module == null){
            return IDLE;
        }

        if (module.getOwnedExtensions().size() == building.getMaxBuildingLevel())
        {
            AdvancementUtils.TriggerAdvancementPlayersForColony(building.getColony(), AdvancementTriggers.MAX_FIELDS.get()::trigger);
        }

        if (module.hasNoExtensions())
        {
            if (worker.getCitizenData() != null)
            {
                worker.getCitizenData().triggerInteraction(new StandardInteraction(Component.translatable(NO_FREE_FIELDS), ChatPriority.BLOCKING));
            }
            worker.getCitizenData().setJobStatus(JobStatus.STUCK);
            return IDLE;
        }

        final IBuildingExtension fieldToWork = module.getBuildingExtensionToWorkOn();

        final int amountOfCompostInBuilding = InventoryUtils.hasBuildingEnoughElseCount(building, this::isCompost, 1);
        final int amountOfCompostInInv = InventoryUtils.getItemCountInItemHandler(worker.getInventoryCitizen(), this::isCompost);

        if (amountOfCompostInBuilding + amountOfCompostInInv <= 0)
        {
            if (building.requestFertilizer() && !building.hasWorkerOpenRequestsOfType(worker.getCitizenData().getId(), TypeToken.of(StackList.class)))
            {
                final List<ItemStack> compostAbleItems = new ArrayList<>();
                compostAbleItems.add(new ItemStack(ModItems.compost, 1));
                compostAbleItems.add(new ItemStack(Items.BONE_MEAL, 1));
                worker.getCitizenData().createRequestAsync(new StackList(compostAbleItems, RequestSystemTranslationConstants.REQUEST_TYPE_FERTILIZER, STACKSIZE, 1));
            }
        }
        else if (amountOfCompostInInv <= 0 && amountOfCompostInBuilding > 0)
        {
            int requestSize = 64;
            if (fieldToWork instanceof FarmField farmField) {
                int a = farmField.getRadius(NORTH) + farmField.getRadius(SOUTH) + 1;
                int b = farmField.getRadius(EAST) + farmField.getRadius(WEST) + 1;
                requestSize = a * b - 1;
            }
            needsCurrently = new Tuple<>(this::isCompost, requestSize);
            return GATHERING_REQUIRED_MATERIALS;
        }

        if (fieldToWork instanceof FarmField farmField)
        {
            if(isMissingFarmland && SpecialSeedManager.isSpecialSeed(farmField.getSeed().getItem())){
                final Block farmland = SpecialSeedManager.getRequiredSoil(farmField.getSeed().getItem());
                Item itemFarmland = Item.BY_BLOCK.get(farmland);
                final int amountOfFarmlandInBuilding = InventoryUtils.hasBuildingEnoughElseCount(building, itemStack -> isItemOfFarmland(itemStack,farmland), 0);
                final int amountOfFarmlandInInv = InventoryUtils.getItemCountInItemHandler(worker.getInventoryCitizen(), itemStack -> isItemOfFarmland(itemStack,farmland));
                if (amountOfFarmlandInInv + amountOfFarmlandInBuilding <= 8)
                {
                    ItemStack stack = new ItemStack(itemFarmland);
                    stack.setCount(stack.getMaxStackSize());
                    checkIfRequestForItemExistOrCreateAsync(stack, stack.getMaxStackSize(), 1);
                }
                else if (amountOfFarmlandInInv <= 0 && amountOfFarmlandInBuilding > 0)
                {
                    needsCurrently = new Tuple<>(itemStack -> isItemOfFarmland(itemStack,farmland), STACKSIZE);
                    isMissingFarmland = false;
                    return GATHERING_REQUIRED_MATERIALS;
                }
            }
            else{
                isMissingFarmland = false;
            }

            if (checkForToolOrWeapon(ModEquipmentTypes.hoe.get()))
            {
                worker.getCitizenData().setJobStatus(JobStatus.STUCK);
                return PREPARING;
            }
            ItemStack seeds = farmField.getSeed();
            final int count = worker.getCitizenInventoryHandler().getItemCountInInventory(seeds.getItem());
            if (count < seeds.getMaxStackSize() / 2)
            {
                seeds.setCount(seeds.getMaxStackSize());
                checkIfRequestForItemExistOrCreateAsync(seeds, seeds.getMaxStackSize(), 1);
            }
            worker.getCitizenData().setVisibleStatus(FARMING_ICON);
            worker.getCitizenData().setJobStatus(JobStatus.WORKING);
            IAIState state = checkNextWorkspaceAndState(farmField,
                    pos -> this.newFindHarvestableSurface(pos, farmField) != null,
                    pos -> this.newFindHoeableSurface(pos, farmField) != null,
                    pos -> this.newFindPlantableSurface(pos,farmField) != null);
            if(state != null){
                farmField.setFieldStage(FarmField.Stage.PLANTED);
                setDelay(5);
                return state;
            }
            farmField.setFieldStage(FarmField.Stage.EMPTY);
            return changeField(false, module, farmField);
        }
        else if (fieldToWork != null)
        {
            Log.getLogger().warn("Farmer found non-FarmField extension: {}", fieldToWork.getClass());
        }
        return IDLE;
    }

    /**
     * This (re)initializes a field. Checks the block above to see if it is a plant, if so, breaks it. Then tills.
     *
     * @return the next state to go into.
     */
    private IAIState workAtField()
    {
        final BuildingExtensionsModule module = building.getModule(BuildingExtensionsModule.class);
        if(module == null) {
            return IDLE;
        }
        final IBuildingExtension field = module.getCurrentExtension();

        worker.getCitizenData().setVisibleStatus(FARMING_ICON);
        if (field instanceof FarmField farmField)
        {
            if (farmField.getSeed().isEmpty()){
                return PREPARING;
            }
            if (building.getWorkingOffset() != null)
            {
                final BlockPos position = farmField.getPosition().below().south(building.getWorkingOffset().getZ()).east(building.getWorkingOffset().getX());

                // Still moving to the block
                if (!walkToSafePos(position.above()))
                {
                    return getState();
                }

                switch ((AIWorkerState) getState())
                {
                    case FARMER_HARVEST :
                    {
                        BlockPos pos = newFindHarvestableSurface(position, farmField);
                        if (pos != null) {
                            equipHoe();
                            if (newHarvestIfAbleWithRightClick(pos, farmField)) {
                                setDelay(getDelayAfterHarvest());
                                return FARMER_HOE;
                            }
                            if (newHarvestIfAble(position, farmField)) {
                                setDelay(getDelayAfterHarvest());
                                return FARMER_HOE;
                            } else {
                                return getState();
                            }
                        }
                    }
                    case FARMER_HOE :
                    {
                        BlockPos pos = newFindHoeableSurface(position,farmField);

                        if(pos != null){
                            // Minecraft.getInstance().gui.getChat().addMessage(Component.literal("[Debug] Place Is Hoeable"));
                            if (newHoeIfAble(pos, farmField))
                            {
                                return FARMER_PLANT;
                            }
                            else if(!isMissingFarmland){
                                return getState();

                            }
                            else{
                                farmField.setFieldStage(FarmField.Stage.HOED);
                                return PREPARING;
                            }
                        }
                    }
                    case FARMER_PLANT :
                    {
                        BlockPos pos = newFindPlantableSurface(position,farmField);
                        if( pos != null ) {
                            // Minecraft.getInstance().gui.getChat().addMessage(Component.literal("[Debug] Place Is Plantable"));
                            if(!newPlantCrop(farmField.getSeed(), pos)){
                                farmField.setFieldStage(FarmField.Stage.HOED);
                                return PREPARING;
                            }
                        }
                        else {
                            if(isMissingSeed) {
                                final int amountOfFarmlandInBuilding = InventoryUtils.hasBuildingEnoughElseCount(building, itemStack -> itemStack.getItem() == farmField.getSeed().getItem(), 0);
                                if (amountOfFarmlandInBuilding > 0)
                                {
                                    needsCurrently = new Tuple<>(itemStack -> itemStack.getItem() == farmField.getSeed().getItem(), STACKSIZE);
                                    isMissingSeed = false;
                                    needRecheck = false;
                                    return GATHERING_REQUIRED_MATERIALS;
                                }
                                else {
                                    ItemStack item = farmField.getSeed();
                                    item.setCount(item.getMaxStackSize());
                                    checkIfRequestForItemExistOrCreateAsync(item, item.getMaxStackSize(), 1);
                                }
                            }
                        }
                        break;
                    }
                    default :
                    {
                        farmField.setFieldStage(FarmField.Stage.HOED);
                        return PREPARING;
                    }
                }
                building.setPrevPos(position);
            }

            IAIState state = checkNextWorkspaceAndState(farmField,
                    pos -> this.newFindHarvestableSurface(pos, farmField) != null,
                    pos -> this.newFindHoeableSurface(pos, farmField) != null,
                    pos -> this.newFindPlantableSurface(pos,farmField) != null);
            if(state != null){
                return state;

            }
            if (building.getWorkingOffset() == null)
            {
                farmField.setFieldStage(FarmField.Stage.EMPTY);
                return changeField(true, module, farmField);
            }
        }
        else
        {
            return IDLE;
        }
        farmField.setFieldStage(FarmField.Stage.HOED);
        return PREPARING;
    }

    @NotNull
    private IAIState changeField(boolean needDump, BuildingExtensionsModule module, FarmField farmField) {
        if(needDump) {
            shouldDumpInventory = true;
        }
        module.markDirty();
        if (!needRecheck || ++repeatTime > 3)
        {
            repeatTime = 0;
            module.resetCurrentExtension();
        }
        else if(isMissingSeed) {
            final int amountOfFarmlandInBuilding = InventoryUtils.hasBuildingEnoughElseCount(building, itemStack -> itemStack.getItem() == farmField.getSeed().getItem(), 0);
            if (amountOfFarmlandInBuilding > 0)
            {
                needsCurrently = new Tuple<>(itemStack -> itemStack.getItem() == farmField.getSeed().getItem(), STACKSIZE);
                isMissingSeed = false;
                needRecheck = false;
                return GATHERING_REQUIRED_MATERIALS;
            }
            else {
                ItemStack item = farmField.getSeed();
                item.setCount(item.getMaxStackSize());
                checkIfRequestForItemExistOrCreateAsync(item, item.getMaxStackSize(), 1);
            }
        }
        needRecheck = false;
        building.setPrevPos(null);
        return PREPARING;
    }

    // Basic Action and Listener

    /**
     * Pretest of farmland to work on and the work state.
     *
     * @param farmField the field we work on now.
     * @param harvestAble is the farmland harvest able.
     * @param hoeAble is the farmland hoe able.
     * @param plantAble is the farmland plant able.
     * @return the work state we get on this farmland.
     */
    private IAIState checkNextWorkspaceAndState(@NotNull final FarmField farmField, @NotNull final Predicate<BlockPos> harvestAble, @NotNull final Predicate<BlockPos> hoeAble, @NotNull final Predicate<BlockPos> plantAble)
    {
        BlockPos position;
        if(building.getWorkingOffset() == null){
            building.setWorkingOffset(nextValidCell(farmField));
        }
        while(building.getWorkingOffset() != null)
        {
            position = farmField.getPosition().below().south(building.getWorkingOffset().getZ()).east(building.getWorkingOffset().getX());
            if(harvestAble.test(position)){
                return FARMER_HARVEST;
            }
            if(hoeAble.test(position)){
                return FARMER_HOE;
            }
            if(plantAble.test(position)){
                return FARMER_PLANT;
            }
            building.setWorkingOffset(nextValidCell(farmField));
        }
        return null;
    }

    /**
     * Finds the position of the surface near the specified position
     *
     * @param position the location to begin the search
     * @return the position of the surface block or null if it can't be found
     */
    private BlockPos getSurfacePos(final BlockPos position)
    {
        return getSurfacePos(position, 0);
    }

    /**
     * Finds the position of the surface near the specified position
     *
     * @param position the location to begin the search
     * @param depth    the depth of the search for the surface
     * @return the position of the surface block or null if it can't be found
     */
    private BlockPos getSurfacePos(final BlockPos position, final Integer depth)
    {
        if (Math.abs(depth) > MAX_DEPTH || !WorldUtil.isBlockLoaded(world, position))
        {
            return null;
        }
        final BlockState curBlockState = world.getBlockState(position);
        @Nullable final Block curBlock = curBlockState.getBlock();
        if ((curBlockState.isSolid() && !(curBlock instanceof PumpkinBlock) && curBlock != Blocks.MELON  && !(curBlock instanceof WebBlock)) || curBlockState.liquid())
        {
            if (depth < 0)
            {
                return position;
            }
            return getSurfacePos(position.above(), depth + 1);
        }
        else
        {
            if (depth > 0)
            {
                return position.below();
            }
            return getSurfacePos(position.below(), depth - 1);
        }
    }

    /**
     * Checks if the crop should be harvested.
     *
     * @param position the position to check.
     * @return position of harvestable block or null
     */
    private BlockPos newFindHarvestableSurface(@NotNull BlockPos position, FarmField farmField)
    {
        position = getSurfacePos(position);
        if (position == null)
        {
            return null;
        }
        BlockState surfaceState = world.getBlockState(position.above());
        Block surfaceBlock = surfaceState.getBlock();

        if (surfaceBlock == Blocks.PUMPKIN || surfaceBlock == Blocks.MELON)
        {
            return position;
        }

        if (surfaceBlock instanceof @NotNull CropBlock crop)
        {
            if (crop.isMaxAge(surfaceState))
            {
                return position;
            }
            final int amountOfCompostInInv = InventoryUtils.getItemCountInItemHandler(worker.getInventoryCitizen(), this::isCompost);
            if (amountOfCompostInInv == 0)
            {
                return null;
            }

            if (InventoryUtils.shrinkItemCountInItemHandler(worker.getInventoryCitizen(), this::isCompost))
            {
                new CompostParticleMessage(position.above())
                        .sendToTargetPoint((ServerLevel) world, null, position.getX(), position.getY(), position.getZ(), BLOCK_BREAK_SOUND_RANGE);
                crop.growCrops(world, position.above(), surfaceState);
                surfaceState = world.getBlockState(position.above());
                surfaceBlock = surfaceState.getBlock();
                if (surfaceBlock instanceof CropBlock)
                {
                    crop = (CropBlock) surfaceBlock;
                }
                else
                {
                    return null;
                }
            }
            return crop.isMaxAge(surfaceState) ? position : null;
        }
        else if (surfaceBlock instanceof MinecoloniesCropBlock minecoloniesCrop)
        {
            if (minecoloniesCrop.isMaxAge(surfaceState))
            {
                return position;
            }
            final int amountOfCompostInInv = InventoryUtils.getItemCountInItemHandler(worker.getInventoryCitizen(), this::isCompost);
            if (amountOfCompostInInv == 0)
            {
                return null;
            }

            if (InventoryUtils.shrinkItemCountInItemHandler(worker.getInventoryCitizen(), this::isCompost))
            {
                new CompostParticleMessage(position.above())
                        .sendToTargetPoint((ServerLevel) world, null, position.getX(), position.getY(), position.getZ(), BLOCK_BREAK_SOUND_RANGE);
                minecoloniesCrop.attemptGrow(surfaceState, (ServerLevel) world, position.above());
                surfaceState = world.getBlockState(position.above());
                surfaceBlock = surfaceState.getBlock();
                if (surfaceBlock instanceof MinecoloniesCropBlock)
                {
                    minecoloniesCrop = (MinecoloniesCropBlock) surfaceBlock;
                }
                else
                {
                    return null;
                }
            }
            return minecoloniesCrop.isMaxAge(surfaceState) ? position : null;
        }
        if (surfaceBlock instanceof BushBlock){
            if (surfaceBlock instanceof FungusBlock || surfaceBlock instanceof MushroomBlock){
                return null;
            }
            if (surfaceBlock instanceof FlowerBlock || surfaceBlock instanceof TallGrassBlock || surfaceBlock instanceof SaplingBlock){
                return position;
            }
            if (surfaceBlock instanceof StemBlock stemBlock){
                if(!(farmField.getSeed().getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof StemBlock stemBlock2) ){
                    return position;
                }
                if(stemBlock == stemBlock2 && (building.getWorkingOffset() != null && (building.getWorkingOffset().getX() + building.getWorkingOffset().getZ()) % 2 != 0)){
                    if (isBoneMealAble(position.above(),stemBlock)) {
                        final int amountOfCompostInInv = InventoryUtils.getItemCountInItemHandler(worker.getInventoryCitizen(), this::isCompost);
                        if (amountOfCompostInInv != 0 && InventoryUtils.shrinkItemCountInItemHandler(worker.getInventoryCitizen(), this::isCompost))
                        {
                            new CompostParticleMessage(position.above())
                                    .sendToTargetPoint((ServerLevel) world, null, position.getX(), position.getY(), position.getZ(), BLOCK_BREAK_SOUND_RANGE);
                            stemBlock.performBonemeal((ServerLevel) world, world.getRandom(), position.above(), surfaceState);
                        }
                    }
                    return null;
                }
                return position;
            }
            if (surfaceBlock instanceof BonemealableBlock bonemealable) {
                if (!isBoneMealAble(position.above(),bonemealable)) {
                    return position;
                }
                final int amountOfCompostInInv = InventoryUtils.getItemCountInItemHandler(worker.getInventoryCitizen(), this::isCompost);
                if (amountOfCompostInInv == 0)
                {
                    return null;
                }

                if (InventoryUtils.shrinkItemCountInItemHandler(worker.getInventoryCitizen(), this::isCompost))
                {
                    new CompostParticleMessage(position.above())
                            .sendToTargetPoint((ServerLevel) world, null, position.getX(), position.getY(), position.getZ(), BLOCK_BREAK_SOUND_RANGE);
                    bonemealable.performBonemeal((ServerLevel) world, world.getRandom(), position.above(), surfaceState);
                    surfaceState = world.getBlockState(position.above());
                    surfaceBlock = surfaceState.getBlock();
                    if (!(surfaceBlock instanceof BushBlock && surfaceBlock instanceof BonemealableBlock))
                    {
                        return null;
                    }
                    bonemealable = (BonemealableBlock) surfaceBlock;
                    return isBoneMealAble(position.above(),bonemealable) ? null : position;
                }
            }
        }
        return null;
    }


    /**
     * Checks if we can harvest by right click, and does so if we can.
     *
     * @param pos the block to harvest.
     * @param farmField is the farm field we are havesting.
     * @return true if we harvested by right click.
     */
    private boolean newHarvestIfAbleWithRightClick(BlockPos pos, FarmField farmField){
        // Right click.
        BlockState state = world.getBlockState(pos.above());
        if(state.getBlock() instanceof BushBlock || state.getBlock() instanceof CropBlock){
            BlockHitResult hitResult = new BlockHitResult(Vec3.atCenterOf(pos.above()), Direction.UP, pos.above(), false);
            FakePlayer fakePlayer = getFakePlayer();
            ItemStack seedStack = state.getCloneItemStack(hitResult, world, pos.above(),fakePlayer);
            if(farmField.getSeed().getItem() == seedStack.getItem()){
                // equip tools on fakePlayer.
                ItemStack mainHandStack = worker.getMainHandItem();
                ItemStack stackCopy = mainHandStack.copy();
                fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, stackCopy);
                // right click event.
                ItemInteractionResult result = state.useItemOn(stackCopy, world, fakePlayer, InteractionHand.MAIN_HAND, hitResult);
                if (result.consumesAction()) {
                    // additional drop manager.
                    final ItemStack tool = worker.getMainHandItem();
                    final int fortune = ItemStackUtils.getFortuneOf(tool, world);
                    List<ItemStack> localItems = new ArrayList<>(state.getDrops(new LootParams.Builder((ServerLevel) world)
                            .withLuck(fortune)
                            .withOptionalParameter(LootContextParams.BLOCK_ENTITY, world.getBlockEntity(pos.above()))
                            .withParameter(LootContextParams.ORIGIN, worker.position())
                            .withParameter(LootContextParams.TOOL, tool)));
                    List<ItemStack> extendedLocalItems = increaseBlockDropsSpecial(localItems);
                    //add the drops to the citizen
                    for (final ItemStack item : extendedLocalItems)
                    {
                        InventoryUtils.transferItemStackIntoNextBestSlotInItemHandler(item, worker.getInventoryCitizen());
                    }
                    return true;
                }
            }
        }
        // Return to Break crop
        return false;
    }

    /**
     * Checks if we can harvest, and does so if we can.
     *
     * @param position the block to harvest.
     * @return true if we harvested or not supposed to.
     */
    private boolean newHarvestIfAble(BlockPos position, FarmField farmField)
    {
        position = newFindHarvestableSurface(position, farmField);
        if (position != null)
        {
            if (mineBlock(position.above()))
            {
                assert worker.getCitizenColonyHandler().getColonyOrRegister() != null;
                worker.getCitizenColonyHandler().getColonyOrRegister().getStatisticsManager().increment(CROPS_HARVESTED, worker.getCitizenColonyHandler().getColonyOrRegister().getDay());
                worker.getCitizenExperienceHandler().addExperience(0.5);
            }
            else
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if the ground should be hoed and the block above removed.
     *
     * @param position  the position to check.
     * @param farmField the field close to this position.
     * @return position of hoeable surface or null if there is not one
     */
    private BlockPos newFindHoeableSurface(@NotNull BlockPos position, @NotNull final FarmField farmField)
    {
        if (checkForToolOrWeapon(ModEquipmentTypes.hoe.get()) || isMissingFarmland)
        {
            return null;
        }
        position = getSurfacePos(position);
        if (position == null)
        {
            return null;
        }
        ItemStack seed = farmField.getSeed();
        if(farmField.getSeed().getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof StemBlock){
            if (building.getWorkingOffset() != null && (building.getWorkingOffset().getX() + building.getWorkingOffset().getZ()) % 2 == 0){
                return null;
            }
        }
        BlockState blockState = world.getBlockState(position);
        boolean shouldGenerateWater = false;
        if (isUnderWater(seed)) {
            shouldGenerateWater = !(blockState.is(Blocks.WATER)
                    || (world.getBlockState(position.above()).getBlock() instanceof CropBlock)
                    || (world.getBlockState(position.above()).getBlock() instanceof BushBlock)
                    || (world.getBlockState(position.above()).getBlock() instanceof MinecoloniesCropBlock));
            if(shouldGenerateWater || blockState.is(Blocks.WATER)) {
                position = position.below();
                blockState = world.getBlockState(position);
            }
        }
        if(SpecialSeedManager.isSpecialSeed(seed.getItem())){
            final Block farmland = SpecialSeedManager.getRequiredSoil(seed.getItem());
            if (farmField.isNoPartOfField(world, position)
                    || (world.getBlockState(position.above()).getBlock() instanceof CropBlock)
                    || (world.getBlockState(position.above()).getBlock() instanceof BushBlock)
                    || (world.getBlockState(position.above()).getBlock() instanceof BlockScarecrow)
                    || (blockState.getBlock() == farmland)
                    || (!(blockState.is(BlockTags.DIRT) || blockState.is(Blocks.WATER) || SpecialSeedManager.isSpecialSoil(blockState.getBlock())) && !(blockState.getBlock() instanceof MinecoloniesFarmland) && !(blockState.getBlock() instanceof FarmBlock))
                    || (world.getBlockState(position.above()).getBlock() instanceof MinecoloniesCropBlock)
            )
            {
                return null;
            }
            final BlockState aboveState = world.getBlockState(position.above());
            if (aboveState.canBeReplaced() && !(aboveState.getBlock() instanceof MinecoloniesCropBlock))
            {
                world.destroyBlock(position.above(), true);
            }
            return position;
        }
        if (isNoFarmland(seed)) {
            if (farmField.isNoPartOfField(world, position)
                    || (world.getBlockState(position.above()).getBlock() instanceof CropBlock)
                    || (world.getBlockState(position.above()).getBlock() instanceof BushBlock)
                    || (world.getBlockState(position.above()).getBlock() instanceof BlockScarecrow)
                    || (!(blockState.is(BlockTags.DIRT) || blockState.is(Blocks.WATER) || SpecialSeedManager.isSpecialSoil(blockState.getBlock())) && !(blockState.getBlock() instanceof MinecoloniesFarmland) && !(blockState.getBlock() instanceof FarmBlock))
                    || (world.getBlockState(position.above()).getBlock() instanceof MinecoloniesCropBlock)
            )
            {
                return null;
            }
            final BlockState aboveState = world.getBlockState(position.above());
            if (aboveState.canBeReplaced() && !(aboveState.getBlock() instanceof MinecoloniesCropBlock))
            {
                world.destroyBlock(position.above(), true);
            }
            if (!blockState.is(BlockTags.DIRT))
            {
                return position;
            }
            return shouldGenerateWater ? position : null;
        }
        if (farmField.isNoPartOfField(world, position)
                || (world.getBlockState(position.above()).getBlock() instanceof CropBlock)
                || (world.getBlockState(position.above()).getBlock() instanceof BushBlock)
                || (world.getBlockState(position.above()).getBlock() instanceof BlockScarecrow)
                || (!(blockState.is(BlockTags.DIRT) || blockState.is(Blocks.WATER) || SpecialSeedManager.isSpecialSoil(blockState.getBlock())) && !(blockState.getBlock() instanceof MinecoloniesFarmland) && !(blockState.getBlock() instanceof FarmBlock))
                || (world.getBlockState(position.above()).getBlock() instanceof MinecoloniesCropBlock)
        )
        {
            return null;
        }

        if(isRightFarmLandForCrop(farmField, blockState)) {
            return shouldGenerateWater ? position : null;
        }

        final BlockState aboveState = world.getBlockState(position.above());
        if (aboveState.canBeReplaced() && !(aboveState.getBlock() instanceof MinecoloniesCropBlock || aboveState.is(Blocks.WATER)))
        {
            world.destroyBlock(position.above(), true);
        }

        if (!isRightFarmLandForCrop(farmField, blockState))
        {
            return position;
        }

        final BlockHitResult blockHitResult = new BlockHitResult(Vec3.ZERO, Direction.UP, position, false);
        final UseOnContext useOnContext = new UseOnContext(world,
                null,
                InteractionHand.MAIN_HAND,
                getInventory().getStackInSlot(InventoryUtils.getFirstSlotOfItemHandlerContainingEquipment(getInventory(), ModEquipmentTypes.hoe.get(), TOOL_LEVEL_WOOD_OR_GOLD, building.getMaxEquipmentLevel())),
                blockHitResult);
        final BlockState toolModifiedState = blockState.getToolModifiedState(useOnContext, ItemAbilities.HOE_TILL, true);
        if (toolModifiedState == null || !(toolModifiedState.getBlock() instanceof FarmBlock))
        {
            return null;
        }

        return position;
    }

    /**
     * Checks if we can hoe, and does so if we can.
     *
     * @param position  the position to check.
     * @param farmField the field close to this position.
     * @return true if the farmer should move on.
     */
    private boolean newHoeIfAble(BlockPos position, final FarmField farmField)
    {
        if (!checkForToolOrWeapon(ModEquipmentTypes.hoe.get()))
        {
            if (world.getBlockState(position.above()).is(Blocks.WATER) || mineBlock(position.above()))
            {
                final ItemStack seed = farmField.getSeed();
                equipHoe();
                worker.swing(worker.getUsedItemHand());
                if(SpecialSeedManager.isSpecialSeed(seed.getItem())){
                    final Block farmland = SpecialSeedManager.getRequiredSoil(seed.getItem());
                    final int amountOfFarmlandInInv = InventoryUtils.getItemCountInItemHandler(worker.getInventoryCitizen(), itemStack -> isItemOfFarmland(itemStack,farmland));
                    if (amountOfFarmlandInInv == 0 || !InventoryUtils.shrinkItemCountInItemHandler(worker.getInventoryCitizen(), itemStack -> isItemOfFarmland(itemStack,farmland)))
                    {
                        isMissingFarmland = true;
                        return false;
                    }
                }
                if(!newCreateCorrectFarmlandForSeed(seed, position)){
                    return false;
                }
                CitizenItemUtils.damageItemInHand(worker, InteractionHand.MAIN_HAND, 1);
                worker.decreaseSaturationForContinuousAction();
                assert worker.getCitizenColonyHandler().getColonyOrRegister() != null;
                worker.getCitizenColonyHandler().getColonyOrRegister().getStatisticsManager().increment(LAND_TILLED, worker.getCitizenColonyHandler().getColonyOrRegister().getDay());
                return true;
            }
            return false;
        }
        return true;
    }

    /**
     * Create the correct farmland for a given seed.
     * @param seed the crop.
     * @param pos the position.
     */
    private boolean newCreateCorrectFarmlandForSeed(final ItemStack seed, BlockPos pos) {
        if (isUnderWater(seed)){
            final BlockState blockState = world.getBlockState(pos.above());
            if(!blockState.is(Blocks.WATER)){
                world.setBlock(pos.above(), Blocks.WATER.defaultBlockState(), 3);
                return false;
            }
        }
        else{
            final BlockState blockState = world.getBlockState(pos);
            if(blockState.is(Blocks.WATER)){
                if(worker.getCitizenData().getEntity().isPresent() && worker.getBlockY() <= pos.getY()) {
                    Vec3 vec = worker.blockPosition().above().getCenter();
                    worker.getCitizenData().getEntity().get().teleportTo(vec.x, pos.getY() + 1.5, vec.z);
                }
                if(SpecialSeedManager.isSpecialSeed(seed.getItem())){
                    world.setBlock(pos, SpecialSeedManager.getRequiredSoil(seed.getItem()).defaultBlockState(), 3);
                    return true;
                }
                else {
                    world.setBlock(pos, Blocks.DIRT.defaultBlockState(), 3);
                }
            }
        }
        final BlockState blockState = world.getBlockState(pos);
        if(SpecialSeedManager.isSpecialSeed(seed.getItem())){
            if(worker.getCitizenData().getEntity().isPresent() && worker.getBlockY() <= pos.getY()) {
                Vec3 vec = worker.blockPosition().above().getCenter();
                worker.getCitizenData().getEntity().get().teleportTo(vec.x, pos.getY() + 1.5, vec.z);
            }
            checkFarmlandAndReturnSoli(blockState, state -> !state.getCollisionShape(world, pos).isEmpty()
                    && !normal.test(state)
                    && state.getBlock() != SpecialSeedManager.getRequiredSoil(seed.getItem())
            );
            world.setBlock(pos, SpecialSeedManager.getRequiredSoil(seed.getItem()).defaultBlockState(), 3);
            return true;
        }
        if (isNoFarmland(seed)) {
            if(!blockState.is(BlockTags.DIRT)){
                if(worker.getCitizenData().getEntity().isPresent() && worker.getBlockY() <= pos.getY()) {
                    Vec3 vec = worker.blockPosition().above().getCenter();
                    worker.getCitizenData().getEntity().get().teleportTo(vec.x, pos.getY() + 1.5, vec.z);
                }
                checkFarmlandAndReturnSoli(blockState, state -> !state.getCollisionShape(world, pos).isEmpty()
                        && !normal.test(state)
                        && !state.is(BlockTags.DIRT));
                world.setBlock(pos, Blocks.DIRT.defaultBlockState(), 3);
            }
            return true;
        }
        if (seed.getItem() instanceof ItemCrop itemCrop)
        {
            checkFarmlandAndReturnSoli(blockState, state -> !state.getCollisionShape(world, pos).isEmpty() && !normal.test(state));
            world.setBlockAndUpdate(pos, ((MinecoloniesCropBlock) itemCrop.getBlock()).getPreferredFarmland().defaultBlockState());
        }
        else
        {
            final UseOnContext useOnContext = new UseOnContext(world, null, InteractionHand.MAIN_HAND,
                    getInventory().getStackInSlot(InventoryUtils.getFirstSlotOfItemHandlerContainingEquipment(getInventory(), ModEquipmentTypes.hoe.get(), TOOL_LEVEL_WOOD_OR_GOLD, building.getMaxEquipmentLevel())),
                    new BlockHitResult(pos.getCenter(), Direction.UP, pos, false));
            final BlockState toolModifiedState = blockState.getToolModifiedState(useOnContext, ItemAbilities.HOE_TILL, true);
            if(toolModifiedState != null && toolModifiedState.getBlock() instanceof FarmBlock){
                world.setBlockAndUpdate(pos, toolModifiedState);
                return true;
            }
            checkFarmlandAndReturnSoli(blockState, state -> !state.getCollisionShape(world, pos).isEmpty() && !normal.test(state));
            world.setBlockAndUpdate(pos, Blocks.FARMLAND.defaultBlockState());
        }
        return true;
    }

    /**
     * Check if this farmland block is needed to return back, if so, return the previous block.
     * @param farmlandState the state we're testing this on.
     * @param condition condition we say the farmland is not normal.
     */
    private void checkFarmlandAndReturnSoli(BlockState farmlandState, Predicate<BlockState> condition){
        if(condition.test(farmlandState)){
            ItemStack stack = new ItemStack(farmlandState.getBlock(),1);
            InventoryUtils.addItemStackToItemHandler(worker.getItemHandlerCitizen(),stack);
        }
    }

    /**
     * Check if this is the right farm land for the specific crop.
     * @param farmField the field we're testing this for.
     * @param blockState the state we're testing this on.
     * @return true if so.
     */
    private boolean isRightFarmLandForCrop(final FarmField farmField, final BlockState blockState)
    {
        if (farmField.getSeed().getItem() instanceof ItemCrop itemCrop)
        {
            return blockState.getBlock() == ((MinecoloniesCropBlock) itemCrop.getBlock()).getPreferredFarmland();
        }
        else
        {
            return blockState.getBlock() instanceof FarmBlock;
        }
    }


    @Override
    public void onBlockDropReception(final List<ItemStack> blockDrops)
    {
        super.onBlockDropReception(blockDrops);
        for (final ItemStack stack : blockDrops)
        {
            building.getModule(STATS_MODULE).incrementBy(ITEM_OBTAINED + ";" + stack.getItem().getDescriptionId(), stack.getCount());
        }
    }

    /**
     * Checks if the ground should be planted.
     *
     * @param position  the position to check.
     * @param farmField the field close to this position.
     * @return position of plantable surface or null
     */
    private BlockPos newFindPlantableSurface(@NotNull BlockPos position, @NotNull final FarmField farmField)
    {
        final ItemStack seed = farmField.getSeed();
        if (seed.isEmpty())
        {
            return null;
        }
        else if(farmField.getSeed().getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof StemBlock){
            if (building.getWorkingOffset() != null && (building.getWorkingOffset().getX() + building.getWorkingOffset().getZ()) % 2 == 0){
                return null;
            }
        }
        final int slot = worker.getCitizenInventoryHandler().findFirstSlotInInventoryWith(seed.getItem());
        if (slot == -1)
        {
            isMissingSeed = true;
            needRecheck = true;
            return null;
        }
        position = getSurfacePos(position);
        if (position == null || farmField.isNoPartOfField(world, position))
        {
            return null;
        }
        BlockState belowState = world.getBlockState(position);
        if(isUnderWater(seed)){
            if(belowState.is(Blocks.WATER)) {
                position = position.below();
                belowState = world.getBlockState(position);
            }
            else{
                return null;
            }
        }
        BlockState state = world.getBlockState(position.above());
        if(state.getBlock() instanceof CropBlock
                || state.getBlock() instanceof FungusBlock
                || state.getBlock() instanceof BushBlock
                || state.getBlock() instanceof StemBlock
                || belowState.getBlock() instanceof BlockScarecrow
                || state.getBlock() instanceof MinecoloniesCropBlock)
        {
            return null;
        }
        if(SpecialSeedManager.isSpecialSeed(seed.getItem())){
            if(!(SpecialSeedManager.getRequiredSoil(seed.getItem()) == belowState.getBlock())){
                return null;
            }
            return position;
        }
        if(isNoFarmland(seed) ){
            if(!belowState.is(BlockTags.DIRT)){
                return null;
            }
        }
        else if(!isRightFarmLandForCrop(farmField, world.getBlockState(position))){
            return null;
        }
        return position;
    }

    /**
     * Plants the crop at a given location.
     *
     * @param item     the crop.
     * @param position the location.
     * @return true if successful.
     */
    private boolean newPlantCrop(final ItemStack item, @NotNull final BlockPos position)
    {
        if (item == null || item.isEmpty())
        {
            return false;
        }
        final int slot = worker.getCitizenInventoryHandler().findFirstSlotInInventoryWith(item.getItem());
        if (slot == -1)
        {
            item.setCount(item.getMaxStackSize());
            checkIfRequestForItemExistOrCreateAsync(item, item.getMaxStackSize(), 1);
            needRecheck = true;
            isMissingSeed = true;
            return false;
        }

        if (item.getItem() instanceof BlockItem blockItem
                && (blockItem.getBlock() instanceof CropBlock || blockItem.getBlock() instanceof StemBlock || blockItem.getBlock() instanceof MinecoloniesCropBlock || blockItem.getBlock() instanceof BushBlock)
                && (!isUnderWater(item) || world.getBlockState(position.above()).is(Blocks.WATER)))
        {
            @NotNull final Item seed = item.getItem();
            InteractionResult placeResult = blockItem.place(
                    new BlockPlaceContext(FakePlayerFactory.getMinecraft((ServerLevel) world),
                            InteractionHand.MAIN_HAND, new ItemStack(seed),
                            new BlockHitResult(position.getCenter(),Direction.UP,position,false))
            );
            if(!placeResult.consumesAction()){
                world.setBlockAndUpdate(position.above(), ((BlockItem) seed).getBlock().defaultBlockState());
            }
            worker.decreaseSaturationForContinuousAction();
            getInventory().extractItem(slot, 1, false);
        }
        return true;
    }

    /**
     * Sets the hoe as held item.
     */
    private void equipHoe()
    {
        CitizenItemUtils.setHeldItem(worker, InteractionHand.MAIN_HAND, getHoeSlot());
    }

    // All the util function;

    /**
     * Methods to clarify special seed by Tags
     */
    private static boolean isUnderWater(@NotNull ItemStack stack) {
        return stack.is(ModTag.SEEDS_UNDERWATER);
    }
    private static boolean isNoFarmland(@NotNull ItemStack stack) {
        return stack.is(ModTag.SEEDS_NOFARMLAND);
    }

    /**
     * Bonemeal target check with fewer param
     */
    private boolean isBoneMealAble(BlockPos position,BonemealableBlock bonemealable) {
        BlockState state = world.getBlockState(position);
        return bonemealable.isValidBonemealTarget(
                world,
                position,
                state
        );
    }

    /**
     * Check the farmland is the right special block.
     */
    private boolean isItemOfFarmland(final ItemStack itemStack, final Block Farmland){
        if(itemStack.getItem() instanceof BlockItem blockItem){
            return blockItem.getBlock() == Farmland;
        }
        return false;
    }

    /**
     * Deal with new research
     */
    private int getDelayAfterHarvest() {
        double additional = 50 * (worker.getCitizenColonyHandler().getColonyOrRegister().getResearchManager().getResearchEffects().getEffectStrength(PRECISE_FARMING));
        return 5 + (int) (additional / (1 + (Math.pow(getPrimarySkillLevel(), 1.5) / 100)));
    }

    /**
     * Fetch the next available block within the field. Uses mathematical quadratic equations to determine the coordinates by an index. Considers max radii set in the field gui.
     *
     * @return the new offset position
     */
    protected BlockPos nextValidCell(FarmField farmField)
    {
        if (building.getWorkingOffset() == null)
        {
            building.setWorkingOffset(BlockPos.ZERO);
        }

        int x = building.getWorkingOffset().getX();
        int z = building.getWorkingOffset().getZ();
        boolean flag1,flag2,flag3,flag4;

        do
        {
            if (z > FarmField.MAX_RANGE)
            {
                return null;
            }

            flag1 = Math.abs(x) == Math.abs(z);
            flag2 = x >= 0;
            flag3 = z > 0;
            if(flag1){
                if(flag2) {
                    z++;
                }
                else if(flag3){
                    z--;
                }
                else{
                    x++;
                }
                continue;
            }

            flag4 = Math.abs(x) > Math.abs(z);
            if(flag4){
                if(flag2){
                    z++;
                }
                else{
                    z--;
                }
            }
            else{
                if(flag3){
                    x--;
                }
                else{
                    x++;
                }
            }
        }
        while (
                -z > farmField.getRadius(NORTH)
                        || x > farmField.getRadius(EAST)
                        || z > farmField.getRadius(SOUTH)
                        || -x > farmField.getRadius(Direction.WEST)
        );

        return new BlockPos(x, 0, z);
    }

    /**
     * Check if itemStack can be used as compost.
     *
     * @param itemStack the stack to check.
     * @return true if so.
     */
    private boolean isCompost(final ItemStack itemStack)
    {
        if (itemStack.getItem() == ModItems.compost)
        {
            return true;
        }
        return itemStack.getItem() == Items.BONE_MEAL;
    }

    @Override
    public Class<BuildingFarmer> getExpectedBuildingClass()
    {
        return BuildingFarmer.class;
    }

    /**
     * Called to check when the InventoryShouldBeDumped.
     *
     * @return true if the conditions are met
     */
    @Override
    protected boolean wantInventoryDumped()
    {
        if (shouldDumpInventory || job.getActionsDone() >= getActionRewardForCraftingSuccess())
        {
            shouldDumpInventory = false;
            return true;
        }
        return super.wantInventoryDumped();
    }

    @Override
    public boolean hasWorkToDo()
    {
        return true;
    }

    @Override
    protected int getActionsDoneUntilDumping()
    {
        return MAX_BLOCKS_MINED;
    }

    @Override
    protected int getActionRewardForCraftingSuccess()
    {
        return MAX_BLOCKS_MINED;
    }

    @Override
    protected void updateRenderMetaData()
    {
        worker.setRenderMetadata((getState() == FARMER_PLANT || getState() == FARMER_HARVEST) ? RENDER_META_WORKING : "");
    }

    @Override
    protected List<ItemStack> increaseBlockDrops(final List<ItemStack> drops)
    {
        assert worker.getCitizenColonyHandler().getColonyOrRegister() != null;
        final double increaseCrops = worker.getCitizenColonyHandler().getColonyOrRegister().getResearchManager().getResearchEffects().getEffectStrength(FARMING);
        final int multiplyCrops = 1 + (int)worker.getCitizenColonyHandler().getColonyOrRegister().getResearchManager().getResearchEffects().getEffectStrength(PRECISE_FARMING);
        if (increaseCrops == 0 && multiplyCrops <= 1)
        {
            return drops;
        }

        final List<ItemStack> newDrops = new ArrayList<>();
        for (final ItemStack stack : drops)
        {
            final ItemStack drop = stack.copy();
            if (worker.getRandom().nextDouble() < increaseCrops)
            {
                drop.setCount(drop.getCount() * 2);
            }
            drop.setCount(drop.getCount() + stack.getCount() * multiplyCrops);
            newDrops.add(drop);
        }

        return newDrops;
    }

    protected List<ItemStack> increaseBlockDropsSpecial(final List<ItemStack> drops) {
        assert worker.getCitizenColonyHandler().getColonyOrRegister() != null;
        final double increaseCrops = worker.getCitizenColonyHandler().getColonyOrRegister().getResearchManager().getResearchEffects().getEffectStrength(FARMING);
        final int multiplyCrops = 1 + (int)worker.getCitizenColonyHandler().getColonyOrRegister().getResearchManager().getResearchEffects().getEffectStrength(PRECISE_FARMING);
        if (increaseCrops == 0 && multiplyCrops <= 1)
        {
            return List.of();
        }

        final List<ItemStack> newDrops = new ArrayList<>();
        for (final ItemStack stack : drops)
        {
            final ItemStack drop = stack.copy();
            drop.setCount(0);
            if (worker.getRandom().nextDouble() < increaseCrops)
            {
                drop.setCount(stack.getCount());
            }
            drop.setCount(drop.getCount() + stack.getCount() * multiplyCrops);
            newDrops.add(drop);
        }

        return newDrops;
    }

    @Override
    public int getBreakSpeedLevel()
    {
        return getSecondarySkillLevel();
    }

    /**
     * Get the slot in which the hoe is in.
     *
     * @return slot number
     */
    private int getHoeSlot()
    {
        return InventoryUtils.getFirstSlotOfItemHandlerContainingEquipment(getInventory(), ModEquipmentTypes.hoe.get(), TOOL_LEVEL_WOOD_OR_GOLD, building.getMaxEquipmentLevel());
    }

    /**
     * Returns the farmer's worker instance. Called from outside this class.
     *
     * @return citizen object
     */
    @Nullable
    public AbstractEntityCitizen getCitizen()
    {
        return worker;
    }

    @Override
    public boolean canGoIdle()
    {
        if (building.getModule(FARMER_FIELDS).getBuildingExtensionToWorkOn() == null)
        {
            return !super.hasWorkToDo();
        }

        return false;
    }

    @Override
    public boolean holdEfficientTool(@NotNull final BlockState target, final BlockPos pos)
    {
        final int bestSlot = getMostEfficientTool(target, pos);
        if (bestSlot == NO_TOOL)
        {
            worker.getCitizenData().setJobStatus(JobStatus.WORKING);
            return true;
        }
        return super.holdEfficientTool(target, pos);
    }
}
