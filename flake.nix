{
  description = "LiquidBounce development environment";

  inputs = { nixpkgs.url = "github:NixOS/nixpkgs/nixos-25.11"; };

  outputs = { self, nixpkgs }:
    let
      system = "x86_64-linux";
      pkgs = nixpkgs.legacyPackages.${system};
      jcef_src = pkgs.fetchFromGitHub {
	  owner = "CCBlueX";
	  repo = "java-cef";
	  rev = "6a29c74d7a3ceba91b38529491b72b0826c3e20b";
	  hash = "sha256-swrnFNO6JrM6JnsRZQUCgsEhU07WB3bCgsegg7ltPZY=";
	} ;
      jcef = pkgs.callPackage jcef_src { };
      libs = with pkgs; [
        temurin-bin
        pciutils
        nodejs_24
        libpulseaudio
        libGL
        glfw
        openal
        # stdenv.cc.cc.lib
        git
        xorg.libX11
        xorg.libXcursor
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
        xorg.libxcb
        libxkbcommon
        xorg.libX11
        xorg.libXcomposite
        xorg.libXdamage
        xorg.libXext
        xorg.libXfixes
        xorg.libXrandr
        libgbm
        gtk3
        pango
        cairo
        alsa-lib
        dbus
        at-spi2-core
        cups
        xorg.libxshmfence
        wayland
        libdecor
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
