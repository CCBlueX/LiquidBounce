declare const client: Client;

type Client = {
  getSessionService: () => SessionService;
  getModuleManager: () => ModuleManager;
  isUpdateAvailable: () => boolean;
};

type SessionService = {
  getUsername: () => string;
  getFaceUrl: () => string;
  getAccountType: () => string;
  getLocation: () => string;
};

type ModuleManager = {
  getCategories: () => string[];
  iterator: () => ModuleIterator;
};

type ModuleIterator = {
  hasNext: () => boolean;
  next: () => Module;
};

type Module = {
  getCategory: () => Category;
  getName: () => string;
  getEnabled: () => boolean;
};

type Category = {
  getReadableName: () => string;
};
