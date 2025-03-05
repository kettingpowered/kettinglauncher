[discord-widget]: https://canary.discord.com/api/guilds/1172551819138965605/widget.png
[discord-invite]: https://discord.kettingpowered.org/

[build-status]: https://img.shields.io/github/actions/workflow/status/kettingpowered/kettinglauncher/gradle-publish.yml
[build-link]: https://github.com/kettingpowered/kettinglauncher/actions

[latest-release]: https://img.shields.io/github/v/release/kettingpowered/kettinglauncher
[release-link]: https://github.com/kettingpowered/kettinglauncher/releases/latest

[issues]: https://github.com/kettingpowered/kettinglauncher/issues

<img align="right" alt="Ketting Logo" src="./assets/ketting.png?raw=true" height="150" width="150">

[![discord-widget][]][discord-invite]
[![build-status][]][build-link]
[![latest-release][]][release-link]

# Ketting Launcher

Ketting Launcher is an all-in-one launcher for all Ketting versions. This can essentially be used as a server jar, but for all versions of Ketting.

# How to use

### Before running the launcher

- Make sure that you have Java 17 installed. You can download it [here](https://adoptium.net/temurin/archive/?version=17).
- Make sure you are in a clean directory, as the launcher will download all the files it needs to run in the current directory. **Do not run the launcher jar in your downloads folder!**

### Running the launcher

1. Download the latest version of Ketting Launcher from the [releases page][release-link].
2. Make a new folder and put the launcher in it.
3. Open a terminal of choice and run the jar with
    ```sh
    java -jar kettinglauncher-X.X.X.jar -minecraftVersion <version>
    ```
    (see [launch arguments](#arguments) for more info)

# Arguments

### Note

These arguments are subject to change, and may not work in future versions. For a full list of arguments, run the launcher with `-help`

| Argument                      | Description                                       |
|-------------------------------|---------------------------------------------------|
| `-help`                       | Shows the help menu                               |
| `-noui`                       | Disables the fancy UI (DOESN'T DISABLE GUI.) [^3] |
| `-nologo`                     | Disables the big logo                             |
| `-accepteula`                 | Accepts the EULA automatically                    |
| `-dau or -daus`               | Disables automatic server updates                 |
| `-daul`                       | Disables automatic launcher updates               |
| `-createLaunchScripts`        | Creates launch scripts to launch Ketting directly |
| `-installOnly`                | Only installs the server and exits after          |
| `-minecraftVersion <version>` | Sets the Minecraft version to use [^1]            |
| `-forgeVersion <version>`     | Sets the Forge version to use [^1], [^2]          |
| `-kettingVersion <version>`   | Sets the Ketting version to use [^1], [^2]        |

| Unstable arguments:       | Description                                                                             |
|---------------------------|-----------------------------------------------------------------------------------------|
| `--launchTarget <target>` | Sets the forge launch target, this should not be used unless you know what you're doing |

All args above can also be set by setting environment variables or jvm Properties.
Let `x` be an arbitrary argument from the above lists, with the character `-` removed (e.g. `-dau` becomes `dau`). 
The corresponding Environment Variable to that argument would be: `kettinglauncher_` followed by `x` (e.g. `kettinglauncher_dau` in case of the earlier example).
The corresponding jvm Property to that argument would be: `kettinglauncher.` followed by `x` (e.g. `kettinglauncher.dau` in case of the earlier example).

There is also the jvm Property `kettinglauncher.debug`.
That cannot be set any other way, due to java limitations and how the launcher is laid out.
We do not recommend activating that, since it activates a ton of extra information, that is most of the time useless.

### What if my I don't know how / my hosting provider does not allow me to use arguments?

You can create a file called `mcversion.txt` in the same directory as the jar, and put the Minecraft version in there. This will be used as the Minecraft version and will take priority over the `-minecraftVersion` argument.

# FAQ

### Why is this launcher not on the official Ketting website?

This will be added in the future, when I have time to create a new website.

### My server is not starting, what do I do?

Before you create an issue, check if you followed all steps in the [how to use](#how-to-use) section. If you did, create an issue on the [issue tracker][issues].

### I found a bug, what do I do?

**Note:** For any bugs not related to the launcher itself, please create an issue on the respective repository. E.g. Ketting-1-20-x.
<br>
Make sure that the bug is not already reported on the [issue tracker][issues]. If it is not, create a new issue with as much information as possible.

### I have a question that is not listed here, what do I do?

Feel free to join our [Discord server][discord-invite] and ask your question there.

[^1]: only if Ketting supports it

[^2]: This argument will not update the Minecraft Version. If the current Server Minecraft Version is detected to be `1.20.1`, but e.g. `forgeVersion` is set to `48.1.0`, it won't update to `1.20.2`.

[^3]: `-noui` disables the code that prints all the versions of the server. `-noui` implies `-nologo`. If you instead want to disable the gui of the minecraft server, use `-nogui`. The Argument `-nogui` is not listed here, since it's an argument, which is handled by the Minecraft server.
