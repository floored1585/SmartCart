//
// SmartCart copyright 2015 Ian Clark
//
// Distributed under the MIT License
// http://opensource.org/licenses/MIT
//
package net.f85.SmartCart;

import net.f85.SmartCart.*;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.*;
import org.bukkit.configuration.file.*;
import java.util.logging.*;

public class SmartCart extends JavaPlugin {


  public static SmartCart plugin;
  public static SmartCartListener listener;
  public static SmartCartUtil util;
  public static CommandExecutorUtil commandExecutor;
  public static Logger log;
  public static FileConfiguration config;


  @Override
  public void onEnable() {

    plugin = this;

    // Generate the default config file
    plugin.saveDefaultConfig();

    config = plugin.getConfig();

    listener = new SmartCartListener(this);
    util = new SmartCartUtil(this);
    log = getLogger();

    // Set up command executor
    commandExecutor = new CommandExecutorUtil(this);
    Bukkit.getPluginCommand("sc").setExecutor(commandExecutor);

    getLogger().info("Successfully activated SmartCart");
  }


  @Override
  public void onDisable() {
    getLogger().info("Successfully deactivated SmartCart");
  }


}
