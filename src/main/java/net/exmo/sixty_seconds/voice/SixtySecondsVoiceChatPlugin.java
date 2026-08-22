package net.exmo.sixty_seconds.voice;

import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import net.exmo.sixty_seconds.SixtySeconds;
import net.exmo.sixty_seconds.bridge.SixtySecGameWorldComponent;
import net.exmo.sixty_seconds.content.item.RadioItem;
import net.minecraft.server.level.ServerPlayer;

/**
 * 简易语音插件（对讲机频道）：收到任意玩家话筒语音时，
 * 若该玩家已接入对讲机频道（{@link RadioItem#CHANNELS}），把语音转发给同频道的其他持机玩家。
 * 其余场景不做任何拦截，交还默认语音逻辑。
 */
public class SixtySecondsVoiceChatPlugin implements VoicechatPlugin {

    @Override
    public String getPluginId() {
        return SixtySeconds.MOD_ID;
    }

    @Override
    public void initialize(VoicechatApi api) {
        VoicechatPlugin.super.initialize(api);
    }

    public void paranoidEvent(MicrophonePacketEvent event) {
        var connection = event.getSenderConnection();
        if (connection == null || !connection.isInstalled() || !connection.isConnected()) {
            return;
        }
        var vcplayer = connection.getPlayer();
        if (vcplayer == null || !(vcplayer.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        SixtySecGameWorldComponent gameWorldComponent = SixtySecGameWorldComponent.KEY.get(player.level());
        if (gameWorldComponent == null) {
            return;
        }
        RadioItem.vcparanoidEvent(gameWorldComponent, player, event);
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(MicrophonePacketEvent.class, this::paranoidEvent);
        VoicechatPlugin.super.registerEvents(registration);
    }
}
