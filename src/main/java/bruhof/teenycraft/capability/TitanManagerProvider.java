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

public class TitanManagerProvider implements ICapabilitySerializable<CompoundTag> {
    public static Capability<ITitanManager> TITAN_MANAGER = CapabilityManager.get(new CapabilityToken<ITitanManager>() { });

    private ITitanManager backend = null;
    private final LazyOptional<ITitanManager> optional = LazyOptional.of(this::createTitanManager);

    private ITitanManager createTitanManager() {
        if (this.backend == null) {
            this.backend = new TitanManager();
        }
        return this.backend;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == TITAN_MANAGER) {
            return optional.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        createTitanManager().saveNBTData(nbt);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        createTitanManager().loadNBTData(nbt);
    }
}
