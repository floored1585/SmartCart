//
// SmartCart copyright 2015 Ian Clark
//
// Distributed under the MIT License
// http://opensource.org/licenses/MIT
//
package net.f85.SmartCart;

import org.bukkit.*;
import org.bukkit.entity.minecart.*;
import org.bukkit.entity.minecart.PoweredMinecart;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.material.*;
import org.bukkit.block.*;
import org.bukkit.entity.*;
import org.bukkit.util.*;
import java.util.regex.*;
import java.util.Arrays;

public class SmartCartVehicle {

  private Minecart cart;
  private DyeColor previousWoolColor;
  private Location currentLocation;
  private Location previousLocation;
  private int[] currentRoughLocation;
  private int[] previousRoughLocation;
  private String signText;
  private int emptyCartTimer = 0;
  // Settables
  private double configSpeed = SmartCart.config.getDouble("normal_cart_speed");
  private String configEndpoint = "";


  public SmartCartVehicle(Minecart vehicle) {
    cart = vehicle;
    cart.setMaxSpeed(SmartCart.config.getDouble("max_cart_speed"));
  }


  // Accessors
  public Minecart getCart() {
    return cart;
  }
  public Location getPreviousLocation() {
    return previousLocation;
  }
  public DyeColor getPreviousWoolColor() {
    return previousWoolColor;
  }
  public Double getConfigSpeed() {
    return configSpeed;
  }
  public void setConfigSpeed(Double speed) {
    configSpeed = speed;
  }
  public String getConfigEndpoint() {
    return configEndpoint;
  }
  public void setConfigEndpoint(String endpoint) {
    configEndpoint = endpoint;
  }
  public String getSignText() {
    return signText;
  }
  public void setPreviousWoolColor(DyeColor color) {
    previousWoolColor = color;
  }
  public void saveCurrentLocation() {
    previousLocation = currentLocation;
    currentLocation = getLocation();
    previousRoughLocation = currentRoughLocation;
    currentRoughLocation = new int[] {
      currentLocation.getBlockX(), currentLocation.getBlockY(), currentLocation.getBlockZ()
    };
  }


  // These methods just pass through to the Minecart class
  public int getEntityId() {
    return getCart().getEntityId();
  }
  public Entity getPassenger() {
    return getCart().getPassenger();
  }
  public Location getLocation() {
    return getCart().getLocation();
  }


  public boolean isNewBlock() {
    return !Arrays.equals(currentRoughLocation, previousRoughLocation);
  }


  public void setEmptyCartTimer() {
    if(
            SmartCart.config.getBoolean("empty_cart_timer_ignore_commandminecart", true) && isCommandMinecart() ||
            SmartCart.config.getBoolean("empty_cart_timer_ignore_explosiveminecart", true) && isExplosiveMinecart() ||
            SmartCart.config.getBoolean("empty_cart_timer_ignore_storagemincart", true) && isStorageMinecart() ||
            SmartCart.config.getBoolean("empty_cart_timer_ignore_hoppermincart", true) && isHopperMinecart() ||
            SmartCart.config.getBoolean("empty_cart_timer_ignore_poweredmincart", true) && isPoweredMinecart() ||
            SmartCart.config.getBoolean("empty_cart_timer_ignore_spawnermincart", true) && isSpawnerMinecart() ||
            SmartCart.config.getInt("empty_cart_timer") == 0
    ) {
      emptyCartTimer = 0;
    } else {
      emptyCartTimer += 1;
      if (emptyCartTimer > SmartCart.config.getInt("empty_cart_timer") * 20) {
        remove(true);
      }
    }
  }


  public void resetEmptyCartTimer() {
    emptyCartTimer = 0;
  }


  // Returns the block beneath the rail
  public Block getBlockBeneath() {
    return getCart().getLocation().add(0D, -1D, 0D).getBlock();
  }


  // Returns true only if cart is in a rail block
  public boolean isOnRail() {
    return getCart().getLocation().getBlock().getType() == Material.RAILS;
  }


  // Returns true if the cart is directly above a control block
  public boolean isOnControlBlock() {
    return SmartCart.util.isControlBlock( getCart().getLocation().add(0D, -1D, 0D).getBlock() );
  }


  // This looks two blocks below the rail for a sign. Sets the signText variable to
  //   the sign contents if the sign is a valid control sign, otherwise "".
  public void readControlSign() {
    Block block = getCart().getLocation().add(0D, -2D, 0D).getBlock();
    // Return if we're not over a sign
    if (block.getType() != Material.WALL_SIGN && block.getType() != Material.SIGN_POST) {
      return;
    }
    // Return if we're not on rails
    if (!isOnRail()) {
      return;
    }
    String text = "";
    org.bukkit.block.Sign sign = (org.bukkit.block.Sign) block.getState(); // Cast to Sign
    for( String value : sign.getLines() ) { // Merge all the sign's lines
      text += (" " + value);
    }
    // Check to see if the sign string matches the control sign prefix; return otherwise
    Pattern p = Pattern.compile(SmartCart.config.getString("control_sign_prefix_regex"));
    Matcher m = p.matcher(text);
    // Return if the control prefix isn't matched
    if (!m.find()) {
      return;
    }
    String signText = m.replaceAll(""); // Remove the control prefix

    Boolean foundEndpoint = false;

    for(String pair : signText.split("\\s*\\|\\s*")) {
      pair = pair.trim();
      p = Pattern.compile("^\\s*([^\\|:]+):([^\\|:]+)\\s*$");
      m = p.matcher(pair);
      if (!m.find()) {
        sendPassengerMessage("Bad sign formatting: \"" + pair + "\".  See https://github.com/floored1585/SmartCart for help.");
        continue; // go to the next pair if it's not really a pair
      }
      String setting = m.group(1).trim();
      String value = m.group(2).trim();

      if(setting.equals("$SPD")) {
        p = Pattern.compile("^\\d*\\.?\\d+$");
        Double minSpeed = 0D;
        Double maxSpeed = SmartCart.config.getDouble("max_cart_speed");
        if(!p.matcher(value).find() || Double.parseDouble(value)>maxSpeed || Double.parseDouble(value)<minSpeed) {
          sendPassengerMessage("Bad speed value: \"" + value + "\". Must be a numeric value (decimals OK) between "
              + minSpeed + " and " + maxSpeed + ".");
          continue;
        }
        configSpeed = Double.parseDouble(value);
      }
      else if(setting.equals("$MSG")) {
        sendPassengerMessage(value);
      }
      else if(setting.equals("$END")) {
        configEndpoint = value;
        sendPassengerMessage("Endpoint set to " + value);
      }
      else if(setting.equals(configEndpoint) || setting.equals("$DEF")) {
        // Skip this if we already found and used the endpoint
        if(foundEndpoint) {
          continue;
        }
        foundEndpoint = true;
        Block blockAhead;
        Vector vector;
        if(value.equals("N")) {
          blockAhead = cart.getLocation().add(0D,0D,-1D).getBlock();
          vector = new Vector(0,0,-1);
        } else if (value.equals("S")) {
          blockAhead = cart.getLocation().add(0D,0D,1D).getBlock();
          vector = new Vector(0,0,1);
        } else if (value.equals("E")) {
          blockAhead = cart.getLocation().add(1D,0D,0D).getBlock();
          vector = new Vector(1,0,0);
        } else { // W
          blockAhead = cart.getLocation().add(-1D,0D,0D).getBlock();
          vector = new Vector(-1,0,0);
        }
        Entity passenger = cart.getPassenger();
        if (SmartCart.util.isRail(blockAhead)) {
          remove(true);
          SmartCartVehicle newSC = SmartCart.util.spawnCart(blockAhead);
          newSC.getCart().setPassenger(passenger);
          newSC.getCart().setVelocity(vector);
          transferSettings(newSC);
        }
      }
    }
  }


  public boolean isMoving() {
    Vector velocity = getCart().getVelocity();
    return (velocity.getX() != 0D || velocity.getZ() != 0D);
  }


  // Sets the speed to the max, in the direction the cart is already travelling
  public void setSpeed(double speed) {

    // Check if the cart is empty, and if we should boost empty carts
    if (getCart().isEmpty() && !SmartCart.config.getBoolean("boost_empty_carts")) {
      return;
    }

    Vector velocity = getCart().getVelocity();

    // If the cart is moving
    if (isMoving()) {
      // Maintain velocity

      Vector newVelocity = new Vector();

      // Check to see which axis we're moving along and use that to set the new vector
      //   The signum function just returns 1 if passed a positive, -1 if negative
      if ( Math.abs(velocity.getX()) > Math.abs(velocity.getZ()) ) {
        newVelocity.setX( Math.signum(velocity.getX()) * speed );
      } else {
        newVelocity.setZ( Math.signum(velocity.getZ()) * speed );
      }

      // Update the velocity
      getCart().setVelocity(newVelocity);
    }
  }


  // Destroy the cart (from plugin & server)
  public void remove(boolean kill) {

    Entity passenger = getPassenger();

    // Move the passenger out of the cart
    if (passenger != null) {
      // Get location of passenger & add 1 to Y value)
      Location loc = passenger.getLocation();
      loc.setX( cart.getLocation().getBlockX() + 0.5D );
      loc.setY( cart.getLocation().getBlockY());
      loc.setZ( cart.getLocation().getBlockZ() + 0.5D );

      passenger.teleport(loc);
    }

    // Remove from list of carts
    SmartCart.util.removeCart(this);

    // If we need to kill the actual cart, kill it
    if (kill) {
      getCart().remove();
    }
  }


  public void transferSettings(SmartCartVehicle newSC) {
    newSC.setConfigSpeed(configSpeed);
    newSC.setConfigEndpoint(configEndpoint);
  }


  public void executeControl() {

    if (getCart().getPassenger() == null) {
      return;
    }

    Block block = getBlockBeneath();

    //------------------------------------------------------------\
    //                                                            |
    // getData is DEPRECATED, but no alternative is in place yet  |
    //                                                            |
    Wool wool = new Wool( block.getType(), block.getData() );    //
    //                                                            |
    //                                                            |
    //------------------------------------------------------------*

    switch ( wool.getColor() ) {

      case ORANGE:
        setPreviousWoolColor(wool.getColor());
        setSpeed( SmartCart.config.getDouble("slow_cart_speed") );
        break;

      case YELLOW:
        // If the cart is near the center of the block, kill it.  Otherwise, slow it down.
        if ( isLeavingBlock() ) {
          remove(true);
        } else {
          setSpeed(0.1D);
        }
        break;

      case GREEN:
        // If we have already executed this block and aren't moving, abort.
        //   If we have already executed this block and ARE moving, teleport the cart
        //   in the direction the player is facing.
        if ( getPreviousWoolColor() == wool.getColor() ) {
          if ( isMoving() && getBlockAheadPassenger() != null) {
            Block blockAhead = getBlockAheadPassenger();
            Entity passenger = getCart().getPassenger();
            if (SmartCart.util.isRail(blockAhead)) {
              remove(true);
              SmartCartVehicle newSC = SmartCart.util.spawnCart(blockAhead);
              newSC.getCart().setPassenger(passenger);
              transferSettings(newSC);
            }
          }
          return;
        }
        // If the cart is near the center of the block, stop it.  Otherwise, slow it down.
        if ( isLeavingBlock() ) {
          setPreviousWoolColor(wool.getColor());
          setSpeed(0D);
          sendPassengerMessage("Move in the direction you wish to go.");
        } else {
          // Otherwise, slow the cart down
          setSpeed(0.1D);
        }
        break;

      case RED:
        // If we're not half way through the block, return
        if ( !isLeavingBlock() ) {
          setSpeed(0.1D);
          return;
        }
        // If we just executed the elevator, return
        if ( getPreviousWoolColor() == wool.getColor() ) {
          return;
        }

        setPreviousWoolColor(wool.getColor());

        // Get the tp target, destroy old cart, spawn new cart at tp target,
        //   tp passenger, load passenger into new cart
        Block elevator = SmartCart.util.getElevatorBlock(block.getLocation());
        if (elevator == null) {
          return;
        }
        Block tpTarget = elevator.getLocation().add(0,1,0).getBlock();
        Entity passenger = getCart().getPassenger();
        Vector cartVelocity = getCart().getVelocity();

        // Set the new passenger location
        Location passengerLoc = passenger.getLocation();
        passengerLoc.setY( tpTarget.getLocation().getBlockY() );
        // Kill the cart, spawn a new one
        remove(true);
        SmartCartVehicle newCart = SmartCart.util.spawnCart(tpTarget);
        // Teleport passenger to new location and load them in the cart
        passenger.teleport(passengerLoc);
        newCart.getCart().setPassenger(passenger);
        // Get the cart going!
        newCart.getCart().setVelocity(cartVelocity);
        newCart.setSpeed(1);
        // Set the previous wool color of the new cart, to prevent a tp loop
        newCart.setPreviousWoolColor(wool.getColor());
        transferSettings(newCart);
        break;
    }
  }


  public String getPassengerName() {
    if (getPassenger() == null) {
      return "None";
    }
    return getCart().getPassenger().getName();
  }


  // Returns the block directly ahead of the passenger
  public Block getBlockAheadPassenger() {
    // Get the passenger's direction as an integer
    //   -1/3 = pos x
    //   -2/2 = neg z
    //   -3/1 = neg x
    //   -4/0 = pos z

    int passengerDir = Math.round( getCart().getPassenger().getLocation().getYaw() / 90f );
    Block block = null;
    switch (passengerDir) {
      case 0:
      case -4:
        block = getCart().getLocation().add(0,0,1).getBlock();
        break;
      case 1:
      case -3:
        block = getCart().getLocation().add(-1,0,0).getBlock();
        break;
      case 2:
      case -2:
        block = getCart().getLocation().add(0,0,-1).getBlock();
        break;
      case 3:
      case -1:
        block = getCart().getLocation().add(1,0,0).getBlock();
        break;
    }
    return block;
  }


  // Find out if the cart is headed towards or away from the middle of the current block
  public boolean isLeavingBlock() {

    // Gotta check to make sure this exists first
    if (getPreviousLocation() == null) {
      return false;
    }
    // If we just moved to a new block, the previous location is invalid for this check
    if (getPreviousLocation().getBlockX() != getLocation().getBlockX()
        || getPreviousLocation().getBlockZ() != getLocation().getBlockZ()) {
      // This lets you chain control blocks by setting the prev wool color to null unless we
      // just got off an elevator.
      if (Math.abs(getPreviousLocation().getBlockY() - getLocation().getBlockY()) < 2) {
        setPreviousWoolColor(null);
      }
      return false;
    }

    // Get the previous and current locations
    double prevX = Math.abs( getPreviousLocation().getX() );
    double prevZ = Math.abs( getPreviousLocation().getZ() );
    double currX = Math.abs( getLocation().getX() );
    double currZ = Math.abs( getLocation().getZ() );

    // Just get the decimal part of the double
    prevX = prevX - (int) prevX;
    prevZ = prevZ - (int) prevZ;
    currX = currX - (int) currX;
    currZ = currZ - (int) currZ;

    // Get distance from the middle of the block
    double prevDistFromMidX = Math.abs( prevX - 0.5 );
    double prevDistFromMidZ = Math.abs( prevZ - 0.5 );
    double currDistFromMidX = Math.abs( currX - 0.5 );
    double currDistFromMidZ = Math.abs( currZ - 0.5 );

    // Return true if we're headed away from the center of the block
    if ( currDistFromMidX > prevDistFromMidX
        || currDistFromMidZ > prevDistFromMidZ
        || (currDistFromMidX < 0.1 && currDistFromMidZ < 0.1) ) {
      return true;
    }
    return false;
  }

  public void sendPassengerMessage(String message) {
    message = "§6[SmartCart] " + message;
    Entity entity = getPassenger();
    if (entity instanceof Player) {
      ((Player) entity).sendRawMessage(message);
    }
  }

  public boolean isCommandMinecart() {
    return getCart() instanceof CommandMinecart;
  }

  public boolean isExplosiveMinecart() {
    return  getCart() instanceof ExplosiveMinecart;
  }

  public boolean isHopperMinecart() {
    return getCart() instanceof HopperMinecart;
  }

  public boolean isPoweredMinecart() {
    return getCart() instanceof PoweredMinecart;
  }

  public boolean isRideableMinecart() {
    return getCart() instanceof RideableMinecart;
  }

  public boolean isSpawnerMinecart() {
    return  getCart() instanceof SpawnerMinecart;
  }

  public boolean isStorageMinecart() {
    return getCart() instanceof StorageMinecart;
  }
}
