package rip.ysm.harness;

import net.minecraft.client.Minecraft;
//? if neoforge
/*import net.neoforged.neoforge.common.NeoForge;*/
//? if forge
import net.minecraftforge.common.MinecraftForge;
//? if neoforge && <1.20.5
/*import net.neoforged.neoforge.event.TickEvent;*/
//? if forge
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
 * <p>连接自动发起（主菜单出现即连 localhost:25565，orchestrator 起的专用 server；
 * 端口可经系统属性 {@code -Dopenysm.harness.port} 覆盖，默认 25565 零行为差——
 * fix-rc-bind-target：多卡并行 harness 各用各的端口，不再互等互杀）；
 * 死亡自动重生（flat world 出生点仍可能被环境伤害打断走查）。
 * 版本差异（连接入口）用 stonecutter 条件块收口在 {@link #connectToServer}。
 */
public final class GuiTourDriver {

    private static final String CMD_FILE = "cmd.txt";
    private static final String READY_FILE = "harness.ready";
    private static final String ARMED_FILE = "harness.armed";
    private static final String PORT_FILE = "harness.port";
    /** harness server 端口：系统属性 -Dopenysm.harness.port 优先，其次 gameDir/harness.port 文件
     * （fix-rc-probe-diff：MDG runClient 不透传 launcher -D，文件通道供编排器并行隔离端口；
     * 两通道都缺席=25565 历史默认，生产零行为）。 */
    private static final int HARNESS_PORT = resolvePort();

    private static int resolvePort() {
        int viaProp = Integer.getInteger("openysm.harness.port", -1);
        if (viaProp > 0) {
            return viaProp;
        }
        try {
            Path f = gameDir().resolve(PORT_FILE);
            if (Files.exists(f)) {
                int viaFile = Integer.parseInt(new String(Files.readAllBytes(f), java.nio.charset.StandardCharsets.UTF_8).trim());
                if (viaFile > 0) {
                    return viaFile;
                }
            }
        } catch (Exception ignored) {
        }
        return 25565;
    }

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
        //? if neoforge
        /*NeoForge.EVENT_BUS.addListener(GuiTourDriver::onClientTick);*/
        //? if forge
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
        //? if neoforge
        /*return net.neoforged.fml.loading.FMLPaths.GAMEDIR.get();*/
        //? if forge
        return net.minecraftforge.fml.loading.FMLPaths.GAMEDIR.get();
    }

    // ClientTickEvent phase END：neoforge 1.20.5+ = ClientTickEvent.Post（TickEvent.Phase 拆分）。
    // 三代分发器各自完整，走查主体抽 onClientTickBody 共享（harness 代码，生产行为零差）
    //? if neoforge && >=1.20.5 {
    /*private static void onClientTick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
        if (!armed()) {
            return;
        }
        onClientTickBody();
    }*/
    //? }
    //? if neoforge && <1.20.5 {
    /*private static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !armed()) {
            return;
        }
        onClientTickBody();
    }*/
    //? }
    //? if forge {
    private static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !armed()) {
            return;
        }
        onClientTickBody();
    }
    //? }

    private static void onClientTickBody() {
        Minecraft mc = Minecraft.getInstance();
        delayedTunnelCheck(mc);
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
            } else if (mc.player != null && mc.level != null
                    // 21.9 实证修正：mod 首次 join 的 Disclaimer/PlayerModel 自动开屏可能早于
                    // 任何 screen==null tick（旧条件 screen==null 在 21.9 永假 → world 永不标），
                    // 改为排除 vanilla 加载屏即视为已入世界（harness 类，生产 jar 排除）
                    && !(mc.screen instanceof net.minecraft.client.gui.screens.LevelLoadingScreen)) {
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
            return;
        }
        // d3-gpu-218-revive：第三人称切换（世界内模型可见性定点截图用——第一人称看不到
        // 本体，GPU 路径的 in-world 渲染需要视觉证据）。harness 类，生产 jar 排除。
        if (line.equals("camera")) {
            if (mc.player == null || mc.options == null) {
                mark("fail camera");
                return;
            }
            // 1.16.1 无 CameraType（1.16.2 引入，官方 1161 映射零命中，CameraUtil.java:14 同款实证）→
            // 视角切换降级（1.16.x 无 GPU 路径用不到；反射兜底如需再补，勿凭记忆引 SRG 名）
            //? if <1.16.2
            /*mark("fail camera");*/
            //? if >=1.16.2 {
            mc.options.setCameraType(mc.options.getCameraType().isFirstPerson()
                    ? net.minecraft.client.CameraType.THIRD_PERSON_BACK
                    : net.minecraft.client.CameraType.FIRST_PERSON);
            mark("ok camera");
            //?}
            return;
        }
        // fix-rc-probe-diff：姿势诱导命令（RC 探针差分格：站/蹲×2/趴爬）。客户端按键注入，
        // 服务端物理一致（蹲=crouch pose 客户端判定、趴=1 格高隧道内前进触发 SWIMMING）。
        if (line.startsWith("pose ")) {
            String pose = line.substring(5).trim();
            mark(setPose(mc, pose) ? "ok pose " + pose : "fail pose " + pose);
        }
    }

    /** 姿势命令实现；返回 false=未知姿势名。 */
    private static boolean setPose(Minecraft mc, String pose) {
        if (mc.player == null || mc.options == null) {
            return false;
        }
        // 先全释放（命令间互斥）
        mc.options.keyUp.setDown(false);
        mc.options.keyDown.setDown(false);
        mc.options.keyLeft.setDown(false);
        mc.options.keyRight.setDown(false);
        mc.options.keyShift.setDown(false);
        mc.options.keySprint.setDown(false);
        if ("crouch".equals(pose)) {
            mc.options.keyShift.setDown(true);
        } else if ("crouchmove".equals(pose)) {
            // 蹲行：Trissy sneak 动画绑定 onGround&&CROUCHING&&|limbSwing|>阈值（AnimationRegister:41）
            mc.options.keyShift.setDown(true);
            mc.options.keyUp.setDown(true);
        } else if ("crawl2".equals(pose)) {
            // 趴爬 v2（fix-rc-probe-diff）：客户端已 op，先经 sendCommand 以 @s 建四向 1 格隧道
            //（服务端控制台 fill 曾疑似未落——玩家 STANDING 穿过隧道区，v2 客户端自建可自证），
            // 随后持续前进+疾跑触发 SWIMMING（climbing 趴姿动画）。仅 >=1.19 线可用。
            buildTunnelAndCrawl(mc);
        } else if ("crawl".equals(pose)) {
            // 旧趴爬（依赖编排器经服务端控制台预建隧道）
            mc.options.keyUp.setDown(true);
            mc.options.keySprint.setDown(true);
        } else if (!"stand".equals(pose)) {
            return false;
        }
        return true;
    }

    /** 客户端 op 后自建四向 1 格隧道并开始爬行（>=1.19；带方块级自证日志）。 */
    //? if >=1.19 {
    private static void buildTunnelAndCrawl(Minecraft mc) {
        if (mc.player == null || mc.player.connection == null) {
            return;
        }
        String[][] fills = {
                // +x / -x / +z / -z 四向：挖脚下一层（air@~-1）、铺底（stone@~-2）、封顶（stone@~）
                {"execute at @s run fill 2 ~-1 -3 40 ~-1 3 minecraft:air"},
                {"execute at @s run fill 2 ~-2 -3 40 ~-2 3 minecraft:smooth_stone"},
                {"execute at @s run fill 2 ~ -3 40 ~ 3 minecraft:smooth_stone"},
                {"execute at @s run fill -40 ~-1 -3 -2 ~-1 3 minecraft:air"},
                {"execute at @s run fill -40 ~-2 -3 -2 ~-2 3 minecraft:smooth_stone"},
                {"execute at @s run fill -40 ~ -3 -2 ~ 3 minecraft:smooth_stone"},
                {"execute at @s run fill -3 ~-1 2 3 ~-1 40 minecraft:air"},
                {"execute at @s run fill -3 ~-2 2 3 ~-2 40 minecraft:smooth_stone"},
                {"execute at @s run fill -3 ~ 2 3 ~ 40 minecraft:smooth_stone"},
                {"execute at @s run fill -3 ~-1 -40 3 ~-1 -2 minecraft:air"},
                {"execute at @s run fill -3 ~-2 -40 3 ~-2 -2 minecraft:smooth_stone"},
                {"execute at @s run fill -3 ~ -40 3 ~ -2 minecraft:smooth_stone"},
        };
        for (String[] cmd : fills) {
            sendChatCommand(mc, cmd[0]);
        }
        // 命令通道决定性诊断：say 会在服务端日志留 "[Dev] rcprobe-*" 痕迹
        sendChatCommand(mc, "say rcprobe-fills-sent");
        // 直接放入 -z 隧道内（下 1 格、北移 20、面向北）：四向隧道以填充时刻位置为基准，
        // 玩家自发行走线与隧道走廊错位会全程 STANDING 走过（round3 实证）；tp 内置位消除入洞问题。
        sendChatCommand(mc, "tp @s ~ ~-1 ~-20 180 0");
        sendChatCommand(mc, "say rcprobe-tp-sent");
        // 延迟方块自证：fill/tp 经服务端回环，同 tick 读方块是旧值（round3 教训）
        tunnelCheckDelay = 60;
        mc.options.keyUp.setDown(true);
        mc.options.keySprint.setDown(true);
    }

    /** 1.19~1.19.2 签名代 ClientPacketListener 无 sendCommand（1192 named jar javap 实证，命令入口
     *  移 LocalPlayer：1.19=command、1.19.1 起=commandUnsigned（1190/1191 编译实测分界））→ 分档；
     *  1.19.3 起回归 ClientPacketListener.sendCommand。 */
    private static void sendChatCommand(Minecraft mc, String command) {
        //? if <1.19.1
        /*mc.player.command(command);*/
        //? if >=1.19.1 && <1.19.3
        /*mc.player.commandUnsigned(command);*/
        //? if >=1.19.3
        mc.player.connection.sendCommand(command);
    }

    /** 延迟隧道自证倒计时（tick）；<0=未武装。 */
    private static int tunnelCheckDelay = -1;

    private static void delayedTunnelCheck(Minecraft mc) {
        if (tunnelCheckDelay < 0) {
            return;
        }
        if (tunnelCheckDelay-- > 0) {
            return;
        }
        tunnelCheckDelay = -1;
        if (mc.player == null || mc.level == null) {
            return;
        }
        net.minecraft.core.BlockPos feet = new net.minecraft.core.BlockPos(
                (int) Math.floor(mc.player.getX()), (int) Math.floor(mc.player.getY()), (int) Math.floor(mc.player.getZ()));
        for (int dy = -1; dy <= 1; dy++) {
            System.out.println("[GuiTourDriver] tunnel-check-delayed dy=" + dy + " block="
                    + mc.level.getBlockState(feet.offset(0, dy, 0)).getBlock());
        }
    }
    //? }
    //? if <1.19 {
    /*private static void buildTunnelAndCrawl(Minecraft mc) {
        // <1.19 无可靠 sendCommand 面：趴爬格不支持
    }

    private static void delayedTunnelCheck(Minecraft mc) {
        // 同上 no-op：保持 onClientTickBody 调用点全线可编译
    }*/
    //? }

    private static boolean openScreen(Minecraft mc, String name) {
        net.minecraft.client.gui.screens.Screen screen = HarnessScreens.forName(mc, name);
        if (screen == null) {
            return false;
        }
        mc.setScreen(screen);
        return true;
    }

    /** 版本差异收口：1.16.5 公共 4 参构造 ↔ 1.17.1~1.19.4 静态 4 参 ↔ 1.20.1 静态 5 参（quickPlay）。
     *  4/5 参边界实证：vanilla-mc-{1182,1192,1194}/ConnectScreen.java:47/46/45 均 4 参，1201:54 五参。 */
    private static void connectToServer(Minecraft mc) {
        //? if <1.17 {
        /*mc.setScreen(new net.minecraft.client.gui.screens.ConnectScreen(null, mc, "localhost", 25565));
        *///?}
        //? if >=1.17 && <1.20 {
        /*net.minecraft.client.gui.screens.ConnectScreen.startConnecting(null, mc,
                net.minecraft.client.multiplayer.resolver.ServerAddress.parseString("localhost:25565"),
                new net.minecraft.client.multiplayer.ServerData("harness", "localhost:25565", false));
         *///?}
        //? if >=1.20 && <1.20.5 {
        net.minecraft.client.gui.screens.ConnectScreen.startConnecting(null, mc,
                net.minecraft.client.multiplayer.resolver.ServerAddress.parseString("localhost:" + HARNESS_PORT),
                //? if neoforge
                /*new net.minecraft.client.multiplayer.ServerData("harness", "localhost:25565", net.minecraft.client.multiplayer.ServerData.Type.OTHER), false);*/
                //? if forge
                new net.minecraft.client.multiplayer.ServerData("harness", "localhost:" + HARNESS_PORT, false), false);
        //?}
        //? if >=1.20.5 {
        /*// 1.20.5+ 增第 6 参 @Nullable TransferState（vanilla-1.20.6 ConnectScreen.java:55），走查传 null。
        // 26.2 修正：端口接 HARNESS_PORT（此前硬编码 25565，HARNESS_PORT 端口错开对该分支不生效，
        // tour server 起在 25575 时客户端恒连 25565 超时——26.2 首跑实证）
        net.minecraft.client.gui.screens.ConnectScreen.startConnecting(null, mc,
                net.minecraft.client.multiplayer.resolver.ServerAddress.parseString("localhost:" + HARNESS_PORT),
                new net.minecraft.client.multiplayer.ServerData("harness", "localhost:" + HARNESS_PORT, net.minecraft.client.multiplayer.ServerData.Type.OTHER), false, null);*/
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
