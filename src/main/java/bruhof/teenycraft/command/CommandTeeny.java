package bruhof.teenycraft.command;

import bruhof.teenycraft.TeenyBalance;
import bruhof.teenycraft.battle.BattleFigure;
import bruhof.teenycraft.battle.ai.BattleAiProfile;
import bruhof.teenycraft.capability.BattleStateProvider;
import bruhof.teenycraft.capability.TeenyCoinsProvider;
import bruhof.teenycraft.capability.TitanManagerProvider;
import bruhof.teenycraft.item.ModItems;
import bruhof.teenycraft.item.custom.ItemFigure;
import bruhof.teenycraft.networking.ModMessages;
import bruhof.teenycraft.networking.PacketSyncTeenyCoins;
import bruhof.teenycraft.util.AbilityLoader;
import bruhof.teenycraft.util.FigureLoader;
import bruhof.teenycraft.util.NPCFigureBuilder;
import bruhof.teenycraft.util.NPCTeamLoader;
import bruhof.teenycraft.world.arena.ArenaBattleManager;
import bruhof.teenycraft.world.arena.ArenaLoader;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CommandTeeny {
    private record OpponentSetup(List<ItemStack> stacks, BattleAiProfile aiProfile) {
    }


    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        dispatcher.register(Commands.literal("teeny")
                .then(Commands.literal("battle")
                        .then(Commands.literal("start")
                                .then(Commands.argument("npcId", ResourceLocationArgument.id())
                                        .suggests(CommandTeeny::suggestNPCTeams)
                                        .then(Commands.argument("arenaId", ResourceLocationArgument.id())
                                                .suggests(CommandTeeny::suggestArenas)
                                                .executes(ctx -> startBattle(
                                                        ctx,
                                                        ResourceLocationArgument.getId(ctx, "npcId").toString(),
                                                        ResourceLocationArgument.getId(ctx, "arenaId"))))
                                        .executes(ctx -> startBattle(ctx, ResourceLocationArgument.getId(ctx, "npcId").toString(), null)))
                                .then(Commands.literal("arena")
                                        .then(Commands.argument("arenaId", ResourceLocationArgument.id())
                                                .suggests(CommandTeeny::suggestArenas)
                                                .executes(ctx -> startBattle(ctx, null, ResourceLocationArgument.getId(ctx, "arenaId")))))
                                .executes(ctx -> startBattle(ctx, null, null)))
                        .then(Commands.literal("stop").executes(CommandTeeny::endBattle))
                        .then(Commands.literal("status").executes(CommandTeeny::battleStatus)))
                .then(Commands.literal("cast")
                        .then(Commands.argument("abilityId", StringArgumentType.string())
                                .suggests(CommandTeeny::suggestAbilities)
                                .then(Commands.argument("manaCost", IntegerArgumentType.integer(0, 100))
                                        .then(Commands.argument("isGolden", BoolArgumentType.bool())
                                                .then(Commands.argument("casterType", StringArgumentType.string())
                                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(List.of("self", "opponent"), builder))
                                                        .executes(CommandTeeny::debugCast))))))
                .then(Commands.literal("figure")
                        .then(Commands.literal("get")
                                .then(Commands.argument("figureId", StringArgumentType.string())
                                        .suggests(CommandTeeny::suggestFigures)
                                        .executes(CommandTeeny::getFigure)))
                        .then(Commands.literal("level")
                                .then(Commands.argument("value", IntegerArgumentType.integer(1, 20))
                                        .executes(CommandTeeny::setFigureLevel)))
                        .then(Commands.literal("upgrade")
                                .then(Commands.argument("code", StringArgumentType.string())
                                        .executes(CommandTeeny::applyFigureUpgrades)))
                        .then(Commands.literal("xp")
                                .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                        .executes(CommandTeeny::addFigureXp)))
                        .then(Commands.literal("order")
                                .then(Commands.argument("code", StringArgumentType.string())
                                        .executes(CommandTeeny::setFigureOrder)))
                        .then(Commands.literal("golden")
                                .then(Commands.argument("slot", IntegerArgumentType.integer(1, 3))
                                        .then(Commands.argument("active", BoolArgumentType.bool())
                                                .executes(CommandTeeny::setFigureGolden)))))
                .then(Commands.literal("coins")
                        .then(Commands.argument("amount", IntegerArgumentType.integer())
                                .executes(CommandTeeny::modifyCoins))));
    }

    private static CompletableFuture<Suggestions> suggestAbilities(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(AbilityLoader.getAbilityIds(), builder);
    }

    private static CompletableFuture<Suggestions> suggestFigures(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(FigureLoader.getLoadedFigureIds(), builder);
    }

    private static CompletableFuture<Suggestions> suggestNPCTeams(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(NPCTeamLoader.getLoadedTeamIds(), builder);
    }

    private static CompletableFuture<Suggestions> suggestArenas(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(ArenaLoader.getLoadedArenaIds().stream().map(ResourceLocation::toString), builder);
    }

    private static int debugCast(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String abilityId = StringArgumentType.getString(context, "abilityId");
        int manaCost = IntegerArgumentType.getInteger(context, "manaCost");
        boolean isGolden = BoolArgumentType.getBool(context, "isGolden");
        String casterType = StringArgumentType.getString(context, "casterType");

        AbilityLoader.AbilityData data = AbilityLoader.getAbility(abilityId);
        if (data == null) {
            context.getSource().sendFailure(Component.literal("Ability not found: " + abilityId));
            return 0;
        }

        BattleDebugContextResolver.Resolution resolution = BattleDebugContextResolver.resolve(player, casterType);
        LivingEntity caster = resolution.caster();
        LivingEntity enemy = resolution.enemy();

        if (caster == null) {
            context.getSource().sendFailure(Component.literal("Could not find a valid " + casterType + " to cast as!"));
            return 0;
        }

        caster.getCapability(BattleStateProvider.BATTLE_STATE).ifPresent(state -> {
            BattleFigure figure = state.getActiveFigure();
            if (figure == null) {
                context.getSource().sendFailure(Component.literal("Caster figure not found in BattleState!"));
                return;
            }
            bruhof.teenycraft.battle.AbilityExecutor.executeDebugCast(state, caster, figure, data, manaCost, isGolden, enemy);
        });

        return 1;
    }

    private static int getFigure(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String figureId = StringArgumentType.getString(context, "figureId");
        ItemStack stack = FigureLoader.getFigureStack(figureId);
        if (stack.isEmpty()) {
            context.getSource().sendFailure(Component.literal("Figure not found: " + figureId));
            return 0;
        }
        player.getInventory().add(stack);
        return 1;
    }

    private static int setFigureLevel(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        int value = IntegerArgumentType.getInteger(context, "value");
        ItemStack held = player.getMainHandItem();
        if (held.getItem() instanceof ItemFigure) {
            ItemFigure.setLevel(held, value);
            context.getSource().sendSuccess(() -> Component.literal("Level set to " + value), true);
        } else {
            context.getSource().sendFailure(Component.literal("Must be holding a figure!"));
        }
        return 1;
    }

    private static int addFigureXp(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        int amount = IntegerArgumentType.getInteger(context, "amount");
        ItemStack held = player.getMainHandItem();
        if (held.getItem() instanceof ItemFigure) {
            ItemFigure.XpGainResult result = ItemFigure.addXp(held, amount);
            context.getSource().sendSuccess(() -> Component.literal(
                    "Added " + amount + " XP. Level " + result.newLevel()
                            + ", XP " + result.remainingXp()
                            + ", pending upgrades " + result.pendingUpgradePoints()), true);
        } else {
            context.getSource().sendFailure(Component.literal("Must be holding a figure!"));
        }
        return 1;
    }

    private static int setFigureOrder(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String code = StringArgumentType.getString(context, "code");
        ItemStack held = player.getMainHandItem();
        if (held.getItem() instanceof ItemFigure) {
            ItemFigure.setAbilityOrderString(held, code);
            context.getSource().sendSuccess(() -> Component.literal("Ability order set to " + code), true);
        } else {
            context.getSource().sendFailure(Component.literal("Must be holding a figure!"));
        }
        return 1;
    }

    private static int setFigureGolden(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        int slot = IntegerArgumentType.getInteger(context, "slot");
        boolean active = BoolArgumentType.getBool(context, "active");
        ItemStack held = player.getMainHandItem();
        if (held.getItem() instanceof ItemFigure) {
            ItemFigure.setGolden(held, slot, active);
            context.getSource().sendSuccess(() -> Component.literal("Slot " + slot + " golden status: " + active), true);
        } else {
            context.getSource().sendFailure(Component.literal("Must be holding a figure!"));
        }
        return 1;
    }

    private static int applyFigureUpgrades(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String code = StringArgumentType.getString(context, "code");
        ItemStack held = player.getMainHandItem();
        if (held.getItem() instanceof ItemFigure) {
            ItemFigure.applyUpgrades(held, code);
            context.getSource().sendSuccess(() -> Component.literal("Upgrades applied: " + code), true);
        } else {
            context.getSource().sendFailure(Component.literal("Must be holding a figure!"));
        }
        return 1;
    }

    private static int modifyCoins(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        int amount = IntegerArgumentType.getInteger(context, "amount");

        player.getCapability(TeenyCoinsProvider.TEENY_COINS).ifPresent(handler -> {
            handler.addCoins(amount);
            ModMessages.sendToPlayer(new PacketSyncTeenyCoins(handler.getCoins()), player);
            context.getSource().sendSuccess(() ->
                    Component.literal("Teeny Coins changed by " + amount + ". New total: " + handler.getCoins()), true);
        });

        return 1;
    }

    private static int startBattle(CommandContext<CommandSourceStack> context, String npcId, ResourceLocation arenaId) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        player.getCapability(TitanManagerProvider.TITAN_MANAGER).ifPresent(manager -> {
            List<ItemStack> playerTeam = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                playerTeam.add(manager.getTeamStack(i));
            }
            if (playerTeam.isEmpty() || playerTeam.get(0).isEmpty()) {
                context.getSource().sendFailure(Component.literal("You need at least one figure in your team!"));
                return;
            }

            OpponentSetup opponentSetup = buildOpponentSetup(context, npcId);
            if (opponentSetup.stacks().isEmpty() || opponentSetup.stacks().get(0).isEmpty()) {
                return;
            }

            try {
                var session = ArenaBattleManager.startBattle(player, playerTeam, opponentSetup.stacks(), arenaId, opponentSetup.aiProfile());
                BattleFigure activeOpponent = new BattleFigure(opponentSetup.stacks().get(0));
                context.getSource().sendSuccess(() ->
                        Component.literal("Battle Started vs " + activeOpponent.getNickname() + " in " + session.arenaId()), true);
            } catch (IllegalArgumentException | IllegalStateException e) {
                context.getSource().sendFailure(Component.literal(e.getMessage()));
            }
        });
        return 1;
    }

    private static OpponentSetup buildOpponentSetup(CommandContext<CommandSourceStack> context, String npcId) {
        List<ItemStack> opponentStacks = new ArrayList<>();
        BattleAiProfile aiProfile = BattleAiProfile.DEFAULT;
        if (npcId != null) {
            NPCTeamLoader.NPCTeamDefinition npcTeam = NPCTeamLoader.getTeamDefinition(npcId);
            if (npcTeam == null) {
                context.getSource().sendFailure(Component.literal("NPC Team not found: " + npcId));
                return new OpponentSetup(List.of(), BattleAiProfile.DEFAULT);
            }
            aiProfile = npcTeam.aiProfile;
            for (NPCFigureBuilder.NPCFigureData fd : npcTeam.figures) {
                opponentStacks.add(NPCFigureBuilder.build(fd));
            }
        } else {
            ItemStack bossStack = new ItemStack(ModItems.ROBIN.get());
            ItemFigure.initializeFigure(bossStack, "robin", "Boss Robin", "The Gatekeeper", "Titan",
                    new ArrayList<>(), 0, TeenyBalance.BOSS_ROBIN_BASE_HP, TeenyBalance.BOSS_ROBIN_BASE_POWER,
                    TeenyBalance.BOSS_ROBIN_BASE_DODGE, TeenyBalance.BOSS_ROBIN_BASE_LUCK,
                    List.of("birdarang", "staff_slam", "smoke_bomb"), List.of("a", "a", "a"));
            ItemFigure.setLevel(bossStack, TeenyBalance.BOSS_ROBIN_LEVEL);
            ItemFigure.applyUpgrades(bossStack, TeenyBalance.BOSS_ROBIN_UPGRADES);
            opponentStacks.add(bossStack);
        }

        if (opponentStacks.isEmpty() || opponentStacks.get(0).isEmpty()) {
            context.getSource().sendFailure(Component.literal("Failed to build opponent team!"));
            return new OpponentSetup(List.of(), aiProfile);
        }

        return new OpponentSetup(opponentStacks, aiProfile);
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
                String status = String.format("Â§aActive: Â§f%s Â§c[HP: %d/%d] Â§b[Mana: %.1f]",
                        active.getNickname(), active.getCurrentHp(), active.getMaxHp(), battle.getCurrentMana());
                context.getSource().sendSuccess(() -> Component.literal(status), false);
                ResourceLocation arenaId = ArenaBattleManager.getArenaIdForParticipant(player);
                if (arenaId != null) {
                    context.getSource().sendSuccess(() -> Component.literal("Â§7Arena: " + arenaId), false);
                }
                String cds = String.format("Â§7CDs: [%d] [%d] [%d]",
                        active.getCooldown(0), active.getCooldown(1), active.getCooldown(2));
                context.getSource().sendSuccess(() -> Component.literal(cds), false);
            }
        });
        return 1;
    }

    private static int endBattle(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Player player = context.getSource().getPlayerOrException();
        if (player instanceof ServerPlayer serverPlayer) {
            ArenaBattleManager.finishBattleForPlayer(serverPlayer);
            context.getSource().sendSuccess(() -> Component.literal("Battle Ended manually."), true);
        }
        return 1;
    }
}
