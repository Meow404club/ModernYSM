package rip.ysm.yui;

import java.util.ArrayList;
import java.util.List;

/**
 * 卡网格 + 翻页器：5 列 x2 行、格距 55x93（PlayerModelScreen.init :506-538
 * slotX/slotY 同式）；翻页钮 52x14 扁平（:488-501 FlatColorButton，网格内相对
 * 位 prev=+55/next=+165）+ 页码 n/m 居中（:586-591）。网格持有卡片按页显隐；
 * 滚轮翻页为辅（lwjgl3ify GuiScreen 面滚轮桥断裂实证，Pager 按钮是主路径）。
 * r3：格位泛化为 YuiWidget——包卡（YuiPackCard）与模型卡同槽混排，
 * 包前模型后（主线 init :510-522 槽位填充序）。
 */
public class YuiCardGrid extends YuiWidget {

    private static final int COLS = 5;
    private static final int ROWS = 2;
    private static final int PITCH_X = 55;
    private static final int PITCH_Y = 93;
    private static final int PAGE_SIZE = COLS * ROWS;

    /** 网格整体占宽（末列按卡宽收口）。 */
    public static final int GRID_WIDTH = (COLS - 1) * PITCH_X + YuiModelCard.CARD_WIDTH;
    /** 网格整体占高（末行按卡高收口）。 */
    public static final int GRID_HEIGHT = (ROWS - 1) * PITCH_Y + YuiModelCard.CARD_HEIGHT;

    private final List<YuiWidget> cells = new ArrayList<YuiWidget>();
    private final YuiFlatButton prevButton;
    private final YuiFlatButton nextButton;
    private final YuiLabel pageLabel;
    private int page;

    /** pagerY：翻页行顶 y（主线 guiTop+215 同位）。 */
    public YuiCardGrid(int x, int y, int pagerY) {
        super(x, y, GRID_WIDTH, GRID_HEIGHT);
        this.prevButton = new YuiFlatButton(x + 55, pagerY, 52, 14, "<", new Runnable() {
            @Override
            public void run() {
                flipPage(-1);
            }
        });
        this.nextButton = new YuiFlatButton(x + 165, pagerY, 52, 14, ">", new Runnable() {
            @Override
            public void run() {
                flipPage(1);
            }
        });
        this.pageLabel = new YuiLabel(x + GRID_WIDTH / 2, pagerY + 3, 0, 10, "1/1");
        this.pageLabel.align = YuiBackend.Align.CENTER;
    }

    /** 追加卡片（按加入序落格；页内槽位=全局序 % 页容量——格位逐页重复，主线 :506-509 同式）。 */
    public YuiWidget addCard(YuiWidget cell) {
        int index = this.cells.size() % PAGE_SIZE;
        cell.x = this.x + (index % COLS) * PITCH_X;
        cell.y = this.y + (index / COLS) * PITCH_Y;
        this.cells.add(cell);
        applyVisibility();
        return cell;
    }

    public int pageCount() {
        return Math.max(1, (this.cells.size() + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    public void flipPage(int delta) {
        int next = Math.max(0, Math.min(this.page + delta, pageCount() - 1));
        if (next != this.page) {
            this.page = next;
            applyVisibility();
        }
    }

    public int page() {
        return this.page;
    }

    public void setPage(int page) {
        this.page = Math.max(0, Math.min(page, pageCount() - 1));
        applyVisibility();
    }

    private void applyVisibility() {
        for (int i = 0; i < this.cells.size(); i++) {
            this.cells.get(i).visible = i / PAGE_SIZE == this.page;
        }
        this.pageLabel.text = (this.page + 1) + "/" + pageCount();
    }

    @Override
    public void render(YuiBackend backend, int mouseX, int mouseY, float partialTick) {
        for (int i = 0; i < this.cells.size(); i++) {
            YuiWidget cell = this.cells.get(i);
            if (cell.visible) {
                cell.render(backend, mouseX, mouseY, partialTick);
            }
        }
        this.prevButton.render(backend, mouseX, mouseY, partialTick);
        this.nextButton.render(backend, mouseX, mouseY, partialTick);
        this.pageLabel.render(backend, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (this.prevButton.mouseClicked(mouseX, mouseY, button)
                || this.nextButton.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        for (int i = this.cells.size() - 1; i >= 0; i--) {
            YuiWidget cell = this.cells.get(i);
            if (cell.visible && cell.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(int mouseX, int mouseY, double delta) {
        if (pageCount() <= 1 || !isHovered(mouseX, mouseY)) {
            return false;
        }
        flipPage(delta > 0 ? -1 : 1);
        return true;
    }
}
