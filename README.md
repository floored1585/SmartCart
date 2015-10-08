# SmartCart
A simple, lightweight transportation plugin for MineCraft.  No powered rails required!

## Control Blocks
- Black Wool (spawn - use adjacent button)
- Yellow Wool (destroy)
- Green Wool (intersection)
- Red Wool (elevator)
- Orange Wool (slow)

## Requirements
This plugin is built against the [Spigot](http://www.spigotmc.org) MineCraft server.  Your mileage may vary with other servers.

To install, simply place the JAR in your plugins folder.

## Roadmap
- Signs to control spawn & intersection blocks

## Configuration
The following config.yml options are available:

| Keyword | Default | Description |
|---------|---------|-------------|
| boost_empty_carts | false | Plugin ignores empty carts unless true |
| normal_cart_speed | 1 | This doesn't do much at the moment |
| slow_cart_speed | 0.1 | Adjust to change the speed when traveling over slowing blocks |
| pickup_radius | 3 | How many blocks away a freshly spawned cart will look for a player to grab |
| empty_cart_timer | 10 | Number of seconds before an empty cart will despawn |
| empty_cart_timer_ignore_storagemincart | true | empty_cart_timer is ignored for storage carts if true |
| empty_cart_timer_ignore_spawnermincart | true | empty_cart_timer is ignored for spawner carts if true |
| empty_cart_timer_ignore_poweredmincart | true | empty_cart_timer is ignored for powered carts if true |
| empty_cart_timer_ignore_hoppermincart | true | empty_cart_timer is ignored for hopper carts if true |
| empty_cart_timer_ignore_explosiveminecart | true | empty_cart_timer is ignored for explosive carts if true |
| empty_cart_timer_ignore_commandminecart | true | empty_cart_timer is ignored for command carts if true |

## Contribute
If you have specific suggestions or if you find a bug, please don't hesitate to open an issue.  If you have time clone this repo and submit a pull request for your idea, go for it!

## Notice
This software mimics many of the functions of previous (abandoned) minecart plugins, but is written 100% from scratch.

## License
SmartCart is distributed under the MIT license.  Be free!!
