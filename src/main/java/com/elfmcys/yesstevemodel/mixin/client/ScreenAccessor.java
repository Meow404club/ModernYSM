package com.elfmcys.yesstevemodel.mixin.client;
//? if >=1.17 {
import net.minecraft.client.gui.components.Renderable;
//?} else {
/*import net.minecraft.client.gui.components.Widget;
*///?}
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(Screen.class)
public interface ScreenAccessor {
    // 1.16.5 Screen 无 renderables 字段（1.17+ 引入），可渲染控件列表即 protected List<AbstractWidget> buttons
    //（1.16.5 Screen.java:55）；Widget 即 1.16.5 的"可渲染"接口（render(PoseStack,int,int,float)，
    // 与 1.20.1 Renderable 同签名），消费方按元素转型即可。擦除后 List==List，mixin accessor 校验通过。
    //? if >=1.17 {
    @Accessor("renderables")
    List<Renderable> ysm$getRenderables();
    //?} else {
    /*@Accessor("buttons")
    List<Widget> ysm$getRenderables();
    *///?}
}
