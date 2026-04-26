package bruhof.teenycraft.battle.executor;

import bruhof.teenycraft.TeenyBalance;
import bruhof.teenycraft.battle.BattleFigure;
import bruhof.teenycraft.battle.effect.EffectInstance;
import bruhof.teenycraft.capability.IBattleState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class BattleTargeting {
    public record ArmedMine(BattleFigure figure, EffectInstance instance) {
    }

    private BattleTargeting() {
    }

    public static boolean isPairedOpponent(@Nullable IBattleState attackerState, @Nullable LivingEntity candidate) {
        return attackerState != null
                && candidate != null
                && attackerState.getOpponentEntityId() != null
                && attackerState.getOpponentEntityId().equals(candidate.getUUID());
    }

    @Nullable
    public static LivingEntity getPairedOpponent(LivingEntity attacker, IBattleState attackerState) {
        return getPairedOpponent(attacker, attackerState, -1.0);
    }

    @Nullable
    public static LivingEntity getPairedOpponent(LivingEntity attacker, IBattleState attackerState, double maxDistance) {
        if (attacker == null || attackerState == null) {
            return null;
        }

        LivingEntity opponent = attackerState.getOpponentEntity();
        if (!isPairedOpponent(attackerState, opponent) || attackerState.getOpponentBattleState() == null) {
            return null;
        }
        if (maxDistance > 0 && attacker.distanceToSqr(opponent) > maxDistance * maxDistance) {
            return null;
        }
        return opponent;
    }

    @Nullable
    public static LivingEntity getConeTarget(BattleAbilityContext context) {
        LivingEntity pairedOpponent = getPairedOpponent(context.attacker(), context.state());
        if (pairedOpponent == null) {
            return null;
        }

        double maxRange = TeenyBalance.getRangeValue(context.data().rangeTier);
        double halfAngleRad = Math.toRadians(TeenyBalance.RANGED_CONE_ANGLE / 2.0);
        double startHalfWidth = TeenyBalance.RANGED_START_WIDTH / 2.0;

        Vec3 rayOrigin = context.attacker().getEyePosition();
        Vec3 rayDir = context.attacker().getLookAngle().normalize();

        AABB targetBox = pairedOpponent.getBoundingBox();
        Vec3 boxCenter = targetBox.getCenter();
        Vec3 toCenter = boxCenter.subtract(rayOrigin);
        double distAlongRay = toCenter.dot(rayDir);

        if (distAlongRay < 0 || distAlongRay > maxRange) {
            return null;
        }

        Vec3 closestPointOnBox = getClosestPointOnAABB(targetBox, rayOrigin, rayDir);
        Vec3 toClosest = closestPointOnBox.subtract(rayOrigin);
        double distanceAlongRay = toClosest.dot(rayDir);
        Vec3 projection = rayOrigin.add(rayDir.scale(distanceAlongRay));
        double distToRaySq = closestPointOnBox.distanceToSqr(projection);

        double allowedRadius = startHalfWidth + (distAlongRay * Math.tan(halfAngleRad));
        return distToRaySq <= (allowedRadius * allowedRadius) ? pairedOpponent : null;
    }

    @Nullable
    public static ArmedMine findArmedMine(@Nullable IBattleState targetState, int slotIndex, @Nullable UUID casterId) {
        if (targetState == null || casterId == null) {
            return null;
        }

        String effectId = "remote_mine_" + slotIndex;
        for (BattleFigure figure : targetState.getTeam()) {
            EffectInstance mine = figure.getActiveEffects().get(effectId);
            if (mine != null && casterId.equals(mine.casterUUID)) {
                return new ArmedMine(figure, mine);
            }
        }

        return null;
    }

    private static Vec3 getClosestPointOnAABB(AABB box, Vec3 origin, Vec3 dir) {
        double x = clamp(origin.x, box.minX, box.maxX);
        double y = clamp(origin.y, box.minY, box.maxY);
        double z = clamp(origin.z, box.minZ, box.maxZ);
        return new Vec3(x, y, z);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
