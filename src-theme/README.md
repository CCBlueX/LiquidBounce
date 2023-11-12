# LiquidBounce Official Theme

Welcome to the LiquidBounce Theme!
This is our default theme, which is being used in our minecraft hacked-client LiquidBounce.

## Roadmap

[GitHub Issue Roadmap](https://github.com/CCBlueX/LiquidBounce/issues/1025)

### Concepts:

Our [Nextgen concepts](https://github.com/CCBlueX/LiquidBounce/tree/nextgen/src-theme/concepts). They follow the same
design guidelines and can be found in the `src-theme\concepts` folder.

We use Adobe XD as a design tool, but you can also open find the concepts as a PNG file under the folder with the same
name.

- [x] Main Menu
- [x] Singleplayer
- [x] Multiplayer
- [x] Alt Manager
- [x] Proxy Manager (https://github.com/CCBlueX/LiquidBounce/issues/391)
- [ ] Configuration (Scripts, Mods, Themes...)
- [ ] Marketplace (https://github.com/CCBlueX/LiquidBounce/issues/1024)
- [x] ClickGUI
- [x] ClickGUI search bar (https://github.com/CCBlueX/LiquidBounce/issues/800)
- [x] HUD

### Implementation

We are lacking Web DEVs, and therefore, the progress of implementing these design concepts into our client is a slow
progress.
If you are capable to work with Svelte and JS and have a good understanding of turning design concepts into HTML and
CSS, contact us on our support@liquidbounce.net email or on GitHub (the faster way).

- [x] Main Menu
- [ ] Singleplayer
- [ ] Multiplayer
- [ ] Alt Manager
- [ ] Proxy Manager (https://github.com/CCBlueX/LiquidBounce/issues/391)
- [ ] Configuration (Scripts, Mods, Themes...)
- [ ] Marketplace (https://github.com/CCBlueX/LiquidBounce/issues/1024)
- [x] ClickGUI (Basics + Values)
- [x] ClickGUI search bar (https://github.com/CCBlueX/LiquidBounce/issues/800)
- [x] HUD (Basics)
- [ ] HUD (Elements)

## Building

To build the themes, you need to have Node.js installed.
Then run `node bundle.py` to build the theme.

### Testing

To test the theme in development, you can make use of the development server using:

```bash
npm i
npm run dev
```

and go into LiquidBounce and open the local web server using: \

```
.client ultralight show http://localhost:10004/clickgui/public/
```

This will show the page and automatically updates when you make changes to the source code.

### Release

This theme is directly linked to our client and is being updated with every release.
It will automatically be built and added to the client resources.

## Contributing

If you want to contribute to this theme, you can do so by creating a pull request. \
We will review it, and if it fits our design guidelines, we will merge it.

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
- **Your modified application must also be licensed under the GPL** 
