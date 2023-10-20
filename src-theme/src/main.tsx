import React from "react";
import ReactDOM from "react-dom/client";

import { RouterProvider, createBrowserRouter } from "react-router-dom";

import Root from "./root.tsx";

import SplashScreen from "./routes/index.tsx";
import MenuWrapper from "./routes/_menus.tsx";
import TitleScreen from "./routes/_menus/title.tsx";

import Singleplayer from "./routes/_menus/singleplayer.tsx";
import Multiplayer from "./routes/_menus/multiplayer.tsx";

import AccountManager from "./routes/_menus/accounts.tsx";
import ProxyManager from "./routes/_menus/proxies.tsx";

import ClickGUI from "./routes/clickgui.tsx";

import "./globals.css";

const router = createBrowserRouter([
  {
    path: "/",
    element: <Root />,
    children: [
      {
        path: "/",
        element: <SplashScreen />,
      },
      {
        element: <MenuWrapper />,
        children: [
          {
            path: "/title",
            element: <TitleScreen />,
          },
          {
            path: "/singleplayer",
            element: <Singleplayer />,
          },
          {
            path: "/multiplayer",
            element: <Multiplayer />,
          },
          {
            path: "/accounts",
            element: <AccountManager />,
          },
          {
            path: "/proxies",
            element: <ProxyManager />,
          },
        ],
      },
      {
        path: "/clickgui",
        element: <ClickGUI />,
      },
    ],
  },
]);

ReactDOM.createRoot(document.getElementById("root") as HTMLElement).render(
  <React.StrictMode>
    <RouterProvider router={router} />
  </React.StrictMode>
);
