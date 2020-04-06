# Contributing code to the LiquidBounce codebase

Everyone is welcome to contribute their code via pull requests and to file issues on GitHub via our [issues repository](https://github.com/CCBlueX/LiquidBounce1.8-Issues);
however we must enforce certain standards to keep our code more readable and easier to mantain.

Most of our code is written in Kotlin.  

# General

### Kotlin

To make things easier for you: 
* Follow [Kotlin's official coding conventions](https://kotlinlang.org/docs/reference/coding-conventions.html#coding-conventions).
* Have a look at [Kotlin's documentation](https://kotlinlang.org/docs/reference/).

### Java

Some features like LiquidBounce's mixin-injection is written in Java. 
There is a wikipedia article about Java's syntax: [Java Syntax](https://en.wikipedia.org/wiki/Java_syntax).

# File Generation

To document ownership, we include the following text in all code files *(.kt and .java)*:

```kotlin
/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
 ```
 
`@author <author-name>` tags are allowed, but keep it to a minimum and do not fight about who the original author is.
