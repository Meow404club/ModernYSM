package rip.ysm.harness;

import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * 跨版本 GUI 走查 driver（测试基建，非游戏功能）。
 *
 * <p>激活条件（isDevEnv 守卫，双保险）：仅当 gameDir 下存在 {@code harness.armed}
 * 标记（由 harness/tour.sh orchestrator 在拉起 client 前写入、收尾删除）时才工作。
 * 生产环境无该文件 → tick 内一次缓存布尔判断后直接返回，零行为、零开销。
 *
 * <p>协议（与 orchestrator 的文件握手，进程内零 GL 调用——xvfb+llvmpipe 下
 * glReadPixels 会触发 native 堆损坏，截图必须由外部 ffmpeg x11grab 完成，
 * 见 M2.5 的 12 轮误报教训）：
 * <ul>
 *   <li>{@code cmd.txt} ← orchestrator 写指令：{@code open <ScreenName>} /
 *       {@code close} / {@code quit}</li>
 *   <li>{@code harness.ready} ← driver 写结果：{@code title} / {@code world} /
 *       {@code ok <name>} / {@code fail <name>} / {@code ok close}</li>
 * </ul>
 *
 * <p>连接自动发起（主菜单出现即连 localhost:25565，orchestrator 起的专用 server）；
 * 死亡自动重生（flat world 出生点仍可能被环境伤害打断走查）。
 * 版本差异（连接入口）用 stonecutter 条件块收口在 {@link #connectToServer}。
 */
public final class GuiTourDriver {

    private static final String CMD_FILE = "cmd.txt";
    private static final String READY_FILE = "harness.ready";
    private static final String ARMED_FILE = "harness.armed";

    private static Boolean armedCache;
    private static boolean joined;
    private static int diag;
    private static int beat;
    private static int titleTicks;
    /** mark title 后延迟连接：orchestrator 的 await+ffmpeg 抓屏约需 1.5s，
     *  同 tick 立即 connect 会把 000-title 截成 "Joining world..."（1.20.1 实测）。 */
    private static final int CONNECT_DELAY_TICKS = 60;

    private GuiTourDriver() {
    }

    /** 初始化调用点：YsmEventBootstrap.register() 的 client 分支（1 行）。 */
    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(GuiTourDriver::onClientTick);
    }

    /** isDevEnv 守卫：armed 标记懒查一次并缓存（生产文件系统无此文件）。 */
    private static boolean armed() {
        Boolean b = armedCache;
        if (b == null) {
            b = Files.exists(gameDir().resolve(ARMED_FILE));
            armedCache = b;
        }
        return b;
    }

    private static Path gameDir() {
        return net.minecraftforge.fml.loading.FMLPaths.GAMEDIR.get();
    }

    private static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !armed()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (beat++ % 40 == 0) {
            // 走查心跳（armed 门控，生产零输出）：证明 listener 已挂上且 tick 在跑
            System.out.println("[GuiTourDriver] beat joined=" + joined + " player=" + (mc.player != null)
                    + " level=" + (mc.level != null) + " screen=" + mc.screen);
        }
        // flat world 走查也会被环境伤害打断：死亡自动重生，保证逐屏可开
        if (mc.player != null && mc.screen instanceof net.minecraft.client.gui.screens.DeathScreen) {
            mc.player.respawn();
            return;
        }
        if (!joined) {
            if (mc.screen instanceof net.minecraft.client.gui.screens.TitleScreen && mc.level == null) {
                if (titleTicks == 0) {
                    mark("title");
                }
                if (++titleTicks >= CONNECT_DELAY_TICKS) {
                    connectToServer(mc);
                }
            } else if (mc.player != null && mc.level != null && mc.screen == null) {
                // 注意：勿加 getHealth()>0 之类判定——join 瞬间客户端 health sync 未到
                // （getHealth()==0）会永久阻断 mark（M2.5 实证）；死亡由上方 respawn 兜底
                joined = true;
                mark("world");
            } else if (diag++ % 100 == 0) {
                // 诊断：world 门条件逐项打印（仅 armed dev 生效）
                System.out.println("[GuiTourDriver] world gate: player=" + (mc.player != null)
                        + " level=" + (mc.level != null) + " screen=" + mc.screen
                        + " health=" + (mc.player == null ? -1 : mc.player.getHealth()));
            }
            return;
        }
        pollCommand(mc);
    }

    private static void pollCommand(Minecraft mc) {
        Path cmd = gameDir().resolve(CMD_FILE);
        if (!Files.exists(cmd)) {
            return;
        }
        String line;
        try {
            // Java 8 口径（1.16.5 线编译 target 8）：Files.readString 是 11+
            line = new String(Files.readAllBytes(cmd), java.nio.charset.StandardCharsets.UTF_8).trim();
        } catch (Exception e) {
            return;
        }
        if (line.isEmpty()) {
            return;
        }
        try {
            Files.delete(cmd);
        } catch (Exception ignored) {
            return;
        }
        if (line.equals("quit")) {
            mark("ok quit");
            mc.stop();
            return;
        }
        if (line.equals("close")) {
            mc.setScreen(null);
            mark("ok close");
            return;
        }
        if (line.startsWith("open ")) {
            String name = line.substring(5).trim();
            mark(openScreen(mc, name) ? "ok " + name : "fail " + name);
        }
    }

    private static boolean openScreen(Minecraft mc, String name) {
        net.minecraft.client.gui.screens.Screen screen = HarnessScreens.forName(mc, name);
        if (screen == null) {
            return false;
        }
        mc.setScreen(screen);
        return true;
    }

    /** 版本差异收口：1.16.5 公共 4 参构造 ↔ 1.20.1 静态 startConnecting。 */
    private static void connectToServer(Minecraft mc) {
        //? if <1.17 {
        /*mc.setScreen(new net.minecraft.client.gui.screens.ConnectScreen(null, mc, "localhost", 25565));
        *///?} else {
        net.minecraft.client.gui.screens.ConnectScreen.startConnecting(null, mc,
                net.minecraft.client.multiplayer.resolver.ServerAddress.parseString("localhost:25565"),
                new net.minecraft.client.multiplayer.ServerData("harness", "localhost:25565", false), false);
        //?}
    }

    private static void mark(String state) {
        try {
            // Java 8 口径：Files.writeString 是 11+
            Files.write(gameDir().resolve(READY_FILE), (state + "\n").getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        } catch (Exception e) {
            System.out.println("[GuiTourDriver] mark failed: " + e);
        }
    }
}
