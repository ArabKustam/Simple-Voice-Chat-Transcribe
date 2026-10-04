package org.lavacast.svctranscribe.paper;

import de.maxhenkel.voicechat.api.BukkitVoicechatService;
import org.bukkit.Bukkit;
import org.lavacast.pvtranscribe.core.session.TranscriptionService;
import org.lavacast.pvtranscribe.core.util.PlatformLogger;
import org.lavacast.pvtranscribe.voice.simplevoice.SimpleVoiceAdapter;

/**
 * Kept in its own class so Simple Voice Chat classes are only loaded after we checked that it is installed.
 */
final class SimpleVoiceHook {

    private SimpleVoiceHook() {
    }

    static void register(SVCTranscribePlugin plugin, TranscriptionService service, PlatformLogger logger) throws Exception {
        BukkitVoicechatService voicechat = plugin.getServer().getServicesManager().load(BukkitVoicechatService.class);
        if (voicechat == null) {
            throw new IllegalStateException("Simple Voice Chat did not register its BukkitVoicechatService");
        }
        SimpleVoiceAdapter adapter = new SimpleVoiceAdapter(logger, id -> {
            org.bukkit.entity.Player player = Bukkit.getPlayer(id);
            return player != null ? player.getName() : null;
        });
        service.registerVoiceSource(adapter);
        voicechat.registerPlugin(adapter);
    }
}
