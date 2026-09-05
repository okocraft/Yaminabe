# Yaminabe

Yaminabe provides *various* features for Paper and Velocity.

## Requirements

- Java 25
- One of the following platforms:
  - Paper or Folia 26.1+
  - Velocity 4.1+

## Build

### Getting Jar

```shell
./gradlew build
```

Then, platform-specific jars such as `Yaminabe-Paper-x.x.x.jar` and `Yaminabe-Velocity-x.x.x.jar` are located in the `build/libs` directory.

### Run Paper server for debugging

This project can be run a single Paper server for debugging.

```shell
./gradlew runServer
```

After executing this command, Gradle starts a Paper server with LuckPerms and this plugin.
You can join at the address `localhost:25560`.
The server directory is `plugin/run/`.

NOTE: When you run a Minecraft server, you indicate your agreement to [Minecraft Eula](https://www.minecraft.net/en-us/eula).

### Run Velocity proxy for debugging

This project can also run a Velocity proxy with the Velocity module loaded.

```shell
./gradlew runVelocity
```

After executing this command, Gradle starts a Velocity proxy with this plugin.

## License

This project is under the GPL-3.0 license. Please see [LICENSE](LICENSE) for more info.

Copyright © 2024-2026, OKOCRAFT
