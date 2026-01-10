package com.arxyt.colonypathingedition.core.config;

import com.arxyt.colonypathingedition.ColonyPathingEdition;
import com.arxyt.colonypathingedition.core.config.enums.BuilderModeEnum;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.util.FoodUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PathingConfig {
    public static ForgeConfigSpec.BooleanValue EATING_AI_MODULE;
    public static ForgeConfigSpec.BooleanValue SMELTERY_AI_MODULE;

    public static ForgeConfigSpec.BooleanValue TAVERN_ASSIGNMENT_MODULE;
    public static ForgeConfigSpec.BooleanValue ADDITIONAL_MINIMUM_STOCK_MODULE;
    public static ForgeConfigSpec.BooleanValue FOOD_BLACK_LIST_MODULE;
    public static ForgeConfigSpec.BooleanValue HURT_ALERT;
    public static ForgeConfigSpec.BooleanValue ALLOW_RESURRECT;
    public static ForgeConfigSpec.ConfigValue<String> RESURRECT_ITEM;
    public static ForgeConfigSpec.DoubleValue RESURRECT_CHANCE_MULTIPLIER;
    public static ForgeConfigSpec.BooleanValue ALLOW_READ_MIND;
    public static ForgeConfigSpec.ConfigValue<String> READ_MIND_ITEM;

    public static ForgeConfigSpec.BooleanValue MESSAGE_DISPLAY;

    public static ForgeConfigSpec.DoubleValue RAIL_COST_MULTIPLIER;
    public static ForgeConfigSpec.DoubleValue WATER_COST_DEFINER;
    public static ForgeConfigSpec.DoubleValue JUMP_COST_DEFINER;
    public static ForgeConfigSpec.DoubleValue DROP_COST_MULTIPLIER;
    public static ForgeConfigSpec.DoubleValue ROAD_COST_MULTIPLIER;
    public static ForgeConfigSpec.DoubleValue INSHAPE_COST_DEFINER;
    public static ForgeConfigSpec.DoubleValue DOORS_COST_DEFINER;
    public static ForgeConfigSpec.DoubleValue DIVE_COST_DEFINER;
    public static ForgeConfigSpec.DoubleValue CAVE_COST_DEFINER;
    public static ForgeConfigSpec.DoubleValue RAILEXIT_COST_DEFINER;
    public static ForgeConfigSpec.DoubleValue LADDER_SWITCH_COST_DEFINER;
    public static ForgeConfigSpec.DoubleValue SHINGLE_COST_DEFINER;
    public static ForgeConfigSpec.DoubleValue FARMLAND_COST_DEFINER;
    public static ForgeConfigSpec.DoubleValue LEAF_COST_DEFINER;
    public static ForgeConfigSpec.DoubleValue BERRY_COST_DEFINER;

    public static ForgeConfigSpec.DoubleValue ONRAIL_CALLBACK_MULTIPLIER;
    public static ForgeConfigSpec.DoubleValue ONROAD_CALLBACK_MULTIPLIER;
    public static ForgeConfigSpec.DoubleValue ONRAIL_PREFERENCE;
    public static ForgeConfigSpec.DoubleValue ONROAD_PREFERENCE;
    public static ForgeConfigSpec.DoubleValue SWIMMING_PREFERENCE;
    public static ForgeConfigSpec.IntValue CALLBACK_TIMES_TOLERANCE;
    public static ForgeConfigSpec.IntValue NODE_EXTEND_COUNT;

    public static ForgeConfigSpec.DoubleValue RESTAURANT_WAITING_TIME;

    public static ForgeConfigSpec.DoubleValue MAX_PERCENTAGE_HP_FOR_CURE;
    public static ForgeConfigSpec.DoubleValue MAX_HP_FOR_CURE;
    public static ForgeConfigSpec.IntValue HEAL_START;
    public static ForgeConfigSpec.IntValue HEAL_DURATION;

    public static ForgeConfigSpec.BooleanValue LUMBERJACK_WORK_WHEN_UNCONSTRUCTED;
    public static ForgeConfigSpec.BooleanValue LUMBERJACK_PLANT_WITHOUT_SAPLINGS;
    public static ForgeConfigSpec.BooleanValue LUMBERJACK_BREAK_LEAVES_TO_GATHER;
    public static ForgeConfigSpec.IntValue LUMBERJACK_GATHER_WAITING_TIME;

    public static ForgeConfigSpec.EnumValue<BuilderModeEnum> BUILDER_MODE;
    public static ForgeConfigSpec.IntValue BUILDER_GIBBON_RANGE;
    public static ForgeConfigSpec.BooleanValue BUILDER_TAKE_ORDERS_EVERYWHERE;

    public static ForgeConfigSpec.BooleanValue MAX_ANIMAL_MODIFIER;
    public static ForgeConfigSpec.BooleanValue BUTCHER_INSTANT_KILL;

    public static ForgeConfigSpec.BooleanValue DELIVERY_EAT_AT_WAREHOUSE;
    public static ForgeConfigSpec.BooleanValue USE_MAX_STOCK_FIRST;
    public static ForgeConfigSpec.BooleanValue PICK_MATERIAL_AT_HUT;
    public static ForgeConfigSpec.BooleanValue EARLY_ENCHANT;
    public static ForgeConfigSpec.BooleanValue MINIMUM_STOCK_PRECISE;
    public static ForgeConfigSpec.IntValue ENCHANT_LEVEL_SCALE;
    public static ForgeConfigSpec.IntValue MAX_ADDITIONAL_LEVEL_ENCHANT;
    public static ForgeConfigSpec.IntValue LEISURE_TIME;
    public static ForgeConfigSpec.IntValue LEISURE_RATIO;
    public static ForgeConfigSpec.IntValue MAX_PRE_LEISURE_TIME;
    public static ForgeConfigSpec.DoubleValue FOOD_PUNISHER;
    public static ForgeConfigSpec.DoubleValue FOOD_BONUS_NORMAL;
    public static ForgeConfigSpec.DoubleValue FOOD_BONUS_MINECOLONIES;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> GENERAL_FOOD_BLACK_LIST;

    public static ForgeConfigSpec.IntValue MAX_PATHING_DISTANCE;

    public static ForgeConfigSpec init(ForgeConfigSpec.Builder builder) {
        builder.push("Module Opener #模块特性开关#");
        EATING_AI_MODULE = builder
                .comment("Open the module to use the remastered eating AI system (default: true)\n 开启此模块将会启用重制的市民进食AI (默认开启)")
                .define("enableNewEatingModule", true);
        SMELTERY_AI_MODULE = builder
                .comment("Open the module to use the remastered smeltery AI system for chef and stone smeltery(default: true)\n 开启此模块将会启用重制的厨师和冶炼工人AI (默认开启)")
                .define("enableNewSmelteryModule", true);
        builder.pop();
        builder.push("Building Module Opener #房屋模块特性开关#");
        TAVERN_ASSIGNMENT_MODULE = builder
                .comment("Open this to assign visitors directly trough tavern block, conflict with Tweaks addon for Minecolonies (default: false)\n 开启这个可以直接在酒馆雇佣游客，但是与 Tweaks addon for Minecolonies 有冲突 (默认关闭)")
                .define("enableTavernAssignmentModule", false);
        ADDITIONAL_MINIMUM_STOCK_MODULE = builder
                .comment("Open this to add minimum stock module to most of huts, may affect Compatibility addon for MineColonies‘s module (default: true)\n 开启这个可以直接在酒馆雇佣游客，但是可能影响某些 Compatibility addon for MineColonies 内容的工作 (默认开启)")
                .define("additionalMinimumStockModule", true);
        FOOD_BLACK_LIST_MODULE = builder
                .comment("Open this to add food black list to specific huts, you can edit exactly what workers won't eat at those huts(default: true)\n 开启这个可以给某几个特定的小屋增加员工禁食清单，这样员工就不会取食你所选定的食物 (默认开启)")
                .define("foodBlackListModule", true);
        builder.pop();
        builder.push("Easycolony Feature #简易殖民地相关特性开关#");
        HURT_ALERT = builder
                .comment("Is citizen alert when get hurt? (default: true)\n 市民是否警告玩家自己受伤？(默认开启)")
                .define("messageDisplay", true);
        builder.push("Resurrect #复活配置");
        ALLOW_RESURRECT = builder.comment("是否允许玩家手动复活市民：如果允许，玩家将可以使用指定物品复活市民，成功率取决于总幸福度和墓地等级。")
                .define("allow_resurrect", true);
        RESURRECT_ITEM = builder.comment("复活市民所需物品：指定玩家复活市民所需的物品。")
                .define("resurrect_item", "minecraft:totem_of_undying");
        RESURRECT_CHANCE_MULTIPLIER = builder.comment("复活市民几率乘数：会在原有的复活概率基础上额外乘以一个系数。")
                .defineInRange("resurrect_chance_multiplier", 1.0, 0, Integer.MAX_VALUE);
        builder.pop();
        builder.push("Mind Reader #市民思维读取器设置");
        ALLOW_READ_MIND = builder.comment("是否允许玩家读取市民行动：如果允许，玩家将可以手持特定物品右键市民读取市民当前行动。")
                .define("allow_read_mind", true);
        READ_MIND_ITEM = builder.comment("读取行动所需物品：指定玩家读取市民行动所需的物品，不会消耗。")
                .define("read_mind_item", "minecraft:compass");
        builder.pop();
        builder.pop();
        builder.push("Update Message Display #更新信息显示#");
        MESSAGE_DISPLAY = builder
                .comment("Is changelogs display in-game when new version updated? (default: true)\n 是否在游戏内部显示最新版本更新信息？(默认开启)")
                .define("messageDisplay", false);
        builder.pop();
        builder.push("Pathing Cost Modifier #寻路Cost相关设置#");
        builder.push("Multiplier #乘子系数#");
        RAIL_COST_MULTIPLIER = builder
                .comment("Rail path cost multiplier (default: 0.1, original:0.1) #铁轨Cost乘数 (默认 : 0.1 殖民地原设置 : 0.1)#")
                .defineInRange("railCostMultiplier", 0.1, 0.0, 2.0);
        ROAD_COST_MULTIPLIER = builder
                .comment("Road path cost multiplier (default: 0.4, original:1/6) #路径Cost乘数 (默认 : 0.4 殖民地原设置 : 1/6)#")
                .defineInRange("roadCostMultiplier", 0.4, 0.0, 2.0);
        DROP_COST_MULTIPLIER = builder
                .comment("Drop cost multiplier (default: 1.0, original:1.0) #掉落Cost乘数 (默认 : 1.0 殖民地原设置 : 1.0)#\n" +
                        "Notice: The base formula for falling cost has been modified from |dY|³ to (|dY| - 2/5)³ - 8/125. #注意:下落Cost基础公式已经从|dY|³修改为(|dY|-2/5)³-8/125.#")
                .defineInRange("dropCostMultiplier", 1.0, 0.0, 10.0);
        builder.pop();
        builder.push("Basic Cost Definer #基础Cost定义#");
        WATER_COST_DEFINER = builder
                .comment("Water path cost addon (default: 8.0, original:2.0) #水路Cost (默认 : 8.0 殖民地原设置 : 2.0)#")
                .defineInRange("waterCostAddon", 8.0, 0.0, 24.0);
        JUMP_COST_DEFINER = builder
                .comment("Jump cost addon (default: 2.0, original:2.0) #跳跃(准确来说是非梯子爬升)Cost (默认 : 2.0 殖民地原设置 : 2.0)#")
                .defineInRange("jumpCostAddon", 2.0, 0.0, 24.0);
        INSHAPE_COST_DEFINER = builder
                .comment("In shape cost addon (default: 2.0, original:2.0) #实体方块内部行走的Cost (默认 : 2.0 殖民地原设置 : 2.0)#")
                .defineInRange("inShapeCostAddon", 2.0, 0.0, 24.0);
        DOORS_COST_DEFINER = builder
                .comment("Door and trapdoor’s cost addon (default: 3.0, original:3.0) #穿过各种\"门\"的Cost (默认 : 3.0 殖民地原设置 : 3.0)#")
                .defineInRange("doorsCostAddon", 3.0, 0.0, 24.0);
        DIVE_COST_DEFINER = builder
                .comment("Dive cost addon (default: 24.0, original:4.0) #潜水(潜多了会淹死)的Cost (默认 : 24.0 殖民地原设置 : 4.0)#")
                .defineInRange("diveCostAddon", 24.0, 0.0, 24.0);
        CAVE_COST_DEFINER = builder
                .comment("Breathing cave air cost addon (default: 0.3, original:3.0) #钻洞(人工的也算)的Cost,(默认 : 0.3 殖民地原设置 : 3.0)#")
                .defineInRange("caveCostAddon", 0.3, 0.0, 24.0);
        RAILEXIT_COST_DEFINER = builder
                .comment("Exit railway cost addon (default: 4.0, original:4.0) #下铁路(人工的也算)的Cost(探测铁轨为”站点“，不计cost) (默认 : 4.0 殖民地原设置 : 4.0)#")
                .defineInRange("railExitAddon", 4.0, 0.0, 24.0);
        builder.pop();
        builder.push("Typical Cost Definer #针对性Cost定义#");
        LADDER_SWITCH_COST_DEFINER = builder
                .comment("Cost on switch ladder when climbing (default: 4.0) #攀爬时切换梯子的Cost (默认 : 4.0)#")
                .defineInRange("ladderSwitchCostAddon", 4.0, 0.0, 24.0);
        SHINGLE_COST_DEFINER = builder
                .comment("Walk on shingle cost addon (default: 3.0, original:3.0) #上房揭瓦(走在屋瓦上)的Cost (默认 : 3.0 殖民地原设置 : 3.0)#")
                .defineInRange("shingleCostAddon", 3.0, 0.0, 24.0);
        FARMLAND_COST_DEFINER = builder
                .comment("Jump in farmland or drop onto farmland cost addon (default: 8.0) #在农田中跳上跳下的Cost (默认 : 8.0)#")
                .defineInRange("farmlandCostAddon", 8.0, 0.0, 24.0);
        LEAF_COST_DEFINER = builder
                .comment("Walk on leaves cost addon (default: 4.0) #爬树(走在树叶上)的Cost (默认 : 4.0)#")
                .defineInRange("leafCostAddon", 4.0, 0.0, 24.0);
        BERRY_COST_DEFINER = builder
                .comment("Walk in sweet berry bush cost addon (default: 24.0) #被浆果丛扎的Cost (默认 : 24.0)#")
                .defineInRange("berryCostAddon", 24.0, 0.0, 24.0);
        builder.pop();
        builder.push("Advanced Pathfinding Constant Definer #进阶寻路算法常数定义(警告：慎重修改！会对全局寻路产生极大影响！)#");
        ONRAIL_CALLBACK_MULTIPLIER = builder
                .comment("""
                        When traversing callback nodes, there is a heuristic reduction on artificial roads. This value is used to control the rate at which the heuristic for railway callback nodes increases, and its formula is: RAIL_COST_MULTIPLIER * ONRAIL_CALLBACK_MULTIPLIER (default: 1.0)\s
                        走回头路(其节点暂定义为回扣节点)时在人工道路上会有启发值减免,这是用来控制铁路回扣节点启发值增加速度的数值，其算式为 RAIL_COST_MULTIPLIER * ONRAIL_CALLBACK_MULTIPLIER (默认 : 1.0)""")
                .defineInRange("onRailCallbackMutiplier", 1.0, 0.0, 2.0);
        ONROAD_CALLBACK_MULTIPLIER = builder
                .comment("""
                        This value is used to control the rate at which the heuristic for callback nodes on road blocks increases, and its formula is: ROAD_COST_MULTIPLIER * ONROAD_CALLBACK_MULTIPLIER (default: 1.2)\s
                        用来控制道路方块上回扣节点启发值增加速度的数值，其算式为 ROAD_COST_MULTIPLIER * ONROAD_CALLBACK_MULTIPLIER (默认 : 1.2)""")
                .defineInRange("onRoadCallbackMutiplier", 1.2, 0.0, 2.0);
        ONRAIL_PREFERENCE = builder
                .comment("""
                        This is a global heuristic reduction, representing how much villagers prefer railways when pathfinding far away from the target point. The smaller the value, the stronger the preference. (default: 0.85)
                        是一个全局的启发值减免，表现为村民在远离目标点处寻路时对铁路的信任程度，数值越小越信任(默认 : 0.85)""")
                .defineInRange("onRailPreference", 0.85, 0.5, 2.0);
        ONROAD_PREFERENCE = builder
                .comment("""
                        This is a global heuristic reduction, representing how much villagers prefer path blocks when pathfinding far away from the target point. The smaller the value, the stronger the preference. (default: 0.92)
                        是一个全局的启发值减免，表现为村民在远离目标点处寻路时对道路方块的信任程度，数值越小越信任(默认 : 0.92)""")
                .defineInRange("onRoadPreference", 0.92, 0.5, 2.0);
        SWIMMING_PREFERENCE = builder
                .comment("""
                        This is a global heuristic multiplier, representing how much villagers prefer swimming when pathfinding far away from the target point. The smaller the value, the stronger the preference. (default: 1.3)
                        是一个全局的启发值乘子，表现为村民在远离目标点处寻路时对水路的信任程度，数值越小越信任(默认 : 1.3)""")
                .defineInRange("swimmingPreference", 1.3, 0.8, 3);
        CALLBACK_TIMES_TOLERANCE = builder
                .comment("Tolerates how many times callback nodes can be expanded during each pathfinding process. (default: 2)\n 能容忍每次寻路中回扣节点被扩展几次，数值过大可能会造成扩展的无用节点增加 (默认 : 2)")
                .defineInRange("callbackTimesTolerance", 2, 1, 25);
        NODE_EXTEND_COUNT = builder
                .comment("Explore the \"cheapest\" X nodes at every step. (default: 4)\n 每一步寻路时探索最优的 X 个节点 (默认 : 4)")
                .defineInRange("nodeExtendCount", 4, 1, 25);
        builder.pop();
        builder.pop();
        builder.push("Restaurant Related Modifier #餐厅相关逻辑修改#");
        RESTAURANT_WAITING_TIME= builder
                .comment("""
                        Duration citizens wait in the restaurant for cook service (minutes) (default: 0.5, original: 2.0)
                        你的村民在餐厅等待厨师服务的时间(分钟) (默认 : 0.5 殖民地原设置 : 2.0)""")
                .defineInRange("restaurantWaitingTime", 0.5, 0.0, 2.0);
        builder.pop();
        builder.push("Hospital Related Modifier #医院相关逻辑修改#");
        MAX_PERCENTAGE_HP_FOR_CURE= builder
                .comment("""
                        The percentage of HP at which your citizens will seek treatment. (default: 0.2, original: 0.0)
                        你的村民将在剩多少比例的HP时去寻求医生治疗 (默认 : 0.2 殖民地原设置 : 0.0)""")
                .defineInRange("curePercentageHP", 0.2, 0.0, 1.0);
        MAX_HP_FOR_CURE= builder
                .comment("""
                        The absolute amount of HP at which your citizens will seek treatment. (default: 10.0, original: 6.0)
                        你的村民将在剩多少HP时去寻求医生治疗 (默认 : 10.0 殖民地原设置 : 6.0)""")
                .defineInRange("cureHP", 10.0, 6.0, 40.0);
        HEAL_START = builder
                .comment("Citizen will start to heal themselves at that time.\n 市民开始自愈的时间(s)")
                .defineInRange("startHeal", 480, 0, 4095);
        HEAL_DURATION = builder
                .comment("Citizen will randomly cure during this time.\n 市民将在这个时间段内均匀自愈(s)")
                .defineInRange("healDuration", 2000, 0, 65535);
        builder.pop();
        builder.push("LumberJack Modifier #伐木工相关修改#");
        LUMBERJACK_WORK_WHEN_UNCONSTRUCTED = builder
                .comment("Lumberjcak will start to work only if hut is placed.\n 伐木工会在放置工作方块后立即开始工作 (功能在后期有些超模，建议仅在前期开启以分担少量工作量)。")
                .define("lumberjackWorkWhenUnconstructed",false);
        LUMBERJACK_PLANT_WITHOUT_SAPLINGS = builder
                .comment("Lumberjcak will replant without saplings, still consume saplings when have.\n 伐木工在树苗不足时也会正常补种，但是有树苗时依旧会消耗树苗。")
                .define("lumberjackPlantWithoutSaplings",false);
        LUMBERJACK_BREAK_LEAVES_TO_GATHER = builder
                .comment("""
                        A toggle for lumberjacks to collect dropped items on trees in a more realistic but less efficient way (break leaves).
                        When disabled, uncollected items will instead be teleported directly to the lumberjack’s feet.
                        让伐木工收集树上的掉落物看起来更合理一些(破坏树叶)，效率相对比较低下，关闭后捡不到的物品会直接传送到伐木工脚下""")
                .define("lumberjackBreakLeavesToGather" ,true);
        LUMBERJACK_GATHER_WAITING_TIME = builder
                .comment("""
                        Time lumberjacks spend gathering a single item. Not recommended to set too long.
                        伐木工等待收集的时间，不过因为目前检测问题不建议时间过长""")
                .defineInRange("lumberjackGatherWaitingTime",5,2,10);
        builder.pop();
        builder.push("Builder Mode Modifier #土木人修改#");
        BUILDER_MODE = builder
                .comment("""
                        Builder mode (default: GIBBON), optional below: 建筑工人模式, (默认: 长臂猿)，可选项如下：
                        NORMAL: Normal mode, authentic. 常规: 原汁原味的殖民地体验。
                        FORMALIST: Play as a formalist, jumping up and down on the construction site, but work when they just leave their hut. 形式主义者：像个形式主义者一样在工地上蹿下跳，但是会在离开土木小屋后立即开始工作。
                        SENTRY: Play as a sentry, stand at a stable position to build. 哨兵：像一个哨兵一样，站在工地的固定位置工作。
                        GOD: GOD SHOULD BUILD ANYWHERE THEY WANT. 创世神：神就应该想在哪儿干就在那儿干。
                        GIBBON: Play as a gibbon, jumping up and down with a large building range. 长臂猿：默认选项，像猿猴一样上蹿下跳，但是只在一定建造范围内工作。""")
                .defineEnum("builderMode", BuilderModeEnum.GIBBON);
        BUILDER_GIBBON_RANGE = builder.comment("Building range of gibbon mode. 长臂猿模式下的建造范围。")
                        .defineInRange("builderGibbonRange", 20, 0, 128);
        BUILDER_TAKE_ORDERS_EVERYWHERE = builder.comment("Can builder take orders everywhere. 打灰人能否随时随地接单。")
                        .define("builderTakeOrdersEverywhere", true);
        builder.pop();
        builder.push("Herder Modifier #养殖户修改#");
        MAX_ANIMAL_MODIFIER = builder
                .comment("Max animal modifier, would you like to modify the max animals to 2^(building level)? (default: false)\n 养殖场最大生物数是否改为 2^建筑等级 原为 2*建筑等级(动物数目过大会导致卡顿，所以不默认开启),(默认 : false)")
                .define("increaseMaxAnimal",false);
        BUTCHER_INSTANT_KILL = builder
                .comment("Will butcher kill animals instantly? (default: true)\n 屠户是否会秒杀动物 (默认 : true)")
                .define("butcherInstantKill",true);
        builder.pop();
        builder.push("Common Citizens Modifier #通用市民修改#");
        DELIVERY_EAT_AT_WAREHOUSE = builder
                .comment("Open this to enable delivery man eat at warehouse.\n 开启后快递员将会在仓库吃饭")
                .define("deliveryEatAtWareHouse",true);
        USE_MAX_STOCK_FIRST = builder
                .comment("Crafters will use its max stock to craft as default, no need research to unlock.\n 开启后工人会使用库存中余量最多的物品合成，无需点亮对应科技。")
                .define("crafterUseMaxStockFirst",true);
        MINIMUM_STOCK_PRECISE = builder
                .comment("Minimum stock can now be set in item counts rather than in stacks.\n 开启后，最低存量将按个计数，而不是按组。")
                .define("minimumStockPrecise",false);
        PICK_MATERIAL_AT_HUT = builder.comment("Should citizens pick material at their own hut.\n 你的非快递员市民是否应当在他们的小屋方块处取货。")
                        .define("pickMaterialAtHut", true);
        EARLY_ENCHANT = builder.comment("Allows workers to use enchanted tools at their current level.\n 允许工人在工具允许等级时就启用一部分低级附魔工具。")
                .define("earlyEnchant", true);
        ENCHANT_LEVEL_SCALE = builder.comment("Controls how many enchantment levels require 1 worker's hut level upgrade.\n 每升一级可以允许额外附魔等级几级。")
                .defineInRange("enchantLevelScale", 2, 1, 6);
        MAX_ADDITIONAL_LEVEL_ENCHANT = builder.comment("Sets the maximum addtional worker's hut level enchantment tools.\n 设置附魔工具最多需求小屋增加几级可用。")
                .defineInRange("maxAdditionalLevelForEnchantTools", 2, 1, 5);
        FOOD_PUNISHER = builder
                .comment("""
                        Punishment of normal food nutrition for citizens (It's a multiplier) (default: 1, original:0.25)
                        普通食物惩罚乘数 (默认 : 1， 殖民地原设置 : 0.25)""")
                .defineInRange("foodPunisher", 1.0, 0.0, 1.0);
        FOOD_BONUS_NORMAL = builder
                .comment("""
                        Bonus of normal food nutrition for citizens (It's a multiplier on saturation) (default: 0.2)
                        普通食物奖励乘数(取决于食物饱和) (默认 : 0.2)""")
                .defineInRange("normalFoodBonus", 0.2, 0.0, 1.0);
        FOOD_BONUS_MINECOLONIES = builder
                .comment("""
                        Bonus of minecolonies' food nutrition for citizens (It's a multiplier on saturation) (default: 0.5)
                        殖民地食物奖励乘数(取决于食物饱和) (默认 : 0.5)""")
                .defineInRange("minecoloniesFoodBonus", 0.5, 0.0, 1.0);
        GENERAL_FOOD_BLACK_LIST = builder
                .comment("""
                        Write In “Foods" that you don‘t want citizen to eat at colonies.
                        写入你不想市民在殖民地中吃的所有食物。"""
                )
                .defineList("generalFoodBlackList",
                        List.of(),
                        obj -> obj instanceof String);
        LEISURE_TIME = builder
                .comment("""
                        Basic leisure time of citizens (s) (default: 180)
                        市民每次休息的默认时长(秒) (默认 : 180)""")
                .defineInRange("leisureTime", 180, 0, 10000);
        LEISURE_RATIO = builder
                .comment("""
                        Ratio of start a leisure per second. (Number as the x of 1/x) (default: 3600)
                        市民每秒有多少分之一的概率开始休息 (默认 : 0.5)""")
                .defineInRange("leisureRatio", 3600, 1000, 1000000);
        MAX_PRE_LEISURE_TIME = builder
                .comment("""
                        Defines how long would citizen decrease leisure time if their work are pretty “at leisure”, this will multiplied by their homebuilding level. (s) (default: 18)
                        这个时间会在村民空闲时累计，后面抵消它的休闲时间，此处用以控制抵消最大值，此值在实际应用时会乘以其住宅等级 (默认 : 18)""")
                .defineInRange("maxPreLeisureTime", 18, 0, 10000);
        builder.pop();
        builder.push("Basic Logic Modifier #基础逻辑修改#");
        MAX_PATHING_DISTANCE = builder
                .comment("Max pathing distance (default: 1000, original:500)\n 民最大寻路距离,(默认 : 1000 殖民地原设置 : 500)")
                .defineInRange("pathingDistance", 1000, 500, 4095);
        builder.pop();
        return builder.build(); // 返回构建结果
    }

    public static Set<ItemStorage> food_black_list = new HashSet<>();

    public static void onLoad() {
        List<? extends String> list = PathingConfig.GENERAL_FOOD_BLACK_LIST.get();
        if(list != null){
            for (String itemString : list) {
                try {
                    ResourceLocation rl = ResourceLocation.parse(itemString);
                    Item item = ForgeRegistries.ITEMS.getValue(rl);
                    if (item != null && item != Items.AIR) {
                        ItemStack itemStack = new ItemStack(item);
                        if(FoodUtils.EDIBLE.test(itemStack)) {
                            food_black_list.add(new ItemStorage(itemStack));
                            continue;
                        }
                    }
                    ColonyPathingEdition.LOGGER.warn("Wrong item or item not regard as food: {}", itemString);
                } catch (Exception e) {
                    ColonyPathingEdition.LOGGER.error("Failed to parse blacklist item: {}", itemString, e);
                }
            }
        }
    }
}