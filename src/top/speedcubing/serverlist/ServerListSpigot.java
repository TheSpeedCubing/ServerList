package top.speedcubing.serverlist;

import com.mojang.authlib.GameProfile;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import net.md_5.bungee.api.ChatColor;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class ServerListSpigot extends JavaPlugin implements Listener {
    List<?> networkManagers;
    ServerPing.ServerPingPlayerSample playerlist;
    String name;
    String forge;
    int protocol;
    ChatComponentText motd;

    public void reload() {
        reloadConfig();
        FileConfiguration config = getConfig();
        ServerListConfig.Name = config.getBoolean("name.enable");
        ServerListConfig.Forge = config.getBoolean("forgeicon.enable");
        ServerListConfig.Protocol = config.getBoolean("protocol.enable");
        ServerListConfig.Motd = config.getBoolean("motd.enable");
        ServerListConfig.Players = config.getBoolean("players.enable");
        name = ChatColor.translateAlternateColorCodes('&', config.getString("name.value"));
        forge = config.getString("forgeicon.value");
        protocol = config.getInt("protocol.value");
        StringBuilder m = new StringBuilder();
        for (String s : config.getStringList("motd.value")) {
            m.append(ChatColor.translateAlternateColorCodes('&', s)).append("\n");
        }
        if (m.length() != 0)
            m = new StringBuilder(m.substring(0, m.length() - 1));
        motd = new ChatComponentText(m.toString());
        List<String> players = config.getStringList("players.value");
        int size = players.size();
        playerlist = new ServerPing.ServerPingPlayerSample(1, 1);
        playerlist.a(new GameProfile[size]);
        for (int i = 0; i < size; i++) {
            playerlist.c()[i] = new GameProfile(UUID.randomUUID(), ChatColor.translateAlternateColorCodes('&', players.get(i)));
        }
    }

    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getPluginCommand("serverlist").setExecutor(((commandSender, command, s, strings) -> {
            reload();
            commandSender.sendMessage("ServerList reloaded.");
            return true;
        }));
        try {
            ServerConnection conn = MinecraftServer.getServer().aq();
            for (Method method : conn.getClass().getDeclaredMethods()) {
                method.setAccessible(true);
                if (method.getReturnType() == List.class) {
                    networkManagers = Collections.synchronizedList((List<?>) method.invoke(null, conn));
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        saveDefaultConfig();

        reload();
    }

    @EventHandler
    public void onServerListPing(ServerListPingEvent e) throws Exception {
        Field field = NetworkManager.class.getDeclaredField("channel");
        field.setAccessible(true);
        for (Object manager : networkManagers.toArray()) {
            Channel channel = (Channel) field.get(manager);
            if (channel.pipeline().get("speedcubingServerList") == null) {
                channel.pipeline().addBefore("packet_handler", "speedcubingServerList", new ChannelDuplexHandler() {
                    @Override
                    public void write(ChannelHandlerContext channel, Object byteBuf, ChannelPromise promise) throws Exception {
                        if (byteBuf instanceof PacketStatusOutServerInfo) {
                            PacketStatusOutServerInfo packet = (PacketStatusOutServerInfo) byteBuf;
                            Field serverPing = packet.getClass().getDeclaredField("b");
                            serverPing.setAccessible(true);
                            ServerPing ping = (ServerPing) serverPing.get(packet);
                            ping.setServerInfo(new ServerPing.ServerData(ServerListConfig.Name ? name : ping.c().a(), ServerListConfig.Protocol ? protocol : ping.c().b()));
                            if (ServerListConfig.Forge)
                                ping.setFavicon(forge);
                            if (ServerListConfig.Motd)
                                ping.setMOTD(motd);
                            if (ServerListConfig.Players)
                                ping.setPlayerSample(playerlist);
                        }
                        super.write(channel, byteBuf, promise);
                    }
                });
            }
        }
    }
}
