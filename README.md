<div align="center">
<p>
    <img width="200" src="https://raw.githubusercontent.com/CCBlueX/LiquidCloud/master/LiquidBounce/liquidbounceLogo.svg">
</p>

[liquidbounce.net](https://liquidbounce.net) |
[Forum](https://forums.ccbluex.net) |
[Guilded](https://guilded.gg/CCBlueX) |
[YouTube](https://youtube.com/CCBlueX) |
[Twitter](https://twitter.com/CCBlueX)
</div>

LiquidBounce is a free and open-source mixin-based injection-hacked client using the Fabric API for Minecraft.

## Issues

If you notice any bugs or missing features, you can let us know by opening an
issue [here](https://github.com/CCBlueX/LiquidBounce/issues).

## License

This project is subject to the [GNU General Public License v3.0](https://www.gnu.org/licenses/gpl-3.0.en.html). This
does only apply for source code located directly in this clean repository. During the development and compilation
process, additional source code may be used to which we have obtained no rights. Such code is not covered by the GPL
license.

For those who are unfamiliar with the license, here is a summary of its main points. This is by no means legal advice
nor legally binding.

*Actions that you are allowed to do:*

- Use
- Share
- Modify

*If you do decide to use ANY code from the source:*

- **You must disclose the source code of your modified work and the source code you took from this project. This means
  you are not allowed to use code from this project (even partially) in a closed-source (or even obfuscated)
  application.**
- **Your modified application must also be licensed under the GPL.**

## Setting up a workspace

LiquidBounce uses Gradle; to make sure that it is installed properly, you can
check [Gradle's website](https://gradle.org/install/). It also requires Node.js and Python to be installed for
our [theme](https://github.com/CCBlueX/LiquidBounce/tree/nextgen/src-theme).

1. Clone the repository using `git clone --recurse-submodules https://github.com/CCBlueX/LiquidBounce`.
2. CD into the local repository.
3. Run `./gradlew genSources`.
4. Open the folder as a Gradle project in your preferred IDE.
5. Run the client.

## Additional libraries

### Mixins

Mixins can modify classes at runtime before they are loaded. LiquidBounce uses it to inject its code into the
Minecraft client. This way, none of Mojang's copyrighted code is shipped. If you want to learn more about it, check out
its [documentation](https://docs.spongepowered.org/5.1.0/en/plugin/internals/mixins.html).

## Contributing

We appreciate contributions. So if you want to support us, feel free to make changes to LiquidBounce's source code and
submit a pull request.

## Stats

![Alt](https://repobeats.axiom.co/api/embed/ad3a9161793c4dfe50934cd4442d25dc3ca93128.svg "Repobeats analytics image")
