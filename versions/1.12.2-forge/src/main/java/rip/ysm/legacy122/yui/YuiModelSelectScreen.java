package rip.ysm.legacy122.yui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import rip.ysm.yui.YuiPackCard;
import rip.ysm.yui.YuiPanel;
import rip.ysm.yui.YuiPreview;
import rip.ysm.yui.YuiScreen;
import rip.ysm.yui.YuiWidget;

/**
 * 1.12.2 模型选择屏（yui 卡片形态消费者，M-U2 r3 行为语义移植）。
 *
 * <p>功能链不变（0f19e6e，只换皮）：点选→{@link LegacySyncChannel#requestSelect}
 * （网络/校验/持久化/广播零改动）。
 *
 * <p>分组文件夹导航（语义B，主线 PlayerModelScreen 机制移植）：主线是文件夹
 * 导航制非 tab——static currentPath（:115）、包优先模型次之共 10 槽
 * （init :506-537）、进包→该包卡片列表+返回钮（:433-437）、右键上级
 * （:972-976）、页码按路径记忆（pageIndexMap :1213-1223）。1.12.2 数据面=
 * {@link LegacyModelLoader#listPackPaths}/{@link LegacyModelLoader#listPackModels}
 * /{@link LegacyModelLoader#packMeta}（builtin 单级包；嵌套包子层级不建）。
 * 屏内导航=重开屏实例读 static 状态（与主线 init() 重入同构，页内卡/装载队列
 * 重建，动画时间轴在 LegacyCardPreview 按动画名持久=增量刷新不重播）。
 *
 * <p>预览动画（语义A）：卡/左栏经 {@link LegacyCardPreview} 播放
 * properties.preview_animation；hover 三态经卡 hover 边沿+焦点光标触发。
 * 键盘导航：▲▼◀▶ 移动焦点光标（主线 isFocused 分支 :253-256 的宿主面）；
 * 翻页主路径=Pager 按钮+滚轮回退。
 *
 * <p>布局=主线 420x235 几何（r2 起不变）。1.12.2 位图字体/固定管线质感差=
 * 不可消除项（后端代际差）。非 default 模型屏内逐 tick 惰性装载（loadModel
 * 幂等+负缓存），装载完成前空槽。
 */
public final class YuiModelSelectScreen extends YuiScreen {

    private static final int PANEL_WIDTH = 420;
    private static final int PANEL_HEIGHT = 235;
    private static final int PAGE_SIZE = 10;

    /** 当前路径（主线 static currentPath 同为跨实例静态，PlayerModelScreen:115）。 */
    private static String currentPath = "";

    /** 页码按路径记忆（主线 pageIndexMap :1213-1223）。 */
    private static final Map<String, Integer> PAGE_BY_PATH = new HashMap<String, Integer>();

    private final List<String> modelIds = new ArrayList<String>();
    private final List<YuiModelCard> cards = new ArrayList<YuiModelCard>();
    private final List<String> cardIds = new ArrayList<String>();
    private final List<YuiWidget> pageCells = new ArrayList<YuiWidget>();
    private final List<String> pageCellIds = new ArrayList<String>();
    private final List<String> loadQueue = new ArrayList<String>();
    private final List<YuiLabel> nameLabels = new ArrayList<YuiLabel>();
    private String selected;
    private YuiCardGrid grid;
    private int focusIndex = -1;

    public YuiModelSelectScreen() {
        Minecraft mc = Minecraft.getMinecraft();
        this.selected = mc.player != null
                ? LegacyModelRegistry.modelIdOf(mc.player.getUniqueID())
                : LegacyModelRegistry.DEFAULT_MODEL_ID;
        // 数据面：根=default 前置+包卡；包内=该包模型（compareTo 序=主线 :266-273）
        if (currentPath.isEmpty()) {
            this.modelIds.add(LegacyModelRegistry.DEFAULT_MODEL_ID);
        } else {
            this.modelIds.addAll(LegacyModelLoader.listPackModels(currentPath));
        }
        // 装载队列：全部可用模型逐 tick 惰性装载（幂等，r2 语义不变）
        this.loadQueue.addAll(LegacyModelSelectScreen.availableIds());
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

        // 返回钮（主线 :433-437 非根路径同位 20x20；加序在 pane 后=绘制/点击都在其上）
        if (!currentPath.isEmpty()) {
            add(new YuiFlatButton(left + 110, top + 27, 20, 20, "<", new Runnable() {
                @Override
                public void run() {
                    navigateUp();
                }
            }));
        }

        // 卡网格 5x2（:506-538 同式）：包优先模型次之共槽；全部卡入网格，
        // 页内槽位=全局序%10（主线 slotIndex=i+page*10 同几何，页外格隐藏）
        this.grid = new YuiCardGrid(left + 143, top + 28, top + 215);
        add(this.grid);
        this.cards.clear();
        this.cardIds.clear();
        this.pageCells.clear();
        this.pageCellIds.clear();
        List<String> packIds = currentPath.isEmpty()
                ? LegacyModelLoader.listPackPaths() : new ArrayList<String>(0);
        for (int i = 0; i < packIds.size(); i++) {
            final String packPath = packIds.get(i);
            YuiPackCard card = new YuiPackCard(0, 0, packDisplayName(packPath),
                    LegacyPackIcon.icon(packPath), new Runnable() {
                        @Override
                        public void run() {
                            enterPack(packPath);
                        }
                    });
            this.grid.addCard(card);
            this.pageCells.add(card);
            this.pageCellIds.add(null);
        }
        for (int i = 0; i < this.modelIds.size(); i++) {
            final String id = this.modelIds.get(i);
            YuiModelCard card = new YuiModelCard(0, 0, cardDisplayName(id),
                    LegacyCardPreview.card(id), new Runnable() {
                        @Override
                        public void run() {
                            select(id);
                        }
                    });
            card.selected = id.equals(this.selected);
            this.grid.addCard(card);
            this.cards.add(card);
            this.cardIds.add(id);
            this.pageCells.add(card);
            this.pageCellIds.add(id);
        }
        this.grid.setPage(page());
        this.focusIndex = -1;
        applyFocus();

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
        // 页码按路径记忆回写（主线 getCurrentPage/setCurrentPage :1213-1223）
        if (this.grid != null) {
            PAGE_BY_PATH.put(currentPath, Integer.valueOf(this.grid.page()));
        }
    }

    /** 键盘导航：▲/◀ 焦点-1，▼/▶ 焦点+1（页内环绕；翻页走 Pager/滚轮）。 */
    @Override
    public boolean keyPressed(int keyCode, char typedChar) {
        if (keyCode == Keyboard.KEY_UP || keyCode == Keyboard.KEY_LEFT) {
            moveFocus(-1);
            return true;
        }
        if (keyCode == Keyboard.KEY_DOWN || keyCode == Keyboard.KEY_RIGHT) {
            moveFocus(1);
            return true;
        }
        return super.keyPressed(keyCode, typedChar);
    }

    /** 右键空白=上级目录（主线 :972-976 navigateUp 同语义）。 */
    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        boolean consumed = super.mouseClicked(mouseX, mouseY, button);
        if (!consumed && button == 1 && !currentPath.isEmpty()) {
            navigateUp();
            return true;
        }
        return consumed;
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

    /** 点选即发（L3-2 同语义：服务端幂等）；日志行=功能链采证锚点。 */
    private void select(String id) {
        this.selected = id;
        for (int i = 0; i < this.cards.size(); i++) {
            this.cards.get(i).selected = this.cardIds.get(i).equals(id);
        }
        this.setNameLines(id);
        LegacySyncChannel.requestSelect(id);
        System.out.println("[ysm-legacy122] gui selected: " + id);
    }

    /** 进包（主线 :513-518：置路径+清搜索+页 0+init；搜索框无→只置路径/页/重入）。 */
    private void enterPack(String packPath) {
        currentPath = packPath;
        PAGE_BY_PATH.put(packPath, Integer.valueOf(0));
        Minecraft.getMinecraft().displayGuiScreen(new YuiScreenHost1122(YuiModelSelectScreen::new));
    }

    /** 上级（builtin 单级→根；页面记忆保留同主线）。 */
    private void navigateUp() {
        int cut = currentPath.lastIndexOf('/');
        currentPath = cut <= 0 ? "" : currentPath.substring(0, cut);
        Minecraft.getMinecraft().displayGuiScreen(new YuiScreenHost1122(YuiModelSelectScreen::new));
    }

    private int page() {
        Integer p = PAGE_BY_PATH.get(currentPath);
        return p == null ? 0 : p.intValue();
    }

    /** 包显示名：ysm-pack.json name（客户端语言 lang 优先；缺 meta 回退路径末段）。 */
    private static String packDisplayName(String packPath) {
        LegacyModelLoader.PackMeta meta = LegacyModelLoader.packMeta(packPath);
        if (meta != null) {
            String locale = Minecraft.getMinecraft().getLanguageManager()
                    .getCurrentLanguage().getLanguageCode();
            return meta.localizedName(locale);
        }
        return packPath.substring(packPath.lastIndexOf('/') + 1);
    }

    /** 卡显示名：路径末段（主线 displayName=去扩展名模型名同形）。 */
    private static String cardDisplayName(String id) {
        return id.substring(id.lastIndexOf('/') + 1);
    }

    private void moveFocus(int delta) {
        int n = this.pageCells.size();
        if (n == 0) {
            return;
        }
        this.focusIndex = ((this.focusIndex + delta) % n + n) % n;
        // 焦点跨页跟随翻页（页码回写经 tick 落 PAGE_BY_PATH）
        if (this.grid != null && this.focusIndex / PAGE_SIZE != this.grid.page()) {
            this.grid.setPage(this.focusIndex / PAGE_SIZE);
        }
        applyFocus();
    }

    /** 焦点光标落卡：YuiModelCard.focused（描边）+LegacyCardPreview.setFocused（focus 动画）。 */
    private void applyFocus() {
        String focusedModel = null;
        for (int i = 0; i < this.pageCells.size(); i++) {
            YuiWidget cell = this.pageCells.get(i);
            if (cell instanceof YuiModelCard) {
                boolean focused = i == this.focusIndex;
                ((YuiModelCard) cell).focused = focused;
                if (focused) {
                    focusedModel = this.pageCellIds.get(i);
                }
            }
        }
        LegacyCardPreview.setFocused(focusedModel);
    }
}
