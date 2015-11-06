
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

public class TestNewAgent
{

//    //instance variables
//    protected NewAgent ourAgent;
//
//    /**
//     * TestNewAgent()
//     *
//     * Constructor that initializes ourAgent variable
//     */
//    public TestNewAgent()
//    {
//
//        ourAgent = new NewAgent();
//
//
//    }

    /**
     * main
     * IN PROGRESS
     */
    public static void main(String[] args)
    {
        //variables
        NewAgent ourAgent = new NewAgent();
        Episode[] original = null;
        Episode[] found = null;

        double expectedValAlig = 0.0;
        double expectedValCounting = 0.0;

        //test1: give both algorithms sequences that are exactly the same
        //expected result: both algorithms should return a score of 1

        char[] charArray = {'a','a','a','a','a','a','a','a'};

        //set up values
        original = charsToEpisodes(charArray);
        found = charsToEpisodes(charArray);

        expectedValAlig = 1.0;
        expectedValCounting = 1.0;

        if(expectedValAlig == ourAgent.getAlignedMatchesScore(original, found) &&
                expectedValCounting == ourAgent.getCountingScore(original, found))
        {
            System.out.println("YAY!");
        }
        else
        {
            System.out.println("NO!");
        }

        //test2

        //test3

        //test4
    }

    /**
     * charsToEpisodes
     * @param chars array of chars that needs to be converted to Episodes
     * (note, this method does not check that the chars are valid)
     * @return array of Episodes
     */
    private static Episode[] charsToEpisodes (char[] chars)
    {
        //get length of char array
        int arrayLength = chars.length;

        //initialize array of Episodes
        Episode[] eps = new Episode[arrayLength];

        //fill eps array
        for (int i = 0; i<arrayLength; i++)
        {
            //don't care what the sensor value is
            eps[i] = new Episode (chars[i],0);
        }

        return eps;
    }



}