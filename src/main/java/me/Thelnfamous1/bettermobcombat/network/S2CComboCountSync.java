package me.Thelnfamous1.bettermobcombat.network;

import me.Thelnfamous1.bettermobcombat.Constants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record S2CComboCountSync(int mobId, int comboCount) implements CustomPacketPayload {
    public static final Type<S2CComboCountSync> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "combo_count_sync"));

    public static final StreamCodec<FriendlyByteBuf, S2CComboCountSync> STREAM_CODEC = StreamCodec.of(
            (buffer, msg) -> msg.write(buffer),
            S2CComboCountSync::read
    );

    public static S2CComboCountSync read(FriendlyByteBuf buffer) {
        int mobId = buffer.readInt();
        int comboCount = buffer.readInt();
        return new S2CComboCountSync(mobId, comboCount);
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeInt(this.mobId);
        buffer.writeInt(this.comboCount);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
