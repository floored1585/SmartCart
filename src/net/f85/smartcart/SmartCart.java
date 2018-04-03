//
// SmartCart copyright 2015 Ian Clark
//
// Distributed under the MIT License
// http://opensource.org/licenses/MIT
//
package net.f85.smartcart;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
//import java.util.logging.Logger;

public class SmartCart extends JavaPlugin {


    //private static SmartCart plugin;
    private static SmartCartListener listener;
    public static SmartCartUtil util;
    //private static CommandExecutorUtil commandExecutor;
    //private static Logger log;
    public static FileConfiguration config;


    @Override
    public void onEnable() {

        //plugin = this;

        // Generate the default config file
        /*plugin*/this.saveDefaultConfig();

        config = /*plugin*/this.getConfig();

        listener = new SmartCartListener(this);
        util = new SmartCartUtil(this);
        //log = getLogger();

        // Set up command executor
        //commandExecutor = new CommandExecutorUtil(this);
        Bukkit.getPluginCommand("sc").setExecutor(new CommandExecutorUtil(this));

        getLogger().info("Successfully activated SmartCart");
    }


    @Override
    public void onDisable() {
        getLogger().info("Successfully deactivated SmartCart");
    }


}
