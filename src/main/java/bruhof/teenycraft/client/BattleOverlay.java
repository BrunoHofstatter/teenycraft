package bruhof.teenycraft.client;

import bruhof.teenycraft.TeenyCraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import bruhof.teenycraft.capability.ITitanManager;
import bruhof.teenycraft.capability.TitanManagerProvider;
import net.minecraft.world.item.ItemStack;
import java.util.List;

public class BattleOverlay implements IGuiOverlay {
    public static final BattleOverlay INSTANCE = new BattleOverlay();
    
    private static final ResourceLocation TEXTURE = new ResourceLocation("minecraft", "textures/gui/bars.png");

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        if (!ClientBattleData.isBattling()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // PLAYER HUD (Top Left)
        renderHUDSide(guiGraphics, mc, 10, 10, false, screenWidth);

        // ENEMY HUD (Top Right)
        renderHUDSide(guiGraphics, mc, screenWidth - 150, 10, true, screenWidth);
    }

    private void renderHUDSide(GuiGraphics guiGraphics, Minecraft mc, int x, int y, boolean isEnemy, int screenWidth) {
        String name = isEnemy ? ClientBattleData.getEnemyName() : ClientBattleData.getActiveFigureName();
        if (isEnemy && (name == null || name.isEmpty() || name.equals("None"))) return;

        int hp = isEnemy ? ClientBattleData.getEnemyHp() : ClientBattleData.getCurrentHp();
        int maxHp = isEnemy ? ClientBattleData.getEnemyMaxHp() : ClientBattleData.getMaxHp();
        float mana = isEnemy ? ClientBattleData.getEnemyMana() : ClientBattleData.getCurrentMana();
        int activeIdx = isEnemy ? ClientBattleData.getEnemyActiveIndex() : ClientBattleData.getActiveFigureIndex();
        String activeId = isEnemy ? ClientBattleData.getEnemyActiveId() : ClientBattleData.getActiveFigureId();
        String activeModelType = isEnemy ? ClientBattleData.getEnemyActiveModelType() : ClientBattleData.getActiveFigureModelType();
        List<String> effects = isEnemy ? ClientBattleData.getEnemyEffects() : ClientBattleData.getEffects();
        List<String> benchHP = isEnemy ? ClientBattleData.getEnemyBenchInfo() : ClientBattleData.getBenchInfo();
        List<Integer> benchIndices = isEnemy ? ClientBattleData.getEnemyBenchIndices() : ClientBattleData.getBenchIndices();
        List<String> benchIds = isEnemy ? ClientBattleData.getEnemyBenchFigureIds() : ClientBattleData.getBenchFigureIds();

        // Constants for layout
        int itemSize = 32;
        int hpBarWidth = 100;
        int totalWidth = itemSize + 8 + hpBarWidth;

        int basePower = isEnemy ? ClientBattleData.getEnemyBasePower() : ClientBattleData.getBasePower();
        int powerUp = isEnemy ? ClientBattleData.getEnemyPowerUp() : ClientBattleData.getPowerUp();
        int powerDown = isEnemy ? ClientBattleData.getEnemyPowerDown() : ClientBattleData.getPowerDown();
        int[] cooldowns = isEnemy ? ClientBattleData.getEnemyCooldowns() : ClientBattleData.getCooldowns();
        List<String> abilityIds = isEnemy ? ClientBattleData.getEnemyAbilityIds() : ClientBattleData.getAbilityIds();
        List<String> abilityTiers = isEnemy ? ClientBattleData.getEnemyAbilityTiers() : ClientBattleData.getAbilityTiers();
        List<Boolean> goldenStatus = isEnemy ? ClientBattleData.getEnemyAbilityGolden() : ClientBattleData.getAbilityGolden();
        int[] slotProgress = isEnemy ? ClientBattleData.getEnemySlotProgress() : ClientBattleData.getSlotProgress();

        // 1. Big Figure Item
        ItemStack activeStack = ItemStack.EMPTY;
        if (!isEnemy) {
            var titanCap = mc.player.getCapability(TitanManagerProvider.TITAN_MANAGER).resolve();
            if (titanCap.isPresent()) {
                activeStack = titanCap.get().getTeamStack(activeIdx);
            }
        } else {
            activeStack = createTempFigureStack(activeId);
        }

        if (!activeStack.isEmpty()) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(x, y, 0);
            guiGraphics.pose().scale(2.0f, 2.0f, 1.0f);
            guiGraphics.renderItem(activeStack, 0, 0);
            guiGraphics.pose().popPose();
        }

        // 2. Name & HP Bar
        int barX = x + 40;
        int barY = y + 12;
        guiGraphics.fill(barX, barY, barX + hpBarWidth, barY + 8, 0xFF555555); // BG
        float hpPct = maxHp > 0 ? (float) hp / maxHp : 0;
        guiGraphics.fill(barX, barY, barX + (int)(hpBarWidth * hpPct), barY + 8, getHPColor(hpPct));
        guiGraphics.drawString(mc.font, hp + "/" + maxHp, barX + 2, barY, 0x000000, false);
        guiGraphics.drawString(mc.font, (isEnemy ? "§c§l" : "§l") + name, barX, y, 0xFFFFFF);

        // 3. Mana Bar
        int manaY = y + 36;
        int manaHeight = 9;
        guiGraphics.fill(x, manaY, x + totalWidth, manaY + manaHeight, 0xFF555555); // BG
        float manaPct = mana / 100.0f;
        guiGraphics.fill(x, manaY, x + (int)(totalWidth * manaPct), manaY + manaHeight, isEnemy ? 0xFFFF0000 : 0xFF00AAFF);
        guiGraphics.drawCenteredString(mc.font, String.format("%.0f/100", mana), x + (totalWidth / 2), manaY + 1, 0xFFFFFF);

        // 4. Ability Icons on Mana Bar
        for (int i = 0; i < abilityIds.size(); i++) {
            String aid = abilityIds.get(i);
            bruhof.teenycraft.util.AbilityLoader.AbilityData adata = bruhof.teenycraft.util.AbilityLoader.getAbility(aid);
            if (adata == null) continue;

            String tier = (i < abilityTiers.size()) ? abilityTiers.get(i) : "a";
            int cost = bruhof.teenycraft.TeenyBalance.getManaCost(i + 1, tier);
            int cd = (i < cooldowns.length) ? cooldowns[i] : 0;
            boolean isGolden = (i < goldenStatus.size()) ? goldenStatus.get(i) : false;

            boolean canCast = (mana >= cost && cd <= 0);
            float scale = canCast ? 1.2f : 0.8f;
            
            int iconX = x + (int)((cost / 100.0f) * totalWidth);
            int iconY = manaY + 4; // Partially overlapping the bar

            ItemStack aStack = createTempAbilityStack(aid, i, isGolden);
            if (!aStack.isEmpty()) {
                guiGraphics.pose().pushPose();
                // Adjust translation so icons are centered horizontally on their cost point
                guiGraphics.pose().translate(iconX - (8 * scale), iconY, 200);
                guiGraphics.pose().scale(scale, scale, 1.0f);
                guiGraphics.renderItem(aStack, 0, 0);
                guiGraphics.pose().popPose();
            }

            // Damage Label (Filtered: damageTier > 0 AND hitType != none)
            if (adata.damageTier > 0 && !"none".equalsIgnoreCase(adata.hitType)) {
                int totalDmg = 0;
                boolean isMine = adata.effectsOnOpponent.stream().anyMatch(e -> "remote_mine".equals(e.id));
                boolean activeMine = isEnemy ? ClientBattleData.enemyHasActiveMine(i) : ClientBattleData.hasActiveMine(i);

                if (isMine && activeMine) {
                    float paramMult = 1.0f;
                    for (bruhof.teenycraft.util.AbilityLoader.EffectData e : adata.effectsOnOpponent) {
                        if ("remote_mine".equals(e.id)) {
                            if (e.params != null && !e.params.isEmpty()) paramMult = e.params.get(0);
                            break;
                        }
                    }

                    float tierMult = bruhof.teenycraft.TeenyBalance.getDamageMultiplier(adata.damageTier);
                    float maxSnapshot = (basePower * cost * bruhof.teenycraft.TeenyBalance.BASE_DAMAGE_PERMANA * 
                                         bruhof.teenycraft.TeenyBalance.REMOTE_MINE_DAMAGE_MULT * paramMult * tierMult) 
                                         + powerUp - powerDown;
                    
                    int stages = 0;
                    // To find the mine WE placed, we must look at the OPPONENT'S effects list
                    List<String> effList = isEnemy ? ClientBattleData.getEffects() : ClientBattleData.getEnemyEffects();
                    for (String s : effList) {
                        if (s.startsWith("remote_mine_" + i) && s.contains("Mag: ")) {
                            try {
                                String magStr = s.substring(s.indexOf("Mag: ") + 5, s.length() - 1);
                                stages = Integer.parseInt(magStr);
                            } catch (Exception e) {}
                            break;
                        }
                    }

                    float initial = maxSnapshot * bruhof.teenycraft.TeenyBalance.REMOTE_MINE_START_PCT;
                    float pool = maxSnapshot - initial;
                    float weight = pool / bruhof.teenycraft.TeenyBalance.REMOTE_MINE_STAGES;
                    
                    totalDmg = Math.round(initial + (stages * weight));
                } else {
                    float tierMult = bruhof.teenycraft.TeenyBalance.getDamageMultiplier(adata.damageTier);
                    float raw = basePower * cost * bruhof.teenycraft.TeenyBalance.BASE_DAMAGE_PERMANA * tierMult;
                    
                    for (bruhof.teenycraft.util.AbilityLoader.TraitData t : adata.traits) {
                        if ("activate".equals(t.id)) {
                            float req = t.params.isEmpty() ? 2.0f : t.params.get(0);
                            raw *= (req * bruhof.teenycraft.TeenyBalance.ACTIVATE_DAMAGE_MULT);
                        } else if ("charge_up".equals(t.id)) {
                            float sec = t.params.isEmpty() ? 1.0f : t.params.get(0);
                            raw *= (sec * bruhof.teenycraft.TeenyBalance.CHARGE_UP_MULT_PER_SEC);
                        }
                    }
                    totalDmg = Math.round(raw + powerUp - powerDown);
                }
                
                String dmgStr = String.valueOf(Math.max(0, totalDmg));
                int strW = mc.font.width(dmgStr);
                guiGraphics.drawString(mc.font, dmgStr, iconX - (strW / 2), iconY + (int)(16 * scale), 0xFFFFFF, true);
            }

            // Activation Progress Label (Above icon)
            int currentProgress = (i < slotProgress.length) ? slotProgress[i] : 0;
            float reqCasts = 0;
            for (bruhof.teenycraft.util.AbilityLoader.TraitData t : adata.traits) {
                if ("activate".equals(t.id)) {
                    reqCasts = t.params.isEmpty() ? 2.0f : t.params.get(0);
                    break;
                }
            }

            if (reqCasts > 0) {
                String progStr = currentProgress + "/" + (int)reqCasts;
                int pW = mc.font.width(progStr);
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(iconX - (pW * 0.75f / 2), iconY - 8, 200);
                guiGraphics.pose().scale(0.75f, 0.75f, 1.0f);
                guiGraphics.drawString(mc.font, progStr, 0, 0, 0xFFFF00, true);
                guiGraphics.pose().popPose();
            }
        }

        // 4.5 Battery Bar
        float batCharge = isEnemy ? ClientBattleData.getEnemyBatteryCharge() : ClientBattleData.getBatteryCharge();
        float batSpawnPct = isEnemy ? ClientBattleData.getEnemyBatterySpawnPct() : ClientBattleData.getBatterySpawnPct();
        
        int batY = manaY + 32;
        int batHeight = 4;
        guiGraphics.fill(x, batY, x + totalWidth, batY + batHeight, 0xFF444444); // BG
        float batPct = batCharge / 100.0f;
        guiGraphics.fill(x, batY, x + (int)(totalWidth * batPct), batY + batHeight, 0xFFFFFF00); // Yellow Battery
        
        // Render Battery Collectible on Mana Bar
        if (batSpawnPct != -1.0f) {
            int spawnX = x + (int)(totalWidth * batSpawnPct);
            int spawnY = manaY - 8;
            guiGraphics.fill(spawnX - 4, spawnY, spawnX + 4, spawnY + 8, 0xFFFFFF00);
            guiGraphics.fill(spawnX - 3, spawnY + 1, spawnX + 3, spawnY + 7, 0xFFFF8800);
        }

        // 5. Bench Figures
        int benchY = batY + 12; // Adjusted spacing after battery bar
        for (int i = 0; i < benchIndices.size(); i++) {
            ItemStack bStack = ItemStack.EMPTY;
            if (!isEnemy) {
                var titanCap = mc.player.getCapability(TitanManagerProvider.TITAN_MANAGER).resolve();
                if (titanCap.isPresent()) {
                    bStack = titanCap.get().getTeamStack(benchIndices.get(i));
                }
            } else if (i < benchIds.size()) {
                bStack = createTempFigureStack(benchIds.get(i));
            }
            
            if (!bStack.isEmpty()) {
                guiGraphics.renderItem(bStack, x, benchY);
            }
            
            if (i < benchHP.size()) {
                String hpText = benchHP.get(i);
                float bPct = 1.0f;
                try {
                    String[] parts = hpText.split("/");
                    bPct = Float.parseFloat(parts[0]) / Float.parseFloat(parts[1]);
                } catch (Exception e) {}
                guiGraphics.drawString(mc.font, hpText, x + 20, benchY + 4, getHPColor(bPct));
            }
            benchY += 20;
        }

        // 6. Effects
        int effectY = benchY + 4;
        for (String eff : effects) {
            guiGraphics.drawString(mc.font, (isEnemy ? "§e" : "§d") + eff, x, effectY, 0xFFFFFF);
            effectY += 10;
        }
    }

    private int getHPColor(float pct) {
        if (pct > 0.5f) return 0xFF00FF00; // Green
        if (pct > 0.2f) return 0xFFFFFF00; // Yellow
        return 0xFFFF0000; // Red
    }

    private ItemStack createTempFigureStack(String id) {
        if (id == null || id.equals("none")) return ItemStack.EMPTY;
        
        // Find specific figure item (e.g., teenycraft:figure_robin)
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
        if (abilityId == null || abilityId.equals("none")) return ItemStack.EMPTY;
        
        net.minecraft.world.item.Item[] abilityItems = { 
            bruhof.teenycraft.item.ModItems.ABILITY_1.get(), 
            bruhof.teenycraft.item.ModItems.ABILITY_2.get(), 
            bruhof.teenycraft.item.ModItems.ABILITY_3.get() 
        };
        
        if (slot < 0 || slot >= 3) return ItemStack.EMPTY;
        
        ItemStack stack = new ItemStack(abilityItems[slot]);
        var tag = stack.getOrCreateTag();
        tag.putString(bruhof.teenycraft.item.custom.battle.ItemAbility.TAG_ID, abilityId);
        tag.putBoolean(bruhof.teenycraft.item.custom.battle.ItemAbility.TAG_GOLDEN, isGolden);
        return stack;
    }
}
