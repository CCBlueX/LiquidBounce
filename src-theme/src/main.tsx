import React from "react";
import ReactDOM from "react-dom/client";

import { RouterProvider, createHashRouter } from "react-router-dom";

import { WebsocketProvider } from "./contexts/websocket-context.tsx";

import "./globals.css";

const router = createHashRouter([
  {
    path: "/",
    lazy: () => import("./root.tsx"),
    children: [
      {
        path: "/",
        lazy: () => import("./routes/index.tsx"),
      },
      {
        lazy: () => import("./routes/_menus.tsx"),
        children: [
          {
            path: "/title",
            lazy: () => import("./routes/_menus/title.tsx"),
          },
          {
            path: "/singleplayer",
            lazy: () => import("./routes/_menus/singleplayer.tsx"),
          },
          {
            path: "/multiplayer",
            lazy: () => import("./routes/_menus/multiplayer.tsx"),
          },
          {
            path: "/accounts",
            lazy: () => import("./routes/_menus/accounts.tsx"),
          },
          {
            path: "/proxies",
            lazy: () => import("./routes/_menus/proxies.tsx"),
          },
        ],
      },
      {
        path: "/clickgui",
        lazy: () => import("./routes/clickgui.tsx"),
      },
      {
        path: "/modmenu",
        lazy: () => import("./routes/modmenu.tsx"),
      },
      {
        path: "/hud",
        lazy: () => import("./routes/hud.tsx"),
      },
    ],
  },
]);

ReactDOM.createRoot(document.getElementById("root") as HTMLElement).render(
  <React.StrictMode>
    <WebsocketProvider>
      <RouterProvider router={router} />
    </WebsocketProvider>
  </React.StrictMode>
);
