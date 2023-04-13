package com.example.solidapp;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;


public class Computation {

    /**
     * perform DGHV computation
     *
     * encryptIdentifier   the encrypted identifier of the target file
     * files               all the existing files on Solid Pod under /crypto
     * pk0                 the first array item of the public key , use for add/multiply
     *                     All calculation mod pk0
     */
    private ArrayList<String> encryptIdentifier; // target identifier
    private HashMap<String, String> files;  // <plaintext_identifier, encrypted_content>
    private String pk0;  // the first array item of the public key
    private BigInteger pk0_gmp;  // public key 0 in BigInteger version
    private ArrayList<String> allIdentifiers = new ArrayList<>(); // all identifiers in the store, plaintext
    private ArrayList<ArrayList<BigInteger>> filesContent = new ArrayList<>(); // all the file content


    public Computation(ArrayList<String> encryptIdentifier, HashMap<String, String> files, String pk0) {
        this.encryptIdentifier = encryptIdentifier;
        this.files = files;
        this.pk0 = pk0;
        extractPlaintextID();
        extractFileContent();
        pk0_gmp = new BigInteger(this.pk0);
    }

    public void extractPlaintextID() {
        Iterator iterator = files.keySet().iterator();
        while (iterator.hasNext()) {
            allIdentifiers.add((String) iterator.next());
        }
    }

    public void extractFileContent() {
        Iterator iterator = files.keySet().iterator();

        while (iterator.hasNext()) {
            String f = files.get(iterator.next()); // full text of a file
            ArrayList<BigInteger> word = new ArrayList<>();
            String[] fs = f.split("\n"); // every bits

            for (String s : fs) {
                s = s.replaceAll("[^0-9]", "");
                BigInteger line = new BigInteger(s);
                word.add(line);
            }
            filesContent.add(word);
        }
    }

    // STEP ONE C_i + V_i + 1
    public ArrayList<ArrayList<BigInteger>> AddingStep() {
        ArrayList<BigInteger> target = new ArrayList<>();
        for (String s : encryptIdentifier) {
            String s1 = s.substring(0, s.length() - 1);
            BigInteger i = new BigInteger(s1);
            target.add(i);
        }

        ArrayList<ArrayList<BigInteger>> dictionary = new ArrayList<>();
        for (String id : allIdentifiers) {

            String idi = id.substring(0, id.length() - 4); // get rid of .txt

            ArrayList<BigInteger> ids = new ArrayList<>();
            for (int i = 0; i < idi.length(); i++) {
                char c = idi.charAt(i);
                if (c == '0') ids.add(new BigInteger("0"));
                else if (c == '1') ids.add(new BigInteger("1"));
            }
            dictionary.add(ids);
        }
        /**
         *  add                   multiple
         *  1.   a + b = c        1. a * b = c
         *  2.   c = c mod pk0    2. c = c mod pk0
         */
        ArrayList<ArrayList<BigInteger>> result = new ArrayList<>();
        BigInteger one = new BigInteger("1");
        BigInteger two = new BigInteger("2");

        for (ArrayList<BigInteger> plaintextID : dictionary) {
            ArrayList<BigInteger> add_result = new ArrayList<>();
            for (int i = 0; i < plaintextID.size(); i++) {
                BigInteger i_target = target.get(i);  // ci
                //   System.out.println("ci: "+i_target);
                BigInteger i_plaintextID = plaintextID.get(i);  // vi
                //   System.out.println("vi: "+i_plaintextID);
                //  BigInteger k = new BigInteger(String.valueOf(i_target.add(i_plaintextID).add(one).mod(pk0_gmp).mod(two)));
                BigInteger k = new BigInteger(String.valueOf((i_target.add(i_plaintextID).add(one)).mod(pk0_gmp)));// do not mod 2
                //   System.out.println("k:  "+k);
                add_result.add(k);
            }
            result.add(add_result);
        }
        return result;
    }

    // STEP TWO calculate localizer
    public ArrayList<BigInteger> ANDStep(ArrayList<ArrayList<BigInteger>> addResult) {
        ArrayList<BigInteger> andResult = new ArrayList<>();

        for (ArrayList<BigInteger> number : addResult) {
            BigInteger sum = number.get(0);
            for (int i = 1; i < number.size(); i++) {
                sum = sum.multiply(number.get(i)).mod(pk0_gmp);
            }
            //    System.out.println("sum "+ sum);
            andResult.add(sum);
        }
        return andResult;
    }

    // STEP THREE times localizer to file
    public ArrayList<ArrayList<BigInteger>> TimesStep(ArrayList<BigInteger> localizers) {

        ArrayList<ArrayList<BigInteger>> result = new ArrayList<>();

        for (int i = 0; i < localizers.size(); i++) {
            ArrayList<BigInteger> currentResult = new ArrayList<>();
            BigInteger localizer = localizers.get(i);
            ArrayList<BigInteger> file = filesContent.get(i);

            for (BigInteger bit : file) {
                BigInteger k = bit.multiply(localizer).mod(pk0_gmp);
                currentResult.add(k);
            }
            result.add(currentResult);
        }

        return result;
    }

    // STEP FOUR add up file bits according to their position
    public ArrayList<BigInteger> addupStep(ArrayList<ArrayList<BigInteger>> timesResult) {
        int index = findmaxIndex(timesResult);
        ArrayList<BigInteger> result = timesResult.get(index);
        timesResult.remove(index);

        for (ArrayList<BigInteger> list : timesResult) {
            for (int i = 0; i < list.size(); i++) {
                BigInteger cur = list.get(i);
                BigInteger sum = result.get(i);
                BigInteger addup = cur.add(sum).mod(pk0_gmp);

                result.remove(i);
                result.add(i, addup);
            }
        }
      //  System.out.println(result);

        return result;
    }

    public int findmaxIndex(ArrayList<ArrayList<BigInteger>> timesResult) {
        int max = -1;
        int index = -1;
        for (ArrayList<BigInteger> l : timesResult) {
            int len = l.size();
            if (len > max) {
                max = len;
                index = timesResult.indexOf(l);
            }
        }
        return index;
    }

    /**
     * Get the computation final result
     *
     * @return encrypted target file sequence
     */
    public ArrayList<String> getResult() {

        ArrayList<ArrayList<BigInteger>> addResult = AddingStep();
        ArrayList<BigInteger> andResult = ANDStep(addResult);  // localizer of every file
        ArrayList<ArrayList<BigInteger>> timesResult = TimesStep(andResult);
        ArrayList<BigInteger> finalResult = addupStep(timesResult);

        ArrayList<String> result = new ArrayList<>();
        for (BigInteger i: finalResult) {
            result.add(i.toString());
        }

        return result;
    }
}
