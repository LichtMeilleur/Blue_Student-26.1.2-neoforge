package com.licht_meilleur.blue_student.client;

import com.geckolib.cache.model.GeoBone;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.licht_meilleur.blue_student.entity.AbstractStudentEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.lang.reflect.Method;

public class StudentRenderer<T extends AbstractStudentEntity, R extends LivingEntityRenderState & GeoRenderState>
        extends GeoEntityRenderer<T, R> {

    public StudentRenderer(EntityRendererProvider.Context ctx, GeoModel<T> model, float shadowRadius) {
        super(ctx, model);
        this.shadowRadius = shadowRadius;
    }

    @Override
    public void addRenderData(T animatable, Void relatedObject, R renderState, float partialTick) {
        super.addRenderData(animatable, relatedObject, renderState, partialTick);

        renderState.addGeckolibData(StudentRenderTickets.FORM, animatable.getRenderForm());
        renderState.addGeckolibData(StudentRenderTickets.HEAD_YAW_RAD, animatable.getHeadYawRadForRender());
        renderState.addGeckolibData(StudentRenderTickets.HEAD_PITCH_RAD, animatable.getHeadPitchRadForRender());
        renderState.addGeckolibData(StudentRenderTickets.ARM_YAW_RAD, animatable.getArmYawRadForRender());
        renderState.addGeckolibData(StudentRenderTickets.ARM_PITCH_RAD, animatable.getArmPitchRadForRender());
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo<R> renderPassInfo, com.geckolib.renderer.base.BoneSnapshots snapshots) {
        super.adjustModelBonesForRender(renderPassInfo, snapshots);

        Float headYaw = renderPassInfo.renderState().getGeckolibData(StudentRenderTickets.HEAD_YAW_RAD);
        Float headPitch = renderPassInfo.renderState().getGeckolibData(StudentRenderTickets.HEAD_PITCH_RAD);
        Float armYaw = renderPassInfo.renderState().getGeckolibData(StudentRenderTickets.ARM_YAW_RAD);
        Float armPitch = renderPassInfo.renderState().getGeckolibData(StudentRenderTickets.ARM_PITCH_RAD);

        snapshots.ifPresent("Head", head -> {
            head.setRotY(headYaw != null ? headYaw * 0.5f : 0.0f);
            head.setRotX(-(headPitch * 0.5f));
        });

        snapshots.ifPresent("Arm", arm -> {
            arm.setRotY(armYaw * 0.4f);
            arm.setRotX(-(armPitch * 0.9f));
        });
    }

    protected Vec3 worldPosFromCurrentMatrix(PoseStack poseStack, float lx, float ly, float lz) {
        Matrix4f mat = new Matrix4f(poseStack.last().pose());
        Vector4f v = new Vector4f(lx, ly, lz, 1.0f);
        v.mul(mat);
        return new Vec3(v.x, v.y, v.z);
    }

    protected Vec3 tryGetLocatorLocalPos(GeoBone bone, String locatorName) {
        try {
            Method m = bone.getClass().getMethod("getLocators");
            Object map = m.invoke(bone);
            if (map instanceof java.util.Map<?, ?> mp) {
                if (!mp.containsKey(locatorName)) return null;
                Object v = mp.get(locatorName);
                if (v == null) return null;

                return new Vec3(
                        readFieldAsDouble(v, "x"),
                        readFieldAsDouble(v, "y"),
                        readFieldAsDouble(v, "z")
                );
            }
        } catch (Throwable ignored) {
        }

        try {
            Method m = bone.getClass().getMethod("getLocatorPosition", String.class);
            Object r = m.invoke(bone, locatorName);
            if (r == null) return null;

            double x = readFieldAsDouble(r, "x");
            double y = readFieldAsDouble(r, "y");
            double z = readFieldAsDouble(r, "z");

            if (x == 0.0 && y == 0.0 && z == 0.0) return null;

            return new Vec3(x, y, z);
        } catch (Throwable ignored) {
        }

        return null;
    }

    protected double readFieldAsDouble(Object obj, String field) {
        try {
            var f = obj.getClass().getField(field);
            Object v = f.get(obj);
            if (v instanceof Number n) return n.doubleValue();
        } catch (Throwable ignored) {
        }

        try {
            String getterName = "get" + Character.toUpperCase(field.charAt(0)) + field.substring(1);
            Method m = obj.getClass().getMethod(getterName);
            Object v = m.invoke(obj);
            if (v instanceof Number n) return n.doubleValue();
        } catch (Throwable ignored) {
        }

        return 0.0;
    }
}