package com.mstniy.kelimeezber;

import java.util.HashMap;

public class SwedishLexicographicalComparator {
    public static int compare(String a, String b) {
        HashMap<Character, Integer> charMap = new HashMap<>();
        charMap.put('a',  0);
        charMap.put('ä',  0);
        charMap.put('å',  0);
        charMap.put('b',  1);
        charMap.put('c',  2);
        charMap.put('d',  3);
        charMap.put('e',  4);
        charMap.put('f',  5);
        charMap.put('g',  6);
        charMap.put('h',  7);
        charMap.put('i',  8);
        charMap.put('j',  9);
        charMap.put('k', 10);
        charMap.put('l', 11);
        charMap.put('m', 12);
        charMap.put('n', 13);
        charMap.put('o', 14);
        charMap.put('ö', 14);
        charMap.put('p', 15);
        charMap.put('q', 16);
        charMap.put('r', 17);
        charMap.put('s', 18);
        charMap.put('t', 19);
        charMap.put('u', 20);
        charMap.put('v', 21);
        charMap.put('w', 22);
        charMap.put('x', 23);
        charMap.put('y', 24);
        charMap.put('z', 25);
        charMap.put('-', 26);
        charMap.put('.', 27);

        int minLength = a.length();
        if (b.length() < minLength)
            minLength = b.length();
        for (int i = 0; i < minLength; i++) {
            Integer codeA = charMap.get(Character.valueOf(Character.toLowerCase(a.charAt(i))));
            Integer codeB = charMap.get(Character.valueOf(Character.toLowerCase(b.charAt(i))));
            if (codeA == null)
                codeA = Integer.valueOf(charMap.size());
            if (codeB == null)
                codeB = Integer.valueOf(charMap.size());
            if (codeA != codeB)
                return (codeA < codeB)?-1:1;
        }
        if (a.length() < b.length())
            return -1;
        else if (a.length() == b.length())
            return 0;
        else
            return 1;
    }
}
