package com.licht_meilleur.blue_student.student;

import com.licht_meilleur.blue_student.weapon.WeaponSpec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public interface IStudentEntity {
    StudentId getStudentId();

    StudentAiMode getAiMode();
    void setAiMode(StudentAiMode mode);

    BlockPos getSecurityPos();
    void setSecurityPos(BlockPos pos);

    UUID getOwnerUuid();
    void setOwnerUuid(UUID uuid);

    Container getStudentInventory();

    // ===== ammo/reload =====
    int getAmmoInMag();
    void consumeAmmo(int amount);

    boolean isReloading();
    void startReload(WeaponSpec spec);
    void tickReload(WeaponSpec spec);

    void requestLookTarget(LivingEntity target, int priority, int holdTicks);
    void requestLookAwayFrom(LivingEntity target, int priority, int holdTicks);
    void requestLookWorldDir(Vec3 dir, int priority, int holdTicks);
    void requestLookMoveDir(int priority, int holdTicks);
    void requestLookPos(Vec3 pos, int priority, int holdTicks);

    // AimGoalが読む
    LookRequest consumeLookRequest();

    // ===== Evade state =====
    boolean isEvading();
    void setEvading(boolean v);

    enum ShotKind {
        MAIN,
        SUB,
        NONE
    }

    // 互換用（任意）
    default void requestShot() {
        requestShot(ShotKind.MAIN, null);
    }

    // ===== animation / presentation hooks =====
    default void requestShot(ShotKind kind) {
        requestShot(kind, null);
    }

    default void requestShot(ShotKind kind, LivingEntity target) {
        // default no-op
    }

    default void requestReload() {
    }

    default void requestDodge() {
    }

    default void requestJump() {
    }

    default void requestFall() {
    }

    default void requestExit() {
    }

    default void requestSwim() {
    }

    default void requestSit() {
    }

    default void requestBrAction(StudentBrAction action, int holdTicks, boolean restart) {
        requestBrAction(action, holdTicks);
    }

    enum FireChannel {
        MAIN,
        SUB_L,
        SUB_R
    }

    // ===== 新API（チャンネル）=====
    void queueFire(LivingEntity target, FireChannel ch);
    boolean hasQueuedFire(FireChannel ch);
    LivingEntity consumeQueuedFireTarget(FireChannel ch);

    FireChannel getLastConsumedFireChannel();

    // ===== 後方互換（既存コードを壊さない）=====
    default void queueFire(LivingEntity target) {
        queueFire(target, FireChannel.MAIN);
    }

    default void queueFireSub(LivingEntity target) {
        queueFire(target, FireChannel.SUB_L);
    }

    default boolean hasQueuedFire() {
        return hasQueuedFire(FireChannel.MAIN);
    }

    default boolean hasQueuedFireSub() {
        return hasQueuedFire(FireChannel.SUB_L);
    }

    default LivingEntity consumeQueuedFireTarget() {
        return consumeQueuedFireTarget(FireChannel.MAIN);
    }

    default LivingEntity consumeQueuedFireSubTarget() {
        return consumeQueuedFireTarget(FireChannel.SUB_L);
    }

    default boolean consumeQueuedFireIsSub() {
        FireChannel ch = getLastConsumedFireChannel();
        return ch != null && ch != FireChannel.MAIN;
    }

    void requestBrAction(StudentBrAction action, int holdTicks);

    void requestLookDir(Vec3 dir, int yawSpeed, int pitchSpeed);
}