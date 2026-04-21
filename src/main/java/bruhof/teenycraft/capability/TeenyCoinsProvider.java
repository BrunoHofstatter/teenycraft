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

public class TeenyCoinsProvider implements ICapabilitySerializable<CompoundTag> {
    public static Capability<ITeenyCoins> TEENY_COINS = CapabilityManager.get(new CapabilityToken<ITeenyCoins>() { });

    private ITeenyCoins backend = null;
    private final LazyOptional<ITeenyCoins> optional = LazyOptional.of(this::createTeenyCoins);

    private ITeenyCoins createTeenyCoins() {
        if (this.backend == null) {
            this.backend = new TeenyCoins();
        }
        return this.backend;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == TEENY_COINS) {
            return optional.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        createTeenyCoins().saveNBTData(nbt);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        createTeenyCoins().loadNBTData(nbt);
    }
}
