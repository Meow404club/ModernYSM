package com.elfmcys.yesstevemodel.platform.neoforge.bettercombat;

import com.elfmcys.yesstevemodel.network.NetworkHandler;
import com.elfmcys.yesstevemodel.network.message.C2SSwingArmPacket;
import net.bettercombat.api.AttackHand;
import net.bettercombat.api.client.BetterCombatClientEvents;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;

/**
 * 1.20.1-forge BetterCombatAttackHandler 直译（javap 两代比对零断裂——
 * PlayerAttackStart 接口签名/AttackHand.isOffHand 同形）。LocalPlayer
 * swinging/swingTime/swingingArm 字段 1.21.1 仍在（26.3 才删，YSMBinding:170 注）。
 */
public class BetterCombatAttackHandler implements BetterCombatClientEvents.PlayerAttackStart {
	public void onPlayerAttackStart(LocalPlayer localPlayer, AttackHand attackHand) {
		localPlayer.swingingArm = attackHand.isOffHand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
		localPlayer.swingTime = -1;
		localPlayer.swinging = true;
		NetworkHandler.sendToServer(new C2SSwingArmPacket(localPlayer.swingingArm));
	}
}
