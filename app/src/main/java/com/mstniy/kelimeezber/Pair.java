package com.mstniy.kelimeezber;

class Pair {
    public String first;
    public double hardness;

    public long id;
    public int period;
    public String second;

    public Pair(long _id, String _first, String _second, double _hardness, int _period) {
        id = _id;
        first = _first;
        second = _second;
        hardness = _hardness;
        period = _period;
    }
}
