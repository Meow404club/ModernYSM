package com.elfmcys.yesstevemodel.util;

import com.elfmcys.yesstevemodel.client.entity.IPreviewAnimatable;
import com.elfmcys.yesstevemodel.client.renderer.ModelPreviewRenderer;
import com.elfmcys.yesstevemodel.geckolib3.core.AnimatableEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
//? if >=1.16.2
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import rip.ysm.compat.oculus.OculusCompat;

public final class CameraUtil {
    // 1.16.1 无 CameraType（1.16.2 引入，官方 1161 client.txt 零命中；1.16.1 视角=Minecraft
    // 私有 int 字段 thirdPersonView，SRG field_71467_ac。官方 1161 映射未命名该字段 → dev
    // mojmap jar 与生产 SRG 运行时字段名一致）→ 反射一次缓存；取值 0/1/2 与 1.16.2+
    // CameraType.ordinal() 同构。反射失败降级=非第一人称（1.16.5 未绑定 tag 同款守卫语义，
    // 功能差入 tasks.feature-debts）。
    //? if <1.16.2 {
    /*private static final java.lang.reflect.Field THIRD_PERSON_VIEW;

    static {
        java.lang.reflect.Field f = null;
        try {
            f = net.minecraft.client.Minecraft.class.getDeclaredField("field_71467_ac");
            f.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            // 保持 null：getCameraType/isFirstPerson 走降级分支
        }
        THIRD_PERSON_VIEW = f;
    }

    private static int thirdPersonView() {
        try {
            return THIRD_PERSON_VIEW.getInt(Minecraft.getInstance());
        } catch (IllegalAccessException e) {
            return 1;
        }
    }

    // Options.getCameraType().isFirstPerson() 的 <1.16.2 等价（Options 本无该 API）
    public static boolean isFirstPersonView() {
        return thirdPersonView() == 0;
    }
     *///?}
    public static int getCameraType(IContext<? extends Entity> IContext) {
        if (IContext.entity() == Minecraft.getInstance().player && ModelPreviewRenderer.isFirstPerson()) {
            //? if <1.16.2
            /*return thirdPersonView();*/
            //? if >=1.16.2
            return IContext.mc().options.getCameraType().ordinal();
        }
        //? if <1.16.2
        /*return 2;*/
        //? if >=1.16.2
        return CameraType.THIRD_PERSON_FRONT.ordinal();
    }

    public static boolean isFirstPerson(AnimatableEntity<? extends Entity> animatableEntity) {
        //? if <1.16.2
        /*return animatableEntity.getEntity() == Minecraft.getInstance().player && ModelPreviewRenderer.isFirstPerson() && !OculusCompat.isPBRActive() && thirdPersonView() == 0;*/
        //? if >=1.16.2
        return animatableEntity.getEntity() == Minecraft.getInstance().player && ModelPreviewRenderer.isFirstPerson() && !OculusCompat.isPBRActive() && Minecraft.getInstance().options.getCameraType().ordinal() == CameraType.FIRST_PERSON.ordinal();
    }

    public static boolean isThirdPerson(IContext<? extends Entity> IContext) {
        return isThirdPersonModel(IContext.geoInstance());
    }

    public static boolean isThirdPersonModel(AnimatableEntity<?> model) {
        return (model instanceof IPreviewAnimatable) || ModelPreviewRenderer.isPreview();
    }
}
