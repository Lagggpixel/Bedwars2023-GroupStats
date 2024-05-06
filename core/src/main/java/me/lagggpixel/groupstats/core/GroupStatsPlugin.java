package me.lagggpixel.groupstats.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.tomkeuper.bedwars.api.BedWars;
import lombok.Getter;
import me.infinity.groupstats.api.GroupNode;
import me.lagggpixel.groupstats.core.manager.DatabaseManager;
import me.lagggpixel.groupstats.core.manager.GroupManager;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Type;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public final class GroupStatsPlugin extends JavaPlugin implements CommandExecutor {

  private boolean startupCompleted = false;

  public static final Gson GSON = new GsonBuilder()
      .excludeFieldsWithoutExposeAnnotation()
      .disableHtmlEscaping()
      .create();

  public static final Type STATISTIC_MAP_TYPE = new TypeToken<ConcurrentHashMap<String, GroupNode>>() {
  }.getType();

  private DatabaseManager databaseManager;
  private GroupManager groupManager;

  private Metrics metrics;

  @Override
  public void onEnable() {

    metrics = new Metrics(this, 21824);

    this.saveDefaultConfig();

    final PluginManager pluginManager = this.getServer().getPluginManager();
    if (pluginManager.getPlugin("BedWars2023") != null) {
      this.getLogger().info("BedWars2023 found, activating standalone mode...");
    }

    this.getLogger().info("Loading the plugin, please wait...");

    BedWars bedwarsAPI = Bukkit.getServicesManager().getRegistration(BedWars.class).getProvider();
    bedwarsAPI.getAddonsUtil().registerAddon(new Bw2023(this));

    this.databaseManager = new DatabaseManager(this);
    this.metrics.addCustomChart(new SimplePie("storage",
        () -> this.databaseManager.isDbEnabled() ? "MySQL": "SQLite"));
    this.groupManager = new GroupManager(this);
    new GroupStatsExpansion(this).register();

    this.getLogger().info("Loaded the plugin successfully.");
    this.startupCompleted = true;
  }

  @Override
  public void onDisable() {
    this.getLogger().info("Disabling the plugin, please wait...");
    if (startupCompleted) {
      this.groupManager.saveAllAsync();
      this.databaseManager.closeDatabase();
    }
    this.getLogger().info("Plugin disabled successfully.");
  }
}
