const IN_DEV = false;
const DEV_PORT = 15000;

export const REST_BASE = IN_DEV ? `http://localhost:${DEV_PORT}` : window.location.origin;

export const WS_BASE = IN_DEV ? `ws://localhost:${DEV_PORT}` : `ws://${window.location.host}`;
