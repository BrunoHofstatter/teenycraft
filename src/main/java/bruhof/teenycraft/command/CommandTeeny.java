package bruhof.teenycraft.command;

import bruhof.teenycraft.TeenyBalance;
import bruhof.teenycraft.item.ModItems;
import bruhof.teenycraft.entity.custom.EntityTeenyDummy;
import bruhof.teenycraft.entity.ModEntities;
import java.util.Collections;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import bruhof.teenycraft.networking.ModMessages;
import bruhof.teenycraft.networking.PacketSyncBattleData;
import bruhof.teenycraft.world.dimension.ModDimensions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import bruhof.teenycraft.item.custom.ItemFigure;
import bruhof.teenycraft.capability.TitanManagerProvider;
import bruhof.teenycraft.capability.BattleStateProvider;
import bruhof.teenycraft.battle.BattleFigure;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.minecraft.commands.CommandBuildContext;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.commands.SharedSuggestionProvider;
import bruhof.teenycraft.util.AbilityLoader;

public class CommandTeeny {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        dispatcher.register(Commands.literal("teeny")
            .requires(source -> source.hasPermission(2)) // OP Level 2
            
            // TP COMMAND
            .then(Commands.literal("tp")
                .executes(CommandTeeny::teleportDimension)
            )

            // BATTLE COMMANDS
            .then(Commands.literal("battle")
                .then(Commands.literal("start")
                    .executes(CommandTeeny::startBattle)
                )
                .then(Commands.literal("status")
                    .executes(CommandTeeny::battleStatus)
                )
                .then(Commands.literal("end")
                    .executes(CommandTeeny::endBattle)
                )
            )

            // FIGURE COMMANDS
            .then(Commands.literal("figure")
                
                // RESET
                .then(Commands.literal("reset")
                    .executes(CommandTeeny::resetFigure)
                )

                .then(Commands.literal("modify")
                    
                    // LEVEL
                    .then(Commands.literal("level")
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 20))
                            .executes(CommandTeeny::setLevel)
                        )
                    )

                    // ADD XP
                    .then(Commands.literal("add-xp")
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                            .executes(CommandTeeny::addXp)
                        )
                    )

                    // UPGRADES (e.g., HHHP)
                    .then(Commands.literal("upgrades")
                        .then(Commands.argument("pattern", StringArgumentType.string())
                            .executes(CommandTeeny::applyUpgrades)
                        )
                    )

                    // GOLDEN (Slot 1-3, true/false)
                    .then(Commands.literal("golden")
                        .then(Commands.argument("slot", IntegerArgumentType.integer(1, 3))
                            .then(Commands.argument("active", BoolArgumentType.bool())
                                .executes(CommandTeeny::setGolden)
                            )
                        )
                    )

                    // ABILITY ORDER (e.g., 312)
                    .then(Commands.literal("ability-order")
                        .then(Commands.argument("order", StringArgumentType.string())
                            .executes(CommandTeeny::setAbilityOrder)
                        )
                    )

                    // CHIP (By Item ID)
                    .then(Commands.literal("chip")
                        .then(Commands.argument("item", ItemArgument.item(context))
                            .executes(CommandTeeny::installChip)
                        )
                    )
                )
            )
            
            // DEBUG CAST
            .then(Commands.literal("cast")
                .then(Commands.literal("self")
                    .then(Commands.argument("ability", StringArgumentType.string())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(AbilityLoader.getAbilityIds(), builder))
                        .then(Commands.argument("mana", IntegerArgumentType.integer())
                            .then(Commands.argument("golden", BoolArgumentType.bool())
                                .executes(ctx -> castDebug(ctx, true))
                            )
                        )
                    )
                )
                .then(Commands.literal("opponent")
                    .then(Commands.argument("ability", StringArgumentType.string())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(AbilityLoader.getAbilityIds(), builder))
                        .then(Commands.argument("mana", IntegerArgumentType.integer())
                            .then(Commands.argument("golden", BoolArgumentType.bool())
                                .executes(ctx -> castDebug(ctx, false))
                            )
                        )
                    )
                )
            )
        );
    }

    private static int castDebug(CommandContext<CommandSourceStack> context, boolean isSelf) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String abilityId = StringArgumentType.getString(context, "ability");
        int manaCost = IntegerArgumentType.getInteger(context, "mana");
        boolean isGolden = BoolArgumentType.getBool(context, "golden");
        
        bruhof.teenycraft.util.AbilityLoader.AbilityData data = bruhof.teenycraft.util.AbilityLoader.getAbility(abilityId);
        if (data == null) {
            context.getSource().sendFailure(Component.literal("Ability not found: " + abilityId));
            return 0;
        }
        
        player.getCapability(BattleStateProvider.BATTLE_STATE).ifPresent(state -> {
            if (!state.isBattling()) {
                context.getSource().sendFailure(Component.literal("Not in battle!"));
                return;
            }
            
            BattleFigure active = state.getActiveFigure();
            if (active == null) return;
            
            // Execute Debug Cast
            // We need to expose a method in AbilityExecutor for this
            bruhof.teenycraft.battle.AbilityExecutor.executeDebugCast(state, player, active, data, manaCost, isGolden, isSelf);
            
            context.getSource().sendSuccess(() -> Component.literal("Debug Cast: " + abilityId), true);
        });
        
        return 1;
    }

    // ==========================================
    // DIMENSION HANDLERS
    // ==========================================
    
    private static void createPlatform(ServerLevel level) {
        BlockPos center = new BlockPos(0, 63, 0);
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                level.setBlock(center.offset(x, 0, z), Blocks.OBSIDIAN.defaultBlockState(), 3);
            }
        }
    }

    private static int teleportDimension(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ServerLevel currentLevel = player.serverLevel();
        
        if (currentLevel.dimension() == ModDimensions.TEENYVERSE_KEY) {
            // Go back to Overworld
            ServerLevel overworld = currentLevel.getServer().getLevel(Level.OVERWORLD);
            if (overworld != null) {
                player.teleportTo(overworld, player.getX(), player.getY(), player.getZ(), Set.of(), player.getYRot(), player.getXRot());
                context.getSource().sendSuccess(() -> Component.literal("Teleported to Overworld."), true);
            }
        } else {
            // Go to Teenyverse
            ServerLevel teenyverse = currentLevel.getServer().getLevel(ModDimensions.TEENYVERSE_KEY);
            if (teenyverse != null) {
                createPlatform(teenyverse);
                player.teleportTo(teenyverse, 0.5, 64, 0.5, Set.of(), player.getYRot(), player.getXRot());
                context.getSource().sendSuccess(() -> Component.literal("Teleported to Teenyverse (0, 64, 0)."), true);
            } else {
                context.getSource().sendFailure(Component.literal("Teenyverse dimension not found/loaded!"));
            }
        }
        return 1;
    }

    // ==========================================
    // BATTLE HANDLERS
    // ==========================================

    private static int startBattle(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        player.getCapability(TitanManagerProvider.TITAN_MANAGER).ifPresent(manager -> {
            List<ItemStack> team = new ArrayList<>();
            // Get slots 0, 1, 2
            for (int i = 0; i < 3; i++) {
                team.add(manager.getInventory().getStackInSlot(i));
            }

            // Check if team has at least one figure
            boolean hasFigure = team.stream().anyMatch(s -> !s.isEmpty());
            
            if (!hasFigure) {
                context.getSource().sendFailure(Component.literal("Cannot start battle: Team is empty! Add figures to Titan Manager slots 0-2."));
                return;
            }

            player.getCapability(BattleStateProvider.BATTLE_STATE).ifPresent(battle -> {
                // 1. Teleport if needed
                ServerLevel currentLevel = player.serverLevel();
                if (currentLevel.dimension() != ModDimensions.TEENYVERSE_KEY) {
                    ServerLevel teenyverse = currentLevel.getServer().getLevel(ModDimensions.TEENYVERSE_KEY);
                    if (teenyverse != null) {
                         createPlatform(teenyverse);
                         // Teleporting triggers the Vault logic via ModEvents
                         player.teleportTo(teenyverse, 0.5, 64, 0.5, Set.of(), player.getYRot(), player.getXRot());
                    }
                }

                // 2. Initialize Battle Logic (Player Team)
                battle.initializeBattle(team, player);
                
                // 3. Generate Boss Robin (Opponent)
                ItemStack bossStack = new ItemStack(ModItems.ROBIN.get());
                ItemFigure.initializeFigure(bossStack, "robin", "Boss Robin", "The Gatekeeper", "Titan", 
                        new ArrayList<>(), 0, TeenyBalance.BOSS_ROBIN_BASE_HP, TeenyBalance.BOSS_ROBIN_BASE_POWER, 
                        TeenyBalance.BOSS_ROBIN_BASE_DODGE, TeenyBalance.BOSS_ROBIN_BASE_LUCK, 
                        List.of("birdarang", "staff_slam", "smoke_bomb"), List.of("a","a","a"));
                
                ItemFigure.setLevel(bossStack, TeenyBalance.BOSS_ROBIN_LEVEL);
                ItemFigure.applyUpgrades(bossStack, TeenyBalance.BOSS_ROBIN_UPGRADES);
                
                BattleFigure bossFigure = new BattleFigure(bossStack);
                battle.setOpponentTeam(Collections.singletonList(bossFigure));
                
                // 4. Spawn Dummy (Opponent Entity)
                if (currentLevel.dimension() == ModDimensions.TEENYVERSE_KEY || true) { // Allow testing in overworld too
                    // Spawn at 0, 64, 0 if in Teenyverse, or near player if in overworld
                    double x = 0.5, y = 64, z = 0.5;
                    
                    EntityTeenyDummy dummy = ModEntities.TEENY_DUMMY.get().create(player.level());
                    if (dummy != null) {
                        dummy.setPos(x, y, z);
                        // Sync HP
                        dummy.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH)
                             .setBaseValue(bossFigure.getMaxHp());
                        dummy.setHealth(bossFigure.getCurrentHp());
                        dummy.setCustomName(Component.literal("§c" + bossFigure.getNickname()));
                        dummy.setCustomNameVisible(true);
                        
                        player.level().addFreshEntity(dummy);
                        battle.setOpponentEntityUUID(dummy.getUUID());
                    }
                }

                // 5. Refresh Inventory (Gives Abilities + Bench)
                battle.refreshPlayerInventory(player);

                context.getSource().sendSuccess(() -> Component.literal("Battle Started! Active: " + battle.getActiveFigure().getNickname()), true);
            });
        });

        return 1;
    }

    private static int battleStatus(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Player player = context.getSource().getPlayerOrException();
        
        player.getCapability(BattleStateProvider.BATTLE_STATE).ifPresent(battle -> {
            if (!battle.isBattling()) {
                context.getSource().sendSuccess(() -> Component.literal("Not currently in battle."), false);
                return;
            }

            BattleFigure active = battle.getActiveFigure();
            if (active != null) {
                String status = String.format("§aActive: §f%s §c[HP: %d/%d] §b[Mana: %.1f]", 
                    active.getNickname(), active.getCurrentHp(), active.getMaxHp(), battle.getCurrentMana());
                context.getSource().sendSuccess(() -> Component.literal(status), false);
                
                // Show cooldowns
                String cds = String.format("§7CDs: [%d] [%d] [%d]", 
                    active.getCooldown(0), active.getCooldown(1), active.getCooldown(2));
                context.getSource().sendSuccess(() -> Component.literal(cds), false);
            }
        });

        return 1;
    }

    private static int endBattle(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Player player = context.getSource().getPlayerOrException();
        player.getCapability(BattleStateProvider.BATTLE_STATE).ifPresent(battle -> {
            battle.endBattle();
            
            if (player instanceof ServerPlayer serverPlayer) {
                 ModMessages.sendToPlayer(new PacketSyncBattleData(false, "", 0, 0, 0, 0, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), "", 0, 0), serverPlayer);
                 
                 // Teleport Back
                 ServerLevel overworld = serverPlayer.serverLevel().getServer().getLevel(Level.OVERWORLD);
                 if (overworld != null) {
                     serverPlayer.teleportTo(overworld, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(), Set.of(), serverPlayer.getYRot(), serverPlayer.getXRot());
                 }
            }
            
            context.getSource().sendSuccess(() -> Component.literal("Battle Ended manually."), true);
        });
        return 1;
    }

    // ==========================================
    // FIGURE HANDLERS
    // ==========================================

    private static ItemStack getHeldFigure(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Player player = context.getSource().getPlayerOrException();
        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!(stack.getItem() instanceof ItemFigure)) {
            context.getSource().sendFailure(Component.literal("You must be holding a Figure!"));
            throw new CommandSyntaxException(null, Component.literal("Not holding a figure"));
        }
        return stack;
    }

    private static int resetFigure(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ItemStack stack = getHeldFigure(context);
        ItemFigure.resetToFactory(stack);
        context.getSource().sendSuccess(() -> Component.literal("Figure reset to factory settings."), true);
        return 1;
    }

    private static int setLevel(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ItemStack stack = getHeldFigure(context);
        int level = IntegerArgumentType.getInteger(context, "amount");
        ItemFigure.setLevel(stack, level);
        context.getSource().sendSuccess(() -> Component.literal("Set Figure Level to " + level), true);
        return 1;
    }

    private static int addXp(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ItemStack stack = getHeldFigure(context);
        int amount = IntegerArgumentType.getInteger(context, "amount");
        ItemFigure.addXp(stack, amount);
        context.getSource().sendSuccess(() -> Component.literal("Added " + amount + " XP to Figure."), true);
        return 1;
    }

    private static int applyUpgrades(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ItemStack stack = getHeldFigure(context);
        String pattern = StringArgumentType.getString(context, "pattern");
        ItemFigure.applyUpgrades(stack, pattern);
        context.getSource().sendSuccess(() -> Component.literal("Applied upgrades: " + pattern), true);
        return 1;
    }

    private static int setGolden(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ItemStack stack = getHeldFigure(context);
        int slot = IntegerArgumentType.getInteger(context, "slot");
        boolean active = BoolArgumentType.getBool(context, "active");
        ItemFigure.setGolden(stack, slot, active);
        String state = active ? "Golden" : "Normal";
        context.getSource().sendSuccess(() -> Component.literal("Set Ability " + slot + " to " + state), true);
        return 1;
    }

    private static int setAbilityOrder(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ItemStack stack = getHeldFigure(context);
        String order = StringArgumentType.getString(context, "order");
        ItemFigure.setAbilityOrderString(stack, order);
        context.getSource().sendSuccess(() -> Component.literal("Updated Ability Order: " + order), true);
        return 1;
    }

    private static int installChip(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ItemStack figure = getHeldFigure(context);
        ItemInput itemInput = ItemArgument.getItem(context, "item");
        ItemStack chip = itemInput.createItemStack(1, false);

        // Ideally check if item is a ChipItem, but for now we assume anything can be installed as data
        ItemFigure.installChip(figure, chip);
        context.getSource().sendSuccess(() -> Component.literal("Installed Chip: " + chip.getHoverName().getString()), true);
        return 1;
    }
}
