{
  description = "LiquidBounce development environment";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-25.05";
  };

  
  outputs = { self, nixpkgs }:
  let
      system = "x86_64-linux";
      pkgs = nixpkgs.legacyPackages.${system};
      jcef=pkgs.callPackage (
            pkgs.fetchFromGitHub {
            owner = "superblaubeere27";
            repo = "java-cef";
            rev = "de279dda3d4861f4b412708e3ca36fab35fb7a90";
            hash = "sha256-YS9gzqHQbKhYvHjllCTfYQxiIZ0JDWC3wwmSLA4RvXc=";
            }
      ) {};
      libs = with pkgs; [
            neovim
            temurin-bin
	    pciutils
            nodejs_24
	    libpulseaudio
	    libGL
	    glfw
	    openal
	    stdenv.cc.cc.lib
	    git
	    jetbrains.idea-community-bin
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
      ];

  in
  {
    devShells.${system}.default = pkgs.mkShell {
      packages = [];
      buildInputs = libs;
      shellHook = "echo ${jcef}";

      LD_LIBRARY_PATH = pkgs.lib.makeLibraryPath libs;
      PROVIDED_JCEF_PATH = "${jcef}";
    };
  };
}
