package com.elfmcys.yesstevemodel.client.compat.realcamera.platform.forge;

import com.xtracr.realcamera.RealCameraCore;
import net.minecraft.client.Minecraft;

public class RealCameraChecker {
    public static boolean isRealCameraActive() {
        return RealCameraCore.isActive();
    }

    /**
     * fix-fpm-rc R3：RealCamera 绑定 GUI（Model View Screen）打开判定。类名字符串比对——
     * 编译期 libs/ 是 0.6.11a（GUI 面与运行时 0.7.8 有漂移），零符号依赖最稳；
     * GUI 打开期间世界模型渲染强制 CPU 缓冲管线（NativeModelRenderer 消费），
     * 修 GPU 直绘路径下 GUI 读不到 UV（诊断账 R5），关闭界面后原路径恢复（性能零损失面）。
     */
    public static boolean isRealCameraBindGuiOpen() {
        Minecraft client = Minecraft.getInstance();
        return client != null && client.screen != null
                && "com.xtracr.realcamera.gui.ModelViewScreen".equals(client.screen.getClass().getName());
    }
}