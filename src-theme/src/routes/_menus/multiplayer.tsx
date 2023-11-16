import Fuse from "fuse.js";
import { useMemo, useState } from "react";

import Button from "~/components/button";
import Switch from "~/components/switch";

import Footer from "~/features/menus/footer";
import Header from "~/features/menus/header";
import List from "~/features/menus/list";
import Menu from "~/features/menus/menu";

import ServerEntry from "~/features/menus/multiplayer/server-entry";
import { useServers } from "~/features/menus/multiplayer/use-servers";
import VersionSelector from "~/features/menus/multiplayer/version-selector";

// Left Footer Actions
import { ReactComponent as Add } from "~/assets/icons/add.svg";
import { ReactComponent as DirectConnect } from "~/assets/icons/direct-connect.svg";
import { ReactComponent as Change } from "~/assets/icons/change.svg";
import { ReactComponent as Refresh } from "~/assets/icons/refresh.svg";
import SearchBar from "~/features/menus/searchbar";

export default function Multiplayer() {
  const { servers } = useServers();
  const [search, setSearch] = useState("");
  const [favoritesOnly, setFavoritesOnly] = useState(false);
  const [currentVersion, setCurrentVersion] = useState(false);
  const [online, setOnline] = useState(false);
  const [selectedVersion, setSelectedVersion] = useState<string>("1.20.1");

  const filteredServers = useMemo(() => {
    if (!search) return servers;

    const fuse = new Fuse(servers, {
      keys: ["name", "motd", "ip"],
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
        <List>
          {filteredServers.map((server) => (
            <ServerEntry key={server.name} server={server} />
          ))}
        </List>
      </Menu.Content>
      <Footer>
        <Footer.Actions>
          <Button icon={Add}>Add</Button>
          <Button icon={DirectConnect}>Direct</Button>
          <Button icon={Refresh}>Refresh</Button>
          <Button icon={Change}>Change Client Brand</Button>
        </Footer.Actions>
        <Footer.Back />
      </Footer>
    </Menu>
  );
}
