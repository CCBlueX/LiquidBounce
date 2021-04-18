const valueParser = {
    parseBoolean(value) {
        return {
            type: "boolean",
            name: value.getName(),
            value: value.get(),
            setValue: value.set
        };
    },
    parseChoice(value) {
        const possibleValues = [];
        const choicesIterator = value.getChoices();
        if (choicesIterator.length) {
            console.log("Is array");
        } else {
            console.log("Is not array");
        }
/*         while (choicesIterator.hasNext()) {
            possibleValues.push(choicesIterator.next().getName());
        }
 */
        return {
            type: "list",
            name: value.getName(),
            values: possibleValues,
            value: "None"
        };
    },
    parseToggleable(value) {
        return {
            type: "boolean",
            name: value.getName(),
            value: value.get(),
            setValue: value.set
        };
    },
    parseIntRange(value) {
        const range = value.getRange();
        const v = value.get();
        return {
            type: "range",
            name: value.getName(),
            min: range.getStart(),
            max: range.getEndInclusive(),
            step: 1,
            value1: v.getStart(),
            value2: v.getEndInclusive(),
            setValue1: (newValue) => {
                value.set(kotlin.intRange(newValue | 0, v.getEndInclusive()))
            },
            setValue2: (newValue) => {
                value.set(kotlin.intRange(v.getStart(), newValue | 0));
            }
        };
    },
    parse(values) {
        const parsedValues = [];
        const excludedValues = ["hidden", "enabled", "bind"];

        for (let i = 0; i < values.length; i++) {
            const v = values[i];
            const valueName = v.getName();
            const valueType = v.getValueType().toString();

            if (excludedValues.includes(valueName)) {
                continue;
            }

            let parsed;
            switch (valueType) {
                case "BOOLEAN": {
                    parsed = valueParser.parseBoolean(v);
                    break;
                }
/*                 case "CHOICE": {
                    parsed = valueParser.parseChoice(v);
                    break;
                } */
                case "INT_RANGE": {
                    parsed = valueParser.parseIntRange(v);
                    break;
                }
                case "TOGGLEABLE": {
                    parsed = valueParser.parseToggleable(v);
                    break;
                }
                default: {
                    //console.log(valueType);
                }
            }

            if (parsed) {
                parsedValues.push(parsed);
            }
        }

        return parsedValues;
    }
};