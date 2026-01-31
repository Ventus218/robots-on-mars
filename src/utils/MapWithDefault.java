package src.utils;

import java.util.HashMap;

public class MapWithDefault<K, V> extends HashMap<K, V> {

    private V def;

    public MapWithDefault(V def) {
        super();
        this.def = def;
    }

    @Override
    public V get(Object key) {
        return super.getOrDefault(key, def);
    }
}
