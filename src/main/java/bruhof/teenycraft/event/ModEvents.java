package bruhof.teenycraft.event;

import bruhof.teenycraft.TeenyCraft;
import bruhof.teenycraft.capability.ITitanManager;
import bruhof.teenycraft.capability.TitanManagerProvider;
import bruhof.teenycraft.capability.BattleStateProvider;
import bruhof.teenycraft.capability.IBattleState;
import bruhof.teenycraft.item.custom.ItemFigure;
import bruhof.teenycraft.battle.BattleFigure;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraft.world.InteractionResult;
import bruhof.teenycraft.networking.PacketSyncBattleData;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import bruhof.teenycraft.entity.ModEntities;
import bruhof.teenycraft.item.custom.battle.ItemAbility;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import bruhof.teenycraft.battle.AbilityExecutor;
import bruhof.teenycraft.entity.custom.EntityTeenyDummy;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import bruhof.teenycraft.world.dimension.ModDimensions;
import net.minecraft.resources.ResourceLocation;
import bruhof.teenycraft.networking.ModMessages;
import bruhof.teenycraft.networking.PacketSyncTitanData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;

import bruhof.teenycraft.command.CommandTeeny;
import net.minecraftforge.event.RegisterCommandsEvent;

@Mod.EventBusSubscriber(modid = TeenyCraft.MOD_ID)
public class ModEvents {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandTeeny.register(event.getDispatcher(), event.getBuildContext());
    }

    @SubscribeEvent
    public static void onAttachCapabilitiesPlayer(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            if (!event.getObject().getCapability(TitanManagerProvider.TITAN_MANAGER).isPresent()) {
                event.addCapability(new ResourceLocation(TeenyCraft.MOD_ID, "titan_manager"), new TitanManagerProvider());
            }
        }
        
        if (event.getObject() instanceof LivingEntity) {
            if (!event.getObject().getCapability(BattleStateProvider.BATTLE_STATE).isPresent()) {
                event.addCapability(new ResourceLocation(TeenyCraft.MOD_ID, "battle_state"), new BattleStateProvider());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerCloned(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            event.getOriginal().reviveCaps();
        }

        event.getOriginal().getCapability(TitanManagerProvider.TITAN_MANAGER).ifPresent(oldStore -> {
            event.getEntity().getCapability(TitanManagerProvider.TITAN_MANAGER).ifPresent(newStore -> {
                newStore.copyFrom(oldStore);
            });
        });

        if (event.isWasDeath()) {
            event.getOriginal().invalidateCaps();
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(TitanManagerProvider.TITAN_MANAGER).ifPresent(handler -> {
                CompoundTag nbt = new CompoundTag();
                handler.saveNBTData(nbt);
                ModMessages.sendToPlayer(new PacketSyncTitanData(nbt), player);
            });
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(TitanManagerProvider.TITAN_MANAGER).ifPresent(handler -> {
                if (event.getTo() == ModDimensions.TEENYVERSE_KEY) {
                    handler.saveVanillaInventory(player);
                } else if (event.getFrom() == ModDimensions.TEENYVERSE_KEY) {
                    handler.restoreVanillaInventory(player);
                }

                CompoundTag nbt = new CompoundTag();
                handler.saveNBTData(nbt);
                ModMessages.sendToPlayer(new PacketSyncTitanData(nbt), player);
            });
        }
    }

    @SubscribeEvent
    public static void onPlayerJoinWorld(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(TitanManagerProvider.TITAN_MANAGER).ifPresent(handler -> {
                CompoundTag nbt = new CompoundTag();
                handler.saveNBTData(nbt);
                ModMessages.sendToPlayer(new PacketSyncTitanData(nbt), player);
            });
        }
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (!entity.level().isClientSide) {
            entity.getCapability(BattleStateProvider.BATTLE_STATE).ifPresent(state -> {
                boolean wasCharging = state.isCharging();
                state.tick();
                
                // Charge Completion Logic (Generic for NPCs and Players)
                if (wasCharging && !state.isCharging() && state.getChargeTicks() <= 0 && state.getPendingAbility() != null) {
                    AbilityExecutor.finishCharge(state, entity);
                }
            });
        }
    }
    
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.player.level().isClientSide()) {
            event.player.getCapability(BattleStateProvider.BATTLE_STATE).ifPresent(state -> {
                if (!state.isBattling()) return;

                // 1. Hand Lock Logic
                int lockSlot = state.getLockedSlot();
                if (lockSlot >= 0 && lockSlot < 9) {
                    if (event.player.getInventory().selected != lockSlot) {
                        event.player.getInventory().selected = lockSlot;
                        if (event.player instanceof ServerPlayer sp) {
                            sp.connection.send(new ClientboundSetCarriedItemPacket(lockSlot));
                        }
                    }
                }

                // 3. Sync
                if (event.player instanceof ServerPlayer serverPlayer) {
                    BattleFigure active = state.getActiveFigure();
                    
                    // Simple logic to find nearby battle participants for syncing opponent health
                    LivingEntity target = serverPlayer.level().getEntitiesOfClass(LivingEntity.class, serverPlayer.getBoundingBox().inflate(15), 
                        e -> e != serverPlayer && e.getCapability(BattleStateProvider.BATTLE_STATE).isPresent())
                        .stream().findFirst().orElse(null);
                    
                    IBattleState opponentState = (target != null) ? target.getCapability(BattleStateProvider.BATTLE_STATE).orElse(null) : null;
                    BattleFigure opponent = (opponentState != null) ? opponentState.getActiveFigure() : null;
                    
                    if (active != null) {
                        ModMessages.sendToPlayer(new PacketSyncBattleData(
                            true,
                            active.getNickname(),
                            state.getActiveFigureIndex(),
                            active.getCurrentHp(),
                            active.getMaxHp(),
                            state.getCurrentMana(),
                            state.getEffectList(),
                            state.getBenchInfoList(),
                            state.getBenchIndicesList(),
                            opponent != null ? opponent.getNickname() : "None",
                            opponent != null ? opponent.getCurrentHp() : 0,
                            opponent != null ? opponent.getMaxHp() : 100
                        ), serverPlayer);
                    }
                }
            });
        }
    }

    @SubscribeEvent
    public static void onPlayerAttackEntity(AttackEntityEvent event) {
        if (!event.getEntity().level().isClientSide() && event.getEntity() instanceof ServerPlayer player) {
            net.minecraft.world.item.ItemStack held = player.getMainHandItem();
            if (held.getItem() instanceof ItemAbility itemAbility) {
                player.getCapability(BattleStateProvider.BATTLE_STATE).ifPresent(battle -> {
                    if (battle.isBattling()) {
                        bruhof.teenycraft.battle.BattleFigure active = battle.getActiveFigure();
                        if (active != null) {
                            int slot = itemAbility.getSlotIndex();
                            if (player.getAttackStrengthScale(0.5f) >= 1.0f) {
                                AbilityExecutor.executeAttack(player, active, slot, event.getTarget());
                            }
                        }
                    }
                });
                event.setCanceled(true); 
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!event.getLevel().isClientSide() && event.getItemStack().getItem() instanceof ItemFigure) {
            event.getEntity().getCapability(BattleStateProvider.BATTLE_STATE).ifPresent(battle -> {
                if (battle.isBattling()) {
                    String clickedId = ItemFigure.getFigureID(event.getItemStack());
                    int targetIndex = -1;
                    var team = battle.getTeam();
                    for (int i = 0; i < team.size(); i++) {
                        if (team.get(i).getFigureId().equals(clickedId)) {
                            targetIndex = i;
                            break;
                        }
                    }

                    if (targetIndex != -1) {
                        battle.swapFigure(targetIndex, event.getEntity());
                        event.setCanceled(true);
                        event.setCancellationResult(InteractionResult.SUCCESS);
                    }
                }
            });
        }
    }

    @Mod.EventBusSubscriber(modid = TeenyCraft.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModBusEvents {
        @SubscribeEvent
        public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
            event.register(ITitanManager.class);
            event.register(IBattleState.class);
        }

        @SubscribeEvent
        public static void onRegisterAttributes(EntityAttributeCreationEvent event) {
            event.put(ModEntities.TEENY_DUMMY.get(), EntityTeenyDummy.createAttributes().build());
        }
    }
}
