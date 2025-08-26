# Project LiquidBounce Coding Standards

We invite everyone to participate in the development of LiquidBounce by submitting pull requests and opening issues in
this repository. However, we have to enforce certain standards to keep our code readable, consistent and easier to
maintain.

We kindly ask you to use [Kotlin](https://kotlinlang.org/) instead of Java for new code, if possible. In the long term,
it is our goal to largely migrate LiquidBounce to Kotlin.

Contributors: https://github.com/CCBlueX/LiquidBounce/graphs/contributors

## General

This section lists the official conventions of the languages Kotlin and Java. This project tries to follow them as
closely as possible, and we expect outside developers to do the same when working on the client.

**Additional, non-standard conventions will be listed below and must also be followed.**

### Kotlin

* Follow Kotlin's
  official [code conventions](https://kotlinlang.org/docs/reference/coding-conventions.html#coding-conventions).
* Have a look at Kotlin's official [documentation](https://kotlinlang.org/docs/reference/).

### Java

* Have a look at Oracle's [Java Code PDF document](https://www.oracle.com/technetwork/java/codeconventions-150003.pdf).
* Read the Wikipedia article on [Java's Syntax](https://en.wikipedia.org/wiki/Java_syntax).
* Look at Oracle's [Java Tutorial](https://docs.oracle.com/javase/tutorial/java/).

# Files

### Generation

To document the ownership of a file, we include the following text in all code files *(.kt and .java)* at the beginning
of the file:

```kotlin
/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
```

### Tags

You are allowed to use the `@author <author-name>` tag, but try to limit its usage as much as possible.

You are recommended to use the following tags for bypasses:

`@anticheat <anticheat-name>`
`@anticheatVersion <anticheat-version>`
`@testedOn <anticheat-test-server>`
`@note <note-text>` - used for a special comments on a bypass

You aren't allowed to use any other tags.

# Packages

### Naming

Our naming of packages follows the following format:

* `country.company-name.product-name`

*Example:*

* `net.ccbluex.liquidbounce`

If your code is self-contained and not designed exclusively for LiquidBounce, we may allow you to include it in a
separate package outside `net.ccbluex.liquidbounce`. Please note that we have to decide on a case by case basis.

*Example:*
`net.vitox` instead of `net.ccbluex`

Links:

* [Java Package](https://en.wikipedia.org/wiki/Java_package "Wikipedia article").
