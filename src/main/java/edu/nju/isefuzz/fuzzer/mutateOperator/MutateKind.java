package edu.nju.isefuzz.fuzzer.mutateOperator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public abstract class MutateKind {
    public abstract String mutate(ArrayList<String> sConts, int... params);

    public int[] getSomeRandomNum(int len, int step){
        int L = len;
        int S = step;
        Random rand = new Random();
        int[] numbers = new int[L];
        for(int i = 0; i < L; i++){
            numbers[i] = i;
        }

        int[] result = new int[S];
        for(int i = 0; i < S; i++){
            int r = rand.nextInt(L);
            result[i] = numbers[r];
            numbers[r] = numbers[L-1];
            --L;
        }

        Arrays.sort(result);

        return result;
    }
}
