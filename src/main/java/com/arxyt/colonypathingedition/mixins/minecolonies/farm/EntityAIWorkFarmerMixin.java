package com.arxyt.colonypathingedition.mixins.minecolonies.farm;

import com.arxyt.colonypathingedition.core.data.farmlandmap.SpecialSeedManager;
import com.arxyt.colonypathingedition.core.data.tag.ModTag;
import com.arxyt.colonypathingedition.mixins.minecolonies.accessor.AbstractAISkeletonAccessor;
import com.arxyt.colonypathingedition.mixins.minecolonies.accessor.AbstractEntityAIBasicAccessor;
import com.google.common.reflect.TypeToken;
import com.minecolonies.api.advancements.AdvancementTriggers;
import com.minecolonies.api.colony.buildingextensions.IBuildingExtension;
import com.minecolonies.api.colony.interactionhandling.ChatPriority;
import com.minecolonies.api.colony.requestsystem.requestable.StackList;
import com.minecolonies.api.entity.ai.JobStatus;
import com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.api.entity.citizen.VisibleCitizenStatus;
import com.minecolonies.api.equipment.ModEquipmentTypes;
import com.minecolonies.api.items.ModItems;
import com.minecolonies.api.util.InventoryUtils;
import com.minecolonies.api.util.Log;
import com.minecolonies.api.util.Tuple;
import com.minecolonies.api.util.constant.translation.RequestSystemTranslationConstants;
import com.minecolonies.core.Network;
import com.minecolonies.core.blocks.BlockScarecrow;
import com.minecolonies.core.blocks.MinecoloniesCropBlock;
import com.minecolonies.core.blocks.MinecoloniesFarmland;
import com.minecolonies.core.colony.buildingextensions.FarmField;
import com.minecolonies.core.colony.buildings.AbstractBuilding;
import com.minecolonies.core.colony.buildings.modules.BuildingExtensionsModule;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingFarmer;
import com.minecolonies.core.colony.interactionhandling.StandardInteraction;
import com.minecolonies.core.colony.jobs.JobFarmer;
import com.minecolonies.core.entity.ai.workers.crafting.AbstractEntityAICrafting;
import com.minecolonies.core.entity.ai.workers.production.agriculture.EntityAIWorkFarmer;
import com.minecolonies.core.items.ItemCrop;
import com.minecolonies.core.network.messages.client.CompostParticleMessage;
import com.minecolonies.core.util.AdvancementUtils;
import com.minecolonies.core.util.citizenutils.CitizenItemUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState.*;
import static com.minecolonies.api.util.constant.CitizenConstants.BLOCK_BREAK_SOUND_RANGE;
import static com.minecolonies.api.util.constant.Constants.STACKSIZE;
import static com.minecolonies.api.util.constant.EquipmentLevelConstants.TOOL_LEVEL_WOOD_OR_GOLD;
import static com.minecolonies.api.util.constant.StatisticsConstants.CROPS_HARVESTED;
import static com.minecolonies.api.util.constant.StatisticsConstants.LAND_TILLED;
import static com.minecolonies.api.util.constant.TranslationConstants.NO_FREE_FIELDS;


@Mixin(value = EntityAIWorkFarmer.class, remap = false)
public abstract class EntityAIWorkFarmerMixin extends AbstractEntityAICrafting<JobFarmer, BuildingFarmer> implements AbstractAISkeletonAccessor<JobFarmer>, AbstractEntityAIBasicAccessor<AbstractBuilding> {
    @Shadow(remap = false) private int skippedState;
    @Shadow(remap = false) private boolean didWork;
    @Shadow(remap = false) private boolean shouldDumpInventory;

    @Final @Shadow(remap = false) private static VisibleCitizenStatus FARMING_ICON;

    @Shadow(remap = false) protected abstract BlockPos getSurfacePos(final BlockPos position);
    @Shadow(remap = false) protected abstract boolean isCompost(final ItemStack itemStack);
    @Shadow(remap = false) protected abstract int getLevelDelay();
    @Shadow(remap = false) protected abstract boolean isRightFarmLandForCrop(FarmField farmField, BlockState blockState);
    @Shadow(remap = false) protected abstract void equipHoe();

    /**
     * Methods to clarify special seed by Tags
     */
    @Unique private static boolean isUnderWater(@NotNull ItemStack stack) {
        return stack.is(ModTag.SEEDS_UNDERWATER);
    }
    @Unique private static boolean isNoFarmland(@NotNull ItemStack stack) {
        return stack.is(ModTag.SEEDS_NOFARMLAND);
    }

    /**
     * Bonemeal target check with fewer param
     */
    @Unique private boolean isBoneMealAble(BlockPos position,BonemealableBlock bonemealable) {
        BlockState state = getWorld().getBlockState(position);
        return bonemealable.isValidBonemealTarget(
                getWorld(),
                position,
                state,
                false // pIsClient 应为 false（服务端逻辑）
        );
    }

    @Unique private final Predicate<BlockState> normal = blockState -> blockState.getBlock() instanceof MinecoloniesFarmland || blockState.is(Blocks.FARMLAND);

    public EntityAIWorkFarmerMixin(@NotNull final JobFarmer job)
    {
        super(job);
    }


    /**
     * @author ARxyt
     * @reason algorithm fix to avoid /0.
     */
    @Overwrite(remap = false)
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
                -z > farmField.getRadius(Direction.NORTH)
                        || x > farmField.getRadius(Direction.EAST)
                        || z > farmField.getRadius(Direction.SOUTH)
                        || -x > farmField.getRadius(Direction.WEST)
        );

        return new BlockPos(x, 0, z);
    }

    @Inject(method = "prepareForFarming",at = @At("HEAD"),remap = false,cancellable = true)
    private void remasteredPrepareForFarming(CallbackInfoReturnable<IAIState> cir){
        worker.getCitizenData().setJobStatus(JobStatus.IDLE);
        if (building == null || building.getBuildingLevel() < 1)
        {
            worker.getCitizenData().setJobStatus(JobStatus.STUCK);
            cir.setReturnValue(PREPARING);
            return;
        }

        final BuildingExtensionsModule module = building.getFirstModuleOccurance(BuildingExtensionsModule.class);
        if (module.getOwnedExtensions().size() == building.getMaxBuildingLevel())
        {
            AdvancementUtils.TriggerAdvancementPlayersForColony(building.getColony(), AdvancementTriggers.MAX_FIELDS::trigger);
        }

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
            needsCurrently = new Tuple<>(this::isCompost, STACKSIZE);
            cir.setReturnValue(GATHERING_REQUIRED_MATERIALS);
            return;
        }

        if (module.hasNoExtensions())
        {
            if (worker.getCitizenData() != null)
            {
                worker.getCitizenData().triggerInteraction(new StandardInteraction(Component.translatable(NO_FREE_FIELDS), ChatPriority.BLOCKING));
            }
            worker.getCitizenData().setJobStatus(JobStatus.STUCK);
            cir.setReturnValue(IDLE);
            return;
        }

        final IBuildingExtension fieldToWork = module.getExtensionToWorkOn();
        if (fieldToWork instanceof FarmField farmField)
        {
            if(isMissingFarmland && SpecialSeedManager.isSpecialSeed(farmField.getSeed().getItem())){
                final Block farmland = SpecialSeedManager.getRequiredSoil(farmField.getSeed().getItem());
                Item itemFarmland = Item.BY_BLOCK.get(farmland);
                final int amountOfFarmlandInBuilding = InventoryUtils.hasBuildingEnoughElseCount(building, itemStack -> isItemOfFarmland(itemStack,farmland), 0);
                final int amountOfFarmlandInInv = InventoryUtils.getItemCountInItemHandler(getWorker().getInventoryCitizen(), itemStack -> isItemOfFarmland(itemStack,farmland));
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
                    cir.setReturnValue(GATHERING_REQUIRED_MATERIALS);
                    return;
                }
            }
            else{
                isMissingFarmland = false;
            }

            if (checkForToolOrWeapon(ModEquipmentTypes.hoe.get()))
            {
                worker.getCitizenData().setJobStatus(JobStatus.STUCK);
                cir.setReturnValue(PREPARING);
                return;
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
                cir.setReturnValue(state);
                return;
            }
            farmField.setFieldStage(FarmField.Stage.EMPTY);
            module.resetCurrentExtension();
            cir.setReturnValue(PREPARING);
            return;
        }
        else if (fieldToWork != null)
        {
            Log.getLogger().warn("Farmer found non-FarmField extension: {}", fieldToWork.getClass());
        }
        cir.setReturnValue(IDLE);
    }

    private boolean isMissingFarmland = false;

    @Inject(method = "workAtField", at = @At("HEAD"), remap = false, cancellable = true)
    private void remasteredWorkAtField(CallbackInfoReturnable<IAIState> cir){
        final BuildingExtensionsModule module = building.getFirstModuleOccurance(BuildingExtensionsModule.class);
        final IBuildingExtension field = module.getCurrentExtension();

        worker.getCitizenData().setVisibleStatus(FARMING_ICON);
        if (field instanceof FarmField farmField)
        {
            if (farmField.getSeed().isEmpty()){
                cir.setReturnValue(PREPARING);
                return;
            }
            if (building.getWorkingOffset() != null)
            {
                final BlockPos position = farmField.getPosition().below().south(building.getWorkingOffset().getZ()).east(building.getWorkingOffset().getX());

                // Still moving to the block
                if (!walkToSafePos(position.above()))
                {
                    cir.setReturnValue(getState());
                    return;
                }

                switch ((AIWorkerState) getState())
                {
                    case FARMER_HARVEST :
                    {
                        BlockPos pos = newFindHarvestableSurface(position, farmField);
                        if (pos != null) {
                            // Minecraft.getInstance().gui.getChat().addMessage(Component.literal("[Debug] Place Is Harvestable"));
                            if (newHarvestIfAbleWithRightClick(pos, farmField)) {
                                didWork = true;
                                cir.setReturnValue(FARMER_HOE);
                                return;
                            }
                            if (newHarvestIfAble(position, farmField)) {
                                didWork = true;
                                cir.setReturnValue(FARMER_HOE);
                            } else {
                                cir.setReturnValue(getState());
                            }
                            return;
                        }
                    }
                    case FARMER_HOE :
                    {
                        BlockPos pos = newFindHoeableSurface(position,farmField);

                        if(pos != null){
                            // Minecraft.getInstance().gui.getChat().addMessage(Component.literal("[Debug] Place Is Hoeable"));
                            if (newHoeIfAble(pos, farmField))
                            {
                                didWork = true;
                                cir.setReturnValue(FARMER_PLANT);
                            }
                            else if(!isMissingFarmland){
                                cir.setReturnValue(getState());

                            }
                            else{
                                farmField.setFieldStage(FarmField.Stage.HOED);
                                cir.setReturnValue(PREPARING);
                            }
                            return;
                        }
                    }
                    case FARMER_PLANT :
                    {
                        BlockPos pos = newFindPlantableSurface(position,farmField);
                        if( pos != null ) {
                            // Minecraft.getInstance().gui.getChat().addMessage(Component.literal("[Debug] Place Is Plantable"));
                            if(!newPlantCrop(farmField.getSeed(), pos)){
                                didWork = true;
                                farmField.setFieldStage(FarmField.Stage.HOED);
                                cir.setReturnValue(PREPARING);
                                return;
                            }
                        }
                        break;
                    }
                    default :
                    {
                        farmField.setFieldStage(FarmField.Stage.HOED);
                        cir.setReturnValue(PREPARING);
                        return;
                    }
                }
                building.setPrevPos(position);
                setDelay(getLevelDelay());
            }

            IAIState state = checkNextWorkspaceAndState(farmField,
                    pos -> this.newFindHarvestableSurface(pos, farmField) != null,
                    pos -> this.newFindHoeableSurface(pos, farmField) != null,
                    pos -> this.newFindPlantableSurface(pos,farmField) != null);
            if(state != null){
                cir.setReturnValue(state);
                return;
            }
            if (building.getWorkingOffset() == null)
            {
                shouldDumpInventory = true;
                farmField.nextState();
                module.markDirty();
                if (didWork)
                {
                    module.resetCurrentExtension();
                    skippedState = 0;
                }
                didWork = false;
                building.setPrevPos(null);
                cir.setReturnValue(PREPARING);
                return;
            }
        }
        else
        {
            cir.setReturnValue(IDLE);
            return;
        }
        farmField.setFieldStage(FarmField.Stage.HOED);
        cir.setReturnValue(PREPARING);
    }

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

    // Try right click first
    private boolean newHarvestIfAbleWithRightClick(BlockPos pos, FarmField farmField){
        // Right click
        BlockState state = world.getBlockState(pos.above());
        if(state.getBlock() instanceof BushBlock || state.getBlock() instanceof CropBlock){
            BlockHitResult hitResult = new BlockHitResult(Vec3.atCenterOf(pos.above()), Direction.UP, pos.above(), false);
            FakePlayer fakePlayer = getFakePlayer();
            ItemStack seedStack = state.getCloneItemStack(hitResult, world, pos.above(),fakePlayer);
            if(farmField.getSeed().getItem() == seedStack.getItem()){
                InteractionResult result = state.use(world, fakePlayer, InteractionHand.MAIN_HAND, hitResult);
                return result.consumesAction();
            }
        }
        // Break crop
        return false;
    }

    @Unique
    private boolean newHarvestIfAble(BlockPos position, FarmField farmField)
    {
        position = newFindHarvestableSurface(position, farmField);
        if (position != null)
        {
            if (mineBlock(position.above()))
            {
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

    private boolean isItemOfFarmland(final ItemStack itemStack, final Block Farmland){
        if(itemStack.getItem() instanceof BlockItem blockItem){
            return blockItem.getBlock() == Farmland;
        }
        return false;
    }

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
                    final int amountOfFarmlandInInv = InventoryUtils.getItemCountInItemHandler(getWorker().getInventoryCitizen(), itemStack -> isItemOfFarmland(itemStack,farmland));
                    if (amountOfFarmlandInInv == 0 || !InventoryUtils.shrinkItemCountInItemHandler(getWorker().getInventoryCitizen(), itemStack -> isItemOfFarmland(itemStack,farmland)))
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
                worker.getCitizenColonyHandler().getColonyOrRegister().getStatisticsManager().increment(LAND_TILLED, worker.getCitizenColonyHandler().getColonyOrRegister().getDay());
                return true;
            }
            return false;
        }
        return true;
    }

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
        final BlockState toolModifiedState = blockState.getToolModifiedState(useOnContext, ToolActions.HOE_TILL, true);
        if (toolModifiedState == null || !(toolModifiedState.getBlock() instanceof FarmBlock))
        {
            return null;
        }

        return position;
    }

    private void checkFarmlandAndReturnSoli(BlockState farmlandState, Predicate<BlockState> condition){
        if(condition.test(farmlandState)){
            ItemStack stack = new ItemStack(farmlandState.getBlock(),1);
            InventoryUtils.addItemStackToItemHandler(worker.getItemHandlerCitizen(),stack);
        }
    }

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
            final BlockState toolModifiedState = blockState.getToolModifiedState(useOnContext, ToolActions.HOE_TILL, true);
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
     * Add special check for special seeds.
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
     * Special harvest action for special seeds.
     */
    @Unique
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
                Network.getNetwork().sendToPosition(new CompostParticleMessage(position.above()),
                        new PacketDistributor.TargetPoint(position.getX(), position.getY(), position.getZ(), BLOCK_BREAK_SOUND_RANGE, world.dimension()));
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
                Network.getNetwork().sendToPosition(new CompostParticleMessage(position.above()),
                        new PacketDistributor.TargetPoint(position.getX(), position.getY(), position.getZ(), BLOCK_BREAK_SOUND_RANGE, world.dimension()));
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
                        final int amountOfCompostInInv = InventoryUtils.getItemCountInItemHandler(getWorker().getInventoryCitizen(), this::isCompost);
                        if (amountOfCompostInInv != 0 && InventoryUtils.shrinkItemCountInItemHandler(getWorker().getInventoryCitizen(), this::isCompost))
                        {
                            Network.getNetwork().sendToPosition(new CompostParticleMessage(position.above()),
                                    new PacketDistributor.TargetPoint(position.getX(), position.getY(), position.getZ(), BLOCK_BREAK_SOUND_RANGE, getWorld().dimension()));
                            stemBlock.performBonemeal((ServerLevel) getWorld(), getWorld().getRandom(), position.above(), surfaceState);
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
                final int amountOfCompostInInv = InventoryUtils.getItemCountInItemHandler(getWorker().getInventoryCitizen(), this::isCompost);
                if (amountOfCompostInInv == 0)
                {
                    return null;
                }

                if (InventoryUtils.shrinkItemCountInItemHandler(getWorker().getInventoryCitizen(), this::isCompost))
                {
                    Network.getNetwork().sendToPosition(new CompostParticleMessage(position.above()),
                            new PacketDistributor.TargetPoint(position.getX(), position.getY(), position.getZ(), BLOCK_BREAK_SOUND_RANGE, getWorld().dimension()));
                    bonemealable.performBonemeal((ServerLevel) getWorld(), getWorld().getRandom(), position.above(), surfaceState);
                    surfaceState = getWorld().getBlockState(position.above());
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
}
