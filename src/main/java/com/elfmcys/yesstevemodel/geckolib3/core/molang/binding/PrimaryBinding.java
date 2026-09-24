package com.elfmcys.yesstevemodel.geckolib3.core.molang.binding;

import com.elfmcys.yesstevemodel.molang.runtime.binding.ObjectBinding;
import com.elfmcys.yesstevemodel.molang.runtime.binding.StandardBindings;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import org.jetbrains.annotations.Nullable;

// 1.12.2 无 mojmap 面：QueryBinding/variable 组（IContext 链深绑 1.17+ 实体）只在
// >=1.14 注册，<1.14 走 math/loop + legacy 高频真值面（GeckoLibCache 同款门，
// legacy-1222-l2-full；D-molang-1 起注册 rip.ysm.legacy122.LegacyMolangContext——
// 包名沿共享 math Mth 同款契约先例，两线各自提供同包实现，替代 08sta
// LegacyMolangNullBindings 反射注入）
//? if <1.14 {
/*import com.elfmcys.yesstevemodel.geckolib3.core.molang.builtin.MathBinding;
import rip.ysm.legacy122.LegacyMolangContext;*/
//? }
//? if >=1.14 {
import com.elfmcys.yesstevemodel.geckolib3.core.molang.builtin.MathBinding;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.builtin.QueryBinding;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.binding.variable.ControllerVariableBinding;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.binding.variable.ScopedVariableBinding;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.binding.variable.TempVariableRegistry;
//? }

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PrimaryBinding implements ObjectBinding {

    public final Object2ReferenceOpenHashMap<String, Object> bindings = new Object2ReferenceOpenHashMap<>();

    //? if >=1.14 {
    public final ScopedVariableBinding scopedBinding = new ScopedVariableBinding();

    public final ControllerVariableBinding foreignBinding = new ControllerVariableBinding();

    public final TempVariableRegistry tempBinding = new TempVariableRegistry();
    //? }

    private final List<CloseVariable> closeables;

    private final List<ResetVariable> resettables;

    public PrimaryBinding(@Nullable Map<String, Object> map) {
        if (map != null) {
            this.bindings.putAll(map);
        }
        this.bindings.put("math", MathBinding.INSTANCE);
        //? if >=1.14 {
        this.bindings.put("query", QueryBinding.INSTANCE);
        this.bindings.put("q", QueryBinding.INSTANCE);
        //? }
        this.bindings.put("loop", StandardBindings.LOOP_FUNC);
        this.bindings.put("for_each", StandardBindings.FOR_EACH_FUNC);
        //? if <1.14 {
        /*
        // <1.14 正式注册（D-molang-1）：高频真值面 + 未落地名空语义兜底（主线
        // 未赋值态同款），替代 08sta LegacyMolangNullBindings 的 EXTRA_BINDING 反射注入。
        // 注释即活跃：>=1.14 线直编本源文件，<1.14 线由 stonecutter 剥注释激活
        //（math 包 Mth 契约同款注释法）
        this.bindings.put("query", LegacyMolangContext.QUERY);
        this.bindings.put("q", LegacyMolangContext.QUERY);
        this.bindings.put("ysm", LegacyMolangContext.YSM);
        this.bindings.put("ctrl", LegacyMolangContext.CTRL);
        this.bindings.put("tlm", LegacyMolangContext.NULL_NS);
        this.bindings.put("args", LegacyMolangContext.NULL_NS);
        this.bindings.put("fn", LegacyMolangContext.NULL_NS);
        this.bindings.put("context", LegacyMolangContext.NULL_NS);
        this.bindings.put("c", LegacyMolangContext.NULL_NS);
        LegacyMolangContext.Scoped scoped = new LegacyMolangContext.Scoped();
        this.bindings.put("variable", scoped);
        this.bindings.put("v", scoped);
        this.bindings.put("temp", scoped);
        this.bindings.put("t", scoped);
        */
        //? }
        //? if >=1.14 {
        this.bindings.put("variable", this.scopedBinding);
        this.bindings.put("v", this.scopedBinding);
        this.bindings.put("context", this.foreignBinding);
        this.bindings.put("c", this.foreignBinding);
        this.bindings.put("temp", this.tempBinding);
        this.bindings.put("t", this.tempBinding);
        //? }
        this.closeables = this.bindings.values().stream().filter(obj -> obj instanceof CloseVariable).map(obj2 -> (CloseVariable) obj2).collect(Collectors.toCollection(ArrayList::new));
        this.resettables = this.bindings.values().stream().filter(obj3 -> obj3 instanceof ResetVariable).map(obj4 -> (ResetVariable) obj4).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public Object getProperty(String str) {
        return this.bindings.get(str);
    }

    public void reset() {
        for (ResetVariable resetVariable : this.resettables) {
            resetVariable.reset();
        }
    }

    public void dispose() {
        for (CloseVariable closeVariable : this.closeables) {
            closeVariable.dispose();
        }
    }
}