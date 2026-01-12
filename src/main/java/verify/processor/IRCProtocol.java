package verify.processor;

import cn.paradisemc.ZKMIndy;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import org.smartboot.socket.Protocol;
import org.smartboot.socket.transport.AioSession;
import verify.management.PacketManager;
import verify.packet.IRCPacket;
import verify.util.CryptoUtil;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@ZKMIndy
public class IRCProtocol implements Protocol<IRCPacket> {
    private static final byte[] ENC_MAGIC = new byte[]{'E', 'N', 'C', 1};
    private static final int MAX_FRAME_LENGTH = 8 * 1024 * 1024;
    @Getter
    private final PacketManager packetManager = new PacketManager();
    private final Gson gson = new Gson();

    public IRCProtocol() {
    }

    private static boolean isEncryptedPayload(byte[] b) {
        if (b == null || b.length <= ENC_MAGIC.length) {
            return false;
        }
        for (int i = 0; i < ENC_MAGIC.length; i++) {
            if (b[i] != ENC_MAGIC[i]) {
                return false;
            }
        }
        return true;
    }

    private static byte[] deriveAesKey(String keyText) {
        if (keyText == null) {
            return null;
        }
        String s = keyText.trim();
        if (s.isEmpty()) {
            return null;
        }
        byte[] hash = CryptoUtil.sha256(s.getBytes(StandardCharsets.UTF_8));
        byte[] key = new byte[16];
        System.arraycopy(hash, 0, key, 0, key.length);
        return key;
    }

    public byte[] encode(IRCPacket packet) {
        byte[] plain = gson.toJson(packetManager.writePacket(packet)).getBytes(StandardCharsets.UTF_8);
        byte[] enc = CryptoUtil.aesGcmEncrypt(deriveAesKey("8964破解全家死光亲妈猪逼被操烂亲爹没鸡巴生小孩没屁眼操你血妈"), plain, null);
        byte[] b64 = Base64.getUrlEncoder().withoutPadding().encode(enc);
        ByteBuffer out = ByteBuffer.allocate(ENC_MAGIC.length + b64.length);
        out.put(ENC_MAGIC);
        out.put(b64);
        return out.array();
    }

    @Override
    public IRCPacket decode(ByteBuffer readBuffer, AioSession session) {
        int remaining = readBuffer.remaining();
        if (remaining < Integer.BYTES) {
            return null;
        }
        readBuffer.mark();
        int length = readBuffer.getInt();
        if (length < 0 || length > MAX_FRAME_LENGTH) {
            session.close();
            readBuffer.reset();
            readBuffer.position(readBuffer.limit());
            return null;
        }
        if (length > readBuffer.remaining()) {
            readBuffer.reset();
            return null;
        }
        byte[] b = new byte[length];
        readBuffer.get(b);
        readBuffer.mark();

        byte[] payload = b;
        if (isEncryptedPayload(b)) {
            byte[] enc = new byte[b.length - ENC_MAGIC.length];
            System.arraycopy(b, ENC_MAGIC.length, enc, 0, enc.length);
            byte[] decoded;
            try {
                decoded = Base64.getUrlDecoder().decode(enc);
            } catch (IllegalArgumentException ignored) {
                decoded = enc;
            }
            payload = CryptoUtil.aesGcmDecrypt(deriveAesKey("8964破解全家死光亲妈猪逼被操烂亲爹没鸡巴生小孩没屁眼操你血妈"), decoded, null);
        }

        String text = new String(payload, StandardCharsets.UTF_8);

        try {
            JsonObject object = JsonParser.parseString(text).getAsJsonObject();
            return packetManager.readPacket(object);
        } catch (Exception e) {
            session.close();
            return null;
        }
    }
}
