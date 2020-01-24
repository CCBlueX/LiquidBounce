package net.ccbluex.liquidbounce.utils.render;

public class AnimationUtils {

    /**
     * In-out-easing function
     * https://github.com/jesusgollonet/processing-penner-easing
     * @param t Current iteration
     * @param b Starting value
     * @param c Change in value
     * @param d Total iterations
     * @return Eased value
     */
    public static float easeOut(float t, float b, float c, float d) {
        return c * ((t = t / d - 1) * t * t + 1) + b;
    }
}
