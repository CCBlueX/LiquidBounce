var script = registerScript({
    name: "Legacy Script",
    version: "1.0.0",
    authors: ["Please Update Script"]
});

script.on("enable", function () {
    try {
        onEnable();
    } catch (err) {
    }
});

script.on("disable", function () {
    try {
        onDisable();
    } catch (err) {
    }
});

script.on("load", function () {
    try {
        script.setScriptName(scriptName);
        script.setScriptVersion(scriptVersion.toString() + " §7[§4Legacy Script§7]");
        script.setScriptAuthors([scriptAuthor]);
    } catch (err) {
    }

    try {
        onLoad();
    } catch (err) {
    }
});

var _ValueAdapter = function () {

    this.values = [];

    this.add = function (value) {
        this.values.push(value);
    }

    this.getAdaptedValues = function () {
        var valuesObject = {};

        for (var i = 0; i < this.values.length; i++) {
            var currentValue = this.values[i].getValue();

            valuesObject[currentValue.getName()] = currentValue;
        }

        return valuesObject;
    }
}

var _ItemAdaptar = function () {

    this.items = [];

    this.add = function (item) {
        this.items.push(item);
    }

    this.getAdaptedItems = function () {
        return this.items;
    }
}

var _AdaptedValue = function (value) {

    this.get = function () {
        return value.get();
    }

    this.getName = function () {
        return value.getName();
    }

    this.getValue = function () {
        return value;
    }

    this.set = function (newValue) {
        value.set(newValue)
    }
}

var _AdaptedModule = function (module) {

    this.module = module;

    this.moduleManager = Java.type("net.ccbluex.liquidbounce.LiquidBounce").moduleManager;

    this.getName = function () {
        return this.module.getName();
    }

    this.getDescription = function () {
        return this.module.getDescription();
    }

    this.getCategory = function () {
        return this.module.getCategory().displayName;
    }

    this.getState = function () {
        return this.module.getState();
    }

    this.setState = function (state) {
        this.module.setState(state);
    }

    this.getBind = function () {
        return this.module.keyBind;
    }

    this.setBind = function (bind) {
        this.module.keyBind = bind;
    }

    this.getValue = function (name) {
        return new _AdaptedValue(this.module.getValue(name));
    }

    this.register = function () {
        this.moduleManager.registerModule(this.module);
    }

    this.unregister = function () {
        this.moduleManager.unregisterModule(this.module);
    }

    this._getRaw = function () {
        return this.module;
    }
}

Object.defineProperty(_AdaptedModule.prototype, "state", {
    get: function() {
        return this.module.getState();
    },
    set: function (newState) {
        this.module.setState(newState);
    }
});

Object.defineProperty(_AdaptedModule.prototype, "bind", {
    get: function() {
        return this.module.keyBind;
    },
    set: function (newBind) {
        this.module.keyBind = newBind;
    }
});

var _ModuleManager = function () {

    this.moduleManager = Java.type("net.ccbluex.liquidbounce.LiquidBounce").moduleManager;
    this.Module = Java.type("net.ccbluex.liquidbounce.features.module.Module");
    this.ArrayList = Java.type("java.util.ArrayList");

    this.registerModule = function (scriptModule) {
        var moduleConfig = {
            name: scriptModule.getName(),
            description: scriptModule.getDescription(),
            category: scriptModule.getCategory()
        };

        if (scriptModule.addValues) {
            var valueAdapter = new _ValueAdapter();
            scriptModule.addValues(valueAdapter);
            moduleConfig.settings = valueAdapter.getAdaptedValues();
        }

        if (scriptModule.getTag) {
            moduleConfig.tag = scriptModule.getTag();
        }

        script.registerModule(moduleConfig, function (module) {
            var registerEvent = function (eventName, legacyName) {
                if (scriptModule[legacyName]) {
                    module.on(eventName, function (event) {
                        scriptModule[legacyName](event);
                    });
                }
            }

            if (scriptModule.getTag) {
                var Timer = Java.type("java.util.Timer");

                var updateTagTimer = new Timer("updateTagTimer", true);
                updateTagTimer.schedule(function () {
                    module.tag = scriptModule.getTag();
                }, 500, 500);
            }

            registerEvent("update", "onUpdate");
            registerEvent("enable", "onEnable");
            registerEvent("disable", "onDisable");
            registerEvent("packet", "onPacket");
            registerEvent("motion", "onMotion");
            registerEvent("render2D", "onRender2D");
            registerEvent("render3D", "onRender3D");
            registerEvent("jump", "onJump");
            registerEvent("attack", "onAttack");
            registerEvent("key", "onKey");
            registerEvent("move", "onMove");
            registerEvent("step", "onStep");
            registerEvent("stepConfirm", "onStepConfirm");
            registerEvent("world", "onWorld");
            registerEvent("session", "onSession");
            registerEvent("clickBlock", "onClickBlock");
            registerEvent("strafe", "onStrafe");
            registerEvent("slowDown", "onSlowDown");
        });
    }

    this.unregisterModule = function (module, autoDisable) {
        if (module instanceof this.Module || module instanceof _AdaptedModule) {
            if (module instanceof _AdaptedModule)
                module = module._getRaw();

            if (autoDisable === undefined)
                autoDisable = true;

            if (autoDisable)
                module.state = false

            this.moduleManager.unregisterModule(module);
        }
    }

    this.getModule = function (name) {
        return new _AdaptedModule(this.moduleManager.getModule(name));
    }

    this.getModules = function () {
        var modules = new this.ArrayList(this.moduleManager.getModules());
        var adaptedModules = [];

        for (var i = 0; i < modules.size(); i++) {
            adaptedModules.push(new _AdaptedModule(modules[i]));
        }

        return adaptedModules;
    }
}

var _CommandManager = function () {

    this.Command = Java.type("net.ccbluex.liquidbounce.features.command.Command");
    this.commandManager = Java.type("net.ccbluex.liquidbounce.LiquidBounce").commandManager;

    this.registerCommand = function (scriptCommand) {
        script.registerCommand({
            name: scriptCommand.getName(),
            aliases: scriptCommand.getAliases()
        }, function (command) {
            command.on("execute", function (args) {
                scriptCommand.execute(args);
            });
        })
    }

    this.unregisterCommand = function (command) {
        if (command instanceof this.Command) {
            this.commandManager.unregisterCommand(command);
        }
    }

    this.executeCommand = function (command, args) {
        if (typeof command === "string") {
            this.commandManager.executeCommands(command)
        } else {
            command.execute(args);
        }
    }
}

var _CreativeTabs = function () {

    this.registerTab = function (scriptTab) {
        var itemAdapter = new _ItemAdaptar();
        scriptTab.displayAllReleventItems(itemAdapter);

        script.registerTab({
            name: scriptTab.getLabel(),
            icon: "apple",
            items: itemAdapter.getAdaptedItems()
        });
    }
}

var _Item = function () {

    this.createItem = function (args) {
        return Item.create(args);
    }
}

var _Value = function () {

    this.createBlock = function (name, value) {
        return new _AdaptedValue(Setting.block({
            name: name,
            default: value
        }));
    }

    this.createBoolean = function (name, value) {
        return new _AdaptedValue(Setting.boolean({
            name: name,
            default: value
        }))
    }

    this.createFloat = function (name, value, min, max) {
        return new _AdaptedValue(Setting.float({
            name: name,
            default: value,
            min: min,
            max: max
        }));
    }

    this.createInteger = function (name, value, min, max) {
        return new _AdaptedValue(Setting.integer({
            name: name,
            default: value,
            min: min,
            max: max
        }))
    }

    this.createList = function (name, values, value) {
        return new _AdaptedValue(Setting.list({
            name: name,
            values: values,
            default: value
        }));
    }

    this.createText = function (name, value) {
        return new _AdaptedValue(Setting.text({
            name: name,
            default: value
        }));
    }
}

var moduleManager = new _ModuleManager();
var commandManager = new _CommandManager();
var creativeTabs = new _CreativeTabs();
var item = new _Item();
var value = new _Value();
var chat = Chat;