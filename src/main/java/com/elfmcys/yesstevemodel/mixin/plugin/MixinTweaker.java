package com.elfmcys.yesstevemodel.mixin.plugin;

import com.elfmcys.yesstevemodel.util.obfuscate.Keep;
//? if <1.17 {
/*import com.llamalad7.mixinextras.MixinExtrasBootstrap;
*///?}
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class MixinTweaker implements IMixinConfigPlugin {

    @Keep
    public void onLoad(String str) {
        // 1.16.5 Forge 不内置 MixinExtras（1.20.1 Forge 47.x 自带）：common jar 已平铺内嵌进
        // 1.16.5 产物（build.unimined.gradle.kts embedMixinExtras），在此配置加载早期显式引导，
        // 使 EntityRenderDispatcherMixin 的 @WrapWithCondition 生效；onLoad 先于本配置任何
        // mixin 应用，时序安全。1.20.1 侧该调用整体剔除，运行时行为零变化。
        //? if <1.17 {
        /*MixinExtrasBootstrap.init();
        *///?}
    }

    @Keep
    public String getRefMapperConfig() {
        return null;
    }

    @Keep
    public boolean shouldApplyMixin(String str, String str2) {
        return true;
    }

    @Keep
    public void acceptTargets(Set<String> set, Set<String> set2) {
    }

    @Keep
    public List<String> getMixins() {
        return null;
    }

    @Keep
    public void preApply(String str, ClassNode classNode, String str2, IMixinInfo iMixinInfo) {
    }

    @Keep
    public void postApply(String str, ClassNode classNode, String str2, IMixinInfo iMixinInfo) {
    }
}
