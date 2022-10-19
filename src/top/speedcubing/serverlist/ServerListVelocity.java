package top.speedcubing.serverlist;

import com.google.common.collect.ImmutableList;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.api.util.ModInfo;
import net.kyori.adventure.text.Component;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

@Plugin(id = "serverlist", name = "ServerList", version = "1.0-SNAPSHOT", authors = {"speedcubing"})
public class ServerListVelocity {
    List<ServerPing.SamplePlayer> playerlist = new ArrayList<>();
    ModInfo forge;
    ServerPing.Version version;
    Component motd;

    @Inject
    private ServerListVelocity(ProxyServer server, CommandManager commandManager, Logger logger, @DataDirectory final Path folder) {
        commandManager.register(commandManager.metaBuilder("serverlist").build(), (SimpleCommand) invocation -> {
            reload();
            invocation.source().sendMessage(Component.text("ServerList reloaded."));
        });
        reload();
    }

    public void reload() {
        try {
            Reader reader = new FileReader(new File(new File("plugins", "ServerList"), "config.json"));
            JsonObject object = new JsonParser().parse(reader).getAsJsonObject();
            ServerListConfig.Name = object.getAsJsonObject("name").get("enable").getAsBoolean();
            ServerListConfig.Forge = object.getAsJsonObject("forgeicon").get("enable").getAsBoolean();
            ServerListConfig.Protocol = object.getAsJsonObject("protocol").get("enable").getAsBoolean();
            ServerListConfig.Motd = object.getAsJsonObject("motd").get("enable").getAsBoolean();
            ServerListConfig.Players = object.getAsJsonObject("players").get("enable").getAsBoolean();
            StringBuilder m = new StringBuilder();
            for (JsonElement s : object.getAsJsonObject("motd").getAsJsonArray("value")) {
                m.append(s.getAsString()).append("\n");
            }
            if (m.length() != 0)
                m = new StringBuilder(m.substring(0, m.length() - 1));
            motd = Component.text(m.toString());
            List<String> players = new Gson().fromJson(object.getAsJsonObject("players").get("value").getAsJsonArray().toString(), new TypeToken<List<String>>() {
            }.getType());
            playerlist.clear();
            for (String s : players) {
                playerlist.add(new ServerPing.SamplePlayer(s, UUID.randomUUID()));
            }
            forge = new ModInfo(object.getAsJsonObject("forgeicon").get("value").getAsString(), ImmutableList.of());
            version = new ServerPing.Version(object.getAsJsonObject("protocol").get("value").getAsInt(), object.getAsJsonObject("name").get("value").getAsString());
            reader.close();
        } catch (FileNotFoundException e) {
            try {
                File f = new File("plugins", "ServerList");
                f.mkdir();
                File cfg = new File(f, "config.json");
                cfg.createNewFile();
                JsonObject object = new JsonObject();
                JsonObject name = new JsonObject();
                name.addProperty("enable", true);
                name.addProperty("value", "§aname");
                JsonObject forgeicon = new JsonObject();
                forgeicon.addProperty("enable", true);
                forgeicon.addProperty("value", "VANILLA");
                JsonObject protocol = new JsonObject();
                protocol.addProperty("enable", true);
                protocol.addProperty("value", 2);
                JsonObject motd = new JsonObject();
                JsonArray MOTD = new JsonArray();
                MOTD.add("§amotd1");
                MOTD.add("§bmotd2");
                motd.addProperty("enable", true);
                motd.add("value", MOTD);
                JsonObject players = new JsonObject();
                JsonArray PLAYERS = new JsonArray();
                PLAYERS.add("§cplayer1");
                PLAYERS.add("§dplayer2");
                players.addProperty("enable", true);
                players.add("value", PLAYERS);
                object.add("name", name);
                object.add("forgeicon", forgeicon);
                object.add("protocol", protocol);
                object.add("motd", motd);
                object.add("players", players);
                FileWriter writer = new FileWriter(cfg);
                new GsonBuilder().setPrettyPrinting().create().toJson(object, writer);
                writer.close();
                reload();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }


    @Subscribe
    public void onProxyPing(ProxyPingEvent e) {
        ServerPing ping = e.getPing();
        e.setPing(new ServerPing(
                ServerListConfig.Name ? version : ping.getVersion(),
                ServerListConfig.Players ? new ServerPing.Players(ping.getPlayers().get().getOnline(), ping.getPlayers().get().getMax(), playerlist) : ping.getPlayers().get(),
                ServerListConfig.Motd ? motd : ping.getDescriptionComponent(), null,
                ServerListConfig.Forge ? forge : ping.getModinfo().get()
        ));
    }
}
