import { createContext, useContext, useEffect, useMemo, useState } from "react";
import invariant from "tiny-invariant";

type CleanupFunction = () => void;
type CallbackFunction<TData> = (data: TData) => void;

type WebsocketContextType = {
  listen: <TData>(
    event: string,
    callback: CallbackFunction<TData>
  ) => CleanupFunction;
};

const WebsocketContext = createContext<WebsocketContextType | null>(null);

type WebsocketProviderProps = {
  children: React.ReactNode;
};

export function WebsocketProvider({ children }: WebsocketProviderProps) {
  const socket = useMemo(() => new WebSocket("ws://localhost:15743"), []);
  const [listeners, setListeners] = useState<
    Record<string, CallbackFunction<unknown>[]>
  >({});

  function listen<TData>(
    event: string,
    callback: CallbackFunction<TData>
  ): CleanupFunction {
    if (!listeners[event]) {
      listeners[event] = [];
    }

    listeners[event].push(callback as CallbackFunction<unknown>);
    setListeners(listeners);

    return () => {
      listeners[event] = listeners[event].filter((fn) => fn !== callback);
    };
  }

  useEffect(() => {
    function handleMessage(event: MessageEvent) {
      const { type, data } = JSON.parse(event.data);

      if (!listeners[type]) return;

      listeners[type].forEach((callback) => callback(data));
    }

    socket.addEventListener("message", handleMessage);

    return () => {
      socket.removeEventListener("message", handleMessage);
      socket.close();
    };
  }, [socket, listeners]);

  return (
    <WebsocketContext.Provider value={{ listen }}>
      {children}
    </WebsocketContext.Provider>
  );
}

export function useWebsocket() {
  const context = useContext(WebsocketContext);

  invariant(
    context !== null,
    "useWebsocket must be used within a WebsocketProvider"
  );

  return context;
}
