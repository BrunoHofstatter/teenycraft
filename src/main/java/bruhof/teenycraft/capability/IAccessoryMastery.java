package bruhof.teenycraft.capability;

import net.minecraft.nbt.CompoundTag;

import java.util.Set;

public interface IAccessoryMastery {
    int getTier(String accessoryId);
    String getCurrentMilestoneId(String accessoryId);
    long getMilestoneProgress(String accessoryId);
    boolean isCurrentMilestoneComplete(String accessoryId);
    boolean addMilestoneProgress(String accessoryId, String milestoneId, long amount);
    boolean setMilestoneProgress(String accessoryId, String milestoneId, long amount);
    boolean completeMilestone(String accessoryId, String milestoneId);
    boolean unlockNextTier(String accessoryId);
    void setTier(String accessoryId, int tier);
    void reset(String accessoryId);
    void resetAll();
    Set<String> getTrackedAccessoryIds();
    void copyFrom(IAccessoryMastery oldStore);
    void saveNBTData(CompoundTag tag);
    void loadNBTData(CompoundTag tag);
}
