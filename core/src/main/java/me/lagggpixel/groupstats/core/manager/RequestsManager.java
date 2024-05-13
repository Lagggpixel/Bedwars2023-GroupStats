package me.lagggpixel.groupstats.core.manager;

import me.infinity.groupstats.api.GroupNode;
import me.lagggpixel.groupstats.core.GroupProfile;
import me.lagggpixel.groupstats.core.GroupStatsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.json.simple.JSONObject;
import spark.Spark;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Lagggpixel
 * @since May 13, 2024
 */
public class RequestsManager {

  private final GroupStatsPlugin plugin;
  private boolean apiActive;

  public RequestsManager(GroupStatsPlugin instance) {
    this.plugin = instance;
    this.apiActive = this.plugin.getConfig().getBoolean("api.enabled", false);
    if (apiActive) {
      enableApi();
    }
  }

  private void enableApi() {
    if (!apiActive) {
      return;
    }
    int port = this.plugin.getConfig().getInt("api.port");
    if (port == 0) {
      apiActive = false;
      return;
    }
    Spark.port(port);
    this.plugin.getLogger().info("Set GroupStats API to listen on port " + port);
    /**
     * @params uuid - UUID of the player
     * @params name - Name of the player
     */
    Spark.get("/stats", (req, res) -> {
      Set<String> params = req.queryParams();
      OfflinePlayer offlinePlayer;
      if (params.contains("uuid")) {
        try {
          UUID uuid = UUID.fromString(req.queryParams("uuid"));
          offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        } catch (IllegalArgumentException ex) {
          res.status(410);
          return null;
        }
      } else if (params.contains("name")) {
        String username = req.queryParams("name");
        //noinspection deprecation
        offlinePlayer = Bukkit.getOfflinePlayer(username);
      } else {
        res.status(411);
        return null;
      }

      JSONObject json = getPlayerStats(offlinePlayer);
      if (json == null) {
        res.status(400);
        return null;
      }
      res.status(201);
      return json.toString();
    });
  }

  public void onDisable() {
    if (!apiActive) {
      return;
    }
    Spark.stop();
  }

  @SuppressWarnings("unchecked")
  private JSONObject getPlayerStats(OfflinePlayer offlinePlayer) {
    GroupProfile profile = this.plugin.getGroupManager().fetchUnsafe(offlinePlayer.getUniqueId());
    if (profile == null) {
      return null;
    }

    JSONObject json = new JSONObject();

    json.put("name", offlinePlayer.getName());

    ConcurrentHashMap<String, GroupNode> stats = GroupStatsPlugin.GSON.fromJson(profile.getData(),
        GroupStatsPlugin.STATISTIC_MAP_TYPE);

    JSONObject statsObject = new JSONObject();

    //<editor-fold desc="Overall stats">
    JSONObject overallStats = new JSONObject();
    overallStats.put("games played", stats.isEmpty() ? 0
        : stats.values().stream().mapToInt(GroupNode::getGamesPlayed).sum());
    overallStats.put("beds broken", stats.isEmpty() ? 0
        : stats.values().stream().mapToInt(GroupNode::getBedsBroken).sum());
    overallStats.put("beds lost", stats.isEmpty() ? 0
        : stats.values().stream().mapToInt(GroupNode::getBedsLost).sum());
    overallStats.put("kills", stats.isEmpty() ? 0
        : stats.values().stream().mapToInt(GroupNode::getKills).sum());
    overallStats.put("deaths", stats.isEmpty() ? 0
        : stats.values().stream().mapToInt(GroupNode::getDeaths).sum());
    overallStats.put("final kills", stats.isEmpty() ? 0
        : stats.values().stream().mapToInt(GroupNode::getFinalKills).sum());
    overallStats.put("final deaths", stats.isEmpty() ? 0
        : stats.values().stream().mapToInt(GroupNode::getFinalDeaths).sum());
    overallStats.put("wins", stats.isEmpty() ? 0
        : stats.values().stream().mapToInt(GroupNode::getWins).sum());
    overallStats.put("losses", stats.isEmpty() ? 0
        : stats.values().stream().mapToInt(GroupNode::getLosses).sum());
    overallStats.put("winstreak", stats.isEmpty() ? 0
        : stats.values().stream().mapToInt(GroupNode::getWinstreak).sum());
    overallStats.put("highest winstreak", stats.isEmpty() ? 0
        : stats.values().stream().mapToInt(GroupNode::getHighestWinstreak).sum());
    overallStats.put("kdr", this.getRatio(
        stats.isEmpty() ? 0 : stats.values().stream().mapToInt(GroupNode::getKills).sum(),
        stats.isEmpty() ? 0 : stats.values().stream().mapToInt(GroupNode::getDeaths).sum()));
    overallStats.put("fkdr", this.getRatio(
        stats.isEmpty() ? 0 : stats.values().stream().mapToInt(GroupNode::getFinalKills).sum(),
        stats.isEmpty() ? 0 : stats.values().stream().mapToInt(GroupNode::getFinalDeaths).sum()));
    overallStats.put("bblr", this.getRatio(
        stats.isEmpty() ? 0 : stats.values().stream().mapToInt(GroupNode::getBedsBroken).sum(),
        stats.isEmpty() ? 0 : stats.values().stream().mapToInt(GroupNode::getBedsLost).sum()));
    overallStats.put("wlr", this.getRatio(
        stats.isEmpty() ? 0 : stats.values().stream().mapToInt(GroupNode::getWins).sum(),
        stats.isEmpty() ? 0 : stats.values().stream().mapToInt(GroupNode::getLosses).sum()));
    statsObject.put("over-all", overallStats);
    //</editor-fold>


    //<editor-fold desc="Per group stats">
    stats.forEach((name, node) -> {
      JSONObject groupJson = new JSONObject();

      groupJson.put("games played", node.getGamesPlayed());
      groupJson.put("beds broken", node.getBedsBroken());
      groupJson.put("beds lost", node.getBedsLost());
      groupJson.put("kills", node.getKills());
      groupJson.put("deaths", node.getDeaths());
      groupJson.put("final kills", node.getFinalKills());
      groupJson.put("final deaths", node.getFinalDeaths());
      groupJson.put("wins", node.getWins());
      groupJson.put("losses", node.getLosses());
      groupJson.put("winstreak", node.getWinstreak());
      groupJson.put("highest winstreak", node.getHighestWinstreak());
      groupJson.put("kdr", this.getRatio(node, "kdr"));
      groupJson.put("fkdr", this.getRatio(node, "fkdr"));
      groupJson.put("bblr", this.getRatio(node, "bblr"));
      groupJson.put("wlr", this.getRatio(node, "wlr"));

      statsObject.put(name, groupJson);
    });
    //</editor-fold>

    json.put("stats", statsObject);

    return json;
  }

  private double getRatio(int i1, int i2) {
    if (i2 == 0) {
      // Handle division by zero error here, e.g., return Double.NaN or throw an exception.
      return Double.NaN;
    }

    double value = (double) i1 / i2;
    return new BigDecimal(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
  }

  private double getRatio(GroupNode groupNode, String type) {
    double result;
    switch (type) {
      case "kdr":
        int deaths = groupNode.getDeaths();
        if (deaths == 0) {
          deaths = 1;
        }
        result = this.getRatio(groupNode.getKills(), deaths);
        break;
      case "fkdr":
        int finalDeaths = groupNode.getFinalDeaths();
        if (finalDeaths == 0) {
          finalDeaths = 1;
        }
        result = this.getRatio(groupNode.getFinalKills(), finalDeaths);
        break;
      case "bblr":
        int bedsLost = groupNode.getBedsLost();
        if (bedsLost == 0) {
          bedsLost = 1;
        }
        result = this.getRatio(groupNode.getBedsBroken(), bedsLost);
        break;
      case "wlr":
        int losses = groupNode.getLosses();
        if (losses == 0) {
          losses = 1;
        }
        result = this.getRatio(groupNode.getWins(), losses);
        break;
      default:
        result = Double.NaN;
    }
    return result;
  }
}
