package edu.nju.isefuzz.fuzzer.mutateOperator;

import java.util.ArrayList;
import java.util.Random;

public class EasyMut extends MutateKind{

    public EasyMut(){}

    @Override
    public String mutate(ArrayList<String> sConts, int... params) {
        Random rand = new Random();
        int pos = 0;
        int step = 0;
        if(params.length == 0){
            pos = rand.nextInt(sConts.get(0).length());
            step = rand.nextInt(26);
        }else if (params.length != 2) {
            System.out.println("EasyMut 参数传入错误");
            return null;
        }else {
            pos = params[0];
            step = params[1];
        }

        String sCont = sConts.get(0);
        char[] charArr = sCont.toCharArray();
        char oriChar = charArr[pos];

        // Mutate this char and make sure the result is in [a-z].
        char mutChar = oriChar + step > 'z' ?
                (char) ((oriChar + step) % 'z' - 1 + 'a') :
                (char) (oriChar + step);

        // Replace the char and return offspring test input.
        charArr[pos] = mutChar;
        return new String(charArr);
    }
}

