package com.elfmcys.yesstevemodel.util;

import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.world.item.Item;

public class ThreadLocalItemTagSets {

    public static final ThreadLocal<ReferenceOpenHashSet<Item>> ITEM_SET = ThreadLocal.withInitial(() -> new ReferenceOpenHashSet<>(16));

    public static final ThreadLocal<ReferenceArrayList<YsmTag.ItemTag>> TAG_KEY_LIST = ThreadLocal.withInitial(() -> new ReferenceArrayList<>(16));
}