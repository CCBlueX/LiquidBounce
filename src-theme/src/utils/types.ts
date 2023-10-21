export type Server = {
  name: string;
  players: number;
  latency: number;
  motd: string;
  ip: string;
  icon: string;
};

export type World = {
  name: string;
  lastPlayed: string;
  icon: string;
  difficulty: string;
  gameMode: string;
  worldType: string;
};

export type Proxy = {
  type: string;
  location: string;
  host: string;
  port: number;
  username: string;
  password: string;
  direct: boolean;
};

export type Account = {
  username: string;
  uuid: string;
  email: string;
  password?: string;
};
