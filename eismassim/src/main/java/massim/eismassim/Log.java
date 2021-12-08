package massim.eismassim;

/**
 * Very simple Logger.
 */
public abstract class Log {

    public static void log(String message){
        System.out.println(message);
    }

    public static void flog(String format, Object... args){
        System.out.printf(format, args);
    }
}
