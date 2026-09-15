package com.elfmcys.yesstevemodel.mixin.client;

import com.elfmcys.yesstevemodel.client.renderer.ModelPreviewRenderer;
//? if >=1.19.3 && <1.20 {
import com.mojang.blaze3d.vertex.PoseStack;
//?}
//? if >=1.20 {
import net.minecraft.client.gui.GuiGraphics;
//?}
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({InventoryScreen.class})
public class InventoryScreenMixin {
    // 1.20.1：静态方法 renderEntityInInventoryFollowsMouse(GuiGraphics,IIIFF,LivingEntity)。
    // remap 必须走默认 true：1.20.1 生产运行时是 mojmap 类名 + SRG 方法名混合口径
    //（m2-smoke-gate 生产实测：remap=false 时字面 mojmap 名找不到目标=FATAL）。dev 全
    // mojmap 不受影响；1.16.5：无 FollowsMouse 变体，等价注入点为静态方法
    // renderEntityInInventory(IIIFF,LivingEntity)（1.16.5 InventoryScreen.java:101，
    // render():98 调用），remap 同样走默认 true 由 AP 写入 SRG refmap。
    //? if >=1.20 && forge {
    @Inject(at = {@At("HEAD")}, method = {"renderEntityInInventoryFollowsMouse(Lnet/minecraft/client/gui/GuiGraphics;IIIFFLnet/minecraft/world/entity/LivingEntity;)V"})
    private static void renderEntityInInventoryFollowsAnglePre(GuiGraphics guiGraphics, int x, int y, int scale, float angleXComponent, float angleYComponent, LivingEntity entity, CallbackInfo ci) {
        ModelPreviewRenderer.setPreviewMode(true);
    }

    @Inject(at = {@At("RETURN")}, method = {"renderEntityInInventoryFollowsMouse(Lnet/minecraft/client/gui/GuiGraphics;IIIFFLnet/minecraft/world/entity/LivingEntity;)V"})
    private static void renderEntityInInventoryFollowsAnglePost(GuiGraphics guiGraphics, int x, int y, int scale, float angleXComponent, float angleYComponent, LivingEntity entity, CallbackInfo ci) {
        ModelPreviewRenderer.setPreviewMode(false);
    }
    //?}
    // 1.20.2+（neoforge 三线）：bounding box 扩为 x1,y1,x2,y2 五 int + scale/angleX/angleY 三 float。
    // 注释态包裹：1.20.1 vcs 直通编译原文，裸 @Inject 会被 mixin AP 解析 1.20.2+ 签名失败
    //（d8fcfbd 回归实证）；其余 <1.19.3 块同款注释态先例
    //? if neoforge && >=1.20 && <26 {
    /*@Inject(at = {@At("HEAD")}, method = {"renderEntityInInventoryFollowsMouse(Lnet/minecraft/client/gui/GuiGraphics;IIIIIFFFLnet/minecraft/world/entity/LivingEntity;)V"})
    private static void renderEntityInInventoryFollowsAnglePreNeo(GuiGraphics guiGraphics, int x, int y, int x2, int y2, int scale, float angleXComponent, float angleYComponent, float partialTicks, LivingEntity entity, CallbackInfo ci) {
        ModelPreviewRenderer.setPreviewMode(true);
    }

    @Inject(at = {@At("RETURN")}, method = {"renderEntityInInventoryFollowsMouse(Lnet/minecraft/client/gui/GuiGraphics;IIIIIFFFLnet/minecraft/world/entity/LivingEntity;)V"})
    private static void renderEntityInInventoryFollowsAnglePostNeo(GuiGraphics guiGraphics, int x, int y, int x2, int y2, int scale, float angleXComponent, float angleYComponent, float partialTicks, LivingEntity entity, CallbackInfo ci) {
        ModelPreviewRenderer.setPreviewMode(false);
    }
     *///?}
    // 26.x：方法改名 renderEntityInInventoryFollowsMouse → extractEntityInInventoryFollowsMouse
    //（26.1.2 InventoryScreen.java:105，参数形同构 GuiGraphics;IIIIIFFFLivingEntity;——
    // GuiGraphics 由生成树规则改写 GuiGraphicsExtractor）。HEAD/RETURN 双 @Inject 语义不变：
    // 包裹 vanilla 玩家模型 extract，置预览模式标志位。
    //? if neoforge && >=26 {
    /*@Inject(at = {@At("HEAD")}, method = {"extractEntityInInventoryFollowsMouse(Lnet/minecraft/client/gui/GuiGraphics;IIIIIFFFLnet/minecraft/world/entity/LivingEntity;)V"})
    private static void renderEntityInInventoryFollowsAnglePreNeo26(GuiGraphics guiGraphics, int x, int y, int x2, int y2, int scale, float angleXComponent, float angleYComponent, float partialTicks, LivingEntity entity, CallbackInfo ci) {
        ModelPreviewRenderer.setPreviewMode(true);
    }

    @Inject(at = {@At("RETURN")}, method = {"extractEntityInInventoryFollowsMouse(Lnet/minecraft/client/gui/GuiGraphics;IIIIIFFFLnet/minecraft/world/entity/LivingEntity;)V"})
    private static void renderEntityInInventoryFollowsAnglePostNeo26(GuiGraphics guiGraphics, int x, int y, int x2, int y2, int scale, float angleXComponent, float angleYComponent, float partialTicks, LivingEntity entity, CallbackInfo ci) {
        ModelPreviewRenderer.setPreviewMode(false);
    }
     *///?}
    // 1.19.3：renderEntityInInventory 仍是 (IIIFF, LivingEntity) 六参（1.19.3 merged jar
    // javap 实证；GuiGraphics/PoseStack 版与 FollowsMouse 更名 1.19.4 起）→ 与 <1.19.3 同形
    //? if >=1.19.3 && <1.20 {
    /*
    @Inject(at = {@At("HEAD")}, method = {"renderEntityInInventory(IIIFFLnet/minecraft/world/entity/LivingEntity;)V"})
    private static void renderEntityInInventoryFollowsAnglePre(int x, int y, int scale, float angleXComponent, float angleYComponent, LivingEntity entity, CallbackInfo ci) {
        ModelPreviewRenderer.setPreviewMode(true);
    }

    @Inject(at = {@At("RETURN")}, method = {"renderEntityInInventory(IIIFFLnet/minecraft/world/entity/LivingEntity;)V"})
    private static void renderEntityInInventoryFollowsAnglePost(int x, int y, int scale, float angleXComponent, float angleYComponent, LivingEntity entity, CallbackInfo ci) {
        ModelPreviewRenderer.setPreviewMode(false);
    }
     *///?}
    //? if <1.19.3 {
    /*@Inject(at = {@At("HEAD")}, method = {"renderEntityInInventory(IIIFFLnet/minecraft/world/entity/LivingEntity;)V"})
    private static void renderEntityInInventoryFollowsAnglePre(int x, int y, int scale, float angleXComponent, float angleYComponent, LivingEntity entity, CallbackInfo ci) {
        ModelPreviewRenderer.setPreviewMode(true);
    }

    @Inject(at = {@At("RETURN")}, method = {"renderEntityInInventory(IIIFFLnet/minecraft/world/entity/LivingEntity;)V"})
    private static void renderEntityInInventoryFollowsAnglePost(int x, int y, int scale, float angleXComponent, float angleYComponent, LivingEntity entity, CallbackInfo ci) {
        ModelPreviewRenderer.setPreviewMode(false);
    }
     *///?}
}
