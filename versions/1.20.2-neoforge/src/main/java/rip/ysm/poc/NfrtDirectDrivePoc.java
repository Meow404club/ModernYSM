package rip.ysm.poc;

import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

/**
 * NFRT 直驱 POC stub（nfrt-poc-1202）。
 *
 * <p>唯一职责：证明本项目类路径下能对 NFRT 产物工件（NFRT run
 * --write-result gameJarWithNeoForge 产出的 MC 1.20.2 + NeoForge 20.2.93
 * mojmap 直名 jar）完成编译与打包。FML {@code @Mod} 走
 * net.neoforged.fancymodloader:loader:1.0.16，{@code NeoForge} 事件总线类型走
 * NFRT 产物 jar（net/neoforged/neoforge/** 实证在册）。mod 平铺是 GO 后另立卡，
 * 本类不进任何生产产物口径。</p>
 */
@Mod("openysm_nfrt_poc")
public class NfrtDirectDrivePoc {
    /** 编译期引用 NFRT 产物 jar 内的 NeoForge 类型（类型面引用，不触其成员签名依赖链）。 */
    @SuppressWarnings("unused")
    private static final Class<?> NEOFORGE_BUS_TYPE = NeoForge.class;
}
