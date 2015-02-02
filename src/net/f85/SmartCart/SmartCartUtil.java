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
import org.bukkit.material.*;
import org.bukkit.entity.*;
import java.util.*;

public class SmartCartUtil {

  private ArrayList<SmartCartVehicle> cartList = new ArrayList<SmartCartVehicle>();
  private SmartCart plugin;


  public SmartCartUtil(SmartCart plugin) {
    this.plugin = plugin;
  }


  // Accessors!
  public ArrayList<SmartCartVehicle> getCartList() {
    return new ArrayList<SmartCartVehicle>(cartList);
  }


  public int CartListSize() {
    return cartList.size();
  }


  // This returns an ArrayList of all carts in the provided world
  public ArrayList<SmartCartVehicle> getCartList(World world) {

    // This is where the carts we find, if any, will go
    ArrayList<SmartCartVehicle> worldCarts = new ArrayList<SmartCartVehicle>();

    // Loop through all carts, saving the ones that match 
    for (SmartCartVehicle cart : cartList) {
      if (cart.getLocation().getWorld() == world) {
        worldCarts.add(cart);
      }
    }

    return worldCarts;
  }


  public SmartCartVehicle getCartFromList(Minecart requestedCart) {

    // Search for an existing SmartCartVehicle first
    for (SmartCartVehicle cart : cartList) {
      if (cart.getEntityId() == requestedCart.getEntityId()) {
        return cart;
      }
    }

    // If the cart doesn't already exist as a SmartCartVehicle, create it
    SmartCartVehicle newCart = new SmartCartVehicle(requestedCart);
    cartList.add(newCart);
    return newCart;
  }

  public SmartCartVehicle getCartFromList(int entityID) {
    // Search for an existing SmartCartVehicle first
    for (SmartCartVehicle cart : cartList) {
      if (cart.getEntityId() == entityID) {
        return cart;
      }
    }
    return null;
  }


  // Find & remove the cart from cartList
  public void removeCart(SmartCartVehicle deadCart) {
    // Find the cart that is being removed
    for (SmartCartVehicle cart : cartList) {
      if (cart.getEntityId() == deadCart.getEntityId()) {
        cartList.remove(cart);
        break;
      }
    }
  }

  public void removeCart(ArrayList<SmartCartVehicle> removeCartList) {
    for (SmartCartVehicle cart : removeCartList) {
      cartList.remove(cart);
      cart.getCart().remove();
    }
  }


  public boolean isControlBlock(Block block) {
    Block blockAbove = block.getLocation().add(0D, 1D, 0D).getBlock();
    return (block.getType() == Material.WOOL && blockAbove.getType() == Material.RAILS);
  }


  public boolean isSpawnBlock(Block block) {
    return isControlBlock(block) && ( new Wool(block.getType(), block.getData()) ).getColor() == DyeColor.BLACK;
  }


  // This searches methodically through a cube with a side length of (radius * 2 + 1)
  //   for the material passed.  Returns an ArrayList of Blocks containing all matching
  //   material found
  public ArrayList<Block> getBlocksNearby(Block centerBlock, int radius, Material material) {

    ArrayList<Block> blockList = new ArrayList<Block>();

searchloop:
    for (double xOffset = radius; xOffset >= radius * -1; xOffset--) {
      for (double yOffset = radius; yOffset >= radius * -1; yOffset--) {
        for (double zOffset = radius; zOffset >= radius * -1; zOffset--) {
          Block testBlock = centerBlock.getLocation().add(xOffset, yOffset, zOffset).getBlock();
          if (testBlock.getType() == material) {
            blockList.add(testBlock);
          }
        }
      }
    }
    return blockList;
  }


  // Send a message to the player
  public void sendMessage(Entity entity, String message) {
    message = "§6[SmartCart] " + message;
    if (entity instanceof Player) {
      ((Player) entity).sendRawMessage(message);
    }
  }


  // This method returns a delineated list of the worlds on the server
  public String getWorldList(String separator) {

    List<World> worldList = Bukkit.getWorlds();
    String list = "";
    int counter = 1;

    for (World world : worldList) {
      list += world.getName();
      // If we still have more worlds to add, insert separator
      if (worldList.size() > counter) {
        list += separator;
      }
      counter += 1;
    }

    return list;
  }


  // Send the provided list of carts to the provided entity
  public void sendCartList(ArrayList<SmartCartVehicle> sendCartList, Entity entity) {

    // These values are used to format the columns
    int padID = 7;
    int padWorld = 10;
    int padLocation = 18;
    int padPassenger = 20;
    int padAge = 5;

    String message = "\n"
      + padRight("ID", padID)
      + padRight("World", padWorld)
      + padRight("Location", padLocation)
      + padRight("Passenger", padPassenger)
      + padRight("Age", padAge)
      + "\n";

    for (SmartCartVehicle cart : sendCartList) {
      message += ""
        + padRight(Integer.toString(cart.getEntityId()), padID)
        + padRight(cart.getLocation().getWorld().getName(), padWorld)
        + padRight(getLocationString(cart.getLocation()), padLocation)
        + padRight(cart.getPassengerName(), padPassenger)
        + padRight(getAgeString((Entity) cart.getCart()), padAge)
        + "\n";
    }
    message += "Total: " + Integer.toString(sendCartList.size());

    sendMessage(entity, message);
  }


  public static String padRight(String s, int n) {
    return String.format("%1$-" + n + "s", s);  
  }

  public static String padLeft(String s, int n) {
    return String.format("%1$" + n + "s", s);  
  }


  public String getLocationString(Location loc) {
    return Integer.toString( loc.getBlockX() ) + ","
      + Integer.toString( loc.getBlockY() ) + ","
      + Integer.toString( loc.getBlockZ() );
  }


  public String getAgeString(Entity entity) {
    int ticks = entity.getTicksLived();
    int seconds = ticks / 20;
    return String.format("%d:%02d", seconds/60, seconds%60);
  }


  public boolean isRail(Block block) {
    if (block == null) {
      return false;
    }
    if (block.getType() == Material.RAILS) {
      return true;
    }
    return false;
  }


  public boolean isRail(Location loc) {
    return isRail(loc.getBlock());
  }


  // Checks to see if a string is really an integer!
  public static boolean isInteger(String str) {
    if (str == null) {
      return false;
    }
    int length = str.length();
    if (length == 0) {
      return false;
    }
    int i = 0;
    if (str.charAt(0) == '-') {
      if (length == 1) {
        return false;
      }
      i = 1;
    }
    for (; i < length; i++) {
      char c = str.charAt(i);
      if (c <= '/' || c >= ':') {
        return false;
      }
    }
    return true;
  }


  public SmartCartVehicle spawnCart(Block block) {
    if ( isRail(block) ) {
      Location loc = block.getLocation().add(0.5D,0D,0.5D);
      Minecart cart = loc.getWorld().spawn(loc, Minecart.class);
      return getCartFromList(cart);
    }
    return null;
  }


}
