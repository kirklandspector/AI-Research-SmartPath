
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.Collections;
import java.util.Scanner;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * class TestNewAgent
 *
 * This is a class devoted solely to testing that the new algorithms
 * (getAlignedMatchesScore() and getCountingScore())
 * introduced in NewAgent.java are working correctly.
 *
 * @author: Sara Meisburger and Christine Chen
 *
 */

public class TestNewAgent {

    /**
     * main
     * IN PROGRESS
     */
    public static void main(String[] args) {
        //variables
        NewAgent ourAgent = new NewAgent();
        //original and found sequence variables
        Episode[] original = null;
        Episode[] found = null;

        //expected value variables
        double expectedValAlig = 0.0;
        double expectedValCounting = 0.0;

        //test1: give both algorithms sequences that are exactly the same
        //expected result: both algorithms should return a score of 1

        char[] charArray = {'b', 'a', 'c', 'a', 'a', 'c', 'b', 'b'};

        //set up original and found sequences
        original = charsToEpisodes(charArray);
        found = charsToEpisodes(charArray);

        //set up expected values
        expectedValAlig = 1.0;
        expectedValCounting = 1.0;

        double actualValAlig = round(ourAgent.getAlignedMatchesScore(original, found),2);
        double actualValCounting = round(ourAgent.getCountingScore(original, found),2);

        if (expectedValAlig == actualValAlig &&
                expectedValCounting == actualValCounting) {
            System.out.println("***********************************************************\n" +
                    "Test 1 (Identical Original/Found Sequences): Success!\n" +
                    "Expected and Actual Counting Score: " + expectedValCounting +
                    "\nExpected and Actual Aligned Matches Score: " + expectedValAlig + "\n");
        } else {
            System.out.println("***********************************************************\n" +
                    "Test 1 (Identical Original/Found Sequences): FAILURE!\n" +
                    "Expected Counting Score: " + expectedValCounting + "\n" +
                    "Actual Counting Score: " + actualValCounting + "\n" +
                    "Expected Aligned Matches Score: " + expectedValAlig + "\n" +
                    "Actual Aligned Matches Score: " + actualValAlig + "\n");
        }

        //test2///////////////////////////////////////////////////////////////////////////////

        char[] charArrayOrig = {'a', 'b', 'a', 'b', 'a', 'c', 'c', 'c'};
        char[] charArrayFound = {'c', 'a', 'b', 'a', 'b', 'a', 'c', 'c'};

        //set up original and found sequences
        original = charsToEpisodes(charArrayOrig);
        found = charsToEpisodes(charArrayFound);

        //set up expected values
        expectedValAlig = .25;
        expectedValCounting = 0.71;

        actualValAlig = round(ourAgent.getAlignedMatchesScore(original, found),2);
        actualValCounting = round(ourAgent.getCountingScore(original, found),2);

        if (expectedValAlig == actualValAlig &&
                expectedValCounting == actualValCounting) {
            System.out.println("***********************************************************\n" +
                    "Test 2 (Different Original/Found Sequences): Success!\n" +
                    "Expected and Actual Counting Score: " + expectedValCounting +
                    "\nExpected and Actual Aligned Matches Score: " + expectedValAlig + "\n");
        } else {
            System.out.println("***********************************************************\n" +
                    "Test 2 (Different Original/Found Sequences): FAILURE!\n" +
                    "Expected Counting Score: " + expectedValCounting + "\n" +
                    "Actual Counting Score: " + actualValCounting + "\n" +
                    "Expected Aligned Matches Score: " + expectedValAlig + "\n" +
                    "Actual Aligned Matches Score: " + actualValAlig + "\n");
        }


        //test3

        //test4
    }

    /**
     * charsToEpisodes
     *
     * @param chars array of chars that needs to be converted to Episodes
     *              (note, this method does not check that the chars are valid)
     * @return array of Episodes
     */
    private static Episode[] charsToEpisodes(char[] chars) {
        //get length of char array
        int arrayLength = chars.length;

        //initialize array of Episodes
        Episode[] eps = new Episode[arrayLength];

        //fill eps array
        for (int i = 0; i < arrayLength; i++) {
            //don't care what the sensor value is
            eps[i] = new Episode(chars[i], 0);
        }

        return eps;
    }

    /**
     * round
     * @param value value to round
     * @param places number of decimal places to round value to
     * @return rounded value
     *
     * method courtesy of Jonik on StackOverflow
     * http://stackoverflow.com/questions/2808535/round-a-double-to-2-decimal-places
     */
    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}