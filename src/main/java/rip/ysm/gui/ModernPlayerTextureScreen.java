package rip.ysm.gui;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.capability.PlayerCapability;
import com.elfmcys.yesstevemodel.client.entity.PlayerPreviewEntity;
import com.elfmcys.yesstevemodel.client.gui.PlayerModelScreen;
import com.elfmcys.yesstevemodel.client.model.ModelAssembly;
import com.elfmcys.yesstevemodel.client.renderer.ModelPreviewRenderer;
import com.elfmcys.yesstevemodel.client.renderer.RendererManager;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.Animation;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.util.StringPool;
import com.elfmcys.yesstevemodel.util.data.OrderedStringMap;
import rip.ysm.gui.YsmGui;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMap;
import net.minecraft.ChatFormatting;
//? if >=1.20 {
import net.minecraft.client.gui.GuiGraphics;
//?}
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import rip.ysm.gui.components.AnimationRow;
import rip.ysm.gui.components.TextureGrid;
import rip.ysm.gui.components.buttons.FooterButton;
import rip.ysm.gui.components.buttons.IconButton;
import rip.ysm.gui.components.buttons.TabButton;
import rip.ysm.gui.components.groups.CategoryGroup;
import rip.ysm.gui.components.groups.TextureGroup;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import rip.ysm.util.YsmCollections;

import java.util.List;
import java.util.Map;

public class ModernPlayerTextureScreen extends OptionScreen {

    private static final ResourceLocation ICON_TEXTURE = new ResourceLocation(YesSteveModel.MOD_ID, "texture/icon.png");
    private static final List<String> CATEGORY_ORDER = YsmCollections.immutableListOf("_textures", "main", "extra", "arm", "fp_arm", "tac", "carryon", "parcool", "swem", "slashblade", "tlm", "immersive_melodies", "irons_spell_books", "arrow");

    public final ModelAssembly renderContext;
    public final String modelId;
    public final OrderedStringMap<String, ? extends AbstractTexture> textureMap;
    private final PlayerPreviewEntity modelHolder;
    private final List<IconButton> icons = new ArrayList<>();
    private IconButton hoveredIcon;
    private EditBox searchBox;
    private String currentAnimation = StringPool.EMPTY;

    private int previewLeft, previewTop, previewRight, previewBottom;

    private float yaw = 165.0f;
    private float pitch = -5.0f;
    private float zoom = 80.0f;
    private float offsetX = 0.0f;
    private float offsetY = -60.0f;
    private boolean showGround = true;

    private boolean draggingPreview;
    private int draggingButton = -1;

    public ModernPlayerTextureScreen(PlayerModelScreen parent, String modelId, ModelAssembly modelAssembly) {
        super(YsmGui.trans("gui.yes_steve_model.texture_screen.title"), parent);
        this.renderContext = modelAssembly;
        this.modelId = modelId;
        this.textureMap = modelAssembly.getAnimationBundle().getTextures();
        this.modelHolder = new PlayerPreviewEntity();
    }

    @Override
    protected int computePanelWidth() {
        return Math.min(this.width - 40, 780);
    }

    @Override
    protected int computePanelHeight() {
        return Math.min(this.height - 40, 380);
    }

    @Override
    protected boolean shouldUseCompactTabs() {
        return this.width < 640;
    }

    @Override
    protected int computeRowAreaRight() {
        return panelRight - previewWidth() - 6;
    }

    private int previewWidth() {
        return compactTabs ? 200 : 280;
    }

    @Override
    protected void registerGroups() {
        if (!textureMap.isEmpty()) {
            TextureGroup tg = new TextureGroup();
            tg.add(new TextureGrid(this));
            groups.add(tg);
        }
        Object2ReferenceMap<String, Animation> mainAnims = renderContext.getAnimationBundle().getMainAnimations();
        Map<String, List<String>> buckets = new LinkedHashMap<>();
        for (Map.Entry<String, Animation> e : mainAnims.entrySet()) {
            String name = e.getKey();
            if (name.startsWith("——")) continue;
            String key = e.getValue().sourceKey;
            if (key == null) key = "misc";
            buckets.computeIfAbsent(key, k -> new ArrayList<>()).add(name);
        }
        List<String> sortedCats = new ArrayList<>(buckets.keySet());
        sortedCats.sort((a, b) -> {
            int ia = CATEGORY_ORDER.indexOf(a);
            int ib = CATEGORY_ORDER.indexOf(b);
            if (ia < 0) ia = Integer.MAX_VALUE;
            if (ib < 0) ib = Integer.MAX_VALUE;
            if (ia != ib) return Integer.compare(ia, ib);
            return a.compareTo(b);
        });
        for (String cat : sortedCats) {
            CategoryGroup g = new CategoryGroup(cat);
            for (String name : buckets.get(cat)) {
                g.add(new AnimationRow(0, 0, 0, 18, name, this));
            }
            groups.add(g);
        }
    }

    @Override
    protected void init() {
        super.init();
        removeFooter(applyBtn);
        removeFooter(undoBtn);
        removeFooter(cancelBtn);
        applyBtn.visible = false;
        undoBtn.visible = false;
        cancelBtn.visible = false;
        applyBtn.active = false;
        undoBtn.active = false;
        saveBtn.setMessage(YsmGui.trans("gui.yes_steve_model.config.done"));
        saveBtn.setX(panelRight - saveBtn.getWidth());

        previewLeft = panelRight - previewWidth();
        previewTop = rowAreaTop;
        previewRight = panelRight;
        previewBottom = panelBottom - 60;

        icons.clear();
        int iconY = panelTop;
        int iconX = panelRight - 18;
        icons.add(new IconButton(iconX, iconY, 18, 64, 16, () -> this.currentAnimation = "idle", YsmGui.trans("gui.yes_steve_model.model.stop")));
        iconX -= 20;
        icons.add(new IconButton(iconX, iconY, 18, 48, 16, this::resetView, YsmGui.trans("gui.yes_steve_model.model.reset")));
        iconX -= 20;
        icons.add(new IconButton(iconX, iconY, 18, 64, 0, () -> this.showGround = !this.showGround, YsmGui.trans("gui.yes_steve_model.model.ground")));

        int searchW = Mth.clamp(panelRight - panelLeft - 3 * 18 - 2 * 2 - 200, 80, 140);
        int searchX = iconX - 2 - searchW;
        String oldQuery = searchBox != null ? searchBox.getValue() : "";
        searchBox = new EditBox(this.font, searchX, iconY, searchW, 18, YsmGui.trans("gui.yes_steve_model.search.placeholder"));
        searchBox.setTextColor(0xFFFFFF);
        // EditBox.setHint 1.19.4 起（1194 EditBox.java:581）
        //? if >=1.19.4 {
        searchBox.setHint(YsmGui.trans("gui.yes_steve_model.search.placeholder"));
        //?}
        searchBox.setMaxLength(64);
        searchBox.setValue(oldQuery);
        searchBox.setResponder(s -> applySearchFilter());
        //? if <1.17 {
        /*addButton(searchBox);
         *///?} else {
        addRenderableWidget(searchBox);
        //?}
    }

    @Override
    protected void selectGroup(OptionGroup group) {
        super.selectGroup(group);
        applySearchFilter();
    }

    private void applySearchFilter() {
        if (activeGroup == null) return;
        String s = searchBox != null ? searchBox.getValue().toLowerCase().trim() : "";
        for (OptionRow<?> r : activeRows) r.closeOverlay();
        activeRows.clear();
        int rowY = rowAreaTop;
        int rowW = rowAreaRight - rowAreaLeft;
        for (OptionRow<?> template : activeGroup.getRows()) {
            if (!s.isEmpty() && template instanceof AnimationRow && !((AnimationRow) template).matches(s)) continue;
            template.setX(rowAreaLeft);
            template.setY(rowY);
            template.setWidth(rowW);
            activeRows.add(template);
            rowY += template.getHeight() + 2;
        }
        rowContentHeight = rowY - rowAreaTop;
        maxRowScroll = Math.max(0, rowContentHeight - (rowAreaBottom - rowAreaTop));
        rowScrollOffset = Math.min(rowScrollOffset, maxRowScroll);
        rowScrollDisplay = Math.min(rowScrollDisplay, maxRowScroll);
    }

    private void resetView() {
        offsetX = 0.0f;
        offsetY = -60.0f;
        zoom = 80.0f;
        yaw = 165.0f;
        pitch = -5.0f;
    }

    public void selectAnimation(String name) {
        this.currentAnimation = name;
        if (!modelHolder.getAnimationStateMachine().isCurrentAnimation(name)) {
            modelHolder.getAnimationStateMachine().setCurrentAnimation(name);
        }
    }

    public String currentAnimation() {
        return currentAnimation;
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) this.minecraft.setScreen(parentScreen);
    }

    // Screen.render 签名双轴（1.16.5 Screen.java:73 ↔ 1.20.1 GuiGraphics 钩子）
    //? if <1.20 {
    /*@Override
    public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        hoveredIcon = null;
        for (IconButton btn : icons) {
            if (btn.contains(mouseX, mouseY)) {
                hoveredIcon = btn;
                break;
            }
        }
        super.render(pose, mouseX, mouseY, partialTick);
        YsmGui g = new YsmGui(pose);
        for (IconButton btn : icons) drawIcon(g, btn);
    }
     *///?}
     //? if >=1.20 {
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        hoveredIcon = null;
        for (IconButton btn : icons) {
            if (btn.contains(mouseX, mouseY)) {
                hoveredIcon = btn;
                break;
            }
        }
        super.render(graphics, mouseX, mouseY, partialTick);
        YsmGui g = new YsmGui(graphics);
        for (IconButton btn : icons) drawIcon(g, btn);
    }
     //?}

    private void drawIcon(YsmGui g, IconButton btn) {
        boolean hover = btn == hoveredIcon;
        int bg = hover ? 0x90171717 : 0x90000000;
        g.fill(btn.x, btn.y, btn.x + btn.size, btn.y + btn.size, bg);
        int ix = btn.x + (btn.size - 16) / 2;
        int iy = btn.y + (btn.size - 16) / 2;
        g.blit(ICON_TEXTURE, ix, iy, 16, 16, btn.u, btn.v, 16, 16, 256, 256);
    }

    @Override
    protected void collectBlurRegions(List<int[]> out) {
        out.add(new int[]{panelLeft, panelTop, panelRight - panelLeft, 18});
        int tabScroll = Math.round(tabScrollDisplay);
        for (TabButton tb : tabButtons) {
            if (compactTabs) {
                int x = tb.getX() - tabScroll;
                int xRight = x + tb.getWidth();
                int left = Math.max(x, tabAreaLeft);
                int right = Math.min(xRight, tabAreaRight);
                if (right > left) out.add(new int[]{left, tb.getY(), right - left, tb.getHeight()});
            } else {
                int y = tb.getY() - tabScroll;
                int yBot = y + tb.getHeight();
                int top = Math.max(y, tabAreaTop);
                int bot = Math.min(yBot, tabAreaBottom);
                if (bot > top) out.add(new int[]{tb.getX(), top, tb.getWidth(), bot - top});
            }
        }
        if (activeGroup instanceof TextureGroup && !activeRows.isEmpty() && activeRows.get(0) instanceof TextureGrid) {
            TextureGrid grid = (TextureGrid) activeRows.get(0);
            grid.collectBlurRegions(out, Math.round(rowScrollDisplay), rowAreaTop, rowAreaBottom);
        } else {
            int rowScroll = Math.round(rowScrollDisplay);
            for (OptionRow<?> row : activeRows) {
                int y = row.getY() - rowScroll;
                int yBot = y + row.getHeight();
                if (yBot <= rowAreaTop || y >= rowAreaBottom) continue;
                int top = Math.max(y, rowAreaTop);
                int bot = Math.min(yBot, rowAreaBottom);
                out.add(new int[]{row.getX(), top, row.getWidth(), bot - top});
            }
        }
        out.add(new int[]{previewLeft, previewTop, previewRight - previewLeft, previewBottom - previewTop});
        addFooterRect(out, applyBtn);
        addFooterRect(out, undoBtn);
        addFooterRect(out, saveBtn);
        addFooterRect(out, cancelBtn);
        for (IconButton btn : icons) {
            out.add(new int[]{btn.x, btn.y, btn.size, btn.size});
        }
        if (searchBox != null && searchBox.visible) {
            //? if <1.17 {
            /*out.add(new int[]{searchBox.x, searchBox.y, searchBox.getWidth(), searchBox.getHeight()});
             *///?}
            //? if >=1.17 && <1.19.4 {
            /*out.add(new int[]{searchBox.x, searchBox.y, searchBox.getWidth(), searchBox.getHeight()});
             *///?}
            //? if >=1.19.4 {
            out.add(new int[]{searchBox.getX(), searchBox.getY(), searchBox.getWidth(), searchBox.getHeight()});
            //?}
        }
        if (hoveredIcon != null || hoveredRow instanceof AnimationRow) {
            int descY = panelBottom - 32;
            out.add(new int[]{panelLeft, descY, panelRight - panelLeft, 28});
        }
    }

    private static void addFooterRect(List<int[]> out, FooterButton btn) {
        if (btn == null || !btn.visible) return;
        out.add(new int[]{btn.getX(), btn.getY(), btn.getWidth(), btn.getHeight()});
    }

    @Override
    protected void renderExtras(YsmGui g, int mouseX, int mouseY, float partialTick) {
        g.fill(previewLeft, previewTop, previewRight, previewBottom, 0x66000000);
        renderPreview(g, partialTick);
    }

    @Override
    protected void renderDescription(YsmGui g, int descY) {
        if (hoveredIcon != null) {
            g.fill(panelLeft, descY, panelRight, descY + 28, 0x80000000);
            g.drawString(this.font, hoveredIcon.tooltip, panelLeft + 6, descY + 10, -1, false);
            return;
        }
        if (hoveredRow instanceof AnimationRow) {
            AnimationRow row = (AnimationRow) hoveredRow;
            g.fill(panelLeft, descY, panelRight, descY + 28, 0x80000000);
            g.drawString(this.font, row.getMessage(), panelLeft + 6, descY + 4, -1, false);
            g.drawString(this.font, YsmGui.text(row.animKey).withStyle(ChatFormatting.GRAY), panelLeft + 6, descY + 16, 0xFFAAAAAA, false);
        }
    }

    private void renderPreview(YsmGui g, float partialTick) {
        if (this.minecraft == null || this.minecraft.player == null) return;
        if (!modelHolder.getAnimationStateMachine().isCurrentAnimation(currentAnimation)) {
            modelHolder.getAnimationStateMachine().setCurrentAnimation(currentAnimation);
        }
        double scale = this.minecraft.getWindow().getGuiScale();
        int sx = (int) (previewLeft * scale);
        int sy = (int) (this.minecraft.getWindow().getHeight() - previewBottom * scale);
        int sw = (int) ((previewRight - previewLeft) * scale);
        int sh = (int) ((previewBottom - previewTop) * scale);
        YsmGui.enableScissorBox(sx, sy, sw, sh);
        PlayerCapability.get(this.minecraft.player).ifPresent(cap -> {
            modelHolder.initModelWithTexture(modelId, cap.getCurrentTextureName());
            float cx = (previewLeft + previewRight) / 2.0f + offsetX;
            float cy = previewTop + (previewBottom - previewTop) * 0.65f + offsetY;
            ModelPreviewRenderer.renderEntityPreview(cx, cy, zoom, pitch, yaw, this.minecraft.getFrameTime(), modelHolder, RendererManager.getPlayerRenderer(), showGround);
        });
        YsmGui.disableScissorBox();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (IconButton btn : icons) {
                if (btn.contains(mouseX, mouseY)) {
                    btn.onPress.run();
                    return true;
                }
            }
        }
        if (isInPreview(mouseX, mouseY)) {
            draggingPreview = true;
            draggingButton = button;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (draggingPreview && button == draggingButton) {
            draggingPreview = false;
            draggingButton = -1;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingPreview && button == draggingButton) {
            if (button == 0) {
                yaw = (float) (yaw + dragX * 1.2);
                pitch = Mth.clamp((float) (pitch - dragY * 0.8), -90.0f, 90.0f);
            } else if (button == 1) {
                offsetX = (float) (offsetX + dragX);
                offsetY = (float) (offsetY + dragY);
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (isInPreview(mouseX, mouseY)) {
            zoom = Mth.clamp((float) (zoom * (1.0 + delta * 0.1)), 18.0f, 360.0f);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private boolean isInPreview(double mouseX, double mouseY) {
        return mouseX >= previewLeft && mouseX < previewRight && mouseY >= previewTop && mouseY < previewBottom;
    }
}
