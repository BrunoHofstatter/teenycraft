package bruhof.teenycraft.client;

import bruhof.teenycraft.TeenyCraft;
import bruhof.teenycraft.battle.presentation.BattleHudParticipantSnapshot;
import bruhof.teenycraft.battle.presentation.BattleHudSnapshot;
import bruhof.teenycraft.capability.TitanManagerProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.List;

public class BattleOverlay implements IGuiOverlay {
    public static final BattleOverlay INSTANCE = new BattleOverlay();

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        BattleHudSnapshot snapshot = ClientBattleData.getSnapshot();
        if (!snapshot.isBattling()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        renderHUDSide(guiGraphics, mc, 10, 10, snapshot.player(), snapshot.enemy(), false);
        renderHUDSide(guiGraphics, mc, screenWidth - 150, 10, snapshot.enemy(), snapshot.player(), true);
    }

    private void renderHUDSide(
            GuiGraphics guiGraphics,
            Minecraft mc,
            int x,
            int y,
            BattleHudParticipantSnapshot side,
            BattleHudParticipantSnapshot opposingSide,
            boolean isEnemy
    ) {
        if (isEnemy && !side.isVisibleOpponent()) {
            return;
        }

        int itemSize = 32;
        int hpBarWidth = 100;
        int totalWidth = itemSize + 8 + hpBarWidth;

        ItemStack activeStack = resolveActiveStack(mc, side, isEnemy);
        if (!activeStack.isEmpty()) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(x, y, 0);
            guiGraphics.pose().scale(2.0f, 2.0f, 1.0f);
            guiGraphics.renderItem(activeStack, 0, 0);
            guiGraphics.pose().popPose();
        }

        renderHpBar(guiGraphics, mc, x, y, hpBarWidth, side, isEnemy);
        int manaY = renderManaBar(guiGraphics, mc, x, y, totalWidth, side, isEnemy);
        renderAbilityIcons(guiGraphics, mc, x, manaY, totalWidth, side, opposingSide, isEnemy);
        int batteryY = renderBatteryBar(guiGraphics, x, manaY, totalWidth, side);
        int benchY = renderBench(guiGraphics, mc, x, batteryY + 12, side, isEnemy);
        renderEffects(guiGraphics, mc, x, benchY + 4, side.effects(), isEnemy);
    }

    private ItemStack resolveActiveStack(Minecraft mc, BattleHudParticipantSnapshot side, boolean isEnemy) {
        if (isEnemy) {
            return createTempFigureStack(side.activeFigureId());
        }

        return mc.player.getCapability(TitanManagerProvider.TITAN_MANAGER)
                .resolve()
                .map(manager -> manager.getTeamStack(side.activeFigureIndex()))
                .orElse(ItemStack.EMPTY);
    }

    private void renderHpBar(GuiGraphics guiGraphics, Minecraft mc, int x, int y, int hpBarWidth, BattleHudParticipantSnapshot side, boolean isEnemy) {
        int barX = x + 40;
        int barY = y + 12;
        guiGraphics.fill(barX, barY, barX + hpBarWidth, barY + 8, 0xFF555555);
        float hpPct = side.maxHp() > 0 ? (float) side.currentHp() / side.maxHp() : 0.0f;
        guiGraphics.fill(barX, barY, barX + (int) (hpBarWidth * hpPct), barY + 8, getHPColor(hpPct));
        guiGraphics.drawString(mc.font, side.currentHp() + "/" + side.maxHp(), barX + 2, barY, 0x000000, false);
        guiGraphics.drawString(mc.font, (isEnemy ? "\u00A7c\u00A7l" : "\u00A7l") + side.name(), barX, y, 0xFFFFFF);
    }

    private int renderManaBar(GuiGraphics guiGraphics, Minecraft mc, int x, int y, int totalWidth, BattleHudParticipantSnapshot side, boolean isEnemy) {
        int manaY = y + 36;
        guiGraphics.fill(x, manaY, x + totalWidth, manaY + 9, 0xFF555555);
        float manaPct = side.currentMana() / 100.0f;
        guiGraphics.fill(x, manaY, x + (int) (totalWidth * manaPct), manaY + 9, isEnemy ? 0xFFFF0000 : 0xFF00AAFF);
        guiGraphics.drawCenteredString(mc.font, String.format("%.0f/100", side.currentMana()), x + (totalWidth / 2), manaY + 1, 0xFFFFFF);
        return manaY;
    }

    private void renderAbilityIcons(
            GuiGraphics guiGraphics,
            Minecraft mc,
            int x,
            int manaY,
            int totalWidth,
            BattleHudParticipantSnapshot side,
            BattleHudParticipantSnapshot opposingSide,
            boolean isEnemy
    ) {
        List<String> abilityIds = side.abilityIds();
        List<String> abilityTiers = side.abilityTiers();
        List<Boolean> goldenStatus = side.abilityGolden();
        int[] cooldowns = side.cooldowns();
        int[] slotProgress = side.slotProgress();

        for (int i = 0; i < abilityIds.size(); i++) {
            String abilityId = abilityIds.get(i);
            bruhof.teenycraft.util.AbilityLoader.AbilityData abilityData = bruhof.teenycraft.util.AbilityLoader.getAbility(abilityId);
            if (abilityData == null) {
                continue;
            }

            String tier = i < abilityTiers.size() ? abilityTiers.get(i) : "a";
            int cost = bruhof.teenycraft.TeenyBalance.getManaCost(i + 1, tier);
            int cooldown = i < cooldowns.length ? cooldowns[i] : 0;
            boolean isGolden = i < goldenStatus.size() && goldenStatus.get(i);
            float scale = (side.currentMana() >= cost && cooldown <= 0) ? 1.2f : 0.8f;
            int iconX = x + (int) ((cost / 100.0f) * totalWidth);
            int iconY = manaY + 4;

            ItemStack abilityStack = createTempAbilityStack(abilityId, i, isGolden);
            if (!abilityStack.isEmpty()) {
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(iconX - (8 * scale), iconY, 200);
                guiGraphics.pose().scale(scale, scale, 1.0f);
                guiGraphics.renderItem(abilityStack, 0, 0);
                guiGraphics.pose().popPose();
            }

            renderDamageLabel(guiGraphics, mc, iconX, iconY, scale, side, opposingSide, isEnemy, i, cost, abilityData);
            renderActivationProgress(guiGraphics, mc, iconX, iconY, slotProgress, i, abilityData);
        }
    }

    private void renderDamageLabel(
            GuiGraphics guiGraphics,
            Minecraft mc,
            int iconX,
            int iconY,
            float scale,
            BattleHudParticipantSnapshot side,
            BattleHudParticipantSnapshot opposingSide,
            boolean isEnemy,
            int slotIndex,
            int cost,
            bruhof.teenycraft.util.AbilityLoader.AbilityData abilityData
    ) {
        if (abilityData.damageTier <= 0 || "none".equalsIgnoreCase(abilityData.hitType)) {
            return;
        }

        int totalDamage;
        boolean isMine = abilityData.effectsOnOpponent.stream().anyMatch(effect -> "remote_mine".equals(effect.id));
        if (isMine && side.hasActiveMine(slotIndex)) {
            totalDamage = calculateRemoteMineDamage(slotIndex, cost, side, opposingSide.effects(), abilityData);
        } else {
            totalDamage = calculateDirectDamage(cost, side.basePower(), side.powerUp(), side.powerDown(), abilityData);
        }

        String damageText = String.valueOf(Math.max(0, totalDamage));
        int stringWidth = mc.font.width(damageText);
        guiGraphics.drawString(mc.font, damageText, iconX - (stringWidth / 2), iconY + (int) (16 * scale), 0xFFFFFF, true);
    }

    private int calculateRemoteMineDamage(
            int slotIndex,
            int cost,
            BattleHudParticipantSnapshot side,
            List<String> opposingEffects,
            bruhof.teenycraft.util.AbilityLoader.AbilityData abilityData
    ) {
        float paramMult = 1.0f;
        for (bruhof.teenycraft.util.AbilityLoader.EffectData effect : abilityData.effectsOnOpponent) {
            if ("remote_mine".equals(effect.id) && effect.params != null && !effect.params.isEmpty()) {
                paramMult = effect.params.get(0);
                break;
            }
        }

        float tierMult = bruhof.teenycraft.TeenyBalance.getDamageMultiplier(abilityData.damageTier);
        float maxSnapshot = (side.basePower() * cost * bruhof.teenycraft.TeenyBalance.BASE_DAMAGE_PERMANA
                * bruhof.teenycraft.TeenyBalance.REMOTE_MINE_DAMAGE_MULT * paramMult * tierMult)
                + side.powerUp() - side.powerDown();

        int stages = 0;
        for (String effectText : opposingEffects) {
            if (effectText.startsWith("remote_mine_" + slotIndex) && effectText.contains("Mag: ")) {
                try {
                    String magnitude = effectText.substring(effectText.indexOf("Mag: ") + 5, effectText.length() - 1);
                    stages = Integer.parseInt(magnitude);
                } catch (Exception ignored) {
                }
                break;
            }
        }

        float initial = maxSnapshot * bruhof.teenycraft.TeenyBalance.REMOTE_MINE_START_PCT;
        float pool = maxSnapshot - initial;
        float weight = pool / bruhof.teenycraft.TeenyBalance.REMOTE_MINE_STAGES;
        return Math.round(initial + (stages * weight));
    }

    private int calculateDirectDamage(int cost, int basePower, int powerUp, int powerDown, bruhof.teenycraft.util.AbilityLoader.AbilityData abilityData) {
        float tierMult = bruhof.teenycraft.TeenyBalance.getDamageMultiplier(abilityData.damageTier);
        float raw = basePower * cost * bruhof.teenycraft.TeenyBalance.BASE_DAMAGE_PERMANA * tierMult;

        for (bruhof.teenycraft.util.AbilityLoader.TraitData trait : abilityData.traits) {
            if ("activate".equals(trait.id)) {
                float requirement = trait.params.isEmpty() ? 2.0f : trait.params.get(0);
                raw *= (requirement * bruhof.teenycraft.TeenyBalance.ACTIVATE_DAMAGE_MULT);
            } else if ("charge_up".equals(trait.id)) {
                float seconds = trait.params.isEmpty() ? 1.0f : trait.params.get(0);
                raw *= (seconds * bruhof.teenycraft.TeenyBalance.CHARGE_UP_MULT_PER_SEC);
            }
        }

        return Math.round(raw + powerUp - powerDown);
    }

    private void renderActivationProgress(
            GuiGraphics guiGraphics,
            Minecraft mc,
            int iconX,
            int iconY,
            int[] slotProgress,
            int slotIndex,
            bruhof.teenycraft.util.AbilityLoader.AbilityData abilityData
    ) {
        int currentProgress = slotIndex < slotProgress.length ? slotProgress[slotIndex] : 0;
        float requiredCasts = 0;
        for (bruhof.teenycraft.util.AbilityLoader.TraitData trait : abilityData.traits) {
            if ("activate".equals(trait.id)) {
                requiredCasts = trait.params.isEmpty() ? 2.0f : trait.params.get(0);
                break;
            }
        }

        if (requiredCasts <= 0) {
            return;
        }

        String progressText = currentProgress + "/" + (int) requiredCasts;
        int progressWidth = mc.font.width(progressText);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(iconX - (progressWidth * 0.75f / 2), iconY - 8, 200);
        guiGraphics.pose().scale(0.75f, 0.75f, 1.0f);
        guiGraphics.drawString(mc.font, progressText, 0, 0, 0xFFFF00, true);
        guiGraphics.pose().popPose();
    }

    private int renderBatteryBar(GuiGraphics guiGraphics, int x, int manaY, int totalWidth, BattleHudParticipantSnapshot side) {
        int batteryY = manaY + 32;
        guiGraphics.fill(x, batteryY, x + totalWidth, batteryY + 4, 0xFF444444);
        float batteryPct = side.batteryCharge() / 100.0f;
        guiGraphics.fill(x, batteryY, x + (int) (totalWidth * batteryPct), batteryY + 4, 0xFFFFFF00);

        if (side.batterySpawnPct() != -1.0f) {
            int spawnX = x + (int) (totalWidth * side.batterySpawnPct());
            int spawnY = manaY - 8;
            guiGraphics.fill(spawnX - 4, spawnY, spawnX + 4, spawnY + 8, 0xFFFFFF00);
            guiGraphics.fill(spawnX - 3, spawnY + 1, spawnX + 3, spawnY + 7, 0xFFFF8800);
        }

        return batteryY;
    }

    private int renderBench(GuiGraphics guiGraphics, Minecraft mc, int x, int startY, BattleHudParticipantSnapshot side, boolean isEnemy) {
        int benchY = startY;
        List<Integer> benchIndices = side.benchIndices();
        List<String> benchInfo = side.benchInfo();
        List<String> benchFigureIds = side.benchFigureIds();

        for (int i = 0; i < benchIndices.size(); i++) {
            ItemStack benchStack = ItemStack.EMPTY;
            if (!isEnemy) {
                int benchIndex = benchIndices.get(i);
                benchStack = mc.player.getCapability(TitanManagerProvider.TITAN_MANAGER)
                        .resolve()
                        .map(manager -> manager.getTeamStack(benchIndex))
                        .orElse(ItemStack.EMPTY);
            } else if (i < benchFigureIds.size()) {
                benchStack = createTempFigureStack(benchFigureIds.get(i));
            }

            if (!benchStack.isEmpty()) {
                guiGraphics.renderItem(benchStack, x, benchY);
            }

            if (i < benchInfo.size()) {
                String hpText = benchInfo.get(i);
                float hpPct = parseBenchHpPct(hpText);
                guiGraphics.drawString(mc.font, hpText, x + 20, benchY + 4, getHPColor(hpPct));
            }

            benchY += 20;
        }

        return benchY;
    }

    private float parseBenchHpPct(String hpText) {
        try {
            String[] parts = hpText.split("/");
            return Float.parseFloat(parts[0]) / Float.parseFloat(parts[1]);
        } catch (Exception ignored) {
            return 1.0f;
        }
    }

    private void renderEffects(GuiGraphics guiGraphics, Minecraft mc, int x, int startY, List<String> effects, boolean isEnemy) {
        int effectY = startY;
        for (String effect : effects) {
            guiGraphics.drawString(mc.font, (isEnemy ? "\u00A7e" : "\u00A7d") + effect, x, effectY, 0xFFFFFF);
            effectY += 10;
        }
    }

    private int getHPColor(float pct) {
        if (pct > 0.5f) {
            return 0xFF00FF00;
        }
        if (pct > 0.2f) {
            return 0xFFFFFF00;
        }
        return 0xFFFF0000;
    }

    private ItemStack createTempFigureStack(String id) {
        if (id == null || id.equals("none")) {
            return ItemStack.EMPTY;
        }

        ResourceLocation rl = new ResourceLocation(TeenyCraft.MOD_ID, "figure_" + id);
        net.minecraft.world.item.Item item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(rl);
        if (item == null || item == net.minecraft.world.item.Items.AIR) {
            item = bruhof.teenycraft.item.ModItems.FIGURE_BASE.get();
        }

        ItemStack stack = new ItemStack(item);
        stack.getOrCreateTag().putString("FigureID", id);
        return stack;
    }

    private ItemStack createTempAbilityStack(String abilityId, int slot, boolean isGolden) {
        if (abilityId == null || abilityId.equals("none") || slot < 0 || slot >= 3) {
            return ItemStack.EMPTY;
        }

        net.minecraft.world.item.Item[] abilityItems = {
                bruhof.teenycraft.item.ModItems.ABILITY_1.get(),
                bruhof.teenycraft.item.ModItems.ABILITY_2.get(),
                bruhof.teenycraft.item.ModItems.ABILITY_3.get()
        };

        ItemStack stack = new ItemStack(abilityItems[slot]);
        var tag = stack.getOrCreateTag();
        tag.putString(bruhof.teenycraft.item.custom.battle.ItemAbility.TAG_ID, abilityId);
        tag.putBoolean(bruhof.teenycraft.item.custom.battle.ItemAbility.TAG_GOLDEN, isGolden);
        return stack;
    }
}
