package bruhof.teenycraft.entity.custom;

import bruhof.teenycraft.TeenyBalance;
import bruhof.teenycraft.battle.ai.BattleAiGoal;
import bruhof.teenycraft.battle.ai.BattleAiProfile;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class EntityTeenyDummy extends PathfinderMob {
    private BattleAiProfile aiProfile = BattleAiProfile.DEFAULT;

    public EntityTeenyDummy(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1000.0D)
                .add(Attributes.MOVEMENT_SPEED, TeenyBalance.AI_DUMMY_BASE_MOVE_SPEED)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new BattleAiGoal(this));
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    public BattleAiProfile getAiProfile() {
        return aiProfile;
    }

    public void setAiProfile(BattleAiProfile aiProfile) {
        this.aiProfile = aiProfile != null ? aiProfile : BattleAiProfile.DEFAULT;
    }
}
