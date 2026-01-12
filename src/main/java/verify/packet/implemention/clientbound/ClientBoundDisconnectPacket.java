package verify.packet.implemention.clientbound;

import verify.packet.IRCPacket;
import verify.packet.annotations.ProtocolField;

public class ClientBoundDisconnectPacket implements IRCPacket {
    @ProtocolField("r")
    private String reason;

    public ClientBoundDisconnectPacket() {
    }

    public ClientBoundDisconnectPacket(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }
}
