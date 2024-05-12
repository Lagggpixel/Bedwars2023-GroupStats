package me.lagggpixel.groupstats.core.manager;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.table.TableUtils;
import lombok.Getter;
import lombok.SneakyThrows;
import me.lagggpixel.groupstats.core.GroupProfile;
import me.lagggpixel.groupstats.core.GroupStatsPlugin;
import me.lagggpixel.groupstats.core.listener.GroupStatsListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class GroupManager implements Listener {

  private final GroupStatsPlugin instance;
  private final Dao<GroupProfile, UUID> groupProfiles;

  private final Map<UUID, GroupProfile> groupProfileCache;

  @SneakyThrows
  public GroupManager(GroupStatsPlugin instance) {
    this.instance = instance;

    this.groupProfiles = DaoManager.createDao(instance.getDatabaseManager().getConnectionSource(),
        GroupProfile.class);
    TableUtils.createTableIfNotExists(instance.getDatabaseManager().getConnectionSource(),
        GroupProfile.class);

    this.groupProfileCache = new ConcurrentHashMap<>();
    int updateTimer = instance.getConfig().getInt("update-timer");
    instance.getServer().getScheduler()
        .runTaskTimer(instance, new GroupUpdateTask(this), 20 * 60, 20L * 60 * updateTimer);
    instance.getServer().getPluginManager().registerEvents(this, instance);
    new GroupStatsListener(instance);

  }

  @SneakyThrows
  public GroupProfile fetchLoad(UUID uniqueId) {
    Optional<GroupProfile> optionalGroupProfile = Optional.ofNullable(
        groupProfiles.queryForId(uniqueId));
    if (optionalGroupProfile.isPresent()) {
      return optionalGroupProfile.get();
    }
    GroupProfile profile = new GroupProfile(uniqueId);
    groupProfiles.create(profile);
    return profile;
  }

  @SneakyThrows
  @Nullable
  public GroupProfile fetchUnsafe(UUID uniqueId) {
    return groupProfiles.queryForId(uniqueId);
  }


  @SneakyThrows
  public void save(GroupProfile groupProfile) {
    groupProfile.setData(GroupStatsPlugin.GSON.toJson(
        groupProfile.getGroupStatistics())); // Update data and parse object to string
    groupProfiles.update(groupProfile);
  }

  public void saveAll() {
    if (this.getInstance().getServer().getOnlinePlayers().isEmpty()) {
      return;
    }
    if (this.getGroupProfileCache().isEmpty()) {
      return;
    }
    this.groupProfileCache.values().forEach(this::save);
  }
  
  public void saveAllAsync() {
    instance.getDatabaseManager().getHikariExecutor()
        .execute(this::saveAll);
  }
  

  @EventHandler(priority = EventPriority.LOW)
  public void onJoin(PlayerJoinEvent event) {
    instance.getDatabaseManager().getHikariExecutor().execute(() -> {
      GroupProfile groupProfile = this.fetchLoad(event.getPlayer().getUniqueId());
      groupProfile.setGroupStatistics(GroupStatsPlugin.GSON.fromJson(groupProfile.getData(),
          GroupStatsPlugin.STATISTIC_MAP_TYPE));
      groupProfileCache.put(event.getPlayer().getUniqueId(), groupProfile);
    });
  }

  @EventHandler(priority = EventPriority.LOW)
  public void onQuit(PlayerQuitEvent event) {
    instance.getDatabaseManager().getHikariExecutor().execute(() -> {
      this.save(groupProfileCache.get(event.getPlayer().getUniqueId()));
      this.groupProfileCache.remove(event.getPlayer().getUniqueId());
    });
  }
}

