package entrypoint.utils;

public class NanosConverter {

    public static long normalize(long nanos) {
        return (nanos / 1000L) * 1000L;
    }

}
