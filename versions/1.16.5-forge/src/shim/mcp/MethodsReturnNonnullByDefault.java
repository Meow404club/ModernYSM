package mcp;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 1.16.5 classpath 补缺 stub：MCP 注解 {@code mcp.MethodsReturnNonnullByDefault}。
 * 原物随 forge userdev 的 mcp_annotations 分发，unimined 1.16.5 mojmap jar 未携带
 * （全 jar 字节扫描 0 命中），而 forge 侧 net.minecraftforge.common.capabilities.
 * CapabilityProvider / CapabilityDispatcher / util.LazyOptional 及 11 个 package-info
 * 的签名引用它——凡继承 CapabilityProvider 的 capability 实现类在 javac attribution
 * 阶段即报"无法访问 MethodsReturnNonnullByDefault / 找不到类文件"。
 * 本 stub 仅为编译期可解析占位：与原注解的差异是不带 jsr305
 * {@code TypeQualifierDefault} 元注解（避免为编译引入 findbugs/jsr305 依赖），
 * 静态空值分析语义不参与 javac 编译判定，对本仓无影响。
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PACKAGE, ElementType.TYPE, ElementType.METHOD})
public @interface MethodsReturnNonnullByDefault {
}
