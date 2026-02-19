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

        // ============================
        // PLAYER HUD (Top Left)
        // ============================
        renderPlayerHUD(guiGraphics, mc, 10, 10);

        // ============================
        // ENEMY HUD (Top Right)
        // ============================
        renderEnemyHUD(guiGraphics, mc, screenWidth - 160, 10);
    }

    private void renderPlayerHUD(GuiGraphics guiGraphics, Minecraft mc, int x, int y) {
        int hp = ClientBattleData.getCurrentHp();
        int maxHp = ClientBattleData.getMaxHp();
        float mana = ClientBattleData.getCurrentMana();
        String name = ClientBattleData.getActiveFigureName();
        int activeIdx = ClientBattleData.getActiveFigureIndex();

        // Constants for layout
        int itemSize = 32;
        int hpBarWidth = 100;
        int totalWidth = itemSize + 8 + hpBarWidth;

        // 1. Row 1: Big Figure Item + HP Bar
        ItemStack activeStack = ItemStack.EMPTY;
        var titanCap = mc.player.getCapability(TitanManagerProvider.TITAN_MANAGER).resolve();
        if (titanCap.isPresent()) {
            activeStack = titanCap.get().getInventory().getStackInSlot(activeIdx);
        }

        // Render Item
        if (!activeStack.isEmpty()) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(x, y, 0);
            guiGraphics.pose().scale(2.0f, 2.0f, 1.0f);
            guiGraphics.renderItem(activeStack, 0, 0);
            guiGraphics.pose().popPose();
        }

        // Render HP Bar
        int barX = x + 40;
        int barY = y + 12;
        guiGraphics.fill(barX, barY, barX + hpBarWidth, barY + 8, 0xFF555555); // BG
        float hpPct = maxHp > 0 ? (float) hp / maxHp : 0;
        guiGraphics.fill(barX, barY, barX + (int)(hpBarWidth * hpPct), barY + 8, getHPColor(hpPct));
        guiGraphics.drawString(mc.font, hp + "/" + maxHp, barX + 2, barY, 0x000000, false); // BLACK text for visibility
        guiGraphics.drawString(mc.font, "§l" + name, barX, y, 0xFFFFFF);

        // 2. Row 2: Big Horizontal Mana Bar
        int manaY = y + 36;
        int manaHeight = 9; // 1.5x thicker (was 6)
        guiGraphics.fill(x, manaY, x + totalWidth, manaY + manaHeight, 0xFF555555); // BG
        float manaPct = mana / 100.0f;
        guiGraphics.fill(x, manaY, x + (int)(totalWidth * manaPct), manaY + manaHeight, 0xFF00AAFF);
        guiGraphics.drawCenteredString(mc.font, String.format("%.0f/100", mana), x + (totalWidth / 2), manaY + 1, 0xFFFFFF);
        
        // 3. Row 3: Bench Figures (Item + HP Text)
        int benchY = manaY + 12;
        List<String> benchHP = ClientBattleData.getBenchInfo();
        List<Integer> benchIndices = ClientBattleData.getBenchIndices();
        
        for (int i = 0; i < benchIndices.size(); i++) {
            int slot = benchIndices.get(i);
            ItemStack bStack = ItemStack.EMPTY;
            if (titanCap.isPresent()) {
                bStack = titanCap.get().getInventory().getStackInSlot(slot);
            }
            
            // Render Bench Item (Normal size)
            if (!bStack.isEmpty()) {
                guiGraphics.renderItem(bStack, x, benchY);
            }
            
            // Render Bench HP Text
            if (i < benchHP.size()) {
                String hpText = benchHP.get(i);
                // Parse percentage for color
                float bPct = 1.0f;
                try {
                    String[] parts = hpText.split("/");
                    bPct = Float.parseFloat(parts[0]) / Float.parseFloat(parts[1]);
                } catch (Exception e) {}
                
                guiGraphics.drawString(mc.font, hpText, x + 20, benchY + 4, getHPColor(bPct));
            }
            benchY += 20;
        }

        // 4. Row 4: Effects
        int effectY = benchY + 4;
        List<String> effects = ClientBattleData.getEffects();
        for (String eff : effects) {
            guiGraphics.drawString(mc.font, "§d" + eff, x, effectY, 0xFFFFFF);
            effectY += 10;
        }
    }

    private int getHPColor(float pct) {
        if (pct > 0.5f) return 0xFF00FF00; // Green
        if (pct > 0.2f) return 0xFFFFFF00; // Yellow
        return 0xFFFF0000; // Red
    }

    private void renderEnemyHUD(GuiGraphics guiGraphics, Minecraft mc, int x, int y) {
        String enemyName = ClientBattleData.getEnemyName();
        if (enemyName == null || enemyName.isEmpty() || enemyName.equals("None")) return;

        int eHp = ClientBattleData.getEnemyHp();
        int eMaxHp = ClientBattleData.getEnemyMaxHp();
        int startX = x;
        int startY = y;

        // 1. Name
        guiGraphics.drawString(mc.font, "§c§l" + enemyName, startX, startY, 0xFFFFFF);

        // 2. HP Bar
        int barWidth = 100;
        startY += 12;
        guiGraphics.fill(startX, startY, startX + barWidth, startY + 8, 0xFF555555);
        float eHpPct = eMaxHp > 0 ? (float) eHp / eMaxHp : 0;
        guiGraphics.fill(startX, startY, startX + (int)(barWidth * eHpPct), startY + 8, 0xFFFF0000);
        guiGraphics.drawString(mc.font, eHp + "/" + eMaxHp, startX + 2, startY, 0xFFFFFF, false);
        
        // 3. Mana Bar Placeholder
        startY += 12;
        guiGraphics.fill(startX, startY, startX + barWidth, startY + 4, 0xFF333333);
        guiGraphics.drawString(mc.font, "§8Mana: ???", startX, startY + 6, 0xFFFFFF);
    }
}
