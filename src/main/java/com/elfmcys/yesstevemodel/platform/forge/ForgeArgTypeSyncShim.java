//? if forge && <1.16.4 {
/*package com.elfmcys.yesstevemodel.platform.forge;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.ArgumentType;
import net.minecraft.commands.synchronization.ArgumentSerializer;
import net.minecraft.commands.synchronization.ArgumentTypes;
import net.minecraft.commands.synchronization.EmptyArgumentSerializer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.server.command.EnumArgument;
import net.minecraftforge.server.command.ModIdArgument;

// forge 命令参数类型注册补齐（unimined-env-116x；仅 <1.16.4 编译存在）。
//
// 上游代差：forge 32/33/34（1.16.1~1.16.3）的 ForgeMod 从不调用 ArgumentTypes.register
// 注册自家 ModIdArgument/EnumArgument（35.x/1.16.4 起才补：ForgeMod 构造内
// ArgumentTypes.register("forge:enum", ...) / ("forge:modid", ...)，字节码对拍实证）。
// 而自带 /config、/forge 命令树在登录同步（SCommandListPacket）时经
// ArgumentTypes.serialize 序列化这些节点：BY_CLASS 查不到 → 写占位
// new ResourceLocation("")（toString="minecraft:"）→ 客户端 deserialize 同样查不到 →
// LOGGER "Could not deserialize minecraft:" → 返回 null → 进服必断连
// （1161 tour 复现：server.log "Could not serialize ModIdArgument/EnumArgument -
// will not be sent to client!" + client.log "Could not deserialize minecraft:" ×2）。
//
// 本 shim 在两侧 common setup 补注册，口径照抄 35.x 上游（名字 forge:enum/forge:modid；
// ModIdArgument 无状态=EmptyArgumentSerializer(Supplier)；EnumArgument 上游 35.x 才有
// 内建 Serializer，1.16.1~3 类面没有 → 本类自带反射读写 enumClass 字段的序列化器，
// 反射失败退化为哑枚举，保 join 不断、补全降级）。
//
// 整文件单块 /* 形态包裹说明：1201 等非 stonecutter 直通线里块标记与包裹均为纯注释，
// 若类体裸露会按 1.20.1 API 编译（ArgumentSerializer 接口已变，必炸），且 register()
// 会在 35+ 代与 ForgeMod 自身注册撞车。活跃块解包先例=ClientSetupEvent.registerKeyBindings。

public final class ForgeArgTypeSyncShim {

    private ForgeArgTypeSyncShim() {
    }

    // 反射兜底枚举：enumClass 字段读取失败时的反序列化返回值（仅影响补全，不影响 join）。
    private enum Fallback {
        INSTANCE
    }

    public static void register() {
        // Class<EnumArgument> 裸类面与 ArgumentSerializer<EnumArgument<?>> 的 T 推断冲突
        //（Java 8 无 var）→ 双跳转显式定形
        @SuppressWarnings("unchecked")
        Class<EnumArgument<?>> enumCls = (Class<EnumArgument<?>>) (Class<?>) EnumArgument.class;
        ArgumentTypes.register("forge:enum", enumCls, new EnumSerializer());
        ArgumentTypes.register("forge:modid", ModIdArgument.class, new EmptyArgumentSerializer<>(ModIdArgument::new));
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends Enum<?>> enumClassOf(ArgumentType<?> arg) {
        if (arg instanceof EnumArgument) {
            try {
                java.lang.reflect.Field field = EnumArgument.class.getDeclaredField("enumClass");
                field.setAccessible(true);
                return (Class<? extends Enum<?>>) (Class<?>) field.get(arg);
            } catch (ReflectiveOperationException ignored) {
                // fall through
            }
        }
        return Fallback.class;
    }

    private static class EnumSerializer implements ArgumentSerializer<EnumArgument<?>> {
        @Override
        public void serializeToNetwork(EnumArgument<?> arg, FriendlyByteBuf buf) {
            buf.writeUtf(enumClassOf(arg).getName());
        }

        @Override
        public EnumArgument<?> deserializeFromNetwork(FriendlyByteBuf buf) {
            String name = buf.readUtf(256);
            try {
                return EnumArgument.enumArgument((Class) Class.forName(name).asSubclass(Enum.class));
            } catch (ClassNotFoundException | ClassCastException ignored) {
                return EnumArgument.enumArgument(Fallback.class);
            }
        }

        @Override
        public void serializeToJson(EnumArgument<?> arg, JsonObject json) {
            json.addProperty("enum", enumClassOf(arg).getName());
        }
    }
}*/
//?}
