package me.Thelnfamous1.bettermobcombat.network;

import me.Thelnfamous1.bettermobcombat.Constants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record S2CAttackSound(int mobId, double x, double y, double z, String soundId, float volume, float pitch, long seed) implements CustomPacketPayload {
    public static final Type<S2CAttackSound> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "attack_sound"));

    public static final StreamCodec<FriendlyByteBuf, S2CAttackSound> STREAM_CODEC = StreamCodec.of(
            (buffer, msg) -> msg.write(buffer),
            S2CAttackSound::read
    );

    public static S2CAttackSound read(FriendlyByteBuf buffer) {
        int mobId = buffer.readInt();
        double x = buffer.readDouble();
        double y = buffer.readDouble();
        double z = buffer.readDouble();
        String soundId = buffer.readUtf();
        float volume = buffer.readFloat();
        float pitch = buffer.readFloat();
        long seed = buffer.readLong();
        return new S2CAttackSound(mobId, x, y, z, soundId, volume, pitch, seed);
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeInt(this.mobId);
        buffer.writeDouble(this.x);
        buffer.writeDouble(this.y);
        buffer.writeDouble(this.z);
        buffer.writeUtf(this.soundId);
        buffer.writeFloat(this.volume);
        buffer.writeFloat(this.pitch);
        buffer.writeLong(this.seed);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
