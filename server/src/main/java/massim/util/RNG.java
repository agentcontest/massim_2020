package massim.util;

import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Implements the random number generation (wraps standard Java Random for now).
 */
public abstract class RNG {

    private static Random random = new Random(System.currentTimeMillis());

    /**
     * Initializes the rng to the given seed.
     * @param seed the seed for the rng
     */
    public static synchronized void initialize(long seed){
        random = new Random(seed);
    }

    /**
     * @see Random#nextInt()
     */
    public static synchronized int nextInt(){
        return random.nextInt();
    }

    /**
     * (upper bound  exclusive)
     * @see Random#nextInt(int)
     */
    public static synchronized int nextInt(int bound){
        return random.nextInt(bound);
    }

    /**
     * @see Random#nextDouble()
     */
    public static synchronized double nextDouble(){ return random.nextDouble(); }

    /**
     * Shuffles a list with the internal random object.
     * @see Collections#shuffle
     * @param list the list to shuffle
     */
    public static synchronized void shuffle(List<?> list){
        Collections.shuffle(list, random);
    }

    public static synchronized int betweenClosed(int lower, int upper){
        return lower + nextInt(upper - lower + 1);
    }
}
