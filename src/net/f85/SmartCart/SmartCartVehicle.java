//
// SmartCart copyright 2015 Ian Clark
//
// Distributed under the MIT License
// http://opensource.org/licenses/MIT
//
package net.f85.SmartCart;

import org.bukkit.*;
import org.bukkit.material.*;
import org.bukkit.block.*;
import org.bukkit.entity.*;
import org.bukkit.util.*;

public class SmartCartVehicle {


  private Minecart cart;
  private DyeColor previousWoolColor;


  public SmartCartVehicle(Minecart vehicle) {
    cart = vehicle;
  }


  // Accessors
  public Minecart getCart() {
    return cart;
  }
  public DyeColor getPreviousWoolColor() {
    return previousWoolColor;
  }
  public void setPreviousWoolColor(DyeColor color) {
    previousWoolColor = color;
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
      loc.setY( cart.getLocation().getBlockY() + 0.25D );
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


  public void executeControl() {

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
        setSpeed( SmartCart.config.getDouble("slow_cart_speed") );
        break;

      case YELLOW:
        // If the cart is near the center of the block, kill it.  Otherwise, slow it down.
        if (SmartCart.util.isNearCenterOfBlock( (Entity) getCart() )) {
          remove(true);
        } else {
          setSpeed(0.1D);
        }
        break;

      case GREEN:
        // If we have already executed this block, abort
        if ( getPreviousWoolColor() == wool.getColor() ) {
          return;
        }
        // If the cart is near the center of the block, stop it.  Otherwise, slow it down.
        if (SmartCart.util.isNearCenterOfBlock( (Entity) getCart() )) {
          setPreviousWoolColor(wool.getColor());
          setSpeed(0D);
        } else {
          // Otherwise, slow the cart down
          setSpeed(0.1D);
        }
        break;
    }
  }


  public String getPassengerName() {
    if (getPassenger() == null) {
      return "None";
    }
    return getCart().getPassenger().getName();
  }


}
