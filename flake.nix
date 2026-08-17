{
  description = "LiquidBounce development environment";

  inputs = { nixpkgs.url = "github:NixOS/nixpkgs/nixos-26.05"; };

  outputs = { self, nixpkgs }:
    let
      system = "x86_64-linux";
      pkgs = nixpkgs.legacyPackages.${system};

      # nixpkgs freetype 2.14.2 compiles the harfbuzz shaper in by default
      # (FT_CONFIG_OPTION_USE_HARFBUZZ) and dlopens libharfbuzz.so.0 at runtime.
      # The modern harfbuzz (12/13) pulled in by pango/cairo/gtk3 is ABI-
      # incompatible with that 2023-era shaper, which segfaults the JVM in
      # sun.font (FT_Load_Glyph -> af_shaper_get_coverage_hb) while rendering
      # LiquidBounce's font atlas, crashing runClient. Rebuild freetype with
      # the harfbuzz shaper disabled so it never touches harfbuzz.
      freetype = pkgs.freetype.overrideAttrs (o: {
        configureFlags = (o.configureFlags or [ ]) ++ [ "--without-harfbuzz" ];
      });

      jcef_src = pkgs.fetchFromGitHub {
        owner = "CCBlueX";
        repo = "java-cef";
        rev = "aa20e50dbfb858ea50d3cf405b8202462dd10d96";
        hash = "sha256-gLDiARy35KixTnS/G8U5NQvm2hjz4yl+O6dgnWNrGMY=";
      };
      jcef = pkgs.callPackage jcef_src { };
      libs = with pkgs; [
        alsa-lib
        atk
        at-spi2-atk
        at-spi2-core
        cairo
        cups
        dbus
        expat
        flite
        git
        glib
        glfw
        gtk3
        libGL
        libX11
        libXcomposite
        libXcursor
        libXdamage
        libdrm
        libXext
        libXfixes
        libgbm
        libpulseaudio
        libxcb
        libxkbcommon
        libxshmfence
        libXrandr
    
        # CEF (chromium) dependencies
        # libcef
    
        nodejs_24
        nspr
        nss
        openal
        pango
        pciutils
    
        # Provides libstdc++.so.6 / libgcc_s.so.1 needed by LWJGL's bundled
        # libopenal.so at runtime (without it runClient crashes on startup).
        stdenv.cc.cc.lib
    
        temurin-bin-25
        wayland
      ];
      # Must come BEFORE the JDK on the library path so the JDK's libfontmanager
      # resolves libfreetype.so to the harfbuzz-free build above instead of the
      # bundled/stock one (which crashes on modern harfbuzz).
      libraryPath = pkgs.lib.makeLibraryPath ([ freetype ] ++ libs);

    in {
      devShells.${system}.default = pkgs.mkShell {
        packages = [ freetype ] ++ libs;
        buildInputs = libs;

        LD_LIBRARY_PATH = "${freetype}/lib:${libraryPath}";
        PROVIDED_JCEF_PATH = "${jcef}";
      };
    };
  nixConfig.bash-prompt-suffix = "[liquidbounce] ";
}
