package rip.ysm.legacy122;

import java.util.Collections;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.ClientCommandHandler;
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
 * <p>L3-3 热重载触发面：键位 R + 客户端命令 /ysmreload（主线 /ysm model reload
 * ModelCommand.java:59 命令触发语义适形，1.12.2 无 Brigadier 用
 * ClientCommandHandler.instance.registerCommand——ClientCommandHandler.java:49，
 * 经 ClientChatEvent 于客户端线程执行）。触发后 LegacyModelLoader.reloadLoadedModels
 * →集成服在即 rebroadcastAll→选择屏开着就地重建（factory 重跑=列表/包名新值）。
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

    /** L3-3：模型重载键（默认 R；主线 reload 命令的键位适形）。 */
    private static final KeyBinding RELOAD_MODELS =
            new KeyBinding("key.openysm.reload_models", Keyboard.KEY_R, "key.categories.openysm");

    // 服务端登录时 LegacyModelListPacket 下发（volatile 引用换入，读侧无锁）
    private static volatile List<String> available = Collections.emptyList();

    /** init（FMLInitializationEvent，客户端侧）调：注册键位+客户端命令+tick 轮询。 */
    public static void init() {
        ClientRegistry.registerKeyBinding(OPEN_GUI);
        ClientRegistry.registerKeyBinding(RELOAD_MODELS);
        // L3-3：客户端命令（ClientCommandHandler 继承 CommandHandler.registerCommand，
        // CommandHandler.java:108；客户端命令优先于同名服务端命令）
        ClientCommandHandler.instance.registerCommand(new ReloadCommand());
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

    /**
     * 键位轮询（1.12.2 无 InputEvent.KeyInputEvent，ClientTickEvent+isPressed 标准面）。
     * L3-3 重载键注意：1.12.2 整个键循环被 Minecraft.java:1464
     * "currentScreen == null || allowUserInput" 门控——开屏（含聊天打字）时
     * KeyBinding.onTick 根本不入（运行实证：选择屏开着按 R 零触发），
     * 选择屏内触发走 YuiModelSelectScreen.keyPressed 的 R 钩子。
     */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (RELOAD_MODELS.isPressed() && mc.player != null && mc.currentScreen == null) {
            triggerReload("key");
        }
        if (OPEN_GUI.isPressed() && mc.player != null && mc.currentScreen == null) {
            mc.displayGuiScreen(new YuiScreenHost1122(YuiModelSelectScreen::new));
        }
    }

    /**
     * L3-3 重载入口（键位/屏内 R/命令共用）：客户端重装载→集成服在即（单机/LAN
     * 宿主）服务线程重广播（可用列表重下发+指派重广播=同步一致性）→选择屏开着
     * 就地重建（displayGuiScreen 重入 initGui→factory 重跑，listBuiltinModels/
     * listPackModels/packMeta 取新值；命令路径聊天屏开着，instanceof 守卫自然跳过）。
     */
    public static void triggerReload(String source) {
        long start = System.currentTimeMillis();
        int n = LegacyModelLoader.reloadLoadedModels();
        System.out.printf("[ysm-legacy122] reload triggered (%s): models reloaded=%d in %dms%n",
                source, n, System.currentTimeMillis() - start);
        MinecraftServer server = Minecraft.getMinecraft().getIntegratedServer();
        if (server != null) {
            server.addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    LegacySyncChannel.rebroadcastAll(server);
                }
            });
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen instanceof YuiScreenHost1122) {
            mc.displayGuiScreen(new YuiScreenHost1122(YuiModelSelectScreen::new));
        }
    }

    /** L3-3：/ysmreload 客户端命令（ICommand 七方法面，vanilla 1.12.2 ICommand.java）。 */
    private static final class ReloadCommand implements ICommand {
        public String getName() {
            return "ysmreload";
        }

        public String getUsage(ICommandSender sender) {
            return "/ysmreload";
        }

        public List<String> getAliases() {
            return Collections.emptyList();
        }

        public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
            triggerReload("command");
        }

        public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
            return true;
        }

        public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender,
                String[] args, BlockPos pos) {
            return Collections.emptyList();
        }

        public boolean isUsernameIndex(String[] args, int index) {
            return false;
        }

        public int compareTo(ICommand other) {
            return getName().compareTo(other.getName());
        }
    }
}
