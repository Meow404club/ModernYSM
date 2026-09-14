package com.elfmcys.yesstevemodel.util;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 版本中性 Tag 句柄 + Registry key 查询门面。
 * <p>1.20.1 {@code TagKey.create(Registries.X, rl)} + {@code ItemStack.is(TagKey)} +
 * {@code BuiltInRegistries.X.getKey(v)} ↔ 1.16.5 forge {@code ItemTags/EntityTypeTags.createOptional(rl)}
 * （{@code Tags.IOptionalNamedTag}，Tag.contains 直查）+ {@code Registry.X.getKey(v)}。
 * 实据 1.16.5 mojmap jar javap：无 TagKey/core.registries.Registries/BuiltInRegistries；
 * ItemTags.createOptional(ResourceLocation)、EntityTypeTags.createOptional(ResourceLocation)、
 * Registry.ITEM/ENTITY_TYPE/MOB_EFFECT/PARTICLE_TYPE 静态字段在；
 * Tag$Named.getName()、Tag.contains(T)。
 * <p>句柄即门面内部类型（1.20.1 轴持有 TagKey 本体，非 Object 装箱），matches 直调零反射。
 */
public final class YsmTag {
    private YsmTag() {
    }

    /** 物品标签句柄（opaque）。 */
    public static final class ItemTag {
        // 1.16.1 forge 32.x 无 createOptional/IOptionalNamedTag（33.x/36.x 起）→
        // SerializationTags.getInstance().getItems().getTagOrEmpty(rl) 动态查询（官方 1161
        // client.txt 全套命名实证：SerializationTags.getInstance/getItems、TagCollection
        // getTagOrEmpty、Tag.contains）。tag 缺失时 getTagOrEmpty 返回空 tag → contains=false，
        // 与下方 1.16.5 未绑定守卫同语义（不抛不炸）。
        //? if <1.16.2 {
        /*private final ResourceLocation rl;

        private ItemTag(ResourceLocation rl) {
            this.rl = rl;
        }

        static ItemTag of(ResourceLocation rl) {
            return new ItemTag(rl);
        }

        // 1.16.5 未绑定守卫：StaticTagHelper$Wrapper.resolve()（SRG TagRegistry$NamedTag.func_232944_c_）
        // 对 tag == null（可选 tag json 缺失→rebind(null) 永不绑定）抛
        // IllegalStateException("Tag ... used before it was bound")（vanilla-mc-1165 StaticTagHelper.java:69-75；
        // Forge IOptionalNamedTag 只有 isDefaulted()，无 isBound）。
        // 调用链 InnerClassify.getItemType←HandRenderFunction.eval 是渲染线程逐帧热路径，
        // 未绑定时语义=不含任何物品→false。吞点必须在本层（molang eval 入口），不放行到渲染序列中段。
        // 注意：本方法整体处于 <1.17 注释包分支内（stonecutter 块条件不嵌套；注释只能用 // 行注释，
        // 任何块注释的 star-slash 都会提前闭合外层包裹）。
        public boolean matches(ItemStack stack) {
            return net.minecraft.tags.SerializationTags.getInstance().getItems().getTagOrEmpty(this.rl).contains(stack.getItem());
        }

        public ResourceLocation location() {
            return this.rl;
        }
         *///?}
        //? if >=1.16.2 && <1.18.2 {
        /*private final net.minecraftforge.common.Tags.IOptionalNamedTag<Item> tag;

        private ItemTag(net.minecraftforge.common.Tags.IOptionalNamedTag<Item> tag) {
            this.tag = tag;
        }

        static ItemTag of(ResourceLocation rl) {
            return new ItemTag(net.minecraft.tags.ItemTags.createOptional(rl));
        }

        public boolean matches(ItemStack stack) {
            try {
                return this.tag.contains(stack.getItem());
            } catch (IllegalStateException e) {
                return false;
            }
        }

        public ResourceLocation location() {
            return this.tag.getName();
        }
         *///?}
        // 中段（1.17~1.19.2）：TagKey.create(Registry.ITEM_REGISTRY)；TagKey 为 record（location() 同名）
        //? if >=1.18.2 && <1.19.3 {
        /*
        private final net.minecraft.tags.TagKey<Item> tag;

        private ItemTag(net.minecraft.tags.TagKey<Item> tag) {
            this.tag = tag;
        }

        static ItemTag of(ResourceLocation rl) {
            return new ItemTag(net.minecraft.tags.TagKey.create(net.minecraft.core.Registry.ITEM_REGISTRY, rl));
        }

        public boolean matches(ItemStack stack) {
            return stack.is(this.tag);
        }

        public ResourceLocation location() {
            return this.tag.location();
        }
         *///?}
        //? if >=1.19.3 {
        private final net.minecraft.tags.TagKey<Item> tag;

        private ItemTag(net.minecraft.tags.TagKey<Item> tag) {
            this.tag = tag;
        }

        static ItemTag of(ResourceLocation rl) {
            return new ItemTag(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, rl));
        }

        public boolean matches(ItemStack stack) {
            return stack.is(this.tag);
        }

        public ResourceLocation location() {
            return this.tag.location();
        }
        //?}
    }

    /** 实体类型标签句柄（opaque）。 */
    public static final class EntityTypeTag {
        //? if <1.16.2 {
        /*private final ResourceLocation rl;

        private EntityTypeTag(ResourceLocation rl) {
            this.rl = rl;
        }

        static EntityTypeTag of(ResourceLocation rl) {
            return new EntityTypeTag(rl);
        }

        // 同 ItemTag.matches：<1.16.2 走 getTagOrEmpty（缺失=空 tag=不含）。
        public boolean matches(EntityType<?> type) {
            return net.minecraft.tags.SerializationTags.getInstance().getEntityTypes().getTagOrEmpty(this.rl).contains(type);
        }

        public ResourceLocation location() {
            return this.rl;
        }
         *///?}
        //? if >=1.16.2 && <1.18.2 {
        /*private final net.minecraftforge.common.Tags.IOptionalNamedTag<EntityType<?>> tag;

        private EntityTypeTag(net.minecraftforge.common.Tags.IOptionalNamedTag<EntityType<?>> tag) {
            this.tag = tag;
        }

        static EntityTypeTag of(ResourceLocation rl) {
            return new EntityTypeTag(net.minecraft.tags.EntityTypeTags.createOptional(rl));
        }

        // 同 ItemTag.matches：1.16.5 未绑定 tag 防炸（语义=不含）。
        public boolean matches(EntityType<?> type) {
            try {
                return this.tag.contains(type);
            } catch (IllegalStateException e) {
                return false;
            }
        }

        public ResourceLocation location() {
            return this.tag.getName();
        }
         *///?}
        // 中段：TagKey.create(Registry.ENTITY_TYPE_REGISTRY)
        //? if >=1.18.2 && <1.19.3 {
        /*
        private final net.minecraft.tags.TagKey<EntityType<?>> tag;

        private EntityTypeTag(net.minecraft.tags.TagKey<EntityType<?>> tag) {
            this.tag = tag;
        }

        static EntityTypeTag of(ResourceLocation rl) {
            return new EntityTypeTag(net.minecraft.tags.TagKey.create(net.minecraft.core.Registry.ENTITY_TYPE_REGISTRY, rl));
        }

        public boolean matches(EntityType<?> type) {
            return type.is(this.tag);
        }

        public ResourceLocation location() {
            return this.tag.location();
        }
         *///?}
        //? if >=1.19.3 {
        private final net.minecraft.tags.TagKey<EntityType<?>> tag;

        private EntityTypeTag(net.minecraft.tags.TagKey<EntityType<?>> tag) {
            this.tag = tag;
        }

        static EntityTypeTag of(ResourceLocation rl) {
            return new EntityTypeTag(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, rl));
        }

        public boolean matches(EntityType<?> type) {
            return type.is(this.tag);
        }

        public ResourceLocation location() {
            return this.tag.location();
        }
        //?}
    }

    public static ItemTag itemTag(ResourceLocation rl) {
        return ItemTag.of(rl);
    }

    public static EntityTypeTag entityTypeTag(ResourceLocation rl) {
        return EntityTypeTag.of(rl);
    }

    public static ResourceLocation itemKey(Item item) {
        // Registry.ITEM/... 静态字段 1.16.5~1.19.2 同形（1182 Registry.java:204）；BuiltInRegistries 1.19.3+
        //? if <1.19.3 {
        /*return net.minecraft.core.Registry.ITEM.getKey(item);
         *///?} else {
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
        //?}
    }

    public static ResourceLocation blockKey(net.minecraft.world.level.block.Block block) {
        //? if <1.19.3 {
        /*return net.minecraft.core.Registry.BLOCK.getKey(block);
         *///?} else {
        return net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block);
        //?}
    }

    /** 附魔注册表直查（1.16.5 Registry.ENCHANTMENT ↔ 1.20.1 BuiltInRegistries.ENCHANTMENT）。
     * 1.21 起 Registry 接口删除 get(ResourceLocation)（vanilla-1.21.1 Registry.java 仅余
     * getHolder(ResourceLocation):141），且下游 EnchantmentHelper/ItemEnchantments 全收
     * Holder<Enchantment> → >=1.21 返回值即 Holder（同名异返回类型按版本二选一）。 */
    //? if <1.21 {
    public static net.minecraft.world.item.enchantment.Enchantment enchantment(ResourceLocation rl) {
        //? if <1.19.3 {
        /*return net.minecraft.core.Registry.ENCHANTMENT.get(rl);
         *///?} else {
        return net.minecraft.core.registries.BuiltInRegistries.ENCHANTMENT.get(rl);
        //?}
    }
    //?}
    // 1.21 附魔数据驱动化：BuiltInRegistries.ENCHANTMENT 字段删除（vanilla-1.21.1
    // BuiltInRegistries.java 实证）→ 经 HolderLookup.Provider 动态解析
    //? if >=1.21 {
    /*public static net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment> enchantment(net.minecraft.core.HolderLookup.Provider provider, ResourceLocation rl) {
        // HolderGetter 仅收 ResourceKey（vanilla-1.21.1 HolderGetter.java:8）→ rl 先建 key
        return provider.lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                .get(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENCHANTMENT, rl)).orElse(null);
    }*/
    //?}

    /** 物品注册表直查。 */
    public static Item item(ResourceLocation rl) {
        //? if <1.19.3 {
        /*return net.minecraft.core.Registry.ITEM.get(rl);
         *///?} else {
        // 1.21.2 Registry.get(rl) 语义变 Optional<Reference<T>>，直取 T 改名 getValue
        //（vanilla-1.21.3 Registry.java:73 getValue(ResourceLocation)）
        //? if >=1.21.2
        /*return net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(rl);*/
        //? if <1.21.2
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.get(rl);
        //?}
    }

    public static ResourceLocation entityTypeKey(EntityType<?> type) {
        //? if <1.19.3 {
        /*return net.minecraft.core.Registry.ENTITY_TYPE.getKey(type);
         *///?} else {
        return net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(type);
        //?}
    }

    public static ResourceLocation mobEffectKey(MobEffect effect) {
        //? if <1.19.3 {
        /*return net.minecraft.core.Registry.MOB_EFFECT.getKey(effect);
         *///?} else {
        return net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getKey(effect);
        //?}
    }

    /** 1.16.5 轴走 Registry.MOB_EFFECT.get(rl)（对位 1.20.1 BuiltInRegistries.MOB_EFFECT.get(rl)）。 */
    public static MobEffect mobEffect(ResourceLocation rl) {
        //? if <1.19.3 {
        /*return net.minecraft.core.Registry.MOB_EFFECT.get(rl);
         *///?} else {
        // 1.21.2 get(rl) 语义变 Optional<Reference<T>>，直取 T 改名 getValue（同 item()）
        //? if >=1.21.2
        /*return net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getValue(rl);*/
        //? if <1.21.2
        return net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.get(rl);
        //?}
    }

    /** 1.16.5 Registry.getId(T) 内部数值 id（对位 1.20.1 writeId 的数值 id 语义，同版本线自洽）。 */
    public static int mobEffectNetworkId(MobEffect effect) {
        //? if <1.19.3 {
        /*return net.minecraft.core.Registry.MOB_EFFECT.getId(effect);
         *///?} else {
        return net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getId(effect);
        //?}
    }

    /** 1.16.5 Registry.byId(int)（对位 1.20.1 readById）。 */
    public static MobEffect mobEffectByNetworkId(int id) {
        //? if <1.19.3 {
        /*return net.minecraft.core.Registry.MOB_EFFECT.byId(id);
         *///?} else {
        return net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.byId(id);
        //?}
    }

    public static ResourceLocation particleTypeKey(ParticleType<?> type) {
        //? if <1.19.3 {
        /*return net.minecraft.core.Registry.PARTICLE_TYPE.getKey(type);
         *///?} else {
        return net.minecraft.core.registries.BuiltInRegistries.PARTICLE_TYPE.getKey(type);
        //?}
    }
}
