package verify.packet.implemention.serverbound;

import verify.packet.IRCPacket;
import verify.packet.annotations.ProtocolField;

public class ServerBoundMessagePacket implements IRCPacket {
    @ProtocolField("m")
    private String message;

    public ServerBoundMessagePacket() {
    }

    public ServerBoundMessagePacket(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
