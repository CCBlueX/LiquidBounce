# Project LiquidBounce Coding Standards

Everyone is welcome to contribute their code via pull requests and to file issues on GitHub via our [issues repository](https://github.com/CCBlueX/LiquidBounce1.8-Issues "Redirects to https://github.com.");
however we must enforce certain standards to keep our code more readable and easier to mantain.

Most of our code is written in Kotlin, still some features like [LiquidBounce's](https://liquidbounce.net) mixin-injection are written in Java.

Contributors: https://github.com/CCBlueX/LiquidBounce/graphs/contributors

## General

In this section, the official conventions of Kotlin and Java are listed. Our project [LiquidBounce](https://github.com/CCBlueX/LiquidBounce) follows Kotlin's and Java's official coding conventions. Note that we enforce our own coding standards.

**Our own coding standards are listed below this section, apply them always.**

### Kotlin
 
* Follow 
[Kotlin's official coding conventions](https://kotlinlang.org/docs/reference/coding-conventions.html#coding-conventions 
"Redirects to https://kotlinlang.org.").
* Have a look at 
[Kotlin's official documentation](https://kotlinlang.org/docs/reference/ 
"Redirects to https://kotlinlang.org.").

### Java

* Have a look at 
[Oracle's Java Code PDF document](https://www.oracle.com/technetwork/java/codeconventions-150003.pdf ".pdf document").
* Visit the wikipedia article named 
[Java Syntax](https://en.wikipedia.org/wiki/Java_syntax).
* Look at 
[Oracle's Java Tutorial](https://docs.oracle.com/javase/tutorial/java/).

# Rewriting

If a functionality is rewritable in Kotlin instead of Java and results in the same functionality and performance, feel free to rewrite it.

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

If others send us their own code outside of GitHub, want an own root package
and agree with an implementation into LiquidBounce's codebase, 
we put their code into different root packages.
  
*Example:*

`net.vitox` instead of `net.ccbluex`

*Note 1: Until now, this has only been applied if the submitted code contains of muliple files and improves the code or the client a lot. Smaller changes like a variable rename are not going to be applicable to this rule.*

Links:

* [Java Package](https://en.wikipedia.org/wiki/Java_package "Wikipedia article").
