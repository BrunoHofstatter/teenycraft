package bruhof.teenycraft.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AccessoryMasteryProvider implements ICapabilitySerializable<CompoundTag> {
    public static final Capability<IAccessoryMastery> ACCESSORY_MASTERY =
            CapabilityManager.get(new CapabilityToken<IAccessoryMastery>() { });

    private IAccessoryMastery backend;
    private final LazyOptional<IAccessoryMastery> optional = LazyOptional.of(this::createAccessoryMastery);

    private IAccessoryMastery createAccessoryMastery() {
        if (backend == null) {
            backend = new AccessoryMastery();
        }
        return backend;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ACCESSORY_MASTERY) {
            return optional.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        createAccessoryMastery().saveNBTData(tag);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        createAccessoryMastery().loadNBTData(nbt);
    }
}
