package bruhof.teenycraft.world.arena;

import bruhof.teenycraft.TeenyBalance;
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
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ArenaBattleManager {
    private static final Logger LOGGER = LogUtils.getLogger();
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
                                                 ResourceLocation requestedArenaId) {
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

        StructureTemplate template = loadTemplate(player, arena.templateId());
        if (template == null || isEmpty(template.getSize())) {
            throw new IllegalStateException("Arena template is missing or empty: " + arena.templateId());
        }

        BlockPos slotOrigin = getSlotOrigin(slotIndex);
        clearArenaSpace(teenyverse, slotOrigin, template.getSize(), arena.clearPadding());
        if (!placeArenaTemplate(teenyverse, slotOrigin, template)) {
            throw new IllegalStateException("Failed to place arena template: " + arena.templateId());
        }

        Vec3 returnPosition = player.position();
        ResourceKey<Level> returnDimension = player.level().dimension();
        float returnYaw = player.getYRot();
        float returnPitch = player.getXRot();

        preparePlayerInventoryForBattleStart(player);

        Vec3 playerSpawn = arena.playerSpawnAt(slotOrigin);
        player.teleportTo(teenyverse, playerSpawn.x, playerSpawn.y, playerSpawn.z, java.util.Set.of(), arena.playerYaw(), arena.playerPitch());

        player.getCapability(BattleStateProvider.BATTLE_STATE).ifPresent(state -> {
            state.initializeBattle(playerTeam, player);
            state.refreshPlayerInventory(player);
        });

        EntityTeenyDummy dummy = spawnOpponentDummy(teenyverse, slotOrigin, arena, opponentStacks);
        ArenaBattleSession session = new ArenaBattleSession(
                arenaId,
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
        ArenaDefinition arena = ArenaLoader.getArena(session.arenaId());

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

        unregisterSession(session);

        if (teenyverse != null && arena != null) {
            StructureTemplate template = loadTemplate(player, arena.templateId());
            if (template != null && !isEmpty(template.getSize())) {
                clearArenaSpace(teenyverse, session.slotOrigin(), template.getSize(), arena.clearPadding());
            }
        }
    }

    public static ResourceLocation getArenaIdForParticipant(Entity entity) {
        ArenaBattleSession session = SESSIONS_BY_PARTICIPANT.get(entity.getUUID());
        return session != null ? session.arenaId() : null;
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
                                                       List<ItemStack> opponentStacks) {
        List<BattleFigure> opponentFigures = new ArrayList<>();
        for (ItemStack stack : opponentStacks) {
            if (!stack.isEmpty()) {
                opponentFigures.add(new BattleFigure(stack));
            }
        }

        if (opponentFigures.isEmpty()) {
            throw new IllegalStateException("Opponent team is empty.");
        }

        EntityTeenyDummy dummy = ModEntities.TEENY_DUMMY.get().create(level);
        if (dummy == null) {
            throw new IllegalStateException("Failed to create Teeny Dummy.");
        }

        BattleFigure activeOpponent = opponentFigures.get(0);
        Vec3 spawn = arena.opponentSpawnAt(slotOrigin);
        dummy.moveTo(spawn.x, spawn.y, spawn.z, arena.opponentYaw(), arena.opponentPitch());
        if (dummy.getAttribute(Attributes.MAX_HEALTH) != null) {
            dummy.getAttribute(Attributes.MAX_HEALTH).setBaseValue(activeOpponent.getMaxHp());
        }
        dummy.setHealth(activeOpponent.getCurrentHp());
        dummy.setCustomName(net.minecraft.network.chat.Component.literal("§c" + activeOpponent.getNickname()));
        dummy.setCustomNameVisible(true);
        level.addFreshEntity(dummy);

        dummy.getCapability(BattleStateProvider.BATTLE_STATE).ifPresent(bossState -> {
            bossState.setBattling(true);
            bossState.getTeam().clear();
            bossState.getTeam().addAll(opponentFigures);
            if (bossState instanceof BattleState concreteBossState) {
                concreteBossState.setOwnerEntity(dummy);
            }
            bossState.setActiveFigure(0);
        });

        return dummy;
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
}
