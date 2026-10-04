package org.lavacast.pvtranscribe.voice.simplevoice;

import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.EntitySoundPacketEvent;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.LocationalSoundPacketEvent;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.PlayerDisconnectedEvent;
import de.maxhenkel.voicechat.api.events.SoundPacketEvent;
import de.maxhenkel.voicechat.api.events.StaticSoundPacketEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import de.maxhenkel.voicechat.api.packets.MicrophonePacket;
import org.jetbrains.annotations.NotNull;
import org.lavacast.pvtranscribe.core.util.PlatformLogger;
import org.lavacast.pvtranscribe.core.util.RateLimitedLogger;
import org.lavacast.pvtranscribe.core.voice.Audience;
import org.lavacast.pvtranscribe.core.voice.FrameDecoder;
import org.lavacast.pvtranscribe.core.voice.SpeakerInfo;
import org.lavacast.pvtranscribe.core.voice.VoiceFrame;
import org.lavacast.pvtranscribe.core.voice.VoiceInput;
import org.lavacast.pvtranscribe.core.voice.VoiceSourceAdapter;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/**
 * Simple Voice Chat plugin that feeds players' voice into the transcription core.
 * <p>
 * <b>Audio:</b> {@link MicrophonePacketEvent} carries the Opus audio of a speaking player (already decrypted
 * by the voice chat server). The client sends an empty packet when the player stops talking.
 * <p>
 * <b>Who hears the speaker:</b> Simple Voice Chat fires a sound packet event for every receiver of the
 * speaker's audio (proximity, whisper, groups, spectators). Collecting these per speaker gives exactly the
 * players who hear them, so subtitles follow the voice chat's own rules.
 */
public final class SimpleVoiceAdapter implements VoiceSourceAdapter, VoicechatPlugin {

    public static final String ID = "simplevoicechat";
    private static final int SAMPLE_RATE = 48_000;
    private static final long AUDIENCE_WINDOW_MS = 250;

    private final PlatformLogger logger;
    private final RateLimitedLogger errors;
    private final Function<UUID, String> names;
    private final ConcurrentHashMap<UUID, AtomicLong> sequences = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, AudienceWindow> windows = new ConcurrentHashMap<>();
    private volatile VoicechatServerApi api;
    private volatile VoiceInput input;

    /**
     * @param names resolves a player's name (platform specific)
     */
    public SimpleVoiceAdapter(@NotNull PlatformLogger logger, @NotNull Function<UUID, String> names) {
        this.logger = logger;
        this.errors = new RateLimitedLogger(logger, 60, TimeUnit.SECONDS);
        this.names = names;
    }

    // ------------------------------------------------------------------ VoiceSourceAdapter

    @Override
    public @NotNull String id() {
        return ID;
    }

    @Override
    public @NotNull String displayName() {
        return "Simple Voice Chat";
    }

    @Override
    public void start(@NotNull VoiceInput input) {
        this.input = input;
    }

    @Override
    public void stop() {
        // Simple Voice Chat has no way to unregister a plugin; just stop forwarding
        input = null;
        windows.clear();
    }

    @Override
    public @NotNull FrameDecoder createDecoder(boolean stereo) {
        VoicechatServerApi server = api;
        if (server == null) throw new IllegalStateException("Simple Voice Chat is not started yet");
        OpusDecoder decoder = server.createDecoder();
        return new FrameDecoder() {
            @Override
            public short @NotNull [] decode(@NotNull VoiceFrame frame) {
                return decoder.decode(frame.data());
            }

            @Override
            public void reset() {
                decoder.resetState();
            }

            @Override
            public void close() {
                decoder.close();
            }
        };
    }

    @Override
    public boolean hasVoiceClient(@NotNull UUID playerId) {
        VoicechatServerApi server = api;
        if (server == null) return false;
        VoicechatConnection connection = server.getConnectionOf(playerId);
        return connection != null && connection.isInstalled() && connection.isConnected();
    }

    // ------------------------------------------------------------------ VoicechatPlugin

    @Override
    public String getPluginId() {
        return "svc-transcribe";
    }

    @Override
    public void initialize(VoicechatApi voicechatApi) {
        if (voicechatApi instanceof VoicechatServerApi server) api = server;
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(VoicechatServerStartedEvent.class, event -> {
            api = event.getVoicechat();
            logger.info("Connected to Simple Voice Chat.");
        });
        // run after other plugins (high priority value = called later), so cancelled packets are skipped
        registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophone, 100);
        registration.registerEvent(EntitySoundPacketEvent.class, this::onSound, 100);
        registration.registerEvent(LocationalSoundPacketEvent.class, this::onSound, 100);
        registration.registerEvent(StaticSoundPacketEvent.class, this::onSound, 100);
        registration.registerEvent(PlayerDisconnectedEvent.class, event -> {
            sequences.remove(event.getPlayerUuid());
            windows.remove(event.getPlayerUuid());
        });
    }

    private void onMicrophone(MicrophonePacketEvent event) {
        VoiceInput sink = input;
        if (sink == null || event.isCancelled()) return;
        try {
            VoicechatConnection sender = event.getSenderConnection();
            if (sender == null) return;
            UUID playerId = sender.getPlayer().getUuid();
            MicrophonePacket packet = event.getPacket();
            byte[] data = packet.getOpusEncodedData();
            if (data == null || data.length == 0) {
                // the client sends an empty packet when the player stops talking
                sink.onSpeakingStopped(this, playerId);
                return;
            }
            String channel = sender.isInGroup() ? SoundPacketEvent.SOURCE_GROUP
                    : packet.isWhispering() ? "whisper" : SoundPacketEvent.SOURCE_PROXIMITY;
            long sequence = sequences.computeIfAbsent(playerId, id -> new AtomicLong()).incrementAndGet();
            AudienceWindow window = windows.computeIfAbsent(playerId, id -> new AudienceWindow());
            long now = System.currentTimeMillis();
            if (window.silentSince(now) && api != null) {
                // nobody received this speaker's audio recently: report an empty audience, so subtitles are not
                // shown to players who can't hear the speaker
                sink.onAudience(this, playerId, new Audience(channel, Audience.Type.PROXIMITY, Set.of(),
                        api.getVoiceChatDistance(), now));
            }
            String name = names.apply(playerId);
            sink.onAudioFrame(this, new SpeakerInfo(playerId, name != null ? name : playerId.toString()),
                    new VoiceFrame(sequence, data, false, SAMPLE_RATE, channel));
        } catch (Throwable t) {
            errors.warn("microphone", "Failed to handle Simple Voice Chat audio", t);
        }
    }

    private void onSound(SoundPacketEvent<?> event) {
        VoiceInput sink = input;
        if (sink == null || event.isCancelled()) return;
        try {
            VoicechatConnection sender = event.getSenderConnection();
            VoicechatConnection receiver = event.getReceiverConnection();
            if (sender == null || receiver == null) return; // audio from plugins, not from a player
            UUID speaker = sender.getPlayer().getUuid();
            UUID listener = receiver.getPlayer().getUuid();
            if (speaker.equals(listener)) return;

            double radius = -1;
            if (event instanceof EntitySoundPacketEvent entity) {
                radius = entity.getPacket().getDistance();
            } else if (event instanceof LocationalSoundPacketEvent located) {
                radius = located.getPacket().getDistance();
            }

            Audience ready = windows.computeIfAbsent(speaker, id -> new AudienceWindow())
                    .add(event.getSource(), listener, radius, System.currentTimeMillis());
            if (ready != null) sink.onAudience(this, speaker, ready);
        } catch (Throwable t) {
            errors.warn("sound", "Failed to track Simple Voice Chat listeners", t);
        }
    }

    /**
     * Collects the receivers of one speaker for a short time window, then reports them as one audience.
     */
    private static final class AudienceWindow {
        private long start;
        private String source = SoundPacketEvent.SOURCE_PROXIMITY;
        private double radius = -1;
        private Set<UUID> listeners = new HashSet<>();

        private long lastEvent;

        /** True if no receiver was reported for a while (and not more often than once per window). */
        synchronized boolean silentSince(long now) {
            if (now - lastEvent < 600) return false;
            lastEvent = now - 600 + AUDIENCE_WINDOW_MS; // throttle the empty reports
            return true;
        }

        synchronized Audience add(String eventSource, UUID listener, double distance, long now) {
            lastEvent = now;
            Audience result = null;
            if (start != 0 && now - start >= AUDIENCE_WINDOW_MS) {
                result = build(now);
            }
            if (start == 0 || result != null) {
                start = now;
                listeners = new HashSet<>();
                radius = -1;
            }
            source = eventSource == null ? "unknown" : eventSource;
            if (distance > radius) radius = distance;
            listeners.add(listener);
            return result;
        }

        private Audience build(long now) {
            boolean proximity = SoundPacketEvent.SOURCE_PROXIMITY.equals(source);
            return new Audience(source, proximity ? Audience.Type.PROXIMITY : Audience.Type.DIRECT,
                    Set.copyOf(listeners), proximity ? radius : -1, now);
        }
    }
}
