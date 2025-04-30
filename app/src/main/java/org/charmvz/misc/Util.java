package org.charmvz.misc;

import java.util.SortedSet;

public class Util {

    public static String listToString(SortedSet<Integer> c) {
        int lower=-1;
        int prev=-1;
        boolean firsttime = true;

        String result = "";

        for(Integer i : c) {
            if(firsttime){
                lower = i;
                firsttime = false;
            } else {

                if(i == prev+1){
                    // extend previous range
                } else {

                    // output old range
                    if(lower == prev)
                        result += "," + lower;
                    else
                        result += "," + lower + "-" + prev;

                    // start new range
                    lower = i;

                }

            }

            prev = i;
        }

        // finish up
        if(lower == prev)
            result += "," + lower;
        else
            result += "," + lower + "-" + prev;

        // prune ',' at beginning if there is one
        if(result.charAt(0) == ',')
            result = result.substring(1);

        return result;
    }

}
