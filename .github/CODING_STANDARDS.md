# Project LiquidBounce Coding Standards
Everyone is invited to participate in the development of LiquidBounce with pull requests and to open issues on our [separate repository](https://github.com/CCBlueX/LiquidBounce1.8-Issues). However, we have to enforce certain standards to keep our code readable, consistent and easier to maintain.

We kindly ask you to use [Kotlin](https://kotlinlang.org/) instead of Java for new code, if possible. In the long term, it is our goal to largely migrate LiquidBounce to Kotlin.

Contributors: https://github.com/CCBlueX/LiquidBounce/graphs/contributors

## General
This section lists the official conventions of the languages Kotlin and Java. This project tries to follow them as closely as possible and we expect outside developers to do the same when working on the client.

**Additional, non-standard conventions are listed below. These must also be followed.**

### Kotlin
* Follow Kotlin's official [code conventions](https://kotlinlang.org/docs/reference/coding-conventions.html#coding-conventions).
* Have a look at Kotlin's official [documentation](https://kotlinlang.org/docs/reference/).

### Java
* Have a look at Oracle's [Java Code PDF document](https://www.oracle.com/technetwork/java/codeconventions-150003.pdf).
* Read the Wikipedia article on [Java's Syntax](https://en.wikipedia.org/wiki/Java_syntax).
* Look at Oracle's [Java Tutorial](https://docs.oracle.com/javase/tutorial/java/).

# Rewriting
If parts of the codebase that are currently still written in Java can be ported to Kotlin without changing its behaviour, you are welcome to do so. However, please do not simply rely on IntelliJ's auto-conversion feature, but improve the generated code if necessary.

# Files
### Generation

To document the ownership of a file, we include the following text in all code files *(.kt and .java)* at the beginning of the file:
```kotlin
/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
```
 
### Tags
`@author <author-name>` tags are allowed, but minimize its usage as much as possible.

No other tags are allowed.

# Packages
### Naming
Our naming of packages follows the following format:
* `country.company-name.product-name`

*Example:* 
* `net.ccbluex.liquidbounce`

If your code is self-contained and not designed exclusively for LiquidBounce, we may allow you to include it in a separate package outside `net.ccbluex.liquidbounce`. Please note that we have to decide on a case by case basis.
  
*Example:*
`net.vitox` instead of `net.ccbluex`

Links:

* [Java Package](https://en.wikipedia.org/wiki/Java_package "Wikipedia article").
