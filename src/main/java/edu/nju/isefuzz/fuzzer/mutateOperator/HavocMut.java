package edu.nju.isefuzz.fuzzer.mutateOperator;

import java.util.ArrayList;
import java.util.Random;

public class HavocMut extends MutateKind{
    @Override
    public String mutate(ArrayList<String> sConts, int... params) {
        if (params.length != 0) {
            System.out.println("HavocMut 参数传入错误");
            return null;
        }

        CharFlipMut charFlipMut = new CharFlipMut();
        CharInsMut charInsMut = new CharInsMut();
        CharDelMut charDelMut = new CharDelMut();

        String sCont = sConts.get(0);
        int sCentLen = sCont.length();

        Random rand = new Random();
        String str = sCont;
        for(int i = 0; i < 5; ++i){
            int num = rand.nextInt(3);
            ArrayList<String> tempStr = new ArrayList<String>();
            tempStr.add(str);
            if(num == 0){
                int n = rand.nextInt(26);
                int L = rand.nextInt(str.length());
                int S = rand.nextInt(str.length());
                str = charFlipMut.mutate(tempStr, n, L, S);
            }else if(num == 1){
                int n = rand.nextInt(str.length()+1);
                int K = rand.nextInt(str.length());
                str = charInsMut.mutate(tempStr, n, K);
            }else {
                int n = rand.nextInt(str.length());
                int K = rand.nextInt(str.length());
                str = charDelMut.mutate(tempStr, n, K);
            }
        }

        return str;
    }
}
