# SmartCart
A simple, lightweight transportation plugin for MineCraft.  No powered rails required!  [Download It Now!](https://github.com/floored1585/SmartCart/raw/master/bin/SmartCart.jar)

## Control Blocks
- Black Wool (spawn - use adjacent button)
- Yellow Wool (destroy)
- Green Wool (intersection)
- Red Wool (elevator)
- Orange Wool (slow)

## Control Signs
Control signs can be placed two blocks below the rail (just under the supporting block), anywhere on the supporting block, or anywhere immediately next to the rail.  To be recognized as control signs, the sign must begin with "SC: " (by default; this can be changed).  The text that follows this prefix must be in the following format:

<code>\<setting>:\<value>|\<setting>:\<value></code>

For example:

<code>SC: $SPD:0.2 | $MSG:Ciao Amigo</code> will cause the cart speed to be set at 0.2 (half of normal speed) and will send "Ciao Amigo" to the passenger. White space is trimmed (except on the prefix).

One sign can contain as many setting/value pairs as you can fit, but they must be separated by pipe symbols (<code>|</code>), and a single setting/value pair should not span lines on the sign ($MSG is an exception).
If you have multiple lines of settings and values, remember to add a pipe symbol between the lines; it won't be added automatically.  It is also extrememly important to not add colons or pipes anywhere except as seperators.
Below is a list of currently supported settings and values.  If you would like to see more, please open an issue here on GitHub or talk to me in-game at [Uncovery](http://www.uncovery.me)!

| Setting | Example | Description |
|:--------|:-------:|:------------|
| $SPD | <code>$SPD:0.2</code> | Speed - Sets the speed of the cart. 0.4 is vanilla max cart speed. Must be numeric and within the bounds of the server settings. Leading zero and decimal are optional.|
| $MSG | <code>$MSG:Hi There!</code> | Message - Sends the value text to cart's passenger. Do not use colons (<code>:</code>) or pipes (<code>\|</code>) in the text.|
| $END or $TAG | <code>$END:Oz</code> or <code>$TAG:Oz</code> | Endpoint/Tag - Sets the endpoint that this cart will attempt to reach.  After setting $END, you can use signs under intersections (no wool required) to direct the cart to the correct endpoint. See example below.|
| Oz (example) | <code>Oz:W</code> | Endpoint direction - This instructs the cart which direction to go at an intersection (no wool required) to reach the endpoint on the sign.|
| $DEF | <code>$DEF:S</code> | Default direction - This instructs the cart which direction to go at an intersection (no wool required) if no other directions are matched.|
| $LNC | <code>$LNC:N</code> | Launches cart when spawned. Will not trigger the spawn message asking player to move in a direction.|
| $EJT | <code>$EJT:N3</code> | Eject: extends the yellow wool block to put the player a specified number of blocks away. Can have multiple $EJT on the same sign to make more complicated ejects. Valid directions are U, D, N, S, E, W.|


## Command
There is now a command to add a tag/endpoint to a cart while riding that cart. to use it, type /scSetTag <tagname>

## Endpoints/Tags & Intersections

This can be slightly confusing, so here is a full explanation.  With SmartCart, you can affix a label to a cart that tells it where it should end up.  For example, when a cart passes over $END:Oz or $TAG:Oz, from then on the cart knows it is headed to Oz. Any time it encounters a control sign, it checks the sign for directions (in the format of Endpoint/Tag:Direction, where Endpoint/Tag would be "Oz" and Direction would be "N", "S", "E", or "W".  If the cart encounters a sign with directions, it attempts to move in that direction.  These directions are only useful under intersections.  Wool is not required for Endpoints to function, and will be ignored if valid directions (or $DEF) are encountered. Regardless, is recommended to use intersection wool as a fallback unless you use $DEF, in case someone hops on the track in the middle and misses the initial endpoint assignment.


## Requirements
This plugin is built against the [Spigot](http://www.spigotmc.org) MineCraft server.  Your mileage may vary with other servers.

To install, simply place the JAR in your plugins folder.

## Roadmap
- Signs to control spawn & intersection blocks

## Configuration
The following config.yml options are available:

| Keyword | Default | Description |
|:--------|:-------:|:------------|
| boost_empty_carts | false | Plugin ignores empty carts unless true |
| normal_cart_speed | 0.4 | This is the speed carts run at by default |
| max_cart_speed | 0.4 | The fastest a cart will travel.  Expect problems with higher values. |
| slow_cart_speed | 0.1 | Adjust to change the speed when traveling over slowing blocks |
| pickup_radius | 3 | How many blocks away a freshly spawned cart will look for a player to grab |
| control_sign_prefix_regex | "\\s+SC: " | A sign with text matching this regex will be considered a control sign |
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
