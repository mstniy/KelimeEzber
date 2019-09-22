package com.mstniy.kelimeezber;

import java.io.Serializable;

class Pair implements Serializable {
    public long id;
    public String first;
    public String second;
    public int period;
    public int next;

    public Pair(long _id, String _first, String _second, int _period, int _next) {
        id = _id;
        first = _first;
        second = _second;
        period = _period;
        next = _next;
    }
}
