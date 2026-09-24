package rip.ysm.legacy1710;

import com.elfmcys.yesstevemodel.geckolib3.resource.GeckoLibCache;
import com.elfmcys.yesstevemodel.molang.runtime.AssignableVariable;
import com.elfmcys.yesstevemodel.molang.runtime.ExecutionContext;
import com.elfmcys.yesstevemodel.molang.runtime.binding.ObjectBinding;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * <1.14 轴 molang 命名空间空绑定注入（fix-1710-08sta-scale）。
 *
 * 主线（>=1.14）PrimaryBinding 注册 v/variable→ScopedVariableBinding、q/query→
 * QueryBinding、ysm/ctrl/tlm→各 Binding（PrimaryBinding.java:63-75）；变量未赋值时
 * 经 storage 读出 null→asFloat 归 0（ValueConversions）。<1.14 轴这些注册被
 * stonecutter 门整体剔除（IContext 链深绑 1.17+ 实体），`v.roaming.car` 这类表达式
 * 在解析期抛 ParseException→YSMClientMapper.parse 兜底 FloatValue.ZERO——与本轴
 * "常量 0"不可区分，并行槽 scale 语义被破坏（`!v.roaming.car` 应 1 得 0 →
 * 08_sta 全骨隐藏 quadsDrawn=0 实证）。
 *
 * 修法：GeckoLibCache.EXTRA_BINDING 是 private static final 修饰的 HashMap（引用
 * final、内容可变），createMolangParser 每次以 `new HashMap<>(EXTRA_BINDING)` 传给
 * MolangParser，PrimaryBinding.bindings.putAll(map)（PrimaryBinding.java:44）——
 * 反射注入一次，所有后续解析获得与主线未赋值态一致的空语义：
 * v 点路径、q 点路径、ysm 点路径读取得到 null（asFloat 归 0 / asBoolean 归 false /
 * 空合并问号透传右值），与主线未设变量行为逐条对齐（
 * `!v.roaming.car` 得 1、`v.roaming.car` 得 0、`v.player_size??1` 得 1、
 * `(v.hat==0)?1:0` 得 1）。函数式查询（q.xxx(...)）求值期
 * 走 evalSafe 归 0=中性位，与本轴注入前失败路径相同，无行为回退。
 * ponytail: 反射私有静态字段是 <1.14 门在共享代码的既有留白（D-molang-1 变量供给
 * 卡的过渡态），升级路径=共享侧给 <1.14 轴补正式注册后删除本类。
 */
public final class LegacyMolangNullBindings {

    private static final String[] NAMESPACES = {
            "v", "variable", "q", "query", "ysm", "ctrl", "tlm",
            "c", "context", "t", "temp", "args", "fn",
    };

    private static boolean installed;

    private LegacyMolangNullBindings() {
    }

    /**
     * 空语义链节点：既是 ObjectBinding（解析期点路径取链，MolangParserImpl:119
     * 要求中间段非空且 instanceof ObjectBinding）又是 AssignableVariable（叶子
     * 求值返回 null=主线未赋值语义；assign 为空=本轴无脚本赋值面）。
     */
    private static final class NullVariable implements AssignableVariable, ObjectBinding {

        static final NullVariable INSTANCE = new NullVariable();

        @Override
        public Object getProperty(String name) {
            return this;
        }

        @Override
        public Object evaluate(@NotNull ExecutionContext<?> context) {
            return null;
        }

        @Override
        public void assign(@NotNull ExecutionContext<?> context, Object value) {
        }
    }

    /** 命名空间根：v/q/ysm 等首段解析（MolangParserImpl:100 要求非空）。 */
    private static final class FakeNamespace implements ObjectBinding {

        static final FakeNamespace INSTANCE = new FakeNamespace();

        @Override
        public Object getProperty(String name) {
            return NullVariable.INSTANCE;
        }
    }

    private static synchronized void install0() throws ReflectiveOperationException {
        Field field = GeckoLibCache.class.getDeclaredField("EXTRA_BINDING");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> extra = (Map<String, Object>) field.get(null);
        // 注入使 EXTRA_BINDING 非空，createMolangParser <1.14 分支的 isEmpty 守卫
        // 不再补装 math——此处一并补上（MathBinding 与主线同款必需面）
        extra.putIfAbsent("math", com.elfmcys.yesstevemodel.geckolib3.core.molang.builtin.MathBinding.INSTANCE);
        for (String ns : NAMESPACES) {
            extra.putIfAbsent(ns, FakeNamespace.INSTANCE);
        }
        System.out.println("[ysm-legacy1710] molang null bindings installed: namespaces=" + NAMESPACES.length);
    }

    /** 幂等注入；须在任何模型动画解析（buildParsedBundle）前调用。 */
    public static synchronized void install() {
        if (installed) {
            return;
        }
        installed = true;
        try {
            install0();
        } catch (ReflectiveOperationException e) {
            // 注入失败=回退到解析期 ZERO 塌缩（与本卡修复前行为一致，不致命）
            System.out.println("[ysm-legacy1710] molang null bindings install failed: " + e);
        }
    }
}
