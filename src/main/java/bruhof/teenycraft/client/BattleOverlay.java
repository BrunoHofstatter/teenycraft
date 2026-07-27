package bruhof.teenycraft.client;

import bruhof.teenycraft.TeenyBalance;
import bruhof.teenycraft.TeenyCraft;
import bruhof.teenycraft.battle.FigureClassType;
import bruhof.teenycraft.battle.effect.BattleEffect;
import bruhof.teenycraft.battle.effect.EffectRegistry;
import bruhof.teenycraft.battle.presentation.BattleHudEffectSnapshot;
import bruhof.teenycraft.battle.presentation.BattleHudParticipantSnapshot;
import bruhof.teenycraft.battle.presentation.BattleHudSnapshot;
import bruhof.teenycraft.battle.presentation.BattleUiEventPayload;
import bruhof.teenycraft.capability.TitanManagerProvider;
import bruhof.teenycraft.item.ModItems;
import bruhof.teenycraft.item.custom.ItemFigure;
import bruhof.teenycraft.item.custom.battle.ItemAbility;
import bruhof.teenycraft.util.AbilityLoader;
import bruhof.teenycraft.util.FigureLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BattleOverlay implements IGuiOverlay {
    public static final BattleOverlay INSTANCE = new BattleOverlay();
    private static final int RAIL_WIDTH = 140;
    private static final int MIN_RAIL_HEIGHT = 164;
    private static final int BAR_WIDTH = 100;
    private static final int HP_BAR_HEIGHT = 8;
    private static final int MANA_BAR_HEIGHT = 10;
    private static final int BATTERY_BAR_HEIGHT = 8;
    private static final int EFFECTS_PER_ROW = 3;
    private static final int MAX_EFFECT_ROWS = 3;
    private static final int EFFECT_BADGE_WIDTH = 42;
    private static final int EFFECT_BADGE_HEIGHT = 18;
    private static final int EFFECT_BADGE_GAP = 4;
    private static final int BENCH_ROW_HEIGHT = 20;
    private static final int EFFECT_BLOCK_PADDING = 8;
    private static final int ACCESSORY_BLOCK_GAP = 37;
    private static final int BENCH_BLOCK_GAP = 26;
    private static final int CROSSHAIR_INDICATOR_SIZE = 6;
    private static final int CROSSHAIR_INDICATOR_GAP = 4;
    private static final int HP_TEXT_COLOR = 0xFFF5F6FA;
    private static final int BATTERY_TEXT_COLOR = 0xFFF5F6FA;
    private static final int DODGE_GHOST_BLUE = 0xFF63A8FF;
    private static final float ACCESSORY_READY_SCALE = 1.35f;
    private static final float ACCESSORY_IDLE_SCALE = 1.0f;
    private static final int TOFU_PREVIEW_Y = 140;
    private static final int TOFU_PREVIEW_ICON_SIZE = 16;

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        BattleHudSnapshot snapshot = ClientBattleData.getSnapshot();
        if (!snapshot.isBattling()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }

        RailLayout leftRail = new RailLayout(10, 10, RAIL_WIDTH, false);
        RailLayout rightRail = new RailLayout(screenWidth - RAIL_WIDTH - 10, 10, RAIL_WIDTH, true);
        renderRail(guiGraphics, mc, leftRail, snapshot.player(), snapshot.enemy());
        renderRail(guiGraphics, mc, rightRail, snapshot.enemy(), snapshot.player());
        renderCrosshairIndicator(guiGraphics, mc, screenWidth, screenHeight, snapshot);
        renderTransientFeedback(guiGraphics, mc, snapshot, leftRail, rightRail);
        renderTofuPreview(guiGraphics, mc, leftRail, snapshot.player());
        renderTofuPreview(guiGraphics, mc, rightRail, snapshot.enemy());
    }

    private void renderRail(GuiGraphics guiGraphics, Minecraft mc, RailLayout rail, BattleHudParticipantSnapshot side, BattleHudParticipantSnapshot opposingSide) {
        if (rail.mirrored() && !side.isVisibleOpponent()) {
            return;
        }

        int railHeight = getRailHeight(side);
        guiGraphics.fill(rail.x() - 4, rail.y() - 4, rail.x() + rail.width() + 4, rail.y() + railHeight, 0x2B121922);
        guiGraphics.fill(rail.x() - 3, rail.y() - 3, rail.x() + rail.width() + 3, rail.y() + railHeight - 1, 0x561B2433);

        ItemStack activeStack = resolveActiveStack(mc, side, rail.mirrored());
        renderActiveFigureBlock(guiGraphics, mc, rail, side, opposingSide, activeStack);
        int manaY = renderManaBlock(guiGraphics, mc, rail, side, opposingSide);
        int batteryY = renderAccessoryBlock(guiGraphics, mc, rail, side, manaY + ACCESSORY_BLOCK_GAP);
        int benchY = renderBenchBlock(guiGraphics, mc, rail, side, batteryY + BENCH_BLOCK_GAP);
        renderEffectBadges(guiGraphics, mc, rail, side.effects(), benchY + 6);
    }

    private void renderActiveFigureBlock(GuiGraphics guiGraphics, Minecraft mc, RailLayout rail, BattleHudParticipantSnapshot side, BattleHudParticipantSnapshot opposingSide, ItemStack activeStack) {
        int x = rail.x();
        int y = rail.y();
        int barX = x + 40;
        int barY = y + 12;
        if (!activeStack.isEmpty()) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(x, y, 0);
            guiGraphics.pose().scale(2.0f, 2.0f, 1.0f);
            guiGraphics.renderItem(activeStack, 0, 0);
            guiGraphics.pose().popPose();
        }

        guiGraphics.drawString(mc.font, side.name(), barX, y, 0xFFF5F6FA, false);
        renderClassIndicator(guiGraphics, mc, activeStack, resolveActiveStack(mc, opposingSide, !rail.mirrored()), barX + 70, y + 1);
        guiGraphics.fill(barX, barY, barX + BAR_WIDTH, barY + HP_BAR_HEIGHT, 0xFF404A59);
        float hpPct = side.maxHp() > 0 ? side.currentHp() / (float) side.maxHp() : 0.0f;
        guiGraphics.fill(barX, barY, barX + (int) (BAR_WIDTH * hpPct), barY + HP_BAR_HEIGHT, getHpColor(hpPct));
        guiGraphics.drawCenteredString(mc.font, side.currentHp() + "/" + side.maxHp(), barX + (BAR_WIDTH / 2), barY, HP_TEXT_COLOR);
    }

    private int renderManaBlock(GuiGraphics guiGraphics, Minecraft mc, RailLayout rail, BattleHudParticipantSnapshot side, BattleHudParticipantSnapshot opposingSide) {
        int x = rail.x();
        int manaY = rail.y() + 42;
        int fillEnd = x + (int) (rail.width() * Mth.clamp(side.currentMana() / 100.0f, 0.0f, 1.0f));
        guiGraphics.fill(x, manaY, x + rail.width(), manaY + MANA_BAR_HEIGHT, 0xFF2A313C);
        int manaColor = side.blueChannelTicksRemaining() > 0 ? 0xFF123F73 : (rail.mirrored() ? 0xFFB44444 : 0xFF2F8FFF);
        guiGraphics.fill(x, manaY, fillEnd, manaY + MANA_BAR_HEIGHT, manaColor);
        if (side.batterySpawnPct() >= 0.0f) {
            int spawnX = x + (int) (rail.width() * side.batterySpawnPct());
            guiGraphics.fill(spawnX - 2, manaY - 2, spawnX + 2, manaY + MANA_BAR_HEIGHT + 2, 0xFFE77817);
        }
        renderChargeOverlay(guiGraphics, rail, side, manaY);
        guiGraphics.drawCenteredString(mc.font, String.format(Locale.ROOT, "%.0f/100", side.currentMana()), x + (rail.width() / 2), manaY + 1, 0xFFF5F6FA);

        List<String> abilityIds = side.abilityIds();
        List<String> abilityTiers = side.abilityTiers();
        List<Boolean> goldenStatus = side.abilityGolden();
        int[] cooldowns = side.cooldowns();
        int[] slotProgress = side.slotProgress();
        for (int i = 0; i < abilityIds.size(); i++) {
            String abilityId = abilityIds.get(i);
            AbilityLoader.AbilityData abilityData = AbilityLoader.getAbility(abilityId);
            if (abilityData == null) {
                continue;
            }

            String tier = i < abilityTiers.size() ? abilityTiers.get(i) : "a";
            int actualCost = TeenyBalance.getManaCost(i + 1, tier);
            int effectiveCost = TeenyBalance.getEffectiveManaCost(i + 1, tier);
            boolean isGolden = i < goldenStatus.size() && goldenStatus.get(i);
            int cooldown = i < cooldowns.length ? cooldowns[i] : 0;
            boolean available = side.currentMana() >= actualCost && cooldown <= 0 && side.waffleBlockedSlot() != i;
            float scale = available ? 1.15f : 0.85f;
            int iconX = x + (int) ((actualCost / 100.0f) * rail.width());
            int iconY = manaY + 3;
            renderAbilityIcon(guiGraphics, side, abilityData, i, isGolden, iconX, iconY, scale);
            renderAbilityDamage(guiGraphics, mc, side, opposingSide, abilityData, i, effectiveCost, iconX, iconY, scale);
            renderActivateCounter(guiGraphics, mc, slotProgress, i, abilityData, iconX, iconY);
            renderCooldownText(guiGraphics, mc, cooldown, iconX, iconY);
        }

        return manaY;
    }

    private void renderAbilityIcon(GuiGraphics guiGraphics, BattleHudParticipantSnapshot side, AbilityLoader.AbilityData abilityData, int slotIndex, boolean isGolden, int iconX, int iconY, float scale) {
        if (side.waffleBlockedSlot() == slotIndex) {
            float wafflePct = side.waffleTicksRemaining() <= 0 ? 0.0f : Mth.clamp(side.waffleTicksRemaining() / 60.0f, 0.15f, 1.0f);
            int waffleSize = Math.max(10, (int) (16 * wafflePct));
            int alpha = (int) (255 * wafflePct);
            int boxColor = withAlpha(0xFFD58A29, alpha);
            int edgeColor = withAlpha(0xFF7E4A12, alpha);
            guiGraphics.fill(iconX - (waffleSize / 2), iconY - 1, iconX + (waffleSize / 2), iconY - 1 + waffleSize, boxColor);
            guiGraphics.fill(iconX - (waffleSize / 2), iconY - 1, iconX + (waffleSize / 2), iconY, edgeColor);
            drawCenteredScaledText(guiGraphics, Minecraft.getInstance().font, "W", iconX, iconY + 4, 1.0f, withAlpha(0xFF2B1908, alpha), true);
            return;
        }

        ItemStack abilityStack = createTempAbilityStack(abilityData.id, slotIndex, isGolden);
        if (!abilityStack.isEmpty()) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(iconX - (8 * scale), iconY, 200);
            guiGraphics.pose().scale(scale, scale, 1.0f);
            guiGraphics.renderItem(abilityStack, 0, 0);
            guiGraphics.pose().popPose();
        }
    }

    private void renderAbilityDamage(GuiGraphics guiGraphics, Minecraft mc, BattleHudParticipantSnapshot side, BattleHudParticipantSnapshot opposingSide, AbilityLoader.AbilityData abilityData, int slotIndex, int cost, int iconX, int iconY, float scale) {
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
        guiGraphics.drawString(mc.font, damageText, iconX - (mc.font.width(damageText) / 2), iconY + (int) (16 * scale), 0xFFF5F6FA, true);
    }

    private void renderActivateCounter(GuiGraphics guiGraphics, Minecraft mc, int[] slotProgress, int slotIndex, AbilityLoader.AbilityData abilityData, int iconX, int iconY) {
        int currentProgress = slotIndex < slotProgress.length ? slotProgress[slotIndex] : 0;
        float requiredCasts = 0;
        for (AbilityLoader.TraitData trait : abilityData.traits) {
            if ("activate".equals(trait.id)) {
                requiredCasts = trait.params.isEmpty() ? 2.0f : trait.params.get(0);
                break;
            }
        }
        if (requiredCasts > 0) {
            drawCenteredScaledText(guiGraphics, mc.font, currentProgress + "/" + (int) requiredCasts, iconX, iconY - 5, 0.75f, 0xFFF0D25C, true);
        }
    }

    private void renderCooldownText(GuiGraphics guiGraphics, Minecraft mc, int cooldown, int iconX, int iconY) {
        if (cooldown > 0) {
            drawCenteredScaledText(guiGraphics, mc.font, String.format(Locale.ROOT, "%.1f", cooldown / 20.0f), iconX, iconY + 17, 0.75f, 0xFFB2BDD0, true);
        }
    }

    private void renderChargeOverlay(GuiGraphics guiGraphics, RailLayout rail, BattleHudParticipantSnapshot side, int manaY) {
        if (side.blueChannelTicksRemaining() > 0 || side.chargeSlot() < 0 || side.chargeTotalTicks() <= 0) {
            return;
        }
        List<String> abilityTiers = side.abilityTiers();
        int slot = side.chargeSlot();
        String tier = slot < abilityTiers.size() ? abilityTiers.get(slot) : "a";
        int cost = TeenyBalance.getManaCost(slot + 1, tier);
        float progress = 1.0f - (side.chargeTicksRemaining() / (float) side.chargeTotalTicks());
        int reserveEnd = rail.x() + (int) (rail.width() * (cost / 100.0f) * Mth.clamp(progress, 0.0f, 1.0f));
        guiGraphics.fill(rail.x(), manaY, reserveEnd, manaY + MANA_BAR_HEIGHT, 0xCC8E45FF);
    }

    private int renderAccessoryBlock(GuiGraphics guiGraphics, Minecraft mc, RailLayout rail, BattleHudParticipantSnapshot side, int batteryY) {
        int x = rail.x();
        guiGraphics.fill(x, batteryY, x + rail.width(), batteryY + BATTERY_BAR_HEIGHT, 0xFF2A313C);
        float batteryPct = Mth.clamp(side.batteryCharge() / 100.0f, 0.0f, 1.0f);
        boolean accessoryReady = side.hasAccessory() && side.batteryCharge() >= TeenyBalance.ACCESSORY_ACTIVATION_MIN_CHARGE;
        int fillColor = side.accessoryActive() ? 0xFFF0C53A : (accessoryReady ? 0xFFD7B34D : 0xFF8A8F99);
        guiGraphics.fill(x, batteryY, x + (int) (rail.width() * batteryPct), batteryY + BATTERY_BAR_HEIGHT, fillColor);
        guiGraphics.drawCenteredString(mc.font, Math.round(side.batteryCharge() * 2.0f) + "/200", x + (rail.width() / 2), batteryY, BATTERY_TEXT_COLOR);
        ItemStack accessoryStack = createAccessoryStack(side.equippedAccessoryId(), side.accessoryActive());
        if (!accessoryStack.isEmpty()) {
            float scale = accessoryReady || side.accessoryActive() ? ACCESSORY_READY_SCALE : ACCESSORY_IDLE_SCALE;
            int baseX = x + (rail.width() / 2) - 8;
            int baseY = batteryY + 10;
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(baseX - ((TOFU_PREVIEW_ICON_SIZE * scale) - TOFU_PREVIEW_ICON_SIZE) / 2.0f, baseY, 220);
            guiGraphics.pose().scale(scale, scale, 1.0f);
            guiGraphics.renderItem(accessoryStack, 0, 0);
            guiGraphics.pose().popPose();
        }
        return batteryY;
    }

    private int renderBenchBlock(GuiGraphics guiGraphics, Minecraft mc, RailLayout rail, BattleHudParticipantSnapshot side, int startY) {
        int benchY = startY;
        List<Integer> benchIndices = side.benchIndices();
        List<String> benchInfo = side.benchInfo();
        List<String> benchFigureIds = side.benchFigureIds();
        for (int i = 0; i < benchIndices.size(); i++) {
            ItemStack benchStack = rail.mirrored() ? FigureLoader.getFigureStack(i < benchFigureIds.size() ? benchFigureIds.get(i) : "none") : resolveBenchStack(mc, benchIndices.get(i));
            if (!benchStack.isEmpty()) {
                guiGraphics.renderItem(benchStack, rail.x(), benchY);
            }
            if (i < benchInfo.size()) {
                String hpText = benchInfo.get(i);
                guiGraphics.drawString(mc.font, hpText, rail.x() + 20, benchY + 4, getHpColor(parseBenchHpPct(hpText)));
            }
            benchY += BENCH_ROW_HEIGHT;
        }
        return benchY;
    }

    private void renderEffectBadges(GuiGraphics guiGraphics, Minecraft mc, RailLayout rail, List<BattleHudEffectSnapshot> effects, int startY) {
        for (int i = 0; i < Math.min(effects.size(), EFFECTS_PER_ROW * MAX_EFFECT_ROWS); i++) {
            int row = i / EFFECTS_PER_ROW;
            int col = i % EFFECTS_PER_ROW;
            int badgeX = rail.x() + col * (EFFECT_BADGE_WIDTH + EFFECT_BADGE_GAP);
            int badgeY = startY + row * (EFFECT_BADGE_HEIGHT + EFFECT_BADGE_GAP);
            BattleHudEffectSnapshot effect = effects.get(i);
            guiGraphics.fill(badgeX, badgeY, badgeX + EFFECT_BADGE_WIDTH, badgeY + EFFECT_BADGE_HEIGHT, getEffectBadgeColor(effect.id()));
            guiGraphics.fill(badgeX, badgeY, badgeX + EFFECT_BADGE_WIDTH, badgeY + 1, 0xFF0F1720);
            drawCenteredScaledText(guiGraphics, mc.font, abbreviateEffect(effect.id()), badgeX + (EFFECT_BADGE_WIDTH / 2), badgeY + 2, 0.92f, 0xFFF7FAFC, false);
            String timerText = effect.infinite() ? String.valueOf(effect.magnitude()) : String.format(Locale.ROOT, "%.1f", effect.durationTicks() / 20.0f);
            drawCenteredScaledText(guiGraphics, mc.font, timerText, badgeX + (EFFECT_BADGE_WIDTH / 2), badgeY + 11, 0.74f, 0xFFE6EDF7, false);
        }
    }

    private void renderCrosshairIndicator(GuiGraphics guiGraphics, Minecraft mc, int screenWidth, int screenHeight, BattleHudSnapshot snapshot) {
        if (!(mc.player.getMainHandItem().getItem() instanceof ItemAbility itemAbility)) {
            return;
        }

        String abilityId = mc.player.getMainHandItem().getOrCreateTag().getString(ItemAbility.TAG_ID);
        AbilityLoader.AbilityData data = AbilityLoader.getAbility(abilityId);
        if (data == null) {
            return;
        }

        int x = screenWidth / 2 + 10;
        int y = screenHeight / 2 - 8;
        boolean showTargetIndicator = snapshot.enemy().isVisibleOpponent()
                && !"none".equalsIgnoreCase(data.hitType)
                && !"melee".equalsIgnoreCase(data.hitType);

        if (showTargetIndicator) {
            LivingEntity opponent = mc.level.getEntity(snapshot.enemy().entityId()) instanceof LivingEntity living ? living : null;
            if (opponent != null) {
                boolean inRange = isValidRangedTarget(mc, opponent, data);
                int color = inRange ? 0xCC57E06D : 0xCCDB4E4E;
                guiGraphics.fill(x, y, x + CROSSHAIR_INDICATOR_SIZE, y + CROSSHAIR_INDICATOR_SIZE, color);
                guiGraphics.fill(x + 1, y + 1, x + CROSSHAIR_INDICATOR_SIZE - 1, y + CROSSHAIR_INDICATOR_SIZE - 1,
                        inRange ? 0xFFDBFFDE : 0xFFFFE0E0);
            }
        }

        BattleHudParticipantSnapshot playerSide = snapshot.player();
        String tier = itemAbility.getSlotIndex() < playerSide.abilityTiers().size()
                ? playerSide.abilityTiers().get(itemAbility.getSlotIndex())
                : "a";
        int requiredMana = TeenyBalance.getManaCost(itemAbility.getSlotIndex() + 1, tier);
        boolean hasEnoughMana = playerSide.currentMana() >= requiredMana;
        int manaY = y + CROSSHAIR_INDICATOR_SIZE + CROSSHAIR_INDICATOR_GAP;
        guiGraphics.fill(x, manaY, x + CROSSHAIR_INDICATOR_SIZE, manaY + CROSSHAIR_INDICATOR_SIZE,
                hasEnoughMana ? 0xCC2F8FFF : 0xCC3D4652);
        guiGraphics.fill(x + 1, manaY + 1, x + CROSSHAIR_INDICATOR_SIZE - 1, manaY + CROSSHAIR_INDICATOR_SIZE - 1,
                hasEnoughMana ? 0xFFB8DAFF : 0xFF9AA3AD);
    }

    private void renderTransientFeedback(GuiGraphics guiGraphics, Minecraft mc, BattleHudSnapshot snapshot, RailLayout leftRail, RailLayout rightRail) {
        long now = net.minecraft.Util.getMillis();
        Map<String, Integer> anchorCounts = new HashMap<>();

        for (BattleUiFeedbackManager.ActiveEvent activeEvent : BattleUiFeedbackManager.getVisibleEvents()) {
            BattleUiEventPayload payload = activeEvent.payload();
            BattleHudParticipantSnapshot side = resolveEventSide(snapshot, payload.entityId());
            RailLayout rail = side == snapshot.player() ? leftRail : (side == snapshot.enemy() ? rightRail : null);
            if (side == null || rail == null) {
                continue;
            }

            String anchorKey = payload.entityId() + ":" + anchorFor(payload.type());
            int stackIndex = anchorCounts.getOrDefault(anchorKey, 0);
            anchorCounts.put(anchorKey, stackIndex + 1);

            long age = activeEvent.ageMs(now);
            float lifePct = Mth.clamp(age / (float) activeEvent.lifeMs(), 0.0f, 1.0f);
            float alphaPct = lifePct > 0.72f ? 1.0f - ((lifePct - 0.72f) / 0.28f) : 1.0f;
            int alpha = Mth.clamp((int) (255 * alphaPct), 0, 255);
            float popScale = lifePct < 0.18f ? 0.85f + ((lifePct / 0.18f) * 0.30f) : 1.0f;
            int driftY = (int) (lifePct * 18.0f);

            switch (payload.type()) {
                case DAMAGE, DAMAGE_GHOST, HEAL -> renderFloatingValue(guiGraphics, mc.font, rail, payload, stackIndex, driftY, popScale, alpha);
                case CRIT, CLASS_BONUS -> renderFloatingTag(guiGraphics, mc.font, rail, payload, stackIndex, driftY, alpha);
                case MANA, BATTERY -> renderResourceDelta(guiGraphics, mc.font, rail, payload, stackIndex, driftY, alpha);
                case ABILITY, TOFU_GAINED, TOFU_RESULT, PICKUP -> renderIconPopup(guiGraphics, mc, rail, payload, stackIndex, driftY, popScale);
            }
        }
    }

    private void renderFloatingValue(GuiGraphics guiGraphics, Font font, RailLayout rail, BattleUiEventPayload payload, int stackIndex, int driftY, float scale, int alpha) {
        int anchorX = rail.mirrored() ? rail.x() - 12 : rail.x() + rail.width() + 12;
        int anchorY = rail.y() + 8 + (stackIndex * 18) - driftY;
        int amount = Math.max(0, payload.amount());
        String amountText = String.valueOf(amount);
        float sizeScale = scale * (payload.type() == BattleUiEventPayload.Type.HEAL ? 2.8f : 3.2f) * damageSizeMultiplier(amount);
        int color = switch (payload.type()) {
            case HEAL -> withAlpha(0xFF67E27A, alpha);
            case DAMAGE_GHOST -> withAlpha(DODGE_GHOST_BLUE, Math.min(alpha, 180));
            default -> withAlpha(0xFFFFF5F5, alpha);
        };
        if (rail.mirrored()) {
            drawScaledTextRightAligned(guiGraphics, font, amountText, anchorX, anchorY, sizeScale, color, false,
                    payload.type() == BattleUiEventPayload.Type.DAMAGE_GHOST);
            return;
        }
        drawScaledText(guiGraphics, font, amountText, anchorX, anchorY, sizeScale, color, true,
                payload.type() == BattleUiEventPayload.Type.DAMAGE_GHOST);
    }

    private void renderFloatingTag(GuiGraphics guiGraphics, Font font, RailLayout rail, BattleUiEventPayload payload, int stackIndex, int driftY, int alpha) {
        int anchorX = rail.mirrored() ? rail.x() - 10 : rail.x() + rail.width() + 10;
        int anchorY = rail.y() + 40 + (stackIndex * 10) - driftY;
        String text = payload.type() == BattleUiEventPayload.Type.CRIT ? "CRIT +" + payload.amount() : "+" + payload.amount();
        int color = payload.type() == BattleUiEventPayload.Type.CRIT ? withAlpha(0xFFF4D35E, alpha) : withAlpha(0xFF6CE37A, alpha);
        if (rail.mirrored()) {
            drawScaledTextRightAligned(guiGraphics, font, text, anchorX, anchorY, 1.2f, color, false, false);
            return;
        }
        drawScaledText(guiGraphics, font, text, anchorX, anchorY, 1.2f, color, true, false);
    }

    private void renderResourceDelta(GuiGraphics guiGraphics, Font font, RailLayout rail, BattleUiEventPayload payload, int stackIndex, int driftY, int alpha) {
        int anchorX = rail.x() + (rail.width() / 2);
        int anchorY = (payload.type() == BattleUiEventPayload.Type.MANA ? rail.y() + 36 : rail.y() + 74) - driftY - (stackIndex * 8);
        String text = (payload.amount() > 0 ? "+" : "") + payload.amount();
        int color = payload.type() == BattleUiEventPayload.Type.MANA ? withAlpha(payload.amount() >= 0 ? 0xFF76C8FF : 0xFFFFB3B3, alpha) : withAlpha(payload.amount() >= 0 ? 0xFFF0D25C : 0xFFFFB35A, alpha);
        drawCenteredScaledText(guiGraphics, font, text, anchorX, anchorY, 0.9f, color, true);
    }

    private void renderIconPopup(GuiGraphics guiGraphics, Minecraft mc, RailLayout rail, BattleUiEventPayload payload, int stackIndex, int driftY, float scale) {
        ItemStack iconStack = switch (payload.type()) {
            case ABILITY -> createTempAbilityStack(payload.value(), 0, payload.flag());
            case TOFU_GAINED -> new ItemStack(ModItems.TOFU.get());
            case TOFU_RESULT -> resolveEffectIcon(payload.value());
            case PICKUP -> resolvePickupIcon(payload.value());
            default -> ItemStack.EMPTY;
        };
        int iconSize = Math.round(16 * 1.8f * scale);
        int baseX = rail.mirrored() ? rail.x() - 18 - iconSize : rail.x() + rail.width() + 18;
        int baseY = rail.y() + 58 + (stackIndex * 18) - driftY;
        if (!iconStack.isEmpty()) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(baseX, baseY, 250);
            guiGraphics.pose().scale(1.8f * scale, 1.8f * scale, 1.0f);
            guiGraphics.renderItem(iconStack, 0, 0);
            guiGraphics.pose().popPose();
        }
        if (payload.type() == BattleUiEventPayload.Type.TOFU_RESULT) {
            String label = formatEffectLabel(payload.value());
            int labelCenterX = baseX + (iconSize / 2);
            int labelY = baseY + iconSize + 4;
            drawCenteredScaledText(guiGraphics, mc.font, label, labelCenterX, labelY, 0.78f, 0xFFF5F6FA, true);
            return;
        }
        if (payload.type() == BattleUiEventPayload.Type.PICKUP) {
            String label = payload.value().toUpperCase(Locale.ROOT);
            if (rail.mirrored()) {
                drawScaledTextRightAligned(guiGraphics, mc.font, label, baseX - 4, baseY + 3, 0.8f, 0xFFF5F6FA, false, false);
                return;
            }
            drawScaledText(guiGraphics, mc.font, label, baseX + 18, baseY + 3, 0.8f, 0xFFF5F6FA, true, false);
        }
    }

    private void renderTofuPreview(GuiGraphics guiGraphics, Minecraft mc, RailLayout rail, BattleHudParticipantSnapshot side) {
        if (side.currentTofuMana() <= 0 || side.tofuPreviewEffectId().isEmpty()) {
            return;
        }
        if (rail.mirrored() && !side.isVisibleOpponent()) {
            return;
        }

        ItemStack effectStack = resolveEffectIcon(side.tofuPreviewEffectId());
        ItemStack tofuStack = new ItemStack(ModItems.TOFU.get());
        int baseX = rail.mirrored() ? rail.x() - 34 : rail.x() + rail.width() + 18;
        int effectY = rail.y() + TOFU_PREVIEW_Y;
        int tofuY = effectY + 40;
        int labelCenterX = baseX + 8;

        if (!effectStack.isEmpty()) {
            guiGraphics.renderItem(effectStack, baseX, effectY);
        }
        drawCenteredScaledText(guiGraphics, mc.font, formatEffectLabel(side.tofuPreviewEffectId()), labelCenterX, effectY + 18, 0.72f, 0xFFF5F6FA, true);
        guiGraphics.renderItem(tofuStack, baseX, tofuY);
    }

    private BattleHudParticipantSnapshot resolveEventSide(BattleHudSnapshot snapshot, int entityId) {
        if (snapshot.player().entityId() == entityId) {
            return snapshot.player();
        }
        if (snapshot.enemy().entityId() == entityId) {
            return snapshot.enemy();
        }
        return null;
    }

    private String anchorFor(BattleUiEventPayload.Type type) {
        return switch (type) {
            case DAMAGE, DAMAGE_GHOST, HEAL, CRIT, CLASS_BONUS -> "outer";
            case MANA -> "mana";
            case BATTERY -> "battery";
            case ABILITY, TOFU_GAINED, TOFU_RESULT, PICKUP -> "inner";
        };
    }

    private boolean isValidRangedTarget(Minecraft mc, LivingEntity target, AbilityLoader.AbilityData data) {
        double maxRange = TeenyBalance.getRangeValue(data.rangeTier);
        Vec3 origin = mc.player.getEyePosition();
        Vec3 rayDir = mc.player.getLookAngle().normalize();
        Vec3 toCenter = target.getBoundingBox().getCenter().subtract(origin);
        double distAlongRay = toCenter.dot(rayDir);
        if (distAlongRay < 0 || distAlongRay > maxRange) {
            return false;
        }
        Vec3 closestPoint = getClosestPointOnAabb(target.getBoundingBox(), origin);
        Vec3 toClosest = closestPoint.subtract(origin);
        double distanceAlongRay = toClosest.dot(rayDir);
        Vec3 projection = origin.add(rayDir.scale(distanceAlongRay));
        double distToRaySq = closestPoint.distanceToSqr(projection);
        double halfAngleRad = Math.toRadians(TeenyBalance.RANGED_CONE_ANGLE / 2.0);
        double allowedRadius = (TeenyBalance.RANGED_START_WIDTH / 2.0) + (distAlongRay * Math.tan(halfAngleRad));
        if (distToRaySq > (allowedRadius * allowedRadius)) {
            return false;
        }
        HitResult blockHit = mc.level.clip(new ClipContext(origin, target.getEyePosition(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
        return blockHit.getType() == HitResult.Type.MISS;
    }

    private Vec3 getClosestPointOnAabb(AABB box, Vec3 origin) {
        return new Vec3(Mth.clamp(origin.x, box.minX, box.maxX), Mth.clamp(origin.y, box.minY, box.maxY), Mth.clamp(origin.z, box.minZ, box.maxZ));
    }

    private float damageSizeMultiplier(int amount) {
        if (amount >= 100) return 1.25f;
        if (amount >= 50) return 1.12f;
        return 1.0f;
    }

    private int calculateRemoteMineDamage(int slotIndex, int cost, BattleHudParticipantSnapshot side, List<BattleHudEffectSnapshot> opposingEffects, AbilityLoader.AbilityData abilityData) {
        float paramMult = 1.0f;
        for (AbilityLoader.EffectData effect : abilityData.effectsOnOpponent) {
            if ("remote_mine".equals(effect.id) && effect.params != null && !effect.params.isEmpty()) {
                paramMult = effect.params.get(0);
                break;
            }
        }
        float tierMult = TeenyBalance.getDamageMultiplier(abilityData.damageTier);
        float maxSnapshot = (side.basePower() * cost * TeenyBalance.BASE_DAMAGE_PERMANA * TeenyBalance.REMOTE_MINE_DAMAGE_MULT * paramMult * tierMult) + side.powerUp() - side.powerDown();
        int stages = 0;
        for (BattleHudEffectSnapshot effect : opposingEffects) {
            if (effect.id().startsWith("remote_mine_" + slotIndex)) {
                stages = effect.magnitude();
                break;
            }
        }
        float initial = maxSnapshot * TeenyBalance.REMOTE_MINE_START_PCT;
        float pool = maxSnapshot - initial;
        return Math.round(initial + (stages * (pool / TeenyBalance.REMOTE_MINE_STAGES)));
    }

    private int calculateDirectDamage(int cost, int basePower, int powerUp, int powerDown, AbilityLoader.AbilityData abilityData) {
        float tierMult = TeenyBalance.getDamageMultiplier(abilityData.damageTier);
        float raw = basePower * cost * TeenyBalance.BASE_DAMAGE_PERMANA * tierMult;
        for (AbilityLoader.TraitData trait : abilityData.traits) {
            if ("activate".equals(trait.id)) {
                float requirement = trait.params.isEmpty() ? 2.0f : trait.params.get(0);
                raw *= (requirement * TeenyBalance.ACTIVATE_DAMAGE_MULT);
            } else if ("charge_up".equals(trait.id)) {
                float seconds = trait.params.isEmpty() ? 1.0f : trait.params.get(0);
                raw *= (seconds * TeenyBalance.CHARGE_UP_MULT_PER_SEC);
            }
        }
        return Math.round(raw + powerUp - powerDown);
    }

    private float parseBenchHpPct(String hpText) {
        try {
            String[] parts = hpText.split("/");
            return Float.parseFloat(parts[0]) / Float.parseFloat(parts[1]);
        } catch (Exception ignored) {
            return 1.0f;
        }
    }

    private int getRailHeight(BattleHudParticipantSnapshot side) {
        int benchHeight = side.benchIndices().size() * BENCH_ROW_HEIGHT;
        int effectStartY = 105 + benchHeight + 6;
        int effectRows = getEffectRowCount(side.effects());
        int effectHeight = effectRows > 0
                ? (effectRows * EFFECT_BADGE_HEIGHT) + ((effectRows - 1) * EFFECT_BADGE_GAP)
                : 0;
        return Math.max(MIN_RAIL_HEIGHT, effectStartY + effectHeight + EFFECT_BLOCK_PADDING);
    }

    private int getEffectRowCount(List<BattleHudEffectSnapshot> effects) {
        if (effects == null || effects.isEmpty()) {
            return 0;
        }
        int visibleEffects = Math.min(effects.size(), EFFECTS_PER_ROW * MAX_EFFECT_ROWS);
        return Math.min(MAX_EFFECT_ROWS, (visibleEffects + EFFECTS_PER_ROW - 1) / EFFECTS_PER_ROW);
    }

    private int getHpColor(float pct) {
        if (pct > 0.5f) return 0xFF46D66B;
        if (pct > 0.2f) return 0xFFF0D25C;
        return 0xFFED6969;
    }

    private String formatEffectLabel(String effectId) {
        if (effectId == null || effectId.isEmpty()) {
            return "";
        }
        return effectId.replace('_', ' ').toUpperCase(Locale.ROOT);
    }

    private int getEffectBadgeColor(String effectId) {
        BattleEffect effect = EffectRegistry.get(effectId);
        if (effect == null) {
            return 0xFF4C5564;
        }
        return switch (effect.getCategory()) {
            case BUFF -> 0xFF2E7D5E;
            case DEBUFF -> 0xFF8A4242;
            case CONTROL -> 0xFF5B4B92;
            case SPECIAL -> 0xFF7A6D3A;
        };
    }

    private String abbreviateEffect(String effectId) {
        return switch (effectId) {
            case "power_up" -> "PWR";
            case "power_down" -> "PDN";
            case "defense_up" -> "DEF";
            case "defense_down" -> "DDN";
            case "luck_up" -> "LUK";
            case "reset_lock" -> "RST";
            case "stun" -> "STN";
            case "freeze" -> "FRZ";
            case "shock" -> "SHK";
            case "poison" -> "PSN";
            case "flight" -> "FLY";
            case "waffle" -> "WAF";
            case "reflect" -> "RFL";
            case "shield" -> "SHD";
            case "root" -> "ROT";
            case "kiss" -> "KSS";
            case "dance" -> "DNC";
            case "curse" -> "CRS";
            default -> effectId.length() <= 3 ? effectId.toUpperCase(Locale.ROOT) : effectId.substring(0, 3).toUpperCase(Locale.ROOT);
        };
    }

    private void renderClassIndicator(GuiGraphics guiGraphics, Minecraft mc, ItemStack selfStack, ItemStack opponentStack, int x, int y) {
        FigureClassType ownClass = FigureClassType.fromSerialized(ItemFigure.getFigureClass(selfStack));
        FigureClassType enemyClass = FigureClassType.fromSerialized(ItemFigure.getFigureClass(opponentStack));
        if (ownClass.hasAdvantageOver(enemyClass)) {
            guiGraphics.drawString(mc.font, "UP", x, y, 0xFF6CE37A, false);
        } else if (enemyClass.hasAdvantageOver(ownClass)) {
            guiGraphics.drawString(mc.font, "DN", x, y, 0xFFED6969, false);
        }
    }

    private ItemStack resolveActiveStack(Minecraft mc, BattleHudParticipantSnapshot side, boolean isEnemy) {
        if (isEnemy) {
            return FigureLoader.getFigureStack(side.activeFigureId());
        }
        return mc.player.getCapability(TitanManagerProvider.TITAN_MANAGER)
                .resolve()
                .map(manager -> manager.getTeamStack(side.activeFigureIndex()))
                .filter(stack -> !stack.isEmpty())
                .orElseGet(() -> FigureLoader.getFigureStack(side.activeFigureId()));
    }

    private ItemStack resolveBenchStack(Minecraft mc, int benchIndex) {
        return mc.player.getCapability(TitanManagerProvider.TITAN_MANAGER)
                .resolve()
                .map(manager -> manager.getTeamStack(benchIndex))
                .orElse(ItemStack.EMPTY);
    }

    private ItemStack createAccessoryStack(String accessoryId, boolean active) {
        if (accessoryId == null || accessoryId.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ResourceLocation rl = new ResourceLocation(TeenyCraft.MOD_ID, "accessory_" + accessoryId);
        Item item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(rl);
        if (item == null || item == Items.AIR) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(item);
        if (active) {
            stack.getOrCreateTag().putBoolean("BattleAccessoryActive", true);
        }
        return stack;
    }

    private ItemStack createTempAbilityStack(String abilityId, int slot, boolean isGolden) {
        if (abilityId == null || abilityId.equals("none")) {
            return ItemStack.EMPTY;
        }
        Item[] abilityItems = {ModItems.ABILITY_1.get(), ModItems.ABILITY_2.get(), ModItems.ABILITY_3.get()};
        int resolvedSlot = Mth.clamp(slot, 0, abilityItems.length - 1);
        ItemStack stack = new ItemStack(abilityItems[resolvedSlot]);
        var tag = stack.getOrCreateTag();
        tag.putString(ItemAbility.TAG_ID, abilityId);
        tag.putBoolean(ItemAbility.TAG_GOLDEN, isGolden);
        return stack;
    }

    private ItemStack resolvePickupIcon(String pickupId) {
        return switch (pickupId) {
            case "heal" -> new ItemStack(Items.GLISTERING_MELON_SLICE);
            case "mana" -> new ItemStack(Items.EXPERIENCE_BOTTLE);
            case "amp" -> new ItemStack(Items.BLAZE_POWDER);
            case "speed" -> new ItemStack(Items.SUGAR);
            case "launch" -> new ItemStack(Items.FIREWORK_ROCKET);
            case "wall" -> new ItemStack(Items.BRICKS);
            default -> ItemStack.EMPTY;
        };
    }

    private ItemStack resolveEffectIcon(String effectId) {
        return switch (effectId) {
            case "power_up" -> new ItemStack(Items.BLAZE_POWDER);
            case "heal" -> new ItemStack(Items.GHAST_TEAR);
            case "bar_fill" -> new ItemStack(Items.EXPERIENCE_BOTTLE);
            case "cleanse" -> new ItemStack(Items.MILK_BUCKET);
            case "dance" -> new ItemStack(Items.NOTE_BLOCK);
            case "freeze" -> new ItemStack(Items.SNOWBALL);
            case "stun" -> new ItemStack(Items.GLOWSTONE_DUST);
            case "waffle" -> new ItemStack(Items.HONEYCOMB);
            default -> new ItemStack(Items.PAPER);
        };
    }

    private void drawCenteredScaledText(GuiGraphics guiGraphics, Font font, String text, int centerX, int y, float scale, int color, boolean shadow) {
        int width = font.width(text);
        drawScaledText(guiGraphics, font, text, centerX - Math.round((width * scale) / 2.0f), y, scale, color, shadow, false);
    }

    private void drawScaledText(GuiGraphics guiGraphics, Font font, String text, int x, int y, float scale, int color, boolean shadow, boolean strikethrough) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 300);
        guiGraphics.pose().scale(scale, scale, 1.0f);
        guiGraphics.drawString(font, text, 0, 0, color, shadow);
        if (strikethrough) {
            int lineY = font.lineHeight / 2;
            guiGraphics.fill(0, lineY, font.width(text), lineY + 1, color);
        }
        guiGraphics.pose().popPose();
    }

    private void drawScaledTextRightAligned(GuiGraphics guiGraphics, Font font, String text, int rightX, int y, float scale, int color,
                                            boolean shadow, boolean strikethrough) {
        int scaledWidth = Math.round(font.width(text) * scale);
        drawScaledText(guiGraphics, font, text, rightX - scaledWidth, y, scale, color, shadow, strikethrough);
    }

    private int withAlpha(int rgb, int alpha) {
        return (alpha << 24) | (rgb & 0x00FFFFFF);
    }

    private record RailLayout(int x, int y, int width, boolean mirrored) {
    }
}
