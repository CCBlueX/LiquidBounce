#  This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
#
#  Copyright (c) 2015 - 2023 CCBlueX
#
#  LiquidBounce is free software: you can redistribute it and/or modify
#  it under the terms of the GNU General Public License as published by
#  the Free Software Foundation, either version 3 of the License, or
#  (at your option) any later version.
#
#  LiquidBounce is distributed in the hope that it will be useful,
#  but WITHOUT ANY WARRANTY; without even the implied warranty of
#  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
#  GNU General Public License for more details.
#
#  You should have received a copy of the GNU General Public License
#  along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.

# Go through every folder and copy public folder to tmp folder with the name of folder

import os
import shutil

# Delete theme.zip
if os.path.exists("theme.zip"):
    os.remove("theme.zip")

# Delete tmp folder
if os.path.exists("tmp"):
    shutil.rmtree("tmp")

# Copy theme bundle to jar structure
if os.path.exists("resources"):
    shutil.rmtree("resources")

print("Building theme")
os.system("npm i && npm run build")

# Copy theme.json into tmp folder
shutil.copy("theme.json", "dist/theme.json")

# Zip into a theme bundle
shutil.make_archive("theme", "zip", "dist")

# Copy a theme bundle to jar structure
if os.path.exists("resources"):
    shutil.rmtree("resources")

# Create folder structure
os.makedirs("resources/assets/liquidbounce")
shutil.copy("theme.zip", "resources/assets/liquidbounce/default_theme.zip")
