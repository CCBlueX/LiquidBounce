#  This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
#
#  Copyright (c) 2016 - 2023 CCBlueX
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

# Go through every folder and copy public folder to tmp folder with name of folder

import os
import shutil

# Make function that builds theme before copying it to tmp folder
def npm_build(f):
    print("Building theme " + f)
    os.system("cd " + f + " && npm i && npm run build")

# Delete theme.zip
if os.path.exists("theme.zip"):
    os.remove("theme.zip")

# Delete tmp folder
if os.path.exists("tmp"):
    shutil.rmtree("tmp")

# Copy theme bundle to jar structure
if os.path.exists("resources"):
    shutil.rmtree("resources")

# Go through every folder and copy public folder to tmp folder with name of folder
for folder in os.listdir("."):
    if os.path.isdir(folder):
        # Check if theme has package.json
        if os.path.exists(folder + "/package.json"):
            # Build theme
            npm_build(folder)
        
        # Check if theme has public folder to copy
        if os.path.exists(folder + "/public"):
            # Copy theme to tmp folder
            shutil.copytree(folder + "/public", "tmp/" + folder)
        else:
            print("Folder " + folder + " has no public folder")

# Zip into theme bundle
shutil.make_archive("theme", "zip", "tmp")

# Delete tmp folder
if os.path.exists("tmp"):
    shutil.rmtree("tmp")

# Copy theme bundle to jar structure
if os.path.exists("resources"):
    shutil.rmtree("resources")

# Create folder structure
os.makedirs("resources/assets/liquidbounce")
shutil.copy("theme.zip", "resources/assets/liquidbounce/default_theme.zip")
