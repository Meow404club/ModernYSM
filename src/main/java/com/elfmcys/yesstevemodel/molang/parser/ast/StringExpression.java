package com.elfmcys.yesstevemodel.molang.parser.ast;

import com.elfmcys.yesstevemodel.geckolib3.core.molang.util.StringPool;
// 1.12.2 无 net.minecraft.resources/world.entity（mojmap 包，1.14.4 起）——RL/EquipmentSlot
// 缓存面是 1.14+ 语义（accessor typed 面在 >=1.14 门内）
//? if <1.14 {
//? }
//? if >=1.14 {
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
//? }
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public final class StringExpression implements Expression {

    private final String name;

    private final int path;

    // 1.14+ 泛化为 ResourceLocation/EquipmentSlot 语义（getter/setter 由 >=1.14 门收窄，
    // 1.12.2 走 Object 槽位）——字段恒 Object，raw 面合法（legacy-1222-l1-render）
    private Object cachedLocation;

    private Object cachedSlot;

    private boolean slotResolved;

    public StringExpression(@NotNull String str) {
        this.name = Objects.requireNonNull(str, "value");
        this.path = StringPool.computeIfAbsent(str);
    }

    @NotNull
    public String getName() {
        return this.name;
    }

    public int getPath() {
        return this.path;
    }

    @Override
    public <R> R visit(@NotNull ExpressionVisitor<R> expressionVisitor) {
        return expressionVisitor.visitString(this);
    }

    public String toString() {
        return this.name;
    }

    // 1.14+ typed 访问器（raw 面恒 1.20.1 合法）；<1.14 由门换 Object 版
    //（cachedLocation/cachedSlot 字段恒 Object——ResourceLocation 赋值面经 set* 收窄）
    //? if <1.14 {
    /*
    @Nullable
    public Object getResourceLocation() {
        return this.cachedLocation;
    }

    public void setResourceLocation(@Nullable Object resourceLocation) {
        this.cachedLocation = resourceLocation;
    }

    @Nullable
    public Object getCachedSlot() {
        return this.cachedSlot;
    }

    public boolean isSlotResolved() {
        return this.slotResolved;
    }

    public void setCachedSlot(@Nullable Object slot) {
        this.cachedSlot = slot;
        this.slotResolved = true;
    }
     */
    //? }
    //? if >=1.14 {
    @Nullable
    public ResourceLocation getResourceLocation() {
        return (ResourceLocation) this.cachedLocation;
    }

    public void setResourceLocation(@Nullable ResourceLocation resourceLocation) {
        this.cachedLocation = resourceLocation;
    }

    @Nullable
    public EquipmentSlot getCachedSlot() {
        return (EquipmentSlot) this.cachedSlot;
    }

    public boolean isSlotResolved() {
        return this.slotResolved;
    }

    public void setCachedSlot(@Nullable EquipmentSlot slot) {
        this.cachedSlot = slot;
        this.slotResolved = true;
    }
    //? }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (obj instanceof String) {
            return this.name.equals(obj);
        }
        return (obj instanceof StringExpression) && this.path == ((StringExpression) obj).path;
    }

    public int hashCode() {
        return this.name.hashCode();
    }
}