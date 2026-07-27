package bruhof.teenycraft.capability;

import bruhof.teenycraft.accessory.AccessoryProgression;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class AccessoryMastery implements IAccessoryMastery {
    private static final String TAG_ACCESSORIES = "Accessories";
    private static final String TAG_TIER = "Tier";
    private static final String TAG_MILESTONE_ID = "MilestoneId";
    private static final String TAG_PROGRESS = "Progress";
    private static final String TAG_COMPLETE = "Complete";

    private final Map<String, ProgressEntry> entries = new HashMap<>();

    @Override
    public int getTier(String accessoryId) {
        ProgressEntry entry = entries.get(accessoryId);
        return entry != null ? entry.tier : AccessoryProgression.MIN_TIER;
    }

    @Override
    public String getCurrentMilestoneId(String accessoryId) {
        return AccessoryProgression.currentMilestoneId(accessoryId, getTier(accessoryId));
    }

    @Override
    public long getMilestoneProgress(String accessoryId) {
        ProgressEntry entry = entries.get(accessoryId);
        if (entry == null || !entry.milestoneId.equals(getCurrentMilestoneId(accessoryId))) {
            return 0;
        }
        return entry.progress;
    }

    @Override
    public boolean isCurrentMilestoneComplete(String accessoryId) {
        ProgressEntry entry = entries.get(accessoryId);
        return entry != null
                && entry.complete
                && entry.milestoneId.equals(getCurrentMilestoneId(accessoryId));
    }

    @Override
    public boolean addMilestoneProgress(String accessoryId, String milestoneId, long amount) {
        if (amount <= 0 || !isCurrentMilestone(accessoryId, milestoneId)) {
            return false;
        }
        ProgressEntry entry = entry(accessoryId);
        long next = entry.progress > Long.MAX_VALUE - amount ? Long.MAX_VALUE : entry.progress + amount;
        entry.progress = next;
        return true;
    }

    @Override
    public boolean setMilestoneProgress(String accessoryId, String milestoneId, long amount) {
        if (!isCurrentMilestone(accessoryId, milestoneId)) {
            return false;
        }
        entry(accessoryId).progress = Math.max(0, amount);
        return true;
    }

    @Override
    public boolean completeMilestone(String accessoryId, String milestoneId) {
        if (!isCurrentMilestone(accessoryId, milestoneId)) {
            return false;
        }
        entry(accessoryId).complete = true;
        return true;
    }

    @Override
    public boolean unlockNextTier(String accessoryId) {
        ProgressEntry entry = entries.get(accessoryId);
        if (entry == null || entry.tier >= AccessoryProgression.MAX_TIER || !isCurrentMilestoneComplete(accessoryId)) {
            return false;
        }
        entry.tier++;
        resetMilestone(entry, accessoryId);
        return true;
    }

    @Override
    public void setTier(String accessoryId, int tier) {
        if (!isValidAccessoryId(accessoryId)) {
            return;
        }
        ProgressEntry entry = entry(accessoryId);
        entry.tier = AccessoryProgression.clampTier(tier);
        resetMilestone(entry, accessoryId);
    }

    @Override
    public void reset(String accessoryId) {
        if (accessoryId != null) {
            entries.remove(accessoryId);
        }
    }

    @Override
    public void resetAll() {
        entries.clear();
    }

    @Override
    public Set<String> getTrackedAccessoryIds() {
        return Collections.unmodifiableSet(entries.keySet());
    }

    @Override
    public void copyFrom(IAccessoryMastery oldStore) {
        CompoundTag tag = new CompoundTag();
        oldStore.saveNBTData(tag);
        loadNBTData(tag);
    }

    @Override
    public void saveNBTData(CompoundTag tag) {
        CompoundTag accessories = new CompoundTag();
        for (Map.Entry<String, ProgressEntry> mapEntry : entries.entrySet()) {
            ProgressEntry entry = mapEntry.getValue();
            CompoundTag saved = new CompoundTag();
            saved.putInt(TAG_TIER, entry.tier);
            saved.putString(TAG_MILESTONE_ID, entry.milestoneId);
            saved.putLong(TAG_PROGRESS, entry.progress);
            saved.putBoolean(TAG_COMPLETE, entry.complete);
            accessories.put(mapEntry.getKey(), saved);
        }
        tag.put(TAG_ACCESSORIES, accessories);
    }

    @Override
    public void loadNBTData(CompoundTag tag) {
        entries.clear();
        if (!tag.contains(TAG_ACCESSORIES, Tag.TAG_COMPOUND)) {
            return;
        }

        CompoundTag accessories = tag.getCompound(TAG_ACCESSORIES);
        for (String accessoryId : accessories.getAllKeys()) {
            if (!isValidAccessoryId(accessoryId) || !accessories.contains(accessoryId, Tag.TAG_COMPOUND)) {
                continue;
            }
            CompoundTag saved = accessories.getCompound(accessoryId);
            ProgressEntry entry = new ProgressEntry();
            entry.tier = AccessoryProgression.clampTier(saved.getInt(TAG_TIER));
            entry.milestoneId = saved.getString(TAG_MILESTONE_ID);
            entry.progress = Math.max(0, saved.getLong(TAG_PROGRESS));
            entry.complete = saved.getBoolean(TAG_COMPLETE);

            String expectedMilestoneId = AccessoryProgression.currentMilestoneId(accessoryId, entry.tier);
            if (!entry.milestoneId.equals(expectedMilestoneId)) {
                entry.milestoneId = expectedMilestoneId;
                entry.progress = 0;
                entry.complete = false;
            }
            entries.put(accessoryId, entry);
        }
    }

    private boolean isCurrentMilestone(String accessoryId, String milestoneId) {
        return isValidAccessoryId(accessoryId)
                && milestoneId != null
                && !milestoneId.isEmpty()
                && milestoneId.equals(getCurrentMilestoneId(accessoryId));
    }

    private ProgressEntry entry(String accessoryId) {
        return entries.computeIfAbsent(accessoryId, id -> {
            ProgressEntry entry = new ProgressEntry();
            resetMilestone(entry, id);
            return entry;
        });
    }

    private static void resetMilestone(ProgressEntry entry, String accessoryId) {
        entry.milestoneId = AccessoryProgression.currentMilestoneId(accessoryId, entry.tier);
        entry.progress = 0;
        entry.complete = false;
    }

    private static boolean isValidAccessoryId(String accessoryId) {
        return accessoryId != null && !accessoryId.isBlank();
    }

    private static final class ProgressEntry {
        private int tier = AccessoryProgression.MIN_TIER;
        private String milestoneId = "";
        private long progress;
        private boolean complete;
    }
}
