{
  description = "LiquidBounce development environment";

  inputs = { nixpkgs.url = "github:NixOS/nixpkgs/nixos-26.05"; };

  outputs = { self, nixpkgs }:
    let
      system = "x86_64-linux";
      pkgs = nixpkgs.legacyPackages.${system};
      jcef_src = pkgs.fetchFromGitHub {
        owner = "CCBlueX";
        repo = "java-cef";
        rev = "aa20e50dbfb858ea50d3cf405b8202462dd10d96";
        hash = "sha256-gLDiARy35KixTnS/G8U5NQvm2hjz4yl+O6dgnWNrGMY=";
      };
      jcef = pkgs.callPackage jcef_src { };
      libs = with pkgs; [
        temurin-bin-25
        pciutils
        nodejs_24
        libpulseaudio
        libGL
        glfw
        openal
        # stdenv.cc.cc.lib
        git
        libX11
        libXcursor
        flite

        # CEF (chromium) dependencies
        # libcef

        libgbm
        glib
        nss
        nspr
        atk
        at-spi2-atk
        libdrm
        expat
        libxcb
        libxkbcommon
        libX11
        libXcomposite
        libXdamage
        libXext
        libXfixes
        libXrandr
        libgbm
        gtk3
        pango
        cairo
        alsa-lib
        dbus
        at-spi2-core
        cups
        libxshmfence

        wayland
      ];

    in {
      devShells.${system}.default = pkgs.mkShell {
        packages = libs;
        buildInputs = libs;

        LD_LIBRARY_PATH = pkgs.lib.makeLibraryPath libs;
        PROVIDED_JCEF_PATH = "${jcef}";
      };
    };
  nixConfig.bash-prompt-suffix = "[liquidbounce] ";
}
