package verify.packet.implemention.serverbound;

import verify.packet.IRCPacket;
import verify.packet.annotations.ProtocolField;


public class ServerBoundHandshakePacket implements IRCPacket {
    @ProtocolField("u")
    private String username;

    @ProtocolField("t")
    private String token;

    public ServerBoundHandshakePacket() {
    }

    public ServerBoundHandshakePacket(String username, String token) {
        this.username = username;
        this.token = token;
    }

    public String getUsername() {
        return username;
    }

    public String getToken() {
        return token;
    }
}
