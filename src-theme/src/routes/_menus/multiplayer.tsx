import Fuse from "fuse.js";
import { useMemo, useState } from "react";
import { useQuery } from "react-query";

import Button from "~/components/button";
import Switch from "~/components/switch";

import {
  Footer,
  Header,
  List,
  ListItem,
  Menu,
  SearchBar,
} from "~/features/menus";

import ServerEntry from "~/features/menus/multiplayer/server-entry";
import VersionSelector from "~/features/menus/multiplayer/version-selector";

import { getServers } from "~/utils/api";

// Left Footer Actions
import { ReactComponent as Add } from "~/assets/icons/add.svg";
import { ReactComponent as DirectConnect } from "~/assets/icons/direct-connect.svg";
import { ReactComponent as Change } from "~/assets/icons/change.svg";
import { ReactComponent as Refresh } from "~/assets/icons/refresh.svg";

export function Component() {
  const {
    status,
    data: servers,
    error,
    refetch,
  } = useQuery("servers", getServers);
  const [search, setSearch] = useState("");
  const [favoritesOnly, setFavoritesOnly] = useState(false);
  const [currentVersion, setCurrentVersion] = useState(false);
  const [online, setOnline] = useState(false);
  const [selectedVersion, setSelectedVersion] = useState<string>("1.20.1");

  const filteredServers = useMemo(() => {
    if (!search || !servers) {
      return servers;
    }

    const fuse = new Fuse(servers, {
      keys: ["name", "address"],
    });

    return fuse.search(search).map((result) => result.item);
  }, [search, servers]);

  return (
    <Menu>
      <Menu.Content>
        <Header>
          <SearchBar onChange={setSearch} />
          <Switch value={favoritesOnly} onChange={setFavoritesOnly}>
            Favorites Only
          </Switch>
          <Switch value={currentVersion} onChange={setCurrentVersion}>
            Current Version
          </Switch>
          <Switch value={online} onChange={setOnline}>
            Online
          </Switch>
          <VersionSelector
            currentVersion={selectedVersion}
            onChange={setSelectedVersion}
          />
        </Header>
        <List loading={status === "loading"}>
          {filteredServers?.map((server) => (
            <ServerEntry
              key={`${server.name}-${server.icon}`}
              server={server}
            />
          ))}
        </List>
      </Menu.Content>
      <Footer>
        <Footer.Actions>
          <Button icon={Add}>Add</Button>
          <Button icon={DirectConnect}>Direct</Button>
          <Button icon={Refresh} onClick={refetch}>
            Refresh
          </Button>
          <Button icon={Change}>Change Client Brand</Button>
        </Footer.Actions>
        <Footer.Back />
      </Footer>
    </Menu>
  );
}

Component.displayName = "MultiplayerMenu";
