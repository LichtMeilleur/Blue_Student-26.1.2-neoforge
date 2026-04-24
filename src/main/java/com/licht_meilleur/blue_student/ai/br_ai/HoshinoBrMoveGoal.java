package com.licht_meilleur.blue_student.ai.br_ai;

import com.licht_meilleur.blue_student.ai.prediction.EnemyIntentAnalyzer;
import com.licht_meilleur.blue_student.ai.prediction.EnemyIntentRead;
import com.licht_meilleur.blue_student.entity.AbstractStudentEntity;
import com.licht_meilleur.blue_student.student.IStudentEntity;
import com.licht_meilleur.blue_student.student.StudentAiMode;
import com.licht_meilleur.blue_student.student.StudentForm;
import com.licht_meilleur.blue_student.weapon.WeaponSpec;
import com.licht_meilleur.blue_student.weapon.WeaponSpecs;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.EnumSet;

public class HoshinoBrMoveGoal extends Goal {

    private final PathfinderMob mob;
    private final IStudentEntity student;

    private LivingEntity target;

    private int repathCooldown = 0;
    private int commitTicks = 0;

    private MoveIntent currentIntent = MoveIntent.HOLD;

    private static final int REPATH_INTERVAL = 10;

    private static final double SPEED_RETREAT = 1.25;
    private static final double SPEED_APPROACH = 1.15;

    private static final double PREFERRED_MIN = 4.0;
    private static final double PREFERRED_MAX = 8.5;

    public HoshinoBrMoveGoal(PathfinderMob mob, IStudentEntity student) {
        this.mob = mob;
        this.student = student;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    public enum MoveIntent {
        RETREAT,
        APPROACH,
        HOLD
    }

    @Override
    public boolean canUse() {
        if (!(mob.level() instanceof ServerLevel)) return false;

        if (!(student instanceof AbstractStudentEntity ase)) return false;
        if (ase.getForm() != StudentForm.BR) return false;

        StudentAiMode mode = student.getAiMode();
        if (mode != StudentAiMode.FOLLOW && mode != StudentAiMode.SECURITY) return false;

        target = mob.getTarget();
        if (target == null || !target.isAlive()) {
            target = findNearestHostile();
            if (target != null) {
                mob.setTarget(target);
            }
        }

        Player owner = resolveOwner();
        if (owner != null && owner.level() == mob.level()) {
            double dOwner = mob.distanceToSqr(owner);
            if (dOwner > 22.0 * 22.0) return false;
        }

        return target != null;
    }

    @Nullable
    private Player resolveOwner() {
        if (student instanceof AbstractStudentEntity se) {
            Player p = se.getOwnerPlayer();
            if (p != null) return p;
        }
        return null;
    }

    @Override
    public boolean canContinueToUse() {
        if (!(mob.level() instanceof ServerLevel)) return false;

        if (!(student instanceof AbstractStudentEntity ase)) return false;
        if (ase.getForm() != StudentForm.BR) return false;

        if (target == null || !target.isAlive()) return false;

        return true;
    }

    @Override
    public void start() {
        repathCooldown = 0;
        commitTicks = 0;
        currentIntent = MoveIntent.HOLD;
    }

    @Override
    public void stop() {
        target = null;
        mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (!(mob.level() instanceof ServerLevel)) return;
        if (target == null || !target.isAlive()) return;

        if (repathCooldown > 0) repathCooldown--;
        if (commitTicks > 0) commitTicks--;

        final double dist = mob.distanceTo(target);

        // 🔥 向きだけは強めに補正（重要）
        student.requestLookTarget(target, 90, 2);

        EnemyIntentRead read = EnemyIntentAnalyzer.analyze(mob, target);

        if (commitTicks <= 0) {
            currentIntent = decideIntent(dist, read);
        }

        switch (currentIntent) {
            case RETREAT -> moveRetreat();
            case APPROACH -> moveApproach();
            case HOLD -> mob.getNavigation().stop();
        }
    }

    private MoveIntent decideIntent(double dist, EnemyIntentRead read) {

        // 近すぎ → 後退
        if (dist < 3.0) {
            commitTicks = 6;
            return MoveIntent.RETREAT;
        }

        // 遠すぎ → 接近
        if (dist > 9.5) {
            commitTicks = 8;
            return MoveIntent.APPROACH;
        }

        // 予測ベース
        if (read != null) {
            if (read.closingIn && read.meleeThreat > 0.5) {
                commitTicks = 6;
                return MoveIntent.RETREAT;
            }

            if (read.backingOff) {
                commitTicks = 6;
                return MoveIntent.APPROACH;
            }
        }

        // 通常は停止（戦闘AIに任せる）
        commitTicks = 6;
        return MoveIntent.HOLD;
    }

    private void moveRetreat() {
        if (repathCooldown > 0) return;
        repathCooldown = REPATH_INTERVAL;

        Vec3 away = mob.position().subtract(target.position());
        Vec3 flat = new Vec3(away.x, 0, away.z);

        if (flat.lengthSqr() < 1.0e-6) return;

        Vec3 dir = flat.normalize();
        Vec3 pos = mob.position().add(dir.scale(5.0));

        mob.getNavigation().moveTo(pos.x, pos.y, pos.z, SPEED_RETREAT);
    }

    private void moveApproach() {
        if (repathCooldown > 0) return;
        repathCooldown = REPATH_INTERVAL;

        Vec3 pos = target.position();
        mob.getNavigation().moveTo(pos.x, pos.y, pos.z, SPEED_APPROACH);
    }

    private LivingEntity findNearestHostile() {
        WeaponSpec spec = WeaponSpecs.forStudent(
                student.getStudentId(),
                StudentForm.BR,
                IStudentEntity.FireChannel.MAIN
        );

        AABB box = mob.getBoundingBox().inflate(spec.range + 10.0);

        return mob.level()
                .getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e instanceof Monster)
                .stream()
                .min(Comparator.comparingDouble(mob::distanceToSqr))
                .orElse(null);
    }
}