package com.elfmcys.yesstevemodel.mixin.client;
// Renderable 即 1.19.3 前的 Widget（1.19.3 改名：1182/1192 AbstractWidget implements Widget，
// 1194 起 implements Renderable）
//? if >=1.19.4 {
import net.minecraft.client.gui.components.Renderable;
//?}
//? if <1.19.4 {
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
    //? if >=1.19.4 {
    @Accessor("renderables")
    List<Renderable> ysm$getRenderables();
    //?}
    // 1.17~1.19.2：renderables 字段已在（1.17 引入），元素类型为旧名 Widget
    //? if >=1.17 && <1.19.4 {
    /*
    @Accessor("renderables")
    List<Widget> ysm$getRenderables();
     *///?}
    //? if <1.17 {
    /*@Accessor("buttons")
    List<Widget> ysm$getRenderables();
     *///?}
}
