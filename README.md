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
| boost-empty-carts | false | Plugin ignores empty carts unless true |
| normal-cart-speed | 1 | This doesn't do much at the moment |
| slow-cart-speed | 0.1 | Adjust to change the speed when traveling over slowing blocks |
| pickup-radius | 3 | How many blocks away a freshly spawned cart will look for a player to grab |
| empty-cart-timer | 10 | Number of seconds before an empty cart will despawn |
| empty-cart-timer-ignore-storagemincart | true | empty-cart-timer is ignored for storage carts if true |
| empty-cart-timer-ignore-spawnermincart | true | empty-cart-timer is ignored for spawner carts if true |
| empty-cart-timer-ignore-poweredmincart | true | empty-cart-timer is ignored for powered carts if true |
| empty-cart-timer-ignore-hoppermincart | true | empty-cart-timer is ignored for hopper carts if true |
| empty-cart-timer-ignore-explosiveminecart | true | empty-cart-timer is ignored for explosive carts if true |
| empty-cart-timer-ignore-commandminecart | true | empty-cart-timer is ignored for command carts if true |

## Contribute
If you have specific suggestions or if you find a bug, please don't hesitate to open an issue.  If you have time clone this repo and submit a pull request for your idea, go for it!

## Notice
This software mimics many of the functions of previous (abandoned) minecart plugins, but is written 100% from scratch.

## License
SmartCart is distributed under the MIT license.  Be free!!
