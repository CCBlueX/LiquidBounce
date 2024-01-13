package net.janrupf.ujr.example.glfw;

import net.janrupf.ujr.api.javascript.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class is used to demonstrate how to create a custom JavaScript class.
 * <p>
 * Please note that this depicts a rather low level API. Once the Ultralight Java Reborn API is more mature, there
 * will be an alternative way to create custom JavaScript classes.
 */
public class CustomJavaScriptClass {
    private static final Logger LOGGER = LogManager.getLogger();

    private static JSClass cachedJsClass;

    /**
     * Retrieves the JavaScript class for instances of this object.
     *
     * @return the JavaScript class for instances of this object
     */
    public static JSClass getJavaScriptClass() {
        if (cachedJsClass == null) {
            cachedJsClass = new JSClassBuilder(CustomJavaScriptClass.class.getName())
                    .onInitialize(CustomJavaScriptClass::onInitialize)
                    .onFinalize(CustomJavaScriptClass::onFinalize)
                    .onHasProperty(CustomJavaScriptClass::onHasProperty)
                    .onGetProperty(CustomJavaScriptClass::onGetProperty)
                    .onSetProperty(CustomJavaScriptClass::onSetProperty)
                    .onDeleteProperty(CustomJavaScriptClass::onDeleteProperty)
                    .onCallAsFunction(CustomJavaScriptClass::onCallAsFunction)
                    .onCallAsConstructor(CustomJavaScriptClass::onCallAsConstructor)
                    .onConvertToType(CustomJavaScriptClass::onConvertToType)
                    .build();
        }

        return cachedJsClass;
    }

    private static void onInitialize(JSContext context, JSObject object) {
        // This method is called when a new instance of this class is created in JavaScript.
        // You can use this to initialize the object.
        LOGGER.info("Initializing JavaScript object {} in context {}", object, context);
    }


    private static void onFinalize(JSObject object) {
        // This method is called when the JavaScript object is garbage collected.
        // You can use this to clean up resources.

        // NOTE: DO NOT CALL ANYTHING RELATED TO JSContext IN THIS METHOD!
        LOGGER.info("Finalizing JavaScript object {}", object);
    }

    private static boolean onHasProperty(JSContext context, JSObject object, String name) {
        if (name.equals("Symbol.toPrimitive")) {
            // This is important in this case! since log4j is calling toString() on the object,
            // this leads to infinite recursion if we don't return false here.
            return false;
        }

        // This method is called when a property is accessed on the JavaScript object.
        // You can use this to implement custom properties.
        LOGGER.info("Checking if JavaScript object {} has property {} in context {}", object, name, context);

        return name.equals("testProperty");
    }

    private static JSValue onGetProperty(JSContext context, JSValue object, String name) throws JavaScriptValueException {
        // This method is called when a property is accessed on the JavaScript object.
        // You can use this to implement custom properties.
        LOGGER.info("Getting property {} of JavaScript object {} in context {}", name, object, context);

        if (name.equals("testProperty")) {
            return context.makeString("Hello from Java!");
        }

        throw new JavaScriptValueException(context.makeError("Property not found"), "Property not found");
    }

    private static boolean onSetProperty(JSContext context, JSObject object, String name, JSValue value) throws JavaScriptValueException {
        // This method is called when a property is set on the JavaScript object.
        // You can use this to implement custom properties.
        LOGGER.info("Setting property {} of JavaScript object {} to {} in context {}", name, object, value, context);

        // This example only allows setting the property "testProperty",
        // returning false will cause JavaScriptCore to delegate the property to the prototype chain.
        return name.equals("testProperty");
    }

    private static boolean onDeleteProperty(JSContext context, JSObject object, String name) throws JavaScriptValueException {
        // This method is called when a property is deleted on the JavaScript object.
        // You can use this to implement custom properties.
        LOGGER.info("Deleting property {} of JavaScript object {} in context {}", name, object, context);

        // This example only allows deleting the property "testProperty",
        // returning false will cause JavaScriptCore to delegate the property to the prototype chain.

        // NOTE: We simply ignore the delete operation here
        return name.equals("testProperty");
    }

    private static JSValue onCallAsFunction(
            JSContext context,
            String functionName,
            JSObject function,
            JSObject thisObject,
            JSValue[] arguments
    ) throws JavaScriptValueException {
        // This method is called when the JavaScript object is called as a function.
        // You can use this to implement custom functions.
        LOGGER.info("Calling function {} of JavaScript object {} in context {}", functionName, function, context);

        return context.makeString("Hello from Java!");
    }

    private static JSObject onCallAsConstructor(JSContext context, JSObject constructor, JSValue[] arguments) throws JavaScriptValueException {
        // This method is called when the JavaScript object is called as a constructor.
        // You can use this to implement custom constructors.
        LOGGER.info("Calling constructor of JavaScript object {} in context {}", constructor, context);

        return context.makeFromJSONString("{\"testProperty\": \"Hello from Java!\"}").toObject();
    }

    private static JSValue onConvertToType(JSContext context, JSObject object, JSType type) throws JavaScriptValueException {
        // This method is called when the JavaScript object is converted to a different type.
        // You can use this to implement custom conversions.

        // DO NOT attempt to print the object here, this will lead to infinite recursion!

        if (type == JSType.STRING) {
            return context.makeString("CustomJavaJavaScriptObject");
        }

        throw new JavaScriptValueException(context.makeError("Conversion not supported"), "Conversion not supported");
    }
}