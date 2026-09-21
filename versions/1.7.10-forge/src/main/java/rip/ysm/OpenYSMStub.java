package rip.ysm;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import net.minecraft.block.Block;
import net.minecraftforge.common.MinecraftForge;

/**
 * 1.7.10 线入口（legacy-1710-l0-poc，L0 构建面 POC）。
 *
 * 编译面证明：cpw.mods.fml（1.7.10 代 FML 包名）+ net.minecraft.block.Block
 * （MCP stable_12 编译命名空间）+ net.minecraftforge.common.MinecraftForge。
 * 渲染翻译层是 L1+ 的事（RenderPlayer mixin 切面，与 1.12.2 RenderPlayerEvent 不同）。
 */
@Mod(modid = "openysm", name = "OpenYSM", version = "2.6.6.6")
public class OpenYSMStub {

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        // 引用 forge 1.7.10 类 + MC 类（常量字段，无副作用）证明编译/运行类路径
        String id = Block.blockRegistry.getNameForObject(Block.getBlockById(1)).toString();
        MinecraftForge.EVENT_BUS.register(this);
        System.out.println("[ysm-legacy1710] init done: " + id);
    }
}
