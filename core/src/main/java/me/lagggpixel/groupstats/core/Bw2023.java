package me.lagggpixel.groupstats.core;

import com.tomkeuper.bedwars.api.addon.Addon;
import org.bukkit.Bukkit;

/**
 * @author Lagggpixel
 * @since March 26, 2024
 */
public class Bw2023 extends Addon {

  private final GroupStatsPlugin plugin;

  public Bw2023(GroupStatsPlugin plugin) {
    super();
    this.plugin = plugin;
  }

  @Override
  public String getAuthor() {
    //This gets the information directly from the plugin.yml file.
    return plugin.getDescription().getAuthors().get(0);
  }

  @Override
  public GroupStatsPlugin getPlugin() {
    return plugin;
  }

  @Override
  public String getVersion() {
    return plugin.getDescription().getVersion();
  }

  @Override
  public String getDescription() {
    return plugin.getDescription().getDescription();
  }

  @Override
  public String getName() {
    return plugin.getDescription().getName();
  }

  @Override
  public void load() {
    Bukkit.getPluginManager().enablePlugin(plugin);
  }

  @Override
  public void unload() {
    Bukkit.getPluginManager().disablePlugin(plugin);
  }
}
