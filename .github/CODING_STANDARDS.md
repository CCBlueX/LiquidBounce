# Contributing code to the LiquidBounce codebase

Everyone is welcome to contribute their code via pull requests and to file issues on GitHub via our [issues repository](https://github.com/CCBlueX/LiquidBounce1.8-Issues "Redirects to https://github.com.");
however we must enforce certain standards to keep our code more readable and easier to mantain.

Most of our code is written in Kotlin, still some features like LiquidBounce's mixin-injection are written in Java. 

## General

In this section, the official conventions of Kotlin and Java are listed. Our project LiquidBounce follows Kotlin's and Java's coding conventions. 

Below this section are our own rules defined, apply them always.

### Kotlin
 
* Follow 
[Kotlin's official coding conventions](https://kotlinlang.org/docs/reference/coding-conventions.html#coding-conventions 
"Redirects to https://kotlinlang.org.")!
* Have a look at 
[Kotlin's official documentation](https://kotlinlang.org/docs/reference/ 
"Redirects to https://kotlinlang.org.").

### Java

* Have a look at 
[Oracle's Java Code PDF document](https://www.oracle.com/technetwork/java/codeconventions-150003.pdf "It's a pdf document.").
* Visit the wikipedia article named 
[Java Syntax](https://en.wikipedia.org/wiki/Java_syntax).
* Look at 
[Oracle's Java Tutorial](https://docs.oracle.com/javase/tutorial/java/).

# Files

### Generation

To document ownership, we include the following text in all code files *(.kt and .java)* at the beginning of the file:

` Line 0: Start of text `
```kotlin
/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
 
 ```
` Line 6: End of text `
 
### Tags
`@author <author-name>` tags are allowed, but minimize its usage as much as possible.

# Packages

### Naming

Our naming of packages follows the following format:

`my.package.name`

`country.company-name.product-name`

Example: 

`net.ccbluex.liquidbounce`

*   If others send us their own code outside of GitHub and agree with an implementation into LiquidBounce's codebase, 
    we put their work into different root packages.
    
    Example:
    
    `net.vitox` instead of `net.ccbluex`
    
 Links:

* [Java Package](https://en.wikipedia.org/wiki/Java_package "It's a wikipedia article.").
