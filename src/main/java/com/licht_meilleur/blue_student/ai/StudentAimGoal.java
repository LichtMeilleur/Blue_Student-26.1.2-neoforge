package com.licht_meilleur.blue_student.ai;

import com.licht_meilleur.blue_student.entity.AbstractStudentEntity;
import com.licht_meilleur.blue_student.entity.HikariEntity;
import com.licht_meilleur.blue_student.entity.HinaEntity;
import com.licht_meilleur.blue_student.entity.NozomiEntity;
import com.licht_meilleur.blue_student.student.IStudentEntity;
import com.licht_meilleur.blue_student.student.LookRequest;
import com.licht_meilleur.blue_student.student.StudentAiMode;
import com.licht_meilleur.blue_student.student.StudentBrAction;
import com.licht_meilleur.blue_student.student.StudentForm;
import com.licht_meilleur.blue_student.weapon.HitscanWeaponAction;
import com.licht_meilleur.blue_student.weapon.ProjectileWeaponAction;
import com.licht_meilleur.blue_student.weapon.ShotgunHitscanWeaponAction;
import com.licht_meilleur.blue_student.weapon.WeaponAction;
import com.licht_meilleur.blue_student.weapon.WeaponSpec;
import com.licht_meilleur.blue_student.weapon.WeaponSpecs;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class StudentAimGoal extends Goal {

    private final PathfinderMob mob;
    private final IStudentEntity student;

    private final WeaponAction projectileAction = new ProjectileWeaponAction();
    private final WeaponAction hitscanAction = new HitscanWeaponAction();
    private final WeaponAction shotgunHitscanAction = new ShotgunHitscanWeaponAction();

    private LivingEntity fireTarget;
    private IStudentEntity.FireChannel fireChannel = IStudentEntity.FireChannel.MAIN;
    private int aimTicks;

    private static final int AIM_TICKS = 1;

    private LookRequest activeLook;

    public StudentAimGoal(PathfinderMob mob, IStudentEntity student) {
        this.mob = mob;
        this.student = student;
        this.setFlags(EnumSet.of(Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (student.getAiMode() != StudentAiMode.FOLLOW
                && student.getAiMode() != StudentAiMode.SECURITY) return false;

        LivingEntity target = mob.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void tick() {
        if (student.isEvading()) {
            fireTarget = null;
            aimTicks = 0;
            activeLook = null;
            return;
        }

        LookRequest incoming = student.consumeLookRequest();
        if (incoming != null) {
            if (activeLook == null || incoming.priority >= activeLook.priority) {
                activeLook = incoming;
            }
        }

        if (fireTarget == null) {
            if (mob instanceof AbstractStudentEntity ase && ase.getForm() == StudentForm.BR) {
                StudentBrAction a = ase.getBrActionServer();
                boolean dodge = (a == StudentBrAction.DODGE_SHOT);

                IStudentEntity.FireChannel desired = IStudentEntity.FireChannel.MAIN;

                if (!dodge && a != null && a.shotKind == IStudentEntity.ShotKind.SUB) {
                    desired = IStudentEntity.FireChannel.SUB_L;
                }

                LivingEntity t = null;

                if (dodge) {
                    if (student.hasQueuedFire(IStudentEntity.FireChannel.MAIN)) {
                        t = student.consumeQueuedFireTarget(IStudentEntity.FireChannel.MAIN);
                    }
                    if (t != null && t.isAlive()) {
                        fireTarget = t;
                        fireChannel = IStudentEntity.FireChannel.MAIN;
                        aimTicks = 0;
                        stopNavigationIfNeeded();
                    }
                } else {
                    if (student.hasQueuedFire(desired)) {
                        t = student.consumeQueuedFireTarget(desired);
                    }

                    if (t != null && t.isAlive()) {
                        fireTarget = t;
                        fireChannel = desired;
                        aimTicks = 0;
                        stopNavigationIfNeeded();
                    }
                }
            } else {
                LivingEntity t = null;

                if (student.hasQueuedFire(IStudentEntity.FireChannel.SUB_L)) {
                    t = student.consumeQueuedFireTarget(IStudentEntity.FireChannel.SUB_L);
                    if (t != null && t.isAlive()) {
                        fireTarget = t;
                        fireChannel = IStudentEntity.FireChannel.SUB_L;
                        aimTicks = AIM_TICKS;
                        stopNavigationIfNeeded();
                    }
                } else if (student.hasQueuedFire(IStudentEntity.FireChannel.SUB_R)) {
                    t = student.consumeQueuedFireTarget(IStudentEntity.FireChannel.SUB_R);
                    if (t != null && t.isAlive()) {
                        fireTarget = t;
                        fireChannel = IStudentEntity.FireChannel.SUB_R;
                        aimTicks = AIM_TICKS;
                        stopNavigationIfNeeded();
                    }
                } else if (student.hasQueuedFire(IStudentEntity.FireChannel.MAIN)) {
                    t = student.consumeQueuedFireTarget(IStudentEntity.FireChannel.MAIN);
                    if (t != null && t.isAlive()) {
                        fireTarget = t;
                        fireChannel = IStudentEntity.FireChannel.MAIN;
                        aimTicks = AIM_TICKS;
                        stopNavigationIfNeeded();
                    }
                }
            }
        }

        AimResult aim = null;

        if (activeLook != null) {
            aim = computeAimFromLook(activeLook);
        }

        if (aim == null && fireTarget != null && fireTarget.isAlive()) {
            aim = aimAt(fireTarget.getX(), fireTarget.getEyeY(), fireTarget.getZ());
        }

        if (aim == null) {
            aim = computeAimMoveDir();
        }

        if (aim != null) {
            mob.getLookControl().setLookAt(aim.x, aim.y, aim.z, 90f, 90f);

            if (mob instanceof AbstractStudentEntity se) {
                se.setAimAngles(aim.yaw, aim.pitch);

                boolean lockBody = se.shouldLockBodyYawToMoveDir();
                if (!lockBody) {
                    float newYaw = approachAngle(mob.getYRot(), aim.yaw, 35f);
                    mob.setYRot(newYaw);
                    mob.yBodyRot = newYaw;
                    mob.yHeadRot = newYaw;
                }
            }
        }

        if (activeLook != null) {
            if (activeLook.holdTicks > 0) activeLook.holdTicks--;
            if (activeLook.holdTicks <= 0) activeLook = null;
        }

        if (fireTarget == null) return;

        aimTicks--;
        if (aimTicks > 0) return;

        StudentForm form = StudentForm.NORMAL;
        if (mob instanceof AbstractStudentEntity ase) {
            form = ase.getForm();
        }

        final IStudentEntity.FireChannel ch = fireChannel;
        final boolean isSubShot = (ch != IStudentEntity.FireChannel.MAIN);

        final WeaponSpec spec = WeaponSpecs.forStudent(student.getStudentId(), form, fireChannel);

        double dist = mob.distanceTo(fireTarget);
        boolean canSee = mob.getSensing().hasLineOfSight(fireTarget);

        boolean skillAim =
                (mob instanceof NozomiEntity n && n.isTrainSkillActive()) ||
                        (mob instanceof HikariEntity h && h.isGunTrainSkillActive());

        double maxRange = spec.range + (skillAim ? 8.0 : 0.0);

        if ((!canSee && !skillAim) || dist > maxRange) {
            fireTarget = null;
            return;
        }

        if (mob instanceof AbstractStudentEntity se) {
            se.faceTargetForShot(fireTarget, 35f, 25f);
        }

        boolean fired;
        if (spec.fxType == WeaponSpec.FxType.SHOTGUN) {
            fired = shotgunHitscanAction.shoot(student, fireTarget, spec);
        } else {
            fired = switch (spec.type) {
                case PROJECTILE -> projectileAction.shoot(student, fireTarget, spec);
                case HITSCAN -> hitscanAction.shoot(student, fireTarget, spec);
            };
        }

        if (fired) {
            student.requestShot(
                    isSubShot ? IStudentEntity.ShotKind.SUB : IStudentEntity.ShotKind.MAIN,
                    fireTarget
            );
            if (!spec.infiniteAmmo) student.consumeAmmo(1);
        }

        fireTarget = null;
    }

    private void stopNavigationIfNeeded() {
        boolean flying = false;
        if (mob instanceof HinaEntity hina) {
            flying = hina.isFlying();
        }

        boolean stopNav = true;

        if (mob instanceof AbstractStudentEntity se) {
            boolean isSub = (fireChannel != IStudentEntity.FireChannel.MAIN);
            stopNav = se.shouldStopNavigationForShot(isSub);
        }

        if (!flying && stopNav) {
            mob.getNavigation().stop();
        }
    }

    private AimResult computeAimMoveDir() {
        Vec3 v = mob.getDeltaMovement();
        Vec3 hv = new Vec3(v.x, 0, v.z);

        if (hv.lengthSqr() > 1.0e-5) {
            Vec3 p = mob.position().add(hv.normalize().scale(2.0));
            return aimAt(p.x, mob.getEyeY(), p.z);
        }

        if (!mob.getNavigation().isDone()) {
            Path path = mob.getNavigation().getPath();
            if (path != null && !path.isDone()) {
                int idx = path.getNextNodeIndex();
                if (idx < path.getNodeCount()) {
                    var nodePos = path.getNodePos(idx);
                    Vec3 p = new Vec3(
                            nodePos.getX() + 0.5,
                            nodePos.getY() + 0.5,
                            nodePos.getZ() + 0.5
                    );
                    return aimAt(p.x, mob.getEyeY(), p.z);
                }
            }
        }

        return null;
    }

    private AimResult computeAimFromLook(LookRequest r) {
        if (r == null) return null;

        return switch (r.type) {
            case NONE -> null;

            case TARGET -> {
                if (r.target == null || !r.target.isAlive()) yield null;
                yield aimAt(r.target.getX(), r.target.getEyeY(), r.target.getZ());
            }

            case MOVE_DIR -> computeAimMoveDir();

            case POS -> {
                if (r.pos == null) yield null;
                yield aimAt(r.pos.x, r.pos.y, r.pos.z);
            }

            default -> null;
        };
    }

    private AimResult aimAt(double x, double y, double z) {
        double dx = x - mob.getX();
        double dz = z - mob.getZ();
        double dy = y - mob.getEyeY();
        double horiz = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float pitch = (float) (-Math.toDegrees(Math.atan2(dy, horiz)));

        return new AimResult(x, y, z, yaw, pitch);
    }

    private float approachAngle(float cur, float target, float maxStep) {
        float delta = Mth.wrapDegrees(target - cur);
        if (delta > maxStep) delta = maxStep;
        if (delta < -maxStep) delta = -maxStep;
        return cur + delta;
    }

    private record AimResult(double x, double y, double z, float yaw, float pitch) {}
}