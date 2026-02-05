package src.utils;

public record V2D(double x, double y) {

    public V2D div(double a) {
        return mult(1 / a);
    }

    public V2D mult(double a) {
        return new V2D(x * a, y * a);
    }

    public V2D plus(V2D v) {
        return new V2D(x + v.x(), y + v.y());
    }

    public V2D minus(V2D v) {
        return plus(v.mult(-1));
    }

    public double module() {
        return Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));
    }

    public V2D versor() {
        return div(module());
    }

    public double dot(V2D v) {
        return x * v.x() + y * v.y();
    }

    public double distanceTo(V2D v) {
        return Math.sqrt(Math.pow(x - v.x(), 2) + Math.pow(y - v.y(), 2));
    }
}
