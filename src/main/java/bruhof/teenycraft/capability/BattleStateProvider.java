package bruhof.teenycraft.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BattleStateProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {

    public static Capability<IBattleState> BATTLE_STATE = CapabilityManager.get(new CapabilityToken<IBattleState>() { });

    private IBattleState backend = null;
    private final LazyOptional<IBattleState> optional = LazyOptional.of(this::createBattleState);

    private IBattleState createBattleState() {
        if (this.backend == null) {
            this.backend = new BattleState();
        }
        return this.backend;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == BATTLE_STATE) {
            return optional.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        createBattleState().saveNBTData(nbt);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        createBattleState().loadNBTData(nbt);
    }
}
