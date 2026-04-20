package com.licht_meilleur.blue_student.ai;

import com.licht_meilleur.blue_student.entity.AbstractStudentEntity;
import com.licht_meilleur.blue_student.student.IStudentEntity;
import com.licht_meilleur.blue_student.student.StudentAiMode;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.entity.vehicle.boat.ChestBoat;

import java.util.EnumSet;

public class StudentRideWithOwnerGoal extends Goal {
    private final PathfinderMob mob;
    private final IStudentEntity student;

    private Player owner;
    private Entity ownerVehicle;

    public StudentRideWithOwnerGoal(PathfinderMob mob, IStudentEntity student) {
        this.mob = mob;
        this.student = student;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!(mob.level() instanceof ServerLevel)) return false;

        StudentAiMode mode = student.getAiMode();
        if (mode != StudentAiMode.FOLLOW && mode != StudentAiMode.SECURITY) return false;

        owner = resolveOwner();
        if (owner == null || !owner.isAlive()) return false;

        ownerVehicle = owner.getVehicle();

        if (!isBoatLike(ownerVehicle)) return false;

        if (mob.getVehicle() == ownerVehicle) return false;

        return mob.distanceToSqr(owner) < (5.0 * 5.0);
    }

    @Override
    public boolean canContinueToUse() {
        if (!(mob.level() instanceof ServerLevel)) return false;
        if (owner == null || !owner.isAlive()) return false;

        ownerVehicle = owner.getVehicle();
        if (!isBoatLike(ownerVehicle)) return false;

        return mob.getVehicle() != ownerVehicle;
    }

    @Override
    public void start() {
        mob.getNavigation().stop();
    }

    @Override
    public void stop() {
        owner = null;
        ownerVehicle = null;
    }

    @Override
    public void tick() {
        if (owner == null || ownerVehicle == null) return;

        mob.getNavigation().moveTo(owner, 1.4);

        if (mob.distanceToSqr(owner) < (2.2 * 2.2)) {
            if (ownerVehicle.getPassengers().size() < 2) {
                mob.startRiding(ownerVehicle);
                mob.getNavigation().stop();
            }
        }
    }

    private Player resolveOwner() {
        if (student instanceof AbstractStudentEntity se) {
            Player p = se.getOwnerPlayer();
            if (p != null) return p;
        }
        return mob.level().getNearestPlayer(mob, 32.0);
    }

    private boolean isBoatLike(Entity e) {
        return (e instanceof Boat) || (e instanceof ChestBoat);
    }
}