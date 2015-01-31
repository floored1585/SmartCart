//
// SmartCart copyright 2015 Ian Clark
//
// Distributed under the MIT License
// http://opensource.org/licenses/MIT
//
package net.f85.SmartCart;

import net.f85.SmartCart.*;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.vehicle.*;
import org.bukkit.material.Wool;
import java.util.ArrayList;

public class SmartCartListener implements Listener {


  private SmartCart plugin;


  public SmartCartListener(SmartCart plugin) {
    this.plugin = plugin;
    plugin.getServer().getPluginManager().registerEvents(this, plugin);
  }


  @EventHandler
  public void onVehicleUpdate(VehicleUpdateEvent event) {

    Vehicle vehicle = event.getVehicle();

    // Return if vehicle is not a minecart
    if (!( vehicle instanceof Minecart )) {
      return;
    }

    SmartCartVehicle cart = SmartCart.util.getCartFromList( (Minecart) vehicle );

    // Return if minecart is marked for removal, or off rails for any reason
    if ( cart.getCart().isDead() || !cart.isOnRail() ) {
      return;
    }

    if ( cart.isOnControlBlock() ) {
      cart.executeControl();
    }
    else {
      cart.setPreviousWoolColor(null);
      cart.setSpeed( SmartCart.config.getDouble("normal_cart_speed") );
    }

  }


  @EventHandler
  public void onVehicleEnter(VehicleEnterEvent event) {

    Vehicle vehicle = event.getVehicle();

    // Return if vehicle is not a minecart
    if (!( vehicle instanceof Minecart )) {
      return;
    }

    SmartCartVehicle cart = SmartCart.util.getCartFromList( (Minecart) vehicle );

    // Return if minecart is marked for removal, or off rails for any reason
    if ( cart.getCart().isDead() || !cart.isOnRail() ) {
      return;
    }

    // If the cart is stopped, send instructions
    if (!cart.isMoving()) {
      SmartCart.util.sendMessage(event.getEntered(), "Tap in a direction to launch!");
    }


  }


  @EventHandler
  public void onVehicleDestroyed(VehicleDestroyEvent event) {

    Vehicle vehicle = event.getVehicle();

    if (vehicle instanceof Minecart) {
      SmartCart.util.getCartFromList( (Minecart) vehicle ).remove(false);
    }
  }


  @EventHandler
  public void onBlockRedstone(BlockRedstoneEvent event) {

    // Return if the redstone current is turning off instead of on
    if ( event.getOldCurrent() > event.getNewCurrent() ) {
      return;
    }

    // Function takes a location, radius, and material to search for -- get all command blocks
    int search_radius = 1;
    ArrayList<Block> cmdBlockList = SmartCart.util.getBlocksNearby(event.getBlock(), search_radius, Material.WOOL);

    // Return if we didn't find any command blocks
    if (cmdBlockList.size() == 0) {
      return;
    }

    ArrayList<Block> spawnBlocks = new ArrayList<Block>();

    // Check each of the command blocks and put spawn blocks in an arraylist
    for (Block thisBlock : cmdBlockList) {
      if (SmartCart.util.isSpawnBlock(thisBlock)) {
        spawnBlocks.add(thisBlock);
      }
    }

    if (spawnBlocks.size() == 0) {
      return;
    }

    // Now we know block is a control block and the redstone was activating.
    //   Time to take action!

    Block block = spawnBlocks.get(0);
    Location loc = block.getLocation();

    // spawn a cart
    Minecart cart = loc.getWorld().spawn(loc.add(0.5D, 1D, 0.5D), Minecart.class);
    SmartCartVehicle smartCart = SmartCart.util.getCartFromList(cart);

    // pick up a nearby player
    double r = 4;
    for (Entity entity : cart.getNearbyEntities(r, r, r)) {
      if (entity instanceof Player && cart.getPassenger() == null && entity.getVehicle() == null) {
        cart.setPassenger(entity);
        break;
      }
    }
  }


}
