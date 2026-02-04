package src.utils;

public record Tuple<A, B>(A _1, B _2) {
    public static <A, B> Tuple<A, B> of(A a, B b) {
        return new Tuple<>(a, b);
    }
}
