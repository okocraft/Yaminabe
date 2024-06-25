# Yaminabe

Yaminabe is a Paper plugin for providing *various* features.

## Requirements

- Java 21
- Paper or Folia 1.21+

## Build

### Getting Jar

```shell
./gradlew build
```

Then, `Yaminabe-x.x.x.jar` is located in the `build/libs` directory.

### Run server for debugging

This project can be run a single Minecraft server for debugging.

```shell
./gradlew runServer
```

After executing this command, Gradle starts a Paper server with LuckPerms and this plugin.
You can join at the address `localhost:25560`.
The server directory is `plugin/run/`.

NOTE: When you run a Minecraft server, you indicate your agreement to [Minecraft Eula](https://www.minecraft.net/en-us/eula).

## License

This project is under the GPL-3.0 license. Please see [LICENSE](LICENSE) for more info.

Copyright © 2024, OKOCRAFT
