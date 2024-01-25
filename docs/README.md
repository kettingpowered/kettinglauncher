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

| Argument                        | Description                                                                             |
|---------------------------------|-----------------------------------------------------------------------------------------|
| `-help`                         | Shows the help menu                                                                     |
| `-noui`                         | Disables the fancy UI                                                                   |
| `-nologo`                       | Disables the big logo                                                                   |
| `-accepteula`                   | Accepts the EULA automatically                                                          |
| `-dau or -daus`                 | Disables automatic server updates                                                       |
| `-daul`                         | Disables automatic launcher updates                                                     |
| `-installOnly`                  | Only installs the server and exits after                                                |
| `-minecraftVersion <version>`   | Sets the Minecraft version to use (if Ketting supports it)                              |

| Unstable arguments:       | Description                                                                             |
|---------------------------|-----------------------------------------------------------------------------------------|
| `--launchTarget <target>` | Sets the forge launch target, this should not be used unless you know what you're doing |

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