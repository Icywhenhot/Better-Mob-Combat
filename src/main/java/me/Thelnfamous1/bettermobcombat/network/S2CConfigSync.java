package me.Thelnfamous1.bettermobcombat.network;

import me.Thelnfamous1.bettermobcombat.Constants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record S2CConfigSync(String json) implements CustomPacketPayload {
    public static final Type<S2CConfigSync> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "config_sync"));

    public static final StreamCodec<FriendlyByteBuf, S2CConfigSync> STREAM_CODEC = StreamCodec.of(
            (buffer, msg) -> msg.write(buffer),
            S2CConfigSync::read
    );

    public static S2CConfigSync read(FriendlyByteBuf buffer) {
        String json = buffer.readUtf();
        return new S2CConfigSync(json);
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeUtf(this.json);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
