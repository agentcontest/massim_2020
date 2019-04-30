package massim.util;

/**
 * Class for random helper methods.
 */
public abstract class Util {

    public static Integer tryParseInt(String maybeInt) {
        try {
            return Integer.parseInt(maybeInt);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
