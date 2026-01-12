package verify.client;

import cn.paradisemc.ZKMIndy;
import lombok.Getter;
import org.smartboot.socket.MessageProcessor;
import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.transport.AioQuickClient;
import org.smartboot.socket.transport.AioSession;
import verify.packet.IRCPacket;
import verify.packet.implemention.clientbound.*;
import verify.packet.implemention.serverbound.*;
import verify.processor.IRCProtocol;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@ZKMIndy
@Getter
public class IRCTransport {
    private final IRCProtocol protocol = new IRCProtocol();
    private final AioSession session;
    private final Map<String, String> userToIgnMap = new ConcurrentHashMap<>();
    private final Map<String, String> ignToUserMap = new ConcurrentHashMap<>();
    private final AtomicBoolean disconnectedNotified = new AtomicBoolean(false);
    private IRCHandler handler;
    private ScheduledExecutorService scheduler;

    public IRCTransport(String host, int port, IRCHandler handler) throws IOException {
        this.handler = handler;
        MessageProcessor<IRCPacket> processor = new MessageProcessor<IRCPacket>() {
            @Override
            public void process(AioSession session, IRCPacket msg) {
                if (msg instanceof ClientBoundDisconnectPacket) {
                    notifyDisconnected(((ClientBoundDisconnectPacket) msg).getReason());
                }
                if (msg instanceof ClientBoundConnectedPacket) {
                    disconnectedNotified.set(false);
                    IRCTransport.this.handler.onConnected();
                    startScheduler();
                }
                if (msg instanceof ClientBoundUpdateUserListPacket) {
                    userToIgnMap.clear();
                    userToIgnMap.putAll(((ClientBoundUpdateUserListPacket) msg).getUserMap());
                    ignToUserMap.clear();
                    userToIgnMap.forEach((user, ign) -> ignToUserMap.put(ign, user));
                }
                if (msg instanceof ClientBoundMessagePacket) {
                    IRCTransport.this.handler.onMessage(((ClientBoundMessagePacket) msg).getSender(), ((ClientBoundMessagePacket) msg).getMessage());
                }
                if (msg instanceof ClientBoundLoginResultPacket p) {
                    IRCTransport.this.handler.onLoginResult(p.isSuccess(), p.getExpireAt(), p.getTimeWindow(), p.getMessage());
                }
                if (msg instanceof ClientBoundRegisterResultPacket p) {
                    IRCTransport.this.handler.onRegisterResult(p.isSuccess(), p.getExpireAt(), p.getTimeWindow(), p.getMessage());
                }
                if (msg instanceof ClientBoundRechargeResultPacket p) {
                    IRCTransport.this.handler.onRechargeResult(p.isSuccess(), p.getExpireAt(), p.getTimeWindow(), p.getMessage());
                }
                if (msg instanceof ClientBoundCloudConfigPacket p) {
                    String action = p.getAction();
                    if (action == null) {
                        return;
                    }
                    if (action.equalsIgnoreCase("upload")) {
                        IRCTransport.this.handler.onCloudConfigUploadResult(p.isSuccess(), p.getMessage(), p.getMax());
                        return;
                    }
                    if (action.equalsIgnoreCase("get")) {
                        IRCTransport.this.handler.onCloudConfigGetResult(p.isSuccess(), p.getOwner(), p.getName(), p.getContent(), p.getMessage());
                        return;
                    }
                    if (action.equalsIgnoreCase("delete")) {
                        IRCTransport.this.handler.onCloudConfigDeleteResult(p.isSuccess(), p.getOwner(), p.getName(), p.getMessage());
                        return;
                    }
                    if (action.equalsIgnoreCase("list")) {
                        IRCTransport.this.handler.onCloudConfigListResult(p.isSuccess(), p.getNames(), p.getMax(), p.getMessage());
                    }
                }
            }

            @Override
            public void stateEvent(AioSession session, StateMachineEnum stateMachineEnum, Throwable throwable) {
                if (stateMachineEnum == StateMachineEnum.SESSION_CLOSED) {
                    notifyDisconnected("连接已断开");
                }
            }
        };
        AioQuickClient client = new AioQuickClient(host, port, protocol, processor);
        client.setReadBufferSize(8 * 1024 * 1024);
        session = client.start();
    }

    private void notifyDisconnected(String message) {
        if (!disconnectedNotified.compareAndSet(false, true)) {
            return;
        }
        stopScheduler();
        handler.onDisconnected(message);
    }

    public void sendPacket(IRCPacket packet) {
        try {
            byte[] data = protocol.encode(packet);
            session.writeBuffer().writeInt(data.length);
            session.writeBuffer().write(data);
            session.writeBuffer().flush();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isUser(String name) {
        return ignToUserMap.containsKey(name);
    }

    public String getName(String ign) {
        return ignToUserMap.get(ign);
    }

    public String getIgn(String name) {
        return userToIgnMap.get(name);
    }

    public void sendChat(String message) {
        sendPacket(new ServerBoundMessagePacket(message));
    }

    public void sendInGameUsername(String username) {
        sendPacket(new ServerBoundUpdateIgnPacket(username));
    }

    public void sendInGameUsername() {
        sendInGameUsername(handler.getInGameUsername());
    }

    public void connect(String username, String token) {
        sendPacket(new ServerBoundHandshakePacket(username, token));
    }

    public void login(String username, String password, String hwid, java.util.Set<String> qqSet, String phone) {
        sendPacket(new ServerBoundLoginPacket(username, password, hwid, qqSet, phone));
    }

    public void register(String username, String password, String hwid, java.util.Set<String> qqSet, String phone, String cardKey) {
        sendPacket(new ServerBoundRegisterPacket(username, password, hwid, qqSet, phone, cardKey));
    }

    public void recharge(String username, String cardKey) {
        sendPacket(new ServerBoundRechargePacket(username, cardKey));
    }

    public void uploadCloudConfig(String name, String content) {
        sendPacket(new ServerBoundCloudConfigPacket("upload", "", name, content));
    }

    public void getCloudConfig(String name) {
        sendPacket(new ServerBoundCloudConfigPacket("get", "", name, ""));
    }

    public void getCloudConfig(String owner, String name) {
        sendPacket(new ServerBoundCloudConfigPacket("get", owner, name, ""));
    }

    public void listCloudConfigs() {
        sendPacket(new ServerBoundCloudConfigPacket("list", "", "", ""));
    }

    public void deleteCloudConfig(String name) {
        deleteCloudConfig("", name);
    }

    public void deleteCloudConfig(String owner, String name) {
        sendPacket(new ServerBoundCloudConfigPacket("delete", owner, name, ""));
    }

    public void setHandler(IRCHandler handler) {
        this.handler = handler;
    }

    private void startScheduler() {
        if (scheduler != null) {
            return;
        }
        scheduler = Executors.newScheduledThreadPool(1);
        Runnable task = this::sendInGameUsername;
        scheduler.scheduleAtFixedRate(task, 5, 5, TimeUnit.SECONDS);
    }

    private void stopScheduler() {
        ScheduledExecutorService s = scheduler;
        scheduler = null;
        if (s != null) {
            s.shutdownNow();
        }
    }
}
