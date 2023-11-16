import Fuse from "fuse.js";
import { useMemo, useState } from "react";

import Button from "~/components/button";
import Combobox, { Option } from "~/components/combobox";
import Switch from "~/components/switch";

import Footer from "~/features/menus/footer";
import Header from "~/features/menus/header";
import Menu from "~/features/menus/menu";

import WorldEntry from "~/features/menus/singleplayer/world-entry";
import { useWorlds } from "~/features/menus/singleplayer/use-worlds";

// Left Footer Buttons
import { ReactComponent as Add } from "~/assets/icons/add.svg";
import List from "~/features/menus/list";
import SearchBar from "~/features/menus/searchbar";

export default function Singleplayer() {
  const { worlds } = useWorlds();
  const [search, setSearch] = useState("");
  const [favoritesOnly, setFavoritesOnly] = useState(false);
  const [gameModes, setGameModes] = useState<Option[]>([
    {
      label: "Survival",
      value: "survival",
    },
    {
      label: "Creative",
      value: "creative",
    },
    {
      label: "Adventure",
      value: "adventure",
    },
    {
      label: "Spectator",
      value: "spectator",
    },
  ]);
  const [difficulties, setDifficulties] = useState<Option[]>([
    {
      label: "Easy",
      value: "easy",
    },
    {
      label: "Normal",
      value: "normal",
    },
    {
      label: "Hard",
      value: "hard",
    },
    {
      label: "Peaceful",
      value: "peaceful",
    },
  ]);
  const [worldTypes, setWorldTypes] = useState<Option[]>([
    {
      label: "Default",
      value: "default",
    },
    {
      label: "Superflat",
      value: "superflat",
    },
    {
      label: "Large Biomes",
      value: "large_biomes",
    },
  ]);

  const filteredWorlds = useMemo(() => {
    if (!search) return worlds;

    const fuse = new Fuse(worlds, {
      keys: ["name"],
    });

    return fuse.search(search).map((result) => result.item);
  }, [search, worlds]);

  return (
    <Menu>
      <Menu.Content>
        <Header>
          <SearchBar onChange={setSearch} />
          <Switch value={favoritesOnly} onChange={setFavoritesOnly}>
            Favorites Only
          </Switch>
          <Combobox
            options={gameModes}
            onToggle={(option) =>
              setGameModes((prev) =>
                prev.map((item) =>
                  item.value === option.value
                    ? { ...item, checked: !item.checked }
                    : item
                )
              )
            }
          >
            Game Mode
          </Combobox>
          <Combobox
            options={difficulties}
            onToggle={(option) =>
              setDifficulties((prev) =>
                prev.map((item) =>
                  item.value === option.value
                    ? { ...item, checked: !item.checked }
                    : item
                )
              )
            }
          >
            Difficulty
          </Combobox>
          <Combobox
            options={worldTypes}
            onToggle={(option) =>
              setWorldTypes((prev) =>
                prev.map((item) =>
                  item.value === option.value
                    ? { ...item, checked: !item.checked }
                    : item
                )
              )
            }
          >
            World Type
          </Combobox>
        </Header>
        <List>
          {filteredWorlds.map((world) => (
            <WorldEntry key={world.name} world={world} />
          ))}
        </List>
      </Menu.Content>
      <Footer>
        <Footer.Actions>
          <Button icon={Add}>Add</Button>
        </Footer.Actions>
        <Footer.Back />
      </Footer>
    </Menu>
  );
}
