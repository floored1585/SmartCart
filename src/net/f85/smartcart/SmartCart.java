//
// SmartCart copyright 2015 Ian Clark
//
// Distributed under the MIT License
// http://opensource.org/licenses/MIT
//
package net.f85.smartcart;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;

public class SmartCart extends JavaPlugin {


    private static SmartCartListener listener;
    public static SmartCartUtil util;
    public static FileConfiguration config;


    @Override
    public void onEnable() {

        //plugin = this;

        // Generate the default config file
        this.saveDefaultConfig();

        config = this.getConfig();

        listener = new SmartCartListener(this);
        util = new SmartCartUtil(this);

        // Set up command executor
        //commandExecutor = new CommandExecutorUtil(this);
        this.getCommand("sc").setExecutor(new CommandExecutorUtil(this));
        this.getCommand("scSetTag").setExecutor(new CommandSetTag());

        getLogger().info("Successfully activated SmartCart");
    }


    @Override
    public void onDisable() {
        getLogger().info("Successfully deactivated SmartCart");
    }


}
