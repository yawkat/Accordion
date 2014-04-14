Accordion
=========

Peer-to-peer network API to connect multiple Minecraft servers together.

Download
--------

You can download builds on my jenkins: [![Build Status](http://ci.yawk.at/job/Accordion/badge/icon)](http://ci.yawk.at/job/Accordion/)

The maven repository can be found [here](http://ci.yawk.at/plugin/repository/everything/).

Modules
-------

This project currently consists of three modules.

### core

The core module contains netty-based network code that is not specifically linked to minecraft.

### minecraft

The minecraft module contains a minecraft-specific network setup such as different server types. It extends the core module. 

This module also contains the "backbone" system which allows for creation of an efficient peer-to-peer network setup. It is only slightly minecraft-dependend and can be used outside of it as well.

This module does not depend on either BungeeCord or Bukkit.

### minecraft-plugin

Minecraft plugins for bukkit and bungee that provide an API to register packets and transmit them via the network.

### minecraft-example

This module contains an example Bukkit plugin and an example BungeeCord plugin that together form a network with a basic packet exchange.

Building
--------

All modules can be built by running `mvn clean package` in the main directory. The minecraft-example project will create a shaded jar so it can be used as a plugin without further editing.

Example Setup
-------------

For a very basic setup you need one BungeeCord server and one CraftBukkit / Spigot server running on the same computer. Set up the bungee server so it is linked to the bukkit server properly. Now place the main accordion plugin jar and the example plugin jar in the plugins directory.

This setup will allow both servers to "talk" to each other. Now start them and log in. When on the server, run the command `/ping 6`. When you check the logs, you will see a "Ping" output on the bungee server and a "Pong 6" output on the bukkit server. This is what happened:

- The bukkit server sent a "Ping request" to the network, containing the number you entered.
- The bungee server received this "Ping request" and does two things:
	- It prints "Ping" to its console
	- It sends a "Pong reply" to the network with the same number as in the "Ping request".
- The bukkit server receives this pong reply and prints out "Pong " and the number you entered.

For initial discovery, a player must be logged in, but after that it works independent of connected players.

To extend the example setup, you can create up to one more bungee and one more bukkit server. These should, instead of `id: 0`, use `id: 1` in their config files. If you now execute the `ping` command, a lot more happens:

- You get the "Ping" output on all three servers you didn't run the command on
- You get the "Pong" output three times on each server: One for each reply.

Be aware that each server that should be connected to the network must have had a player join at least one. 

Internals
---------

Internally, this network uses a "backbone"-based peer-to-peer system with the bungee servers as the backbone. All bungee servers connect to each other and interchange messages; all other (bukkit) servers connect to this grid of bungee servers and use it as a small intranet. There is no need for one central server. With default settings, each bukkit server is only connected to one bungee server: If that bungee server dies, all connected bukkit servers will lose connection to the network. This can be avoided by either running two bungee servers on each machine (bukkit servers will otherwise just keep trying to connect to the crashed bungee and never get back into the network) or setting the maximum connections to 2 or more (this will cause higher inter-machine traffic, however).

Performance
-----------

Because of the "backbone" system, load on the backbone servers (by default the bungee servers) is relatively high: If you have n servers, each packet will be sent on backbone connections n * (n - 1) times. Other, non-backbone servers have comparitavely low load with only one read per packet. Because of serialization caching, the CPU usage on backbone servers is minimized, however bandwidth may still be a problem.

In live tests, the network was easily able to handle 200 packets per second without any visible problems. Performance in bigger systems is unknown.

Uses
----

This project is based on the MCStrike internal network which operates in the same fashion, though it uses custom data management and packet registration. This project has been rewritten from scratch for open-source usage.

License
-------

This library is released under [LGPL 3](https://www.gnu.org/licenses/lgpl.html).

Libraries
---------

- *Lombok* is used for convenience.
- *Apache Mahout* is used for fast implementations of primitive collections.
- *FindBugs* is used for `@Nullable` annotations not provided by lombok.
- *Log4J* is used for logging. See the `MinecraftNetManager` static constructor on how to disable it.
- *Spigot* is used for the bukkit example plugin.
- *BungeeCord* is used for the bungee example plugin.

Credit
------

Designed and programmed by [yawkat (Jonas Konrad)](http://yawk.at/). Insipiration were the bitcoin and bittorrent protocols. First made for the MCStrike network.
