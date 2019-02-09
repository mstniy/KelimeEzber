package com.mstniy.kelimeezber;

import java.util.HashMap;

public class SwedishLexicographicalComparator {
    public static boolean compare(String a, String b) { // < comparsion
        HashMap<Character, Integer> charMap = new HashMap<>();
        charMap.put('a',  0);
        charMap.put('ä',  1);
        charMap.put('å',  2);
        charMap.put('b',  3);
        charMap.put('c',  4);
        charMap.put('d',  5);
        charMap.put('e',  6);
        charMap.put('f',  7);
        charMap.put('g',  8);
        charMap.put('h',  9);
        charMap.put('i', 10);
        charMap.put('j', 11);
        charMap.put('k', 12);
        charMap.put('l', 13);
        charMap.put('m', 14);
        charMap.put('n', 15);
        charMap.put('o', 16);
        charMap.put('ö', 17);
        charMap.put('p', 18);
        charMap.put('q', 19);
        charMap.put('r', 20);
        charMap.put('s', 21);
        charMap.put('t', 22);
        charMap.put('u', 23);
        charMap.put('v', 24);
        charMap.put('w', 25);
        charMap.put('x', 26);
        charMap.put('y', 27);
        charMap.put('z', 28);
        charMap.put('-', 29);
        charMap.put('.', 30);

        int minLength = a.length(); // Awkward. API level 23 doesn't have integer min?
        if (b.length() < minLength)
            minLength = b.length();
        for (int i=0; i<minLength; i++) {
            Integer codeA = charMap.get(Character.toLowerCase(a.charAt(i)));
            Integer codeB = charMap.get(Character.toLowerCase(b.charAt(i)));
            if (codeA == null)
                codeA = 31;
            if (codeB == null)
                codeB = 31;
            if (codeA != codeB)
                return codeA < codeB;
        }
        return a.length() < b.length();
    }
}
