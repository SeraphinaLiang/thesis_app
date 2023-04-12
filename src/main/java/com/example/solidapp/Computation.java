package com.example.solidapp;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;


public class Computation {

    /**
     * perform DGHV computation
     * <p>
     * encryptIdentifier   the encrypted identifier of the target file
     * files               all the existing files on Solid Pod under /crypto
     * pk0                 the first array item of the public key , use for add/multiply
     */
    private ArrayList<String> encryptIdentifier; // target identifier
    private HashMap<String, String> files;  // <plaintext_identifier, encrypted_content>
    private String pk0;
    private BigInteger pk0_gmp;
    private ArrayList<String> allIdentifiers = new ArrayList<>(); // all identifiers in the store, plaintext
    private ArrayList<String> fileContents = new ArrayList<>();


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
            String f = files.get(iterator.next());
            fileContents.add(f);
        }
    }

    // C_i + V_i + 1
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

    //  calculate localizer
    public ArrayList<BigInteger> AND_Result(ArrayList<ArrayList<BigInteger>> addResult) {
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


    /**
     * Get the computation final result
     *
     * @return encrypted target file sequence
     */
    public ArrayList<String> getResult() {

        ArrayList<ArrayList<BigInteger>> addResult = AddingStep();
        ArrayList<BigInteger> andResult = AND_Result(addResult);  // localizer of every file



        ArrayList<String> result = new ArrayList<>();
        result.add("hello");
        result.add("world");

        return result;
    }
}
