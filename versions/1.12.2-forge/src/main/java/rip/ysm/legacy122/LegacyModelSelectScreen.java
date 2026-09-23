package rip.ysm.legacy122;

import java.util.Collections;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.lwjgl.input.Keyboard;

import rip.ysm.legacy122.yui.YuiModelSelectScreen;
import rip.ysm.legacy122.yui.YuiScreenHost1122;

/**
 * 1.12.2 模型选择 GUI 入口（M-U2 重做：yui 卡片形态消费者）。
 *
 * <p>功能链不变（0f19e6e L3-2）：keybinding（默认 Y，ClientRegistry.registerKeyBinding，
 * forge-1.12.x ClientRegistry.java:60）+ ClientTickEvent 轮询 isPressed（TickEvent.java:53，
 * 1.12.2 无 KeyInputEvent）开屏；列表=default + 服务端登录下发的可用模型（两级 id，
 * {@link #setAvailable} 落点）；点选→YuiModelSelectScreen.select→
 * LegacySyncChannel.requestSelect→服务端校验/持久化/广播。
 *
 * <p>渲染=共享 yui 组件（rip/ysm/yui 中性树）+ YuiScreenHost1122 宿主壳：
 * 5x2 模型卡网格（主线 ModelButton 形态，52x90）+ 卡内实时预览（LegacyCardPreview，
 * POC be0a794 配方）+ 翻页器，替代 L3-2 手写列表屏（用户决策 182 观感统一）。
 * M-U1 冒烟屏（独立 J 键实验屏）已随本卡撤除，验收职责由本屏接替。
 */
@SideOnly(Side.CLIENT)
public final class LegacyModelSelectScreen {

    private static final KeyBinding OPEN_GUI =
            new KeyBinding("key.openysm.select_model", Keyboard.KEY_Y, "key.categories.openysm");

    // 服务端登录时 LegacyModelListPacket 下发（volatile 引用换入，读侧无锁）
    private static volatile List<String> available = Collections.emptyList();

    /** init（FMLInitializationEvent，客户端侧）调：注册键位+tick 轮询。 */
    public static void init() {
        ClientRegistry.registerKeyBinding(OPEN_GUI);
        MinecraftForge.EVENT_BUS.register(LegacyModelSelectScreen.class);
    }

    /** S2C 可用模型列表落点（LegacyModelListPacket.Handler）。 */
    public static void setAvailable(List<String> ids) {
        available = ids == null ? Collections.<String>emptyList() : ids;
    }

    /** 消费侧读取（YuiModelSelectScreen 构造）。 */
    public static List<String> availableIds() {
        return available;
    }

    /** 键位轮询（1.12.2 无 InputEvent.KeyInputEvent，ClientTickEvent+isPressed 标准面）。 */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !OPEN_GUI.isPressed()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player != null && mc.currentScreen == null) {
            mc.displayGuiScreen(new YuiScreenHost1122(YuiModelSelectScreen::new));
        }
    }
}
