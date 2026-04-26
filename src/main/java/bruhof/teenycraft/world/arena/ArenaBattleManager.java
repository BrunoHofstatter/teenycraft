package bruhof.teenycraft.world.arena;

import bruhof.teenycraft.TeenyBalance;
import bruhof.teenycraft.battle.ai.BattleAiProfile;
import bruhof.teenycraft.battle.BattleFigure;
import bruhof.teenycraft.capability.BattleState;
import bruhof.teenycraft.capability.BattleStateProvider;
import bruhof.teenycraft.capability.TitanManagerProvider;
import bruhof.teenycraft.entity.ModEntities;
import bruhof.teenycraft.entity.custom.EntityTeenyDummy;
import bruhof.teenycraft.networking.ModMessages;
import bruhof.teenycraft.networking.PacketSyncBattleData;
import bruhof.teenycraft.networking.PacketSyncTitanData;
import bruhof.teenycraft.world.dimension.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

public final class ArenaBattleManager {
    private static final String ARENA_SPEED_EFFECT_ID = "arena_speed";
    private static final String ARENA_LAUNCH_LOCK_EFFECT_ID = "arena_launch_lock";
    private static final String ARENA_LAUNCH_MOVEMENT_LOCK_EFFECT_ID = "arena_launch_movement_lock";
    private static final String PICKUP_TAG_IS_ARENA_PICKUP = "TeenyArenaPickup";
    private static final String PICKUP_TAG_SLOT_INDEX = "TeenyArenaPickupSlot";
    private static final double LAUNCH_GRAVITY_PER_TICK = 0.08d;
    private static final double LAUNCH_VERTICAL_DRAG = 0.98d;
    private static final double LAUNCH_HORIZONTAL_DRAG = 0.91d;
    private static final int LAUNCH_MAX_SIM_TICKS = 200;
    private static final int LAUNCH_SOLVER_ITERATIONS = 24;
    private static final int SLOT_SPACING = 256;
    private static final int SLOT_Y = 64;
    private static final int SLOT_GRID_WIDTH = 2;
    private static final int SLOT_COUNT = 4;

    private static final Map<UUID, ArenaBattleSession> SESSIONS_BY_PARTICIPANT = new HashMap<>();
    private static final Map<Integer, ArenaBattleSession> SESSIONS_BY_SLOT = new HashMap<>();

    private ArenaBattleManager() {
    }

    public static ArenaBattleSession startBattle(ServerPlayer player,
                                                 List<ItemStack> playerTeam,
                                                 List<ItemStack> opponentStacks,
                                                 ResourceLocation requestedArenaId,
                                                 BattleAiProfile opponentAiProfile) {
        if (SESSIONS_BY_PARTICIPANT.containsKey(player.getUUID())) {
            throw new IllegalStateException("Player is already in an arena battle.");
        }

        ResourceLocation arenaId = requestedArenaId != null ? requestedArenaId : ArenaLoader.getDefaultArenaId();
        if (arenaId == null) {
            throw new IllegalStateException("No arenas are loaded.");
        }

        ArenaDefinition arena = ArenaLoader.getArena(arenaId);
        if (arena == null) {
            throw new IllegalArgumentException("Arena not found: " + arenaId);
        }

        ServerLevel teenyverse = player.serverLevel().getServer().getLevel(ModDimensions.TEENYVERSE_KEY);
        if (teenyverse == null) {
            throw new IllegalStateException("Teenyverse dimension is not available.");
        }

        int slotIndex = findFreeSlotIndex();
        if (slotIndex < 0) {
            throw new IllegalStateException("No free arena slots are available.");
        }

        BlockPos slotOrigin = getSlotOrigin(slotIndex);
        ArenaPlacement placement = buildArenaPlacement(player, arena, slotOrigin);
        clearArenaSpace(teenyverse, placement.clearOrigin(), placement.combinedSize(), arena.clearPadding());
        for (PlacedArenaTemplate placedTemplate : placement.templates()) {
            if (!placeArenaTemplate(teenyverse, placedTemplate.origin(), placedTemplate.template())) {
                throw new IllegalStateException("Failed to place arena template: " + placedTemplate.definition().templateId());
            }
        }

        Vec3 returnPosition = player.position();
        ResourceKey<Level> returnDimension = player.level().dimension();
        float returnYaw = player.getYRot();
        float returnPitch = player.getXRot();

        preparePlayerInventoryForBattleStart(player);

        Vec3 playerSpawn = arena.playerSpawnAt(slotOrigin);
        player.teleportTo(teenyverse, playerSpawn.x, playerSpawn.y, playerSpawn.z, java.util.Set.of(), arena.playerYaw(), arena.playerPitch());

        EntityTeenyDummy dummy = spawnOpponentDummy(teenyverse, slotOrigin, arena, opponentAiProfile);
        initializePairedBattle(player, playerTeam, dummy, opponentStacks);

        ArenaBattleSession session = new ArenaBattleSession(
                arenaId,
                arena,
                slotIndex,
                slotOrigin,
                player.getUUID(),
                dummy.getUUID(),
                returnDimension,
                returnPosition,
                returnYaw,
                returnPitch
        );
        registerSession(session);
        initializeSessionRuntime(teenyverse, session);
        return session;
    }

    public static void finishBattleForPlayer(ServerPlayer player) {
        ArenaBattleSession session = SESSIONS_BY_PARTICIPANT.get(player.getUUID());
        if (session == null) {
            fallbackFinish(player);
            return;
        }

        ServerLevel teenyverse = player.getServer().getLevel(ModDimensions.TEENYVERSE_KEY);
        ServerLevel returnLevel = player.getServer().getLevel(session.returnDimension());

        endBattleState(player);
        ModMessages.sendToPlayer(PacketSyncBattleData.off(), player);

        Entity opponent = teenyverse != null ? teenyverse.getEntity(session.opponentId()) : null;
        if (opponent != null) {
            endBattleState(opponent);
            opponent.discard();
        }

        if (returnLevel != null) {
            Vec3 returnPos = session.returnPosition();
            player.teleportTo(returnLevel, returnPos.x, returnPos.y, returnPos.z, java.util.Set.of(), session.returnYaw(), session.returnPitch());
        }

        restorePlayerInventoryIfNeeded(player, session.returnDimension());

        if (teenyverse != null) {
            cleanupSessionRuntime(teenyverse, session);
        }

        unregisterSession(session);

        ArenaPlacement placement = buildArenaPlacement(player, session.arenaDefinition(), session.slotOrigin());
        if (teenyverse != null && !placement.templates().isEmpty()) {
            clearArenaSpace(teenyverse, placement.clearOrigin(), placement.combinedSize(), session.arenaDefinition().clearPadding());
        }
    }

    public static ResourceLocation getArenaIdForParticipant(Entity entity) {
        ArenaBattleSession session = SESSIONS_BY_PARTICIPANT.get(entity.getUUID());
        return session != null ? session.arenaId() : null;
    }

    public static void tickSessions(MinecraftServer server) {
        if (SESSIONS_BY_SLOT.isEmpty()) {
            return;
        }

        ServerLevel teenyverse = server.getLevel(ModDimensions.TEENYVERSE_KEY);
        if (teenyverse == null) {
            return;
        }

        for (ArenaBattleSession session : List.copyOf(SESSIONS_BY_SLOT.values())) {
            tickSession(teenyverse, session);
        }
    }

    private static void fallbackFinish(ServerPlayer player) {
        endBattleState(player);
        ModMessages.sendToPlayer(PacketSyncBattleData.off(), player);

        ServerLevel overworld = player.serverLevel().getServer().getLevel(Level.OVERWORLD);
        if (overworld != null && player.serverLevel().dimension() == ModDimensions.TEENYVERSE_KEY) {
            player.teleportTo(overworld, player.getX(), player.getY(), player.getZ(), java.util.Set.of(), player.getYRot(), player.getXRot());
        }
    }

    private static void preparePlayerInventoryForBattleStart(ServerPlayer player) {
        player.getCapability(TitanManagerProvider.TITAN_MANAGER).ifPresent(handler -> {
            if (player.level().dimension() == ModDimensions.TEENYVERSE_KEY && !handler.hasSavedVanillaInventory()) {
                handler.saveVanillaInventory(player);
                syncTitanManager(player);
            }
        });
    }

    private static void restorePlayerInventoryIfNeeded(ServerPlayer player, ResourceKey<Level> returnDimension) {
        player.getCapability(TitanManagerProvider.TITAN_MANAGER).ifPresent(handler -> {
            if (returnDimension == ModDimensions.TEENYVERSE_KEY && handler.hasSavedVanillaInventory()) {
                handler.restoreVanillaInventory(player);
            }
            syncTitanManager(player);
        });
    }

    private static void syncTitanManager(ServerPlayer player) {
        player.getCapability(TitanManagerProvider.TITAN_MANAGER).ifPresent(handler -> {
            CompoundTag nbt = new CompoundTag();
            handler.saveNBTData(nbt);
            ModMessages.sendToPlayer(new PacketSyncTitanData(nbt), player);
        });
    }

    private static EntityTeenyDummy spawnOpponentDummy(ServerLevel level,
                                                       BlockPos slotOrigin,
                                                       ArenaDefinition arena,
                                                       BattleAiProfile aiProfile) {
        EntityTeenyDummy dummy = ModEntities.TEENY_DUMMY.get().create(level);
        if (dummy == null) {
            throw new IllegalStateException("Failed to create Teeny Dummy.");
        }

        Vec3 spawn = arena.opponentSpawnAt(slotOrigin);
        dummy.moveTo(spawn.x, spawn.y, spawn.z, arena.opponentYaw(), arena.opponentPitch());
        dummy.setAiProfile(aiProfile);
        level.addFreshEntity(dummy);
        return dummy;
    }

    private static void initializePairedBattle(ServerPlayer player,
                                               List<ItemStack> playerTeam,
                                               EntityTeenyDummy dummy,
                                               List<ItemStack> opponentStacks) {
        BattleState playerState = requireBattleState(player);
        BattleState opponentState = requireBattleState(dummy);

        playerState.initializeBattle(playerTeam, player, dummy);
        opponentState.initializeBattle(opponentStacks, dummy, player);
        if (opponentState.getTeam().isEmpty()) {
            dummy.discard();
            throw new IllegalStateException("Opponent team is empty.");
        }

        playerState.setActiveFigure(0);
        opponentState.setActiveFigure(0);
        playerState.refreshPlayerInventory(player);
        syncDummyPresentation(dummy, opponentState.getActiveFigure());
    }

    private static BattleState requireBattleState(LivingEntity entity) {
        return entity.getCapability(BattleStateProvider.BATTLE_STATE)
                .resolve()
                .filter(BattleState.class::isInstance)
                .map(BattleState.class::cast)
                .orElseThrow(() -> new IllegalStateException("Missing BattleState capability for " + entity.getStringUUID()));
    }

    private static BattleState getBattleState(LivingEntity entity) {
        return entity.getCapability(BattleStateProvider.BATTLE_STATE)
                .resolve()
                .filter(BattleState.class::isInstance)
                .map(BattleState.class::cast)
                .orElse(null);
    }

    private static void syncDummyPresentation(EntityTeenyDummy dummy, BattleFigure activeFigure) {
        if (activeFigure == null) {
            return;
        }

        if (dummy.getAttribute(Attributes.MAX_HEALTH) != null) {
            dummy.getAttribute(Attributes.MAX_HEALTH).setBaseValue(activeFigure.getMaxHp());
        }
        dummy.setHealth(activeFigure.getCurrentHp());
        dummy.setCustomName(net.minecraft.network.chat.Component.literal("Ã‚Â§c" + activeFigure.getNickname()));
        dummy.setCustomNameVisible(true);
    }

    private static void endBattleState(Entity entity) {
        entity.getCapability(BattleStateProvider.BATTLE_STATE).ifPresent(state -> {
            if (state.isBattling()) {
                state.endBattle();
            }
        });
    }

    private static void registerSession(ArenaBattleSession session) {
        SESSIONS_BY_PARTICIPANT.put(session.playerId(), session);
        SESSIONS_BY_PARTICIPANT.put(session.opponentId(), session);
        SESSIONS_BY_SLOT.put(session.slotIndex(), session);
    }

    private static void unregisterSession(ArenaBattleSession session) {
        SESSIONS_BY_PARTICIPANT.remove(session.playerId());
        SESSIONS_BY_PARTICIPANT.remove(session.opponentId());
        SESSIONS_BY_SLOT.remove(session.slotIndex());
    }

    private static int findFreeSlotIndex() {
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (!SESSIONS_BY_SLOT.containsKey(i)) {
                return i;
            }
        }
        return -1;
    }

    private static BlockPos getSlotOrigin(int slotIndex) {
        int gridX = slotIndex % SLOT_GRID_WIDTH;
        int gridZ = slotIndex / SLOT_GRID_WIDTH;
        return new BlockPos(gridX * SLOT_SPACING, SLOT_Y, gridZ * SLOT_SPACING);
    }

    private static StructureTemplate loadTemplate(ServerPlayer player, ResourceLocation templateId) {
        return player.getServer().getStructureManager().getOrCreate(templateId);
    }

    private static ArenaPlacement buildArenaPlacement(ServerPlayer player, ArenaDefinition arena, BlockPos slotOrigin) {
        if (arena.templates().isEmpty()) {
            throw new IllegalStateException("Arena has no template definitions: " + arena.id());
        }

        Map<Integer, Integer> columnWidths = new TreeMap<>();
        Map<Integer, Integer> rowDepths = new TreeMap<>();
        int maxHeight = 0;
        List<LoadedArenaTemplate> loadedTemplates = new java.util.ArrayList<>();

        for (ArenaTemplateDefinition templateDefinition : arena.templates()) {
            StructureTemplate template = loadTemplate(player, templateDefinition.templateId());
            if (template == null || isEmpty(template.getSize())) {
                throw new IllegalStateException("Arena template is missing or empty: " + templateDefinition.templateId());
            }

            loadedTemplates.add(new LoadedArenaTemplate(templateDefinition, template));
            columnWidths.merge(templateDefinition.gridX(), template.getSize().getX(), Math::max);
            rowDepths.merge(templateDefinition.gridZ(), template.getSize().getZ(), Math::max);
            maxHeight = Math.max(maxHeight, template.getSize().getY());
        }

        Map<Integer, Integer> xOffsets = buildGridOffsets(columnWidths);
        Map<Integer, Integer> zOffsets = buildGridOffsets(rowDepths);
        int totalWidth = columnWidths.values().stream().mapToInt(Integer::intValue).sum();
        int totalDepth = rowDepths.values().stream().mapToInt(Integer::intValue).sum();

        List<PlacedArenaTemplate> placedTemplates = new java.util.ArrayList<>();
        for (LoadedArenaTemplate loadedTemplate : loadedTemplates) {
            ArenaTemplateDefinition definition = loadedTemplate.definition();
            BlockPos partOrigin = slotOrigin.offset(
                    xOffsets.getOrDefault(definition.gridX(), 0),
                    0,
                    zOffsets.getOrDefault(definition.gridZ(), 0)
            );
            placedTemplates.add(new PlacedArenaTemplate(definition, loadedTemplate.template(), partOrigin));
        }

        return new ArenaPlacement(slotOrigin, new Vec3i(totalWidth, maxHeight, totalDepth), List.copyOf(placedTemplates));
    }

    private static Map<Integer, Integer> buildGridOffsets(Map<Integer, Integer> spansByGrid) {
        Map<Integer, Integer> offsets = new HashMap<>();
        int runningOffset = 0;
        for (Map.Entry<Integer, Integer> entry : spansByGrid.entrySet()) {
            offsets.put(entry.getKey(), runningOffset);
            runningOffset += entry.getValue();
        }
        return offsets;
    }

    private static boolean placeArenaTemplate(ServerLevel level, BlockPos origin, StructureTemplate template) {
        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setMirror(Mirror.NONE)
                .setRotation(Rotation.NONE)
                .setIgnoreEntities(true);
        return template.placeInWorld(level, origin, origin, settings, RandomSource.create(level.getSeed() ^ origin.asLong()), Block.UPDATE_ALL);
    }

    private static void clearArenaSpace(ServerLevel level, BlockPos origin, Vec3i templateSize, BlockPos clearPadding) {
        BlockPos min = origin.offset(-clearPadding.getX(), -clearPadding.getY(), -clearPadding.getZ());
        BlockPos max = origin.offset(
                templateSize.getX() - 1 + clearPadding.getX(),
                templateSize.getY() - 1 + clearPadding.getY(),
                templateSize.getZ() - 1 + clearPadding.getZ());

        AABB bounds = new AABB(min, max.offset(1, 1, 1));
        level.getEntitiesOfClass(Entity.class, bounds, entity -> !(entity instanceof ServerPlayer))
                .forEach(Entity::discard);

        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }
    }

    private static boolean isEmpty(Vec3i size) {
        return size.getX() <= 0 || size.getY() <= 0 || size.getZ() <= 0;
    }

    private static void initializeSessionRuntime(ServerLevel level, ArenaBattleSession session) {
        purgePickupVisualsForSlot(level, session.slotIndex(), session.slotOrigin());
        for (ArenaBattleSession.ArenaPickupSpawnerRuntime spawner : session.pickupSpawners()) {
            if (spawner.ticksUntilNextSpawn() <= 0) {
                spawnPickup(level, session, spawner);
            }
        }
    }

    private static void tickSession(ServerLevel level, ArenaBattleSession session) {
        tickLaunches(level, session);
        tickWalls(level, session);
        tickPickups(level, session);
    }

    private static void tickPickups(ServerLevel level, ArenaBattleSession session) {
        for (ArenaBattleSession.ArenaPickupSpawnerRuntime spawner : session.pickupSpawners()) {
            ArenaBattleSession.ActiveArenaPickup activePickup = spawner.activePickup();
            if (activePickup != null) {
                Entity entity = level.getEntity(activePickup.entityId());
                if (!(entity instanceof ItemEntity itemEntity) || !itemEntity.isAlive()) {
                    spawner.clearActivePickup();
                    spawner.setTicksUntilNextSpawn(spawner.definition().cooldownTicks());
                    continue;
                }

                LivingEntity collector = findPickupCollector(level, session, activePickup.worldPosition());
                if (collector != null) {
                    collectPickup(level, session, spawner, activePickup, itemEntity, collector);
                }
                continue;
            }

            if (spawner.ticksUntilNextSpawn() > 0) {
                spawner.tickSpawnDelay();
                if (spawner.ticksUntilNextSpawn() > 0) {
                    continue;
                }
            }

            spawnPickup(level, session, spawner);
        }
    }

    private static void tickLaunches(ServerLevel level, ArenaBattleSession session) {
        Iterator<Map.Entry<UUID, ArenaBattleSession.ArenaLaunchRuntime>> iterator = session.activeLaunches().entrySet().iterator();
        while (iterator.hasNext()) {
            ArenaBattleSession.ArenaLaunchRuntime launch = iterator.next().getValue();
            Entity entity = level.getEntity(launch.participantId());
            if (!(entity instanceof LivingEntity livingEntity)) {
                iterator.remove();
                continue;
            }

            launch.tick();
            keepLaunchFacing(livingEntity, launch.yaw(), launch.pitch());

            boolean landed = launch.elapsedTicks() > 2 && livingEntity.onGround();
            boolean timedOut = launch.elapsedTicks() >= launch.timeoutTicks();
            if (landed || timedOut) {
                finishLaunch(livingEntity, launch.yaw(), launch.pitch());
                iterator.remove();
            }
        }
    }

    private static void tickWalls(ServerLevel level, ArenaBattleSession session) {
        Iterator<ArenaBattleSession.ArenaWallRuntime> iterator = session.activeWalls().iterator();
        while (iterator.hasNext()) {
            ArenaBattleSession.ArenaWallRuntime wall = iterator.next();
            wall.tick();
            if (wall.ticksRemaining() <= 0) {
                restoreWall(level, wall);
                iterator.remove();
            }
        }
    }

    private static void spawnPickup(ServerLevel level, ArenaBattleSession session, ArenaBattleSession.ArenaPickupSpawnerRuntime spawner) {
        ArenaPickupSpawnerDefinition definition = spawner.definition();
        if (definition.spots().isEmpty() || definition.variants().isEmpty()) {
            return;
        }

        int spotIndex = selectIndex(definition.spotMode(), definition.spots().size(), spawner.nextSpotIndex(), level.random);
        int variantIndex = selectIndex(definition.variantMode(), definition.variants().size(), spawner.nextVariantIndex(), level.random);
        if (definition.spotMode() == ArenaSelectionMode.CYCLE) {
            spawner.setNextSpotIndex((spotIndex + 1) % definition.spots().size());
        }
        if (definition.variantMode() == ArenaSelectionMode.CYCLE) {
            spawner.setNextVariantIndex((variantIndex + 1) % definition.variants().size());
        }

        Vec3 worldPosition = session.arenaDefinition().positionAt(session.slotOrigin(), definition.spots().get(spotIndex));
        ArenaPickupVariantDefinition variant = definition.variants().get(variantIndex);
        ItemEntity pickupEntity = new ItemEntity(level, worldPosition.x, worldPosition.y, worldPosition.z, createPickupDisplayStack(variant));
        pickupEntity.setNoGravity(true);
        pickupEntity.setInvulnerable(true);
        pickupEntity.setUnlimitedLifetime();
        pickupEntity.setPickUpDelay(Short.MAX_VALUE);
        pickupEntity.setDeltaMovement(Vec3.ZERO);
        pickupEntity.getPersistentData().putBoolean(PICKUP_TAG_IS_ARENA_PICKUP, true);
        pickupEntity.getPersistentData().putInt(PICKUP_TAG_SLOT_INDEX, session.slotIndex());
        level.addFreshEntity(pickupEntity);

        spawner.setActivePickup(new ArenaBattleSession.ActiveArenaPickup(
                pickupEntity.getUUID(),
                worldPosition,
                variant,
                spotIndex,
                variantIndex
        ));
    }

    private static int selectIndex(ArenaSelectionMode mode, int size, int nextCycleIndex, RandomSource random) {
        if (size <= 1) {
            return 0;
        }
        return mode == ArenaSelectionMode.RANDOM ? random.nextInt(size) : Math.floorMod(nextCycleIndex, size);
    }

    private static ItemStack createPickupDisplayStack(ArenaPickupVariantDefinition variant) {
        return switch (variant.type()) {
            case HEAL -> new ItemStack(Items.RED_DYE);
            case MANA -> new ItemStack(Items.LAPIS_LAZULI);
            case AMP -> new ItemStack(Items.BLAZE_POWDER);
            case SPEED -> new ItemStack(Items.SUGAR);
            case LAUNCH -> new ItemStack(Items.FEATHER);
            case WALL -> new ItemStack(Items.BRICK);
        };
    }

    private static LivingEntity findPickupCollector(ServerLevel level, ArenaBattleSession session, Vec3 pickupPosition) {
        double radiusSq = TeenyBalance.ARENA_PICKUP_COLLECTION_RADIUS * TeenyBalance.ARENA_PICKUP_COLLECTION_RADIUS;
        for (UUID participantId : List.of(session.playerId(), session.opponentId())) {
            Entity entity = level.getEntity(participantId);
            if (!(entity instanceof LivingEntity livingEntity)) {
                continue;
            }

            if (session.activeLaunches().containsKey(participantId)) {
                continue;
            }

            BattleState battleState = getBattleState(livingEntity);
            if (battleState == null || !battleState.isBattling() || battleState.getActiveFigure() == null) {
                continue;
            }

            if (livingEntity.position().distanceToSqr(pickupPosition) <= radiusSq) {
                return livingEntity;
            }
        }
        return null;
    }

    private static void collectPickup(ServerLevel level,
                                      ArenaBattleSession session,
                                      ArenaBattleSession.ArenaPickupSpawnerRuntime spawner,
                                      ArenaBattleSession.ActiveArenaPickup activePickup,
                                      ItemEntity pickupEntity,
                                      LivingEntity collector) {
        BattleState battleState = getBattleState(collector);
        if (battleState == null || !battleState.isBattling() || battleState.getActiveFigure() == null) {
            return;
        }

        applyPickupVariant(level, session, collector, battleState, activePickup);
        pickupEntity.discard();
        spawner.clearActivePickup();
        spawner.setTicksUntilNextSpawn(spawner.definition().cooldownTicks());
        level.playSound(null, collector.blockPosition(), net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP, net.minecraft.sounds.SoundSource.PLAYERS, 0.7f, 1.15f);
    }

    private static void applyPickupVariant(ServerLevel level,
                                           ArenaBattleSession session,
                                           LivingEntity collector,
                                           BattleState battleState,
                                           ArenaBattleSession.ActiveArenaPickup activePickup) {
        ArenaPickupVariantDefinition variant = activePickup.variant();
        switch (variant.type()) {
            case HEAL -> battleState.applyResolvedCombatFigureDelta(battleState.getActiveFigure(), variant.amount());
            case MANA -> battleState.addMana(variant.amount());
            case AMP -> battleState.applyEffect("power_up", -1, variant.amount());
            case SPEED -> battleState.applyEffect(ARENA_SPEED_EFFECT_ID, variant.durationTicks(), variant.speedLevel());
            case LAUNCH -> beginLaunch(session, collector, battleState, activePickup.worldPosition(), variant);
            case WALL -> placeWall(level, session, variant);
        }
    }

    private static void beginLaunch(ArenaBattleSession session,
                                    LivingEntity collector,
                                    BattleState battleState,
                                    Vec3 pickupPosition,
                                    ArenaPickupVariantDefinition variant) {
        if (variant.destination() == null) {
            return;
        }

        if (battleState.hasEffect("flight")) {
            battleState.removeEffect("flight");
        }
        battleState.cancelCharge();
        battleState.cancelBlueChannel();
        Vec3 destination = session.arenaDefinition().positionAt(session.slotOrigin(), variant.destination());
        BallisticLaunchSolution solution = solveBallisticLaunch(pickupPosition, destination, variant.arcHeight());
        int launchLockTicks = Math.max(solution.expectedFlightTicks() + 8, 8);
        battleState.applyEffect(ARENA_LAUNCH_LOCK_EFFECT_ID, launchLockTicks, 1);
        battleState.applyEffect(ARENA_LAUNCH_MOVEMENT_LOCK_EFFECT_ID, launchLockTicks, 1);
        startLaunchMovement(collector, pickupPosition, solution.initialVelocity(), collector.getYRot(), collector.getXRot());

        session.activeLaunches().put(collector.getUUID(), new ArenaBattleSession.ArenaLaunchRuntime(
                collector.getUUID(),
                destination,
                collector.getYRot(),
                collector.getXRot(),
                solution.expectedFlightTicks(),
                launchLockTicks
        ));
    }

    private static void startLaunchMovement(LivingEntity entity, Vec3 position, Vec3 initialVelocity, float yaw, float pitch) {
        entity.teleportTo(position.x, position.y, position.z);
        entity.setNoGravity(false);
        entity.setDeltaMovement(initialVelocity);
        entity.hasImpulse = true;
        entity.hurtMarked = true;
        entity.setYRot(yaw);
        entity.setXRot(pitch);
        entity.fallDistance = 0.0f;
    }

    private static void keepLaunchFacing(LivingEntity entity, float yaw, float pitch) {
        entity.setYRot(yaw);
        entity.setXRot(pitch);
        entity.fallDistance = 0.0f;
    }

    private static void finishLaunch(LivingEntity entity, float yaw, float pitch) {
        keepLaunchFacing(entity, yaw, pitch);
        clearLaunchLock(entity);
    }

    private static void clearLaunchLock(LivingEntity entity) {
        entity.setNoGravity(false);
        entity.hurtMarked = true;
        entity.fallDistance = 0.0f;
        BattleState state = getBattleState(entity);
        if (state != null && state.hasEffect(ARENA_LAUNCH_LOCK_EFFECT_ID)) {
            state.removeEffect(ARENA_LAUNCH_LOCK_EFFECT_ID);
        }
        if (state != null && state.hasEffect(ARENA_LAUNCH_MOVEMENT_LOCK_EFFECT_ID)) {
            state.removeEffect(ARENA_LAUNCH_MOVEMENT_LOCK_EFFECT_ID);
        }
    }

    private static BallisticLaunchSolution solveBallisticLaunch(Vec3 start, Vec3 destination, double arcHeight) {
        double targetApexY = Math.max(start.y, destination.y) + Math.max(0.0d, arcHeight);

        double lowVy = 0.0d;
        double highVy = 0.6d;
        while (simulateVerticalApex(highVy) < targetApexY - start.y && highVy < 6.0d) {
            highVy *= 1.5d;
        }

        for (int i = 0; i < LAUNCH_SOLVER_ITERATIONS; i++) {
            double midVy = (lowVy + highVy) * 0.5d;
            double apex = simulateVerticalApex(midVy);
            if (apex < targetApexY - start.y) {
                lowVy = midVy;
            } else {
                highVy = midVy;
            }
        }

        double solvedVy = highVy;
        int flightTicks = simulateLandingTicks(solvedVy, destination.y - start.y);
        if (flightTicks <= 0) {
            flightTicks = 1;
        }

        double horizontalFactor = geometricSum(LAUNCH_HORIZONTAL_DRAG, flightTicks);
        if (horizontalFactor <= 0.0d) {
            horizontalFactor = 1.0d;
        }

        Vec3 delta = destination.subtract(start);
        double vx = delta.x / horizontalFactor;
        double vz = delta.z / horizontalFactor;
        return new BallisticLaunchSolution(new Vec3(vx, solvedVy, vz), flightTicks);
    }

    private static double simulateVerticalApex(double initialVy) {
        double y = 0.0d;
        double vy = initialVy;
        double apex = 0.0d;

        for (int tick = 0; tick < LAUNCH_MAX_SIM_TICKS; tick++) {
            y += vy;
            apex = Math.max(apex, y);
            vy = (vy - LAUNCH_GRAVITY_PER_TICK) * LAUNCH_VERTICAL_DRAG;
            if (vy <= 0.0d && y <= apex) {
                if (tick > 1) {
                    break;
                }
            }
        }

        return apex;
    }

    private static int simulateLandingTicks(double initialVy, double targetDeltaY) {
        double y = 0.0d;
        double vy = initialVy;

        for (int tick = 1; tick <= LAUNCH_MAX_SIM_TICKS; tick++) {
            y += vy;
            vy = (vy - LAUNCH_GRAVITY_PER_TICK) * LAUNCH_VERTICAL_DRAG;
            if (tick > 1 && vy < 0.0d && y <= targetDeltaY) {
                return tick;
            }
        }

        return LAUNCH_MAX_SIM_TICKS;
    }

    private static double geometricSum(double ratio, int terms) {
        if (terms <= 0) {
            return 0.0d;
        }
        if (Math.abs(1.0d - ratio) < 1.0e-6d) {
            return terms;
        }
        return (1.0d - Math.pow(ratio, terms)) / (1.0d - ratio);
    }

    private static void placeWall(ServerLevel level, ArenaBattleSession session, ArenaPickupVariantDefinition variant) {
        if (variant.wallBlock() == null || variant.wallBlocks().isEmpty()) {
            return;
        }

        Block wallBlock = ForgeRegistries.BLOCKS.getValue(variant.wallBlock());
        if (wallBlock == null || wallBlock == Blocks.AIR) {
            return;
        }

        BlockState placedState = wallBlock.defaultBlockState();
        Map<BlockPos, BlockState> replacedBlocks = new HashMap<>();
        for (BlockPos localBlock : variant.wallBlocks()) {
            BlockPos worldBlock = session.arenaDefinition().blockAt(session.slotOrigin(), localBlock);
            replacedBlocks.put(worldBlock.immutable(), level.getBlockState(worldBlock));
            level.setBlock(worldBlock, placedState, Block.UPDATE_ALL);
        }

        session.activeWalls().add(new ArenaBattleSession.ArenaWallRuntime(replacedBlocks, placedState, variant.durationTicks()));
    }

    private static void restoreWall(ServerLevel level, ArenaBattleSession.ArenaWallRuntime wall) {
        for (Map.Entry<BlockPos, BlockState> entry : wall.replacedBlocks().entrySet()) {
            level.setBlock(entry.getKey(), entry.getValue(), Block.UPDATE_ALL);
        }
    }

    private static void cleanupSessionRuntime(ServerLevel level, ArenaBattleSession session) {
        purgePickupVisualsForSlot(level, session.slotIndex(), session.slotOrigin());

        for (ArenaBattleSession.ArenaPickupSpawnerRuntime spawner : session.pickupSpawners()) {
            ArenaBattleSession.ActiveArenaPickup activePickup = spawner.activePickup();
            if (activePickup != null) {
                Entity entity = level.getEntity(activePickup.entityId());
                if (entity != null) {
                    entity.discard();
                }
                spawner.clearActivePickup();
            }
        }

        for (ArenaBattleSession.ArenaWallRuntime wall : List.copyOf(session.activeWalls())) {
            restoreWall(level, wall);
        }
        session.activeWalls().clear();

        for (UUID participantId : List.copyOf(session.activeLaunches().keySet())) {
            Entity entity = level.getEntity(participantId);
            if (entity instanceof LivingEntity livingEntity) {
                clearLaunchLock(livingEntity);
            }
        }
        session.activeLaunches().clear();
    }

    private static void purgePickupVisualsForSlot(ServerLevel level, int slotIndex, BlockPos slotOrigin) {
        AABB slotBounds = new AABB(
                slotOrigin.getX() - 16,
                slotOrigin.getY() - 32,
                slotOrigin.getZ() - 16,
                slotOrigin.getX() + SLOT_SPACING + 16,
                slotOrigin.getY() + 128,
                slotOrigin.getZ() + SLOT_SPACING + 16
        );

        level.getEntitiesOfClass(ItemEntity.class, slotBounds, itemEntity ->
                        itemEntity.getPersistentData().getBoolean(PICKUP_TAG_IS_ARENA_PICKUP)
                                && itemEntity.getPersistentData().getInt(PICKUP_TAG_SLOT_INDEX) == slotIndex)
                .forEach(Entity::discard);
    }

    private record LoadedArenaTemplate(ArenaTemplateDefinition definition, StructureTemplate template) {
    }

    private record PlacedArenaTemplate(ArenaTemplateDefinition definition, StructureTemplate template, BlockPos origin) {
    }

    private record ArenaPlacement(BlockPos clearOrigin, Vec3i combinedSize, List<PlacedArenaTemplate> templates) {
    }

    private record BallisticLaunchSolution(Vec3 initialVelocity, int expectedFlightTicks) {
    }
}
