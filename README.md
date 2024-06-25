# Hullabaloo
![Mod icon](/src/main/resources/assets/hullabaloo/icon.png "Mod icon")
### A tech mod with a particular focus on automation and item management
#### Vacuum Hopper
A hopper that sucks items off of the ground.

#### Mob Attractor
A block that teleports many entities (using LOTS of energy) in a large radius onto a single block.

#### Cobblestone Generator
An upgradeable cobblestone generator that can produce incredible amounts of cobblestone.

#### Block Activator
A block that can break blocks, place blocks, use items, and attack entities using energy and (optionally) tools.

# Download
There are a couple places you can download this mod from.
1. [CurseForge](https://www.curseforge.com/minecraft/mc-mods/hullabaloo)
2. [Modrinth](https://modrinth.com/mod/hullabaloo)
3. [GitHub](https://github.com/Gitko01/Hullabaloo/releases)

# Changelog (v1.0.0 -> v1.1.0)
- Fixed a bug where stored energy would not be saved in the mob attractor
- Fixed a bug where block activators would override some of each other's variables
- Removed a message that would pop up in the log each time the IO on a cobblestone generator was updated
- Tweaked the mob attractor energy consumption to be significantly cheaper
- Added shift click into the cobblestone generator upgrade slot
- Added shift click into the vacuum hopper filter slot
- Mob attractor UI tweaks
- Block activator UI tweaks
- Vacuum hopper UI tweaks
- Cobblestone generator UI tweaks
- Changed mod description, authors, and logo
- Changed mod block and item descriptions

# Bugs
Please report all bugs in this GitHub repo's issues section.

# Credits and Libraries Used
The original vacuum hopper was first created in the OpenBlocks mod.  
The original block activator (autonomous activator) was first created in the Thermal Expansion mod.  
(1.19.2, 1.19.4): [Fake Player API](https://github.com/CafeteriaGuild/fake-player-api)

# Planned Features
- None as of now

# Building Mod
1. Download source code for this mod
2. Unzip source code into a folder
3. Open build.gradle in your favorite IDE __or__ the command line
4. Run the build task in Gradle
5. Check <mod_folder>/build/libs for this mod's .jar file (usually named hullabaloo-<mod_version>+<mc_version>.jar)