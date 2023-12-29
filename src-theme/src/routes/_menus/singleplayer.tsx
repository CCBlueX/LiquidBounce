import Fuse from "fuse.js";
import { useMemo, useState } from "react";
import { useQuery } from "react-query";

import Button from "~/components/button";
import Combobox, { Option } from "~/components/combobox";
import Switch from "~/components/switch";

import { Footer, Header, List, Menu, SearchBar } from "~/features/menus";

import WorldEntry from "~/features/menus/singleplayer/world-entry";

// Left Footer Buttons
import { ReactComponent as Add } from "~/assets/icons/add.svg";

import { World, getWorlds } from "~/utils/api";

function useWorlds() {
  return useQuery<World[], Error>("worlds", getWorlds);
}

export function Component() {
  const { status, data: worlds, error } = useWorlds();
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

  let filteredWorlds = useMemo(() => {
    if (!search || !worlds) {
      return worlds;
    }

    const fuse = new Fuse(worlds, {
      keys: ["name"],
    });

    return fuse.search(search).map((result) => result.item);
  }, [search, worlds]);

  // if (favoritesOnly) {
  //   filteredWorlds = filteredWorlds?.filter((world) => world.favorite);
  // }

  if (gameModes.some((mode) => mode.checked)) {
    filteredWorlds = filteredWorlds?.filter((world) =>
      gameModes.some((mode) => mode.checked && mode.value === world.gameMode)
    );
  }

  if (difficulties.some((difficulty) => difficulty.checked)) {
    filteredWorlds = filteredWorlds?.filter((world) =>
      difficulties.some(
        (difficulty) =>
          difficulty.checked && difficulty.value === world.difficulty
      )
    );
  }

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
        <List loading={status === "loading"} error={error?.message}>
          {filteredWorlds?.map((world) => (
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

Component.displayName = "SingleplayerMenu";
