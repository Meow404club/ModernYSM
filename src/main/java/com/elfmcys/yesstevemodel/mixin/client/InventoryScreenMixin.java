package com.elfmcys.yesstevemodel.mixin.client;

import com.elfmcys.yesstevemodel.client.renderer.ModelPreviewRenderer;
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
    //? if >=1.20 {
    @Inject(at = {@At("HEAD")}, method = {"renderEntityInInventoryFollowsMouse(Lnet/minecraft/client/gui/GuiGraphics;IIIFFLnet/minecraft/world/entity/LivingEntity;)V"})
    private static void renderEntityInInventoryFollowsAnglePre(GuiGraphics guiGraphics, int x, int y, int scale, float angleXComponent, float angleYComponent, LivingEntity entity, CallbackInfo ci) {
        ModelPreviewRenderer.setPreviewMode(true);
    }

    @Inject(at = {@At("RETURN")}, method = {"renderEntityInInventoryFollowsMouse(Lnet/minecraft/client/gui/GuiGraphics;IIIFFLnet/minecraft/world/entity/LivingEntity;)V"})
    private static void renderEntityInInventoryFollowsAnglePost(GuiGraphics guiGraphics, int x, int y, int scale, float angleXComponent, float angleYComponent, LivingEntity entity, CallbackInfo ci) {
        ModelPreviewRenderer.setPreviewMode(false);
    }
    //?} else {
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
