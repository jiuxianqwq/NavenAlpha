package verify.packet.implemention.serverbound;

import verify.packet.IRCPacket;
import verify.packet.annotations.ProtocolField;

public class ServerBoundUpdateIgnPacket implements IRCPacket {
    @ProtocolField("n")
    private String name;

    public ServerBoundUpdateIgnPacket() {
    }

    public ServerBoundUpdateIgnPacket(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
