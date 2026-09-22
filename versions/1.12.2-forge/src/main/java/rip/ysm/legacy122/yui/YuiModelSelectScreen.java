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
import rip.ysm.yui.YuiColors;
import rip.ysm.yui.YuiFlatButton;
import rip.ysm.yui.YuiLabel;
import rip.ysm.yui.YuiModelCard;
import rip.ysm.yui.YuiPanel;
import rip.ysm.yui.YuiPreview;
import rip.ysm.yui.YuiScreen;
import rip.ysm.yui.YuiWidget;

/**
 * 1.12.2 模型选择屏（yui 卡片形态消费者，M-U2 r2 布局对齐主线）。
 *
 * <p>功能链不变（0f19e6e，只换皮）：列表=default + 服务端登录下发
 * （{@link LegacyModelSelectScreen#setAvailable}），点选→
 * {@link LegacySyncChannel#requestSelect}（网络/校验/持久化/广播零改动）。
 *
 * <p>布局=主线 PlayerModelScreen 几何逐项对位（init :389-390/:506-538、render
 * :567-568/:586-591、renderModelPreview :805-887）：420x235 面板居中
 * （guiLeft=(w-420)/2、guiTop=(h-235)/2），左栏 135（大预览 scissor 5..130 ×
 * +29..+200、锚 +190、模型名 split 125 @+205、版本串 @+226 暗灰）+ 右栏 282
 * （网格 slotX=+143+55*(i%5)、slotY=+28+93*(i/5)；翻页钮 52x14 @+198/+308、
 * 页码居中 +279）。1.12.2 位图字体/固定管线质感差=不可消除项（后端代际差）。
 * 主线搜索框/分类/上传图标不在此期（无功能对应，纯装饰不建）。
 *
 * <p>非 default 模型屏内逐 tick 惰性装载（loadModel 幂等+负缓存），装载完成前
 * 空槽。翻页主路径=Pager 按钮；滚轮（host tick 轮询喂入）与 ▲▼/◀▶ 键为回退。
 * 854x480@scale2（GUI 427x240）起面板完整可见；更小窗=面板裁剪，与主线小窗
 * 同性质（ponytail：RFB 窗口下限 854x480，无更小场景）。
 */
public final class YuiModelSelectScreen extends YuiScreen {

    private static final int PANEL_WIDTH = 420;
    private static final int PANEL_HEIGHT = 235;

    private final List<String> modelIds = new ArrayList<String>();
    private final List<YuiModelCard> cards = new ArrayList<YuiModelCard>();
    private final List<String> loadQueue = new ArrayList<String>();
    private final List<YuiLabel> nameLabels = new ArrayList<YuiLabel>();
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
        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;

        // 双面板 135+3 缝+282（PlayerModelScreen.render :567-568 同位；无顶强调线）
        YuiPanel leftPanel = new YuiPanel(left, top, 135, PANEL_HEIGHT);
        leftPanel.accentLine = false;
        add(leftPanel);
        YuiPanel rightPanel = new YuiPanel(left + 138, top, 282, PANEL_HEIGHT);
        rightPanel.accentLine = false;
        add(rightPanel);

        // 左栏大预览：当前选中模型，头/身随鼠标（renderModelPreview :816 scissor 同位）
        add(new YuiWidget(left + 5, top + 29, 125, 171) {
            @Override
            public void render(YuiBackend backend, int mouseX, int mouseY, float partialTick) {
                YuiPreview pane = LegacyCardPreview.pane();
                backend.preview(pane, this.x, this.y,
                        this.x + this.width, this.y + this.height, mouseX, mouseY, partialTick);
            }
        });

        // 左栏模型名：split 125 @+205 起，行距 10，居中 135（:870-883 同几何）；
        // 标签持引用，select() 时刷新（左栏预览读 registry 即时切换，名字不能滞后）
        this.nameLabels.clear();
        this.setNameLines(this.selected);

        // 左栏版本串（:592-605 同位暗灰）
        YuiLabel version = new YuiLabel(left + 2, top + 226, 0, 10, "openysm legacy122");
        version.color = YuiColors.TEXT_DIM;
        version.shadow = false;
        add(version);

        // 卡网格 5x2（:506-538 同式）+ 翻页行（:488-501/@586-591 同位）
        this.grid = new YuiCardGrid(left + 143, top + 28, top + 215);
        add(this.grid);
        for (int i = 0; i < this.modelIds.size(); i++) {
            final String id = this.modelIds.get(i);
            YuiModelCard card = new YuiModelCard(0, 0, id, LegacyCardPreview.card(id), new Runnable() {
                @Override
                public void run() {
                    select(id);
                }
            });
            card.selected = id.equals(this.selected);
            this.grid.addCard(card);
            this.cards.add(card);
        }

        // Done（1.12.2 验收要求显式关屏；主线翻页行语言 52x14 扁平）
        add(new YuiFlatButton(left + 360, top + 215, 52, 14,
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

    /** 左栏模型名两行刷新（split 125，同 layout 几何）。 */
    private void setNameLines(String name) {
        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;
        List<String> lines = YuiLabel.split(this.backend, name == null ? "" : name, 125, 2);
        while (this.nameLabels.size() < lines.size()) {
            YuiLabel line = new YuiLabel(left + 67, top + 205, 0, 10, "");
            line.align = YuiBackend.Align.CENTER;
            this.nameLabels.add(line);
            add(line);
        }
        for (int i = 0; i < this.nameLabels.size(); i++) {
            YuiLabel label = this.nameLabels.get(i);
            label.text = i < lines.size() ? lines.get(i) : "";
            label.x = left + 67;
            label.y = top + 205 + i * 10;
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
        this.setNameLines(id);
        LegacySyncChannel.requestSelect(id);
        System.out.println("[ysm-legacy122] gui selected: " + id);
    }
}
