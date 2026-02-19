package bruhof.teenycraft.event;

import bruhof.teenycraft.TeenyCraft;
import bruhof.teenycraft.capability.ITitanManager;
import bruhof.teenycraft.capability.TitanManagerProvider;
import bruhof.teenycraft.capability.BattleStateProvider;
import bruhof.teenycraft.item.custom.ItemFigure;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraft.world.InteractionResult;
import bruhof.teenycraft.networking.PacketSyncBattleData;
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
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

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

        // Battle State generally resets on death, but if we want to persist it (e.g. keeping team info), we could copy.
        // For now, let's assume death ends the battle, so we don't copy the active battle state.
        // However, if we store persistent "last used team" there, we might want to copy.
        // Let's leave it empty for now, effectively resetting battle state on death.

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
            // TODO: Sync Battle State if necessary (often battle state is server-authoritative and only syncs events)
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(TitanManagerProvider.TITAN_MANAGER).ifPresent(handler -> {
                // VAULT LOGIC
                if (event.getTo() == ModDimensions.TEENYVERSE_KEY) {
                    handler.saveVanillaInventory(player);
                } else if (event.getFrom() == ModDimensions.TEENYVERSE_KEY) {
                    handler.restoreVanillaInventory(player);
                }

                // SYNC
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
    public static void onPlayerTick(net.minecraftforge.event.TickEvent.PlayerTickEvent event) {
        if (event.phase == net.minecraftforge.event.TickEvent.Phase.END && !event.player.level().isClientSide()) {
            event.player.getCapability(BattleStateProvider.BATTLE_STATE).ifPresent(state -> {
                state.tick();
                
                // SYNC
                if (state.isBattling() && event.player instanceof ServerPlayer serverPlayer) {
                    bruhof.teenycraft.battle.BattleFigure active = state.getActiveFigure();
                    bruhof.teenycraft.battle.BattleFigure opponent = state.getActiveOpponent();
                    
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
            // Check if holding ItemAbility
            net.minecraft.world.item.ItemStack held = player.getMainHandItem();
            if (held.getItem() instanceof ItemAbility itemAbility) {
                // Check Battle State
                player.getCapability(BattleStateProvider.BATTLE_STATE).ifPresent(battle -> {
                    if (battle.isBattling()) {
                        bruhof.teenycraft.battle.BattleFigure active = battle.getActiveFigure();
                        if (active != null) {
                            int slot = itemAbility.getSlotIndex();
                            
                            // Check Cooldown (Vanilla)
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
                    
                    // Find the index of this figure in the team
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
        }

        @SubscribeEvent
        public static void onRegisterAttributes(EntityAttributeCreationEvent event) {
            event.put(ModEntities.TEENY_DUMMY.get(), EntityTeenyDummy.createAttributes().build());
        }
    }
}
