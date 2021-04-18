const valueParser = {
    parseValue(value) {
        const v = value.getValue$liquidbounce();

        switch (typeof v) {
            case "boolean": {
                return {
                    type: "boolean",
                    name: value.getName(),
                    value: v,
                    setValue: value.setValue$liquidbounce
                };
            }
            case "object": {
                switch (true) {
                    case v.toString().includes("Color4b"): {
                        return {
                            type: "color",
                            name: value.getName(),
                            value: `rgba(${v.getR()}, ${v.getG()}, ${v.getB()}, ${255 / v.getA()})`
                        };
                    }
                }

                break;
            }
            case "array": {
                break;
            }
            default: {
                console.log(typeof v);
            }
        }
    },
    parseRange(value) {
        const v = value.getValue$liquidbounce();
        const range = value.getRange();

        if (typeof v === "number") {
            return {
                type: "range",
                name: value.getName(),
                min: range.getStart(),
                max: range.getEndInclusive(),
                step: 0.1, // TODO: use step
                value1: v,
                value2: null,
                setValue1: v.setValue$liquidbounce,
                setValue2: null
            };
        } else {
            return {
                type: "range",
                name: value.getName(),
                min: range.getStart(),
                max: range.getEndInclusive(),
                step: 0.1, // TODO: Range get step
                value1: v.getStart(),
                value2: v.getEndInclusive(),
                setValue1: range.setStart,
                setValue2: range.setEndInclusive
            };
        }
    },
    parseList(value) {
        const possibleValues = [];
        const choices = value.getChoices();
        for (let i = 0; i < choices.length; i++) {
            possibleValues.push(choices[i]);
        }

        return {
            type: "list",
            name: value.getName(),
            values: possibleValues,
            value: value.getValue$liquidbounce().getChoiceName()
        };
    },
    parseConfigurable(value) {
        const possibleValues = [];
        const choices = value.getChoices();
        for (let i = 0; i < choices.length; i++) {
            possibleValues.push(choices[i]);
        }

        return {
            type: "list",
            name: value.getName(),
            values: possibleValues,
            value: value.getActive()
        };
    },
    parse(values) {
        const parsedValues = [];
        const excludedValues = ["hidden", "enabled", "bind"];

        for (let i = 0; i < values.length; i++) {
            const v = values[i];
            const valueName = v.getName();
            const className = v.toString().split("@").shift();

            if (excludedValues.includes(valueName)) {
                continue;
            }

            let parsed;

            switch (className) {
                case "net.ccbluex.liquidbounce.config.Value": {
                    parsed = valueParser.parseValue(v);

                    break;
                }
                case "net.ccbluex.liquidbounce.config.RangedValue": {
                    parsed = valueParser.parseRange(v);

                    break;
                }
                case "net.ccbluex.liquidbounce.config.ChooseListValue": {
                    parsed = valueParser.parseList(v);

                    break;
                }
                case "net.ccbluex.liquidbounce.config.ChoiceConfigurable": {
                    parsed = valueParser.parseConfigurable(v);

                    break;
                }
                default: {
                    // console.log(className);
                }
            }

            if (parsed) {
                parsedValues.push(parsed);
            }
        }

        return parsedValues;
    }
};