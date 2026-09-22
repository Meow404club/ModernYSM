package rip.ysm.legacy122;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSlot;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

import java.util.Collections;
import java.util.List;

/**
 * 1.12.2 模型选择 GUI（L3-2）。
 *
 * 手写 GuiScreen+GuiButton（vanilla 1.12.2 GuiScreen.java:40/GuiButton.java:10 实证；
 * forge GuiConfig 绑 Configuration 字段面不适动态模型枚举，弃用——research 卡裁决）。
 * 列表=GuiSlot 子类（vanilla GuiVideoSettings.java:61-63 嵌槽鼠标转发先例），首项
 * default + 服务端登录下发的可用模型（两级 id），点击→LegacySyncChannel.requestSelect
 * →服务端校验/持久化/广播；客户端收 S2C 登记后渲染惰性装载，即选即生效。
 * 入口=keybinding（默认 Y，ClientRegistry.registerKeyBinding，forge-1.12.x
 * ClientRegistry.java:60）+ ClientTickEvent 轮询 isPressed（1.12.2 标准轮询面，
 * TickEvent.java:53）。
 */
@SideOnly(Side.CLIENT)
public final class LegacyModelSelectScreen extends GuiScreen {

    private static final KeyBinding OPEN_GUI =
            new KeyBinding("key.openysm.select_model", Keyboard.KEY_Y, "key.categories.openysm");

    // 服务端登录时 LegacyModelListPacket 下发（volatile 引用换入，读侧无锁）
    private static volatile List<String> available = Collections.emptyList();

    private ModelSlot slot;
    private String selected = LegacyModelRegistry.DEFAULT_MODEL_ID;

    public LegacyModelSelectScreen() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player != null) {
            selected = LegacyModelRegistry.modelIdOf(mc.player.getUniqueID());
        }
    }

    /** init（FMLInitializationEvent，客户端侧）调：注册键位+tick 轮询。 */
    public static void init() {
        ClientRegistry.registerKeyBinding(OPEN_GUI);
        MinecraftForge.EVENT_BUS.register(LegacyModelSelectScreen.class);
    }

    /** S2C 可用模型列表落点（LegacyModelListPacket.Handler）。 */
    public static void setAvailable(List<String> ids) {
        available = ids == null ? Collections.<String>emptyList() : ids;
    }

    @Override
    public void initGui() {
        this.slot = new ModelSlot();
        this.addButton(new GuiButton(1, this.width / 2 - 100, this.height - 28,
                I18n.format("gui.done")));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.slot.drawScreen(mouseX, mouseY, partialTicks);
        this.drawCenteredString(this.fontRenderer, "YSM Models", this.width / 2, 12, 0xFFFFFF);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 1) {
            this.mc.displayGuiScreen(null);
        }
    }

    @Override
    public void handleMouseInput() throws java.io.IOException {
        super.handleMouseInput();
        this.slot.handleMouseInput();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false; // 选择期间世界不停（GuiIngameMenu 同语义）
    }

    private String idAt(int index) {
        return index <= 0 ? LegacyModelRegistry.DEFAULT_MODEL_ID : available.get(index - 1);
    }

    /** 键位轮询（1.12.2 无 InputEvent.KeyInputEvent，ClientTickEvent+isPressed 标准面）。 */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !OPEN_GUI.isPressed()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player != null && mc.currentScreen == null) {
            mc.displayGuiScreen(new LegacyModelSelectScreen());
        }
    }

    /** 滚动列表：default + 服务端可用模型两级 id；点击即选即发。 */
    private final class ModelSlot extends GuiSlot {
        ModelSlot() {
            super(LegacyModelSelectScreen.this.mc, LegacyModelSelectScreen.this.width,
                    LegacyModelSelectScreen.this.height, 32,
                    LegacyModelSelectScreen.this.height - 52, 14);
        }

        @Override
        protected int getSize() {
            return 1 + available.size();
        }

        @Override
        protected void elementClicked(int index, boolean doubleClick, int mouseX, int mouseY) {
            selected = idAt(index);
            LegacySyncChannel.requestSelect(selected);
            System.out.println("[ysm-legacy122] gui selected: " + selected);
        }

        @Override
        protected boolean isSelected(int index) {
            return idAt(index).equals(selected);
        }

        @Override
        protected void drawBackground() {
            LegacyModelSelectScreen.this.drawDefaultBackground();
        }

        @Override
        protected void drawSlot(int index, int x, int y, int slotHeight, int mouseX, int mouseY, float pt) {
            String id = idAt(index);
            this.mc.fontRenderer.drawStringWithShadow(id,
                    this.left + this.width / 2 - this.mc.fontRenderer.getStringWidth(id) / 2,
                    y + 3, isSelected(index) ? 0xFFFF55 : 0xFFFFFF);
        }
    }
}
