package rip.ysm.legacy122.yui;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;

import org.lwjgl.input.Keyboard;

import rip.ysm.legacy122.LegacyModelLoader;
import rip.ysm.legacy122.LegacyModelRegistry;
import rip.ysm.legacy122.LegacyModelSelectScreen;
import rip.ysm.legacy122.LegacySyncChannel;
import rip.ysm.yui.YuiBackend;
import rip.ysm.yui.YuiCardGrid;
import rip.ysm.yui.YuiFlatButton;
import rip.ysm.yui.YuiLabel;
import rip.ysm.yui.YuiModelCard;
import rip.ysm.yui.YuiPanel;
import rip.ysm.yui.YuiScreen;

/**
 * 1.12.2 模型选择屏（yui 卡片形态消费者，M-U2 重做）。
 *
 * <p>功能链不变（0f19e6e，只换皮）：列表=default + 服务端登录下发
 * （{@link LegacyModelSelectScreen#setAvailable}），点选→
 * {@link LegacySyncChannel#requestSelect}（网络/校验/持久化/广播零改动）。
 * 布局=YuiPanel + 标题 + YuiCardGrid（5x2 翻页）+ Done；卡内实时预览=
 * {@link LegacyCardPreview}（POC 配方），非 default 模型屏内逐 tick 惰性装载
 * （loadModel 幂等+负缓存），装载完成前空槽。
 *
 * <p>翻页主路径=Pager 按钮；滚轮（host tick 轮询喂入）与 ▲▼/◀▶ 键为回退
 * （lwjgl3ify GuiScreen 面滚轮桥断裂实证，M-U1）。
 */
public final class YuiModelSelectScreen extends YuiScreen {

    private static final int PANEL_WIDTH = 300;

    private final List<String> modelIds = new ArrayList<String>();
    private final List<YuiModelCard> cards = new ArrayList<YuiModelCard>();
    private final List<String> loadQueue = new ArrayList<String>();
    private String selected;
    private YuiCardGrid grid;

    public YuiModelSelectScreen() {
        Minecraft mc = Minecraft.getMinecraft();
        this.selected = mc.player != null
                ? LegacyModelRegistry.modelIdOf(mc.player.getUniqueID())
                : LegacyModelRegistry.DEFAULT_MODEL_ID;
        this.modelIds.add(LegacyModelRegistry.DEFAULT_MODEL_ID);
        this.modelIds.addAll(LegacyModelSelectScreen.availableIds());
        for (int i = 1; i < this.modelIds.size(); i++) {
            this.loadQueue.add(this.modelIds.get(i));
        }
    }

    @Override
    protected void layout() {
        int px = (this.width - PANEL_WIDTH) / 2;
        int py = 24;
        int ph = this.height - 64;
        add(new YuiPanel(px, py, PANEL_WIDTH, ph));

        YuiLabel title = new YuiLabel(this.width / 2, py + 8, 0, 10, "YSM Models");
        title.align = YuiBackend.Align.CENTER;
        add(title);

        int gridX = px + (PANEL_WIDTH - YuiCardGrid.GRID_WIDTH) / 2;
        int gridY = py + 26;
        this.grid = new YuiCardGrid(gridX, gridY, gridY + YuiCardGrid.GRID_HEIGHT + 6);
        add(this.grid);
        for (int i = 0; i < this.modelIds.size(); i++) {
            final String id = this.modelIds.get(i);
            YuiModelCard card = new YuiModelCard(0, 0, id, new LegacyCardPreview(id), new Runnable() {
                @Override
                public void run() {
                    select(id);
                }
            });
            card.selected = id.equals(this.selected);
            this.grid.addCard(card);
            this.cards.add(card);
        }

        // Done（FlatIconButton 115x15 扁平语言）
        add(new YuiFlatButton(this.width / 2 - 57, py + ph + 8, 115, 15,
                I18n.format("gui.done"), new Runnable() {
                    @Override
                    public void run() {
                        Minecraft.getMinecraft().displayGuiScreen(null);
                    }
                }));
    }

    /** 非 default 模型逐 tick 惰性装载（loadModel 幂等/负缓存；一 tick 一款防开屏卡顿）。 */
    @Override
    public void tick() {
        if (!this.loadQueue.isEmpty()) {
            LegacyModelLoader.loadModel(this.loadQueue.remove(0));
        }
    }

    @Override
    public boolean keyPressed(int keyCode, char typedChar) {
        // 翻页键盘回退：▲/◀ 上一页，▼/▶ 下一页
        if (keyCode == Keyboard.KEY_UP || keyCode == Keyboard.KEY_LEFT) {
            this.grid.flipPage(-1);
            return true;
        }
        if (keyCode == Keyboard.KEY_DOWN || keyCode == Keyboard.KEY_RIGHT) {
            this.grid.flipPage(1);
            return true;
        }
        return super.keyPressed(keyCode, typedChar);
    }

    /** 点选即发（L3-2 同语义：服务端幂等）；日志行=功能链采证锚点。 */
    private void select(String id) {
        this.selected = id;
        for (int i = 0; i < this.cards.size(); i++) {
            this.cards.get(i).selected = this.modelIds.get(i).equals(id);
        }
        LegacySyncChannel.requestSelect(id);
        System.out.println("[ysm-legacy122] gui selected: " + id);
    }
}
