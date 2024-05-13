package me.lagggpixel.groupstats.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.tomkeuper.bedwars.api.BedWars;
import lombok.Getter;
import me.infinity.groupstats.api.GroupNode;
import me.lagggpixel.groupstats.core.manager.DatabaseManager;
import me.lagggpixel.groupstats.core.manager.GroupManager;
import me.lagggpixel.groupstats.core.manager.RequestsManager;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.eclipse.jetty.server.Authentication;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.Spark;

import java.lang.reflect.Type;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public final class GroupStatsPlugin extends JavaPlugin implements CommandExecutor {

  private boolean startupCompleted = false;
  private boolean bw2023 = false;

  public static final Gson GSON = new GsonBuilder()
      .excludeFieldsWithoutExposeAnnotation()
      .disableHtmlEscaping()
      .create();

  public static final Type STATISTIC_MAP_TYPE = new TypeToken<ConcurrentHashMap<String, GroupNode>>() {
  }.getType();

  private RequestsManager requestsManager;
  private DatabaseManager databaseManager;
  private GroupManager groupManager;

  private Metrics metrics;

  @Override
  public void onEnable() {
    this.saveDefaultConfig();

    final PluginManager pluginManager = this.getServer().getPluginManager();
    if (pluginManager.getPlugin("BedWars2023") == null) {
      if (pluginManager.getPlugin("BedWarsProxy") == null) {
        this.getLogger().severe("BedWars2023 or BedWarsProxy not found, disabling...");
        this.setEnabled(false);
        return;
      } else {
        this.getLogger().info("BedWarsProxy found, using it as a datastore.");
      }
    } else {
      this.getLogger().info("BedWars2023 found, activating standalone mode...");
      this.bw2023 = true;
    }

    this.getLogger().info("Loading the plugin, please wait...");

    BedWars bedwarsAPI = Bukkit.getServicesManager().getRegistration(BedWars.class).getProvider();
    bedwarsAPI.getAddonsUtil().registerAddon(new Bw2023(this));

    this.databaseManager = new DatabaseManager(this);
    this.groupManager = new GroupManager(this);
    this.requestsManager = new RequestsManager(this);
    new GroupStatsExpansion(this).register();

    metrics = new Metrics(this, 16815);
    metrics.addCustomChart(new SimplePie("bedwars_plugin_type", () -> "bedwars2023"));
    metrics.addCustomChart(new SimplePie("database_type", () -> databaseManager.isDbEnabled() ? "MySQL" : "SQLite"));

    this.getLogger().info("Loaded the plugin successfully.");
    this.startupCompleted = true;
  }

  @Override
  public void onDisable() {
    this.getLogger().info("Disabling the plugin, please wait...");
    if (startupCompleted) {
      if (isBw2023()) {
        this.groupManager.saveAll();
      }
      this.databaseManager.closeDatabase();
    }
    if (requestsManager != null) {
      requestsManager.onDisable();
    }
    this.getLogger().info("Plugin disabled successfully.");
  }
}
