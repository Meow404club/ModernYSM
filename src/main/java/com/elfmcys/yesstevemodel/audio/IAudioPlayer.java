package com.elfmcys.yesstevemodel.audio;

public interface IAudioPlayer {
    void release();

    // 不叫 isStopped：撞 vanilla AbstractSoundInstance/AbstractTickableSoundInstance.isStopped
    // 名时，legacy reobf（1.20.1 MDG legacyforge）按名把调用点映射成 m_7801_，而我们自己的
    // 接口声明不参与映射 → 进世界音效系统一运行即 NoSuchMethodError（javap 实证）。
    // hasStopped 在 1.16.5/1.20.1 vanilla + forge API 全无同名方法（tools/audit_reobf_collision.py
    // 配套审计），映射表不含此名，永不被重映射。
    boolean hasStopped();
}