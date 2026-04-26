package bruhof.teenycraft.world.arena;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ArenaBattleSession {
    private final ResourceLocation arenaId;
    private final ArenaDefinition arenaDefinition;
    private final int slotIndex;
    private final BlockPos slotOrigin;
    private final UUID playerId;
    private final UUID opponentId;
    private final ResourceKey<Level> returnDimension;
    private final Vec3 returnPosition;
    private final float returnYaw;
    private final float returnPitch;
    private final List<ArenaPickupSpawnerRuntime> pickupSpawners = new ArrayList<>();
    private final List<ArenaWallRuntime> activeWalls = new ArrayList<>();
    private final Map<UUID, ArenaLaunchRuntime> activeLaunches = new LinkedHashMap<>();

    public ArenaBattleSession(ResourceLocation arenaId,
                              ArenaDefinition arenaDefinition,
                              int slotIndex,
                              BlockPos slotOrigin,
                              UUID playerId,
                              UUID opponentId,
                              ResourceKey<Level> returnDimension,
                              Vec3 returnPosition,
                              float returnYaw,
                              float returnPitch) {
        this.arenaId = arenaId;
        this.arenaDefinition = arenaDefinition;
        this.slotIndex = slotIndex;
        this.slotOrigin = slotOrigin;
        this.playerId = playerId;
        this.opponentId = opponentId;
        this.returnDimension = returnDimension;
        this.returnPosition = returnPosition;
        this.returnYaw = returnYaw;
        this.returnPitch = returnPitch;

        for (ArenaPickupSpawnerDefinition definition : arenaDefinition.pickupSpawners()) {
            pickupSpawners.add(new ArenaPickupSpawnerRuntime(definition));
        }
    }

    public ResourceLocation arenaId() {
        return arenaId;
    }

    public ArenaDefinition arenaDefinition() {
        return arenaDefinition;
    }

    public int slotIndex() {
        return slotIndex;
    }

    public BlockPos slotOrigin() {
        return slotOrigin;
    }

    public UUID playerId() {
        return playerId;
    }

    public UUID opponentId() {
        return opponentId;
    }

    public ResourceKey<Level> returnDimension() {
        return returnDimension;
    }

    public Vec3 returnPosition() {
        return returnPosition;
    }

    public float returnYaw() {
        return returnYaw;
    }

    public float returnPitch() {
        return returnPitch;
    }

    public List<ArenaPickupSpawnerRuntime> pickupSpawners() {
        return pickupSpawners;
    }

    public List<ArenaWallRuntime> activeWalls() {
        return activeWalls;
    }

    public Map<UUID, ArenaLaunchRuntime> activeLaunches() {
        return activeLaunches;
    }

    public static final class ArenaPickupSpawnerRuntime {
        private final ArenaPickupSpawnerDefinition definition;
        private int ticksUntilNextSpawn;
        private int nextSpotIndex;
        private int nextVariantIndex;
        private ActiveArenaPickup activePickup;

        ArenaPickupSpawnerRuntime(ArenaPickupSpawnerDefinition definition) {
            this.definition = definition;
            this.ticksUntilNextSpawn = Math.max(0, definition.firstSpawnDelayTicks());
        }

        public ArenaPickupSpawnerDefinition definition() {
            return definition;
        }

        public int ticksUntilNextSpawn() {
            return ticksUntilNextSpawn;
        }

        public void setTicksUntilNextSpawn(int ticksUntilNextSpawn) {
            this.ticksUntilNextSpawn = Math.max(0, ticksUntilNextSpawn);
        }

        public void tickSpawnDelay() {
            if (ticksUntilNextSpawn > 0) {
                ticksUntilNextSpawn--;
            }
        }

        public int nextSpotIndex() {
            return nextSpotIndex;
        }

        public void setNextSpotIndex(int nextSpotIndex) {
            this.nextSpotIndex = nextSpotIndex;
        }

        public int nextVariantIndex() {
            return nextVariantIndex;
        }

        public void setNextVariantIndex(int nextVariantIndex) {
            this.nextVariantIndex = nextVariantIndex;
        }

        public ActiveArenaPickup activePickup() {
            return activePickup;
        }

        public void setActivePickup(ActiveArenaPickup activePickup) {
            this.activePickup = activePickup;
        }

        public void clearActivePickup() {
            this.activePickup = null;
        }
    }

    public record ActiveArenaPickup(UUID entityId,
                                    Vec3 worldPosition,
                                    ArenaPickupVariantDefinition variant,
                                    int spotIndex,
                                    int variantIndex) {
    }

    public static final class ArenaWallRuntime {
        private final Map<BlockPos, BlockState> replacedBlocks;
        private final BlockState placedState;
        private int ticksRemaining;

        ArenaWallRuntime(Map<BlockPos, BlockState> replacedBlocks, BlockState placedState, int ticksRemaining) {
            this.replacedBlocks = replacedBlocks;
            this.placedState = placedState;
            this.ticksRemaining = ticksRemaining;
        }

        public Map<BlockPos, BlockState> replacedBlocks() {
            return replacedBlocks;
        }

        public BlockState placedState() {
            return placedState;
        }

        public int ticksRemaining() {
            return ticksRemaining;
        }

        public void tick() {
            ticksRemaining--;
        }
    }

    public static final class ArenaLaunchRuntime {
        private final UUID participantId;
        private final Vec3 destination;
        private final float yaw;
        private final float pitch;
        private final int expectedFlightTicks;
        private final int timeoutTicks;
        private int elapsedTicks;

        ArenaLaunchRuntime(UUID participantId, Vec3 destination, float yaw, float pitch, int expectedFlightTicks, int timeoutTicks) {
            this.participantId = participantId;
            this.destination = destination;
            this.yaw = yaw;
            this.pitch = pitch;
            this.expectedFlightTicks = expectedFlightTicks;
            this.timeoutTicks = timeoutTicks;
        }

        public UUID participantId() {
            return participantId;
        }

        public Vec3 destination() {
            return destination;
        }

        public float yaw() {
            return yaw;
        }

        public float pitch() {
            return pitch;
        }

        public int expectedFlightTicks() {
            return expectedFlightTicks;
        }

        public int timeoutTicks() {
            return timeoutTicks;
        }

        public int elapsedTicks() {
            return elapsedTicks;
        }

        public void tick() {
            elapsedTicks++;
        }
    }
}
