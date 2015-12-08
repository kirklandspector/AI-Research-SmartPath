import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;



/**
 * class NewAgent
 *
 * This is a "trial" agent that is being used to test a new algorithm for
 * finding the shortest path to the goal. This algorithm looks at
 * sequences of length 8 in episodic memory and combines the scores from
 * the positional weight matrix, a constituency/substring match algorithm, and the
 * number of steps to the goal to find the best possible
 * next move.
 * this is a default task
 * @author: Sara Meisburger and Christine Chen
 *
 */


public class NewAgent extends StateMachineAgent
{
    //constants
    double DUPLICATE_FORGIVENESS = .25; //25% chance a duplicate is permitted
    protected static int NUM_TOP_ACTIONS = 8; //number of top scores we will keep track of
    protected static int COMPARE_SIZE = 8; //length of arrays to compare to get quality score

    //constants for scores
    private static int COUNTING_CONSTANT = 10; //multiplier for counting score
    private static int ALIGNED_CONSTANT= 10; //multiplier for aligned score

    //declare array of recommendations from Recommendation class
    private Recommendation[] topNextActions;

    //array to hold the number of recommendations for each char action based on the info from
    //topNextActions
    private int[] frequencyNextActions;

    //SUS percentage variable
    double percentSUS = 0;
    double percentRandom = 0;
    double percentQuality = 0;

    //number of runs we want to do
    protected static int NUM_RUNS = 5;

    //name of file we are saving run info in, changes based on run number
    protected static String fileName;


    //DEBUGGING
    protected static long totalMachineTime = 0;
    protected static double avgRunTimeCheckCond = 0;
    protected static double avgRunTimeFoundSeq = 0;

    /**
     * NewAgent()
     * constructor
     * calls super from StateMachine Agent
     * initializes topNextActions array and frequencyNextActions array
     */
    public NewAgent()
    {
        super();
        topNextActions = new Recommendation[NUM_TOP_ACTIONS+1];
        frequencyNextActions = new int[env.ALPHABET_SIZE];
    }


    /**
     * exploreEnvironment
     * once an agent is created it is set loose to explore its environment based on a determined number of episodes
     * has an original sequence and compares it to past sequences found in its episodic memory
     * generates a score based on matched subsequences and direct matches and stores top scores in an array
     * determines an average score from array and compares against SUS score
     * if array has higher score, do more frequently recommended char from array
     * if SUS has higher score, try SUS path
     * resets top scores array for each new original sequence
     */
    public void exploreEnvironment() {

        Episode[] originalSequence = new Episode[COMPARE_SIZE]; //initialize originalSequence
        Episode[] foundSequence = new Episode[COMPARE_SIZE]; //initialize foundSequence
        int lastGoalIndex; //index of last goal
        double susCounter = 0; //record how many times we choose SUS (%)
        double randomCounter = 0;//record how many times we choose random
        double qualityCounter = 0;//record how many times we choose quality
        double decisionCounter = 0; //counter for how many times we make a decision
        int stepsFromGoal = 0; //how far found sequence is from last goal index

        //DEBUGGING
        long sumRunTimesCheckCond = 0;
        int numCallsCheckCond = 0;
        long sumRunTimesFoundSeq = 0;
        int numFound = 0;

        //while we have not exceeded the max number of episodes, keep getting new original sequences to test
        //then make moves
        while (episodicMemory.size() < MAX_EPISODES) {
            lastGoalIndex = findLastGoal(episodicMemory.size()); //get last goal index

            //DEBUGGING/////////////////////////
            long startCheckCond = System.currentTimeMillis();
            lastGoalIndex = checkConditions(lastGoalIndex); //check conditions to ensure you can get original seq
            long endCheckCond = System.currentTimeMillis();

            sumRunTimesCheckCond = (endCheckCond-startCheckCond) + sumRunTimesCheckCond;
            numCallsCheckCond++;
            /////////////////////////////////////

            originalSequence = getOriginalSequence(); //get the original sequence of size COMPARE_SIZE

            //reset frequency arrays for each new original sequence
            resetFrequencyArrays();

            //DEBUGGING///////////////////////////////////////
            boolean metFoundCond = true;
            long startFoundCond = 0;
            //iterate through episodic memory and get found sequences of length COMPARE_SIZE
            //get a quality score for each and fill the top scores in the recommendation array
            //compare scores to SUS to determine what move to do next
            for(int w = lastGoalIndex; w >= COMPARE_SIZE; w--){

                if(metFoundCond)
                {
                    //a found sequence has been "found"...time to start over again
                    startFoundCond = System.currentTimeMillis();
                }


                int meetsFoundConditions = checkFoundConditions(w); //check that we can get a found seq
                if(meetsFoundConditions == -1){

                    //we're good to go...
                    //get the end time
                    long endFoundCond = System.currentTimeMillis();

                    //add the difference between endFoundCond and startFoundCond to sumRunTimesCheckFoundCond
                    sumRunTimesFoundSeq = sumRunTimesFoundSeq + (endFoundCond-startFoundCond);

                    //increment counter (keeps track of how many found sequences were found)
                    numFound++;

                    //set metFoundCond to true
                    metFoundCond = true;

                    foundSequence = getFoundSequence(w); //fill found sequence
                    stepsFromGoal = lastGoalIndex - w;

//                    //a found sequence has been found...
//                    //get the end time
//                    long endFoundCond = System.currentTimeMillis();
//
//                    //add the difference between endFoundCond and startFoundCond to sumRunTimesCheckFoundCond
//                    sumRunTimesFoundSeq = sumRunTimesFoundSeq + (endFoundCond-startFoundCond);
//
//                    //increment counter (keeps track of how many found sequences were found)
//                    numFound++;
//
//                    //set metFoundCond to true
//                    metFoundCond = true;

                }
                else {
                    w = meetsFoundConditions; //doesnt meet conditions, start at next goal

                    //IMPORTANT
                    metFoundCond = false;

                    continue;
                }

                //call our quality methods to get scores
                double countingScore = getCountingScore(originalSequence, foundSequence);
                double alignedMatches = getAlignedMatchesScore(originalSequence, foundSequence);

                //get a total quality score based on the two quality methods
                double tempQualityScore = (double)((COUNTING_CONSTANT)*countingScore + (ALIGNED_CONSTANT)*alignedMatches);


                //make a Recommendation object containing the score, steps to Goal, and character command
                //place in the last spot in the topNextActions array
                topNextActions[NUM_TOP_ACTIONS] = new Recommendation(tempQualityScore, stepsFromGoal, episodicMemory.get(w+1).command);

                //sort the array (array will be sorted from ascending to descending)
                Arrays.sort(topNextActions);

                //the last spot holds the Recommendation object with the lowest score, set it to null
                topNextActions[NUM_TOP_ACTIONS] = null;
            }

            double sumOfTopScores  = 0.0;
            //loop through topNextActions and record the frequencies of the different
            //recommended action chars in frequencyNextActions
            for (int i = 0; i < topNextActions.length - 1; i++)
            {
                //get the Recommendation object's recommended action
                char action = topNextActions[i].recommendedAction;

                //add the score of the recommendation object to sumOfTopEightScores
                sumOfTopScores = topNextActions[i].score + sumOfTopScores;

                //get the index of that action in the alphabet array
                int indexOfAction = findAlphabetIndex(action);

                //increment the value in frequencyNextActions[indexOfAction]
                frequencyNextActions[indexOfAction]++;
            }

            //get average of the top scores
            double avgTopScores = sumOfTopScores/NUM_TOP_ACTIONS;

            //get the SUS score to be compared
            determineSusScore();

            //loop through frequencyNextActions and determine the best next move
            int indexBestMove = 0;
            int highestFreq = 0;

            for(int j = 0; j < frequencyNextActions.length; j++)
            {
                if(frequencyNextActions[j]>highestFreq)
                {
                    indexBestMove = j;
                    highestFreq = frequencyNextActions[j];
                }
            }

            decisionCounter++;

            //if the RANDOM_SCORE is higher, do a random move
            if (RANDOM_SCORE > susScore && RANDOM_SCORE > avgTopScores) {
                String pathWeAttempt = "" + generateSemiRandomAction();
                Path finalPath = stringToPath(pathWeAttempt);
                tryPath(finalPath);
                randomCounter++;
            }
            else if (susScore > avgTopScores){
                String pathToAttempt = getSus();
                if (pathToAttempt == null)
                {
                    pathToAttempt = "" + generateSemiRandomAction();
                    susCounter--;
                }
                Path finalPath = stringToPath(pathToAttempt);
                tryPath(finalPath);
                susCounter++;
            }
            //if the avgTopScores is higher, do most frequently recommended char
            else {
                tryPath(stringToPath(Character.toString(alphabet[indexBestMove])));
                qualityCounter++;
            }
        }
        percentSUS = (susCounter/decisionCounter)*100.0;
        percentRandom = (randomCounter/decisionCounter)*100.0;
        percentQuality = (qualityCounter/decisionCounter)*100.0;


        avgRunTimeCheckCond = sumRunTimesCheckCond/(double)numCallsCheckCond;
        avgRunTimeFoundSeq = sumRunTimesFoundSeq/(double)numFound;


    }


    /**
     * ResetFrequencyArrays()
     * resets top next actions array to have dummy recommendation objects
     * resets the frequencyNextActions array
     * for each new original sequence
     */
    private void resetFrequencyArrays(){
        for (int i = 0; i < topNextActions.length; i++)
        {
            topNextActions[i] = new Recommendation(0.0, 0, alphabet[random.nextInt(alphabet.length)]);
        }

        //fill frequencyNextActions with 0s
        for (int i = 0; i < frequencyNextActions.length; i++)
        {
            frequencyNextActions[i] = 0;
        }
    }

    /*
    * getAlignedMatchesScore
    * @param originalEpisodes array of Episodes
    * @param foundEpisodes array of Episodes to compare against original episodes
    * @return scoreToReturn number of directly aligned matches divided by COMPARE_SIZE
    *
    *loop through original and found arrays to tally when a char in the found sequence directly
    * matches with the corresponding char in the original sequence
    */
    protected double getAlignedMatchesScore(Episode[] originalEpisodes, Episode[] foundEpisodes)
    {
        //convert Episode arrays into char arrays
        char[] originalChars = new char[COMPARE_SIZE];
        char[] foundChars = new char[COMPARE_SIZE];

        //convert episodes to char arrays
        for (int i = 0; i<COMPARE_SIZE; i++)
        {
            originalChars[i] = originalEpisodes[i].command;
            foundChars[i] = foundEpisodes[i].command;
        }

        //initialize counter
        int numAlignedChars = 0;

        //iterate through the two char arrays to determine how many direct aligned matches there are
        for(int i = 0; i<COMPARE_SIZE; i++)
        {
            if(originalChars[i] == (foundChars[i]))
            {
                numAlignedChars++;
            }
        }

        //the total direct aligned matches over the COMPARE_SIZE to get a score between 0-1
        double scoreToReturn = (double)numAlignedChars/COMPARE_SIZE;

        return scoreToReturn;
    }



    /**
     *getCountingScore()
     * @param original array of Episodes
     * @param found array of Episodes to compare against original episodes
     * @return finalCountingScore sum of the lengths of the matching subsequences divided by the total
     * number of subsequences based on sequence length
     * finds all subsequences of original and found sequences
     * compares all found subsequences against original subsequences
     * score is determined by the sum of the length of matching subsequences divided by max number of subseq
     */
    protected double getCountingScore(Episode[] original, Episode[] found){

        //create array list of strings to hold all subsequences for original and found
        ArrayList<String> originalSubsequences = new ArrayList<String>();
        ArrayList<String> foundSubsequences = new ArrayList<String>();

        int count = 0; //counter for how many subsequences match, not currently used but good to have
        int score = 0; //score for matching subsequences, longer sub = higher score
        int totalLengthOfSeqs = 0; //sum of length of all subsequences

        double finalCountingScore; //score to return

        //convert Episode arrays into char arrays
        char[] originalChars = new char[COMPARE_SIZE];
        char[] foundChars = new char[COMPARE_SIZE];

        //fill character arrays with commands from original and found episodes
        for (int i = 0; i<COMPARE_SIZE; i++)
        {
            originalChars[i] = original[i].command;
            foundChars[i] = found[i].command;
        }

        //append char arrays into string so we can manipulate to get subsequences
        String originalString = new String (originalChars);
        String foundString = new String (foundChars);

        //fill arrayLists with all subsequences of original and found strings we created
        for(int i=1; i<=originalString.length(); i++){ // i determines length of string
            for(int j=0; j<=originalString.length()-i; j++){ // j determines where we start (indice) in string
                originalSubsequences.add(originalString.substring(j,j+i));
                foundSubsequences.add(foundString.substring(j,j+i));
            }
        }
        //for each subsequence in original, compare to see if it is in found list of subsequences
        for(int p=0; p<originalSubsequences.size(); p++){

            //sum up all the lengths of the subsequences to use later when determining score
            String mySubstring = originalSubsequences.get(p);
            totalLengthOfSeqs = totalLengthOfSeqs + mySubstring.length();

            for(int q=0; q<foundSubsequences.size(); q++){
                if(originalSubsequences.get(p).equals(foundSubsequences.get(q))){
                    String temp = originalSubsequences.get(p);
                    count++; //increment counter if we found a matching subsequence

                    //get length of matching subsequence
                    //longer matching subsequences mean higher scores
                    score = temp.length() + score;

                    //avoid overcounting, once a subsequence is found to be matched,
                    //remove that subseq from found subsequences arraylist
                    foundSubsequences.remove(q);
                    break;
                }
            }
        }

        //calculate finalCountingScore between 0-1
        finalCountingScore = (double)score/totalLengthOfSeqs;
        return finalCountingScore;
    }

    /**
     * checkConditions
     * @param lastGoalIndex, the most recent goal
     * @return index of goal that has at least COMPARE_SIZE episodes before it
     */
    private int checkConditions(int lastGoalIndex){

        //while we don't have a goal in episodic memory, keep making random moves
        while (lastGoalIndex == -1) {
            String pathWeAttempt = "" + generateSemiRandomAction();
            Path finalPath = stringToPath(pathWeAttempt);
            tryPath(finalPath);

            lastGoalIndex = findLastGoal(episodicMemory.size()-1);
        }


        //If we've just reached the goal in the last 8 characters, then generate random steps until long enough
        while (lastGoalIndex >= episodicMemory.size() - COMPARE_SIZE || episodicMemory.size() < COMPARE_SIZE || lastGoalIndex < COMPARE_SIZE){
            String pathWeAttempt = "" + generateSemiRandomAction();
            Path finalPath = stringToPath(pathWeAttempt);
            tryPath(finalPath);

            lastGoalIndex = findLastGoal(episodicMemory.size()-1);
        }

        return lastGoalIndex;
    }

    /**
     * checkFoundConditions
     * @param indice some indice in Episodic memory passed in from explore Environment that we
     * want to verify is valid (i.e. has COMPARE_SIZE valid episodes directly preceding it in episodic memory)
     * @return if there is a goal in the COMPARE_SIZE episodes directly preceding indice, return the index of the goal,
     * otherwise return -1 to indicate all is well.
     */
    private int checkFoundConditions(int indice){
        //start i at the furthest possible character, increment i to move towards indice
        //go until i<indice because we don't care if the episode at indice is a goal or not
        for(int i=(indice-COMPARE_SIZE)+1; i<indice; i++){

            //if the episode is a goal, return the index of that episode
            if(episodicMemory.get(i).sensorValue == GOAL){
                return i;
            }
        }
        return -1;
    }
    /**
     * getOriginalSequence()
     * @return an array of the most recent Episodes (array has size COMPARE_SIZE)
     */
    private Episode[] getOriginalSequence(){

        //fill the array we will be comparing with the most recent episodes
        Episode[] originalSequence = new Episode[COMPARE_SIZE];

        for (int k=1; k<=COMPARE_SIZE; k++){

            originalSequence[k-1] = (episodicMemory.get(episodicMemory.size()-k));
        }
        return originalSequence;
    }

    /**
     * getFoundSequence()
     * @param indice some valid indice in the Episodic Memory that is passed in from exploreEnvironment()
     * @return an array of the Episodes starting at indice and counting back by COMPARE_SIZE
     */
    private Episode[] getFoundSequence(int indice){

        Episode[] foundSequence = new Episode[COMPARE_SIZE];

        for (int j=1; j<=COMPARE_SIZE; j++){

            foundSequence[j-1] = (episodicMemory.get(indice));
            indice--;
        }
        return foundSequence;
    }

    /**
     * tryGenLearningCurves()
     * overwriting method from StateMachineAgent.java to use the NewAgent
     */
    public static void tryGenLearningCurves()
    {
        try {

            FileWriter csv = new FileWriter(fileName);
            for(int i = 0; i < NUM_MACHINES; ++i) {
                System.out.println("machine number: " + (i+1));
                NewAgent gilligan = new NewAgent();

                long startTime = System.currentTimeMillis();
                gilligan.exploreEnvironment();
                long endTime = System.currentTimeMillis();

                totalMachineTime = endTime - startTime;
                gilligan.recordLearningCurve(csv);
            }
            csv.close();
        }
        catch (IOException e) {
            System.out.println("tryAllCombos: Could not create file, what a noob...");
            System.exit(-1);
        }
    }//tryGenLearningCurves

    /**
     * recordLearningCurve
     *
     * examine's the agents memory and prints out how many steps the agent took
     * to reach the goal each time
     * record percentage of time we use SUS
     *
     * @param csv         an open file to write to
     */
    protected void recordLearningCurve(FileWriter csv) {
        try {
            csv.append(episodicMemory.size() + ",");
            csv.flush();
            int prevGoalPoint = 0; //which episode I last reached the goal at
            csv.append(" SUS constant: " + SUS_CONSTANT + " ,");
            csv.append(" SUS percentage: " + percentSUS + ",");
            csv.append(" Random constant: " + RANDOM_SCORE + ",");
            csv.append(" Random percentage: " + percentRandom + " ,");
            //csv.append(" Machine RunTime: " + totalMachineTime/(double)1000 + ",");
            //csv.append(" Average checkConditions RunTime: " + avgRunTimeCheckCond/(double)1000 + ",");
            //csv.append("" + avgRunTimeFoundSeq/(double)1000 + ",");
            csv.append(" Quality constant: " + ALIGNED_CONSTANT + ",");
            csv.append(" Quality percentage: " + percentQuality + ",");



            for (int i = 0; i < episodicMemory.size(); ++i) {
                Episode ep = episodicMemory.get(i);
                if (ep.sensorValue == GOAL) {
                    csv.append(i - prevGoalPoint + ",");
                    csv.flush();
                    prevGoalPoint = i;
                }//if
            }//for
            csv.append("\n");
            csv.flush();
        } catch (IOException e) {
            System.out.println("recordLearningCurve: Could not write to given csv file.");
            System.exit(-1);
        }
    }
    /**
     * tryOneCombo
     *
     * a helper method for trying one particular combination of SUS/LMS/Random
     * weights.  THis is meant to be called from main()
     *
     * @param csv         an open file to write to
     * @param randWeight  weight for random choice
     * @param susWeight   weight for SUS choice
     * @param qualityWeight   weight for quality choice
     */
    public static void tryOneCombo(FileWriter csv, int randWeight, int susWeight, int qualityWeight)
    {

        double sum = 0;//total num successes
        for (int l = 0; l < NUM_MACHINES; l++) {//test with multiple FSMs

            NewAgent gilligan = new NewAgent();
            RANDOM_SCORE = randWeight;
            SUS_CONSTANT = susWeight;
            COUNTING_CONSTANT = qualityWeight;
            ALIGNED_CONSTANT = qualityWeight;

            gilligan.exploreEnvironment();
            //write the results of this combo to the file
            gilligan.recordLearningCurve(csv);


            sum += gilligan.currentSuccesses;
        }//for
        double averageSuccesses = sum / NUM_MACHINES;

        try {

            System.out.println("tryOneCombo...");
            csv.append("\n");
            csv.flush();
        }
        catch (IOException e) {
            System.out.println("Could not create file, what a noob...");
            System.exit(-1);
        }


    }//tryOneCombo

    /**
     * tryAllCombos
     *
     * exhaustively tests all permutations of weights within specified ranges.
     *
     * TODO: Range values are hard-coded at the moment.  
     */
    public static void tryAllCombos()
    {
        try {
            FileWriter csv = new FileWriter(OUTPUT_FILE);
            //csv.append("Random,SUS,Quality,Average Score\n");

            //constants loops (trying many permutations of values)
            for (int i = 2; i < 30; i+=1) {//random loop
                for (int j = 1; j < 48; j+=1) {//sus loop
                    for (int k = 1; k < 50; k+=1) {//quality loop
                        System.out.println("Testing Random Constant: " + i
                                + " ~~~ Testing SUS Constant: " + j
                                + " ~~~ Testing Quality Constant: " + k);

                        tryOneCombo(csv, i, j, k);

                    }//quality
                }//sus
            }//random
            csv.close();
        }
        catch (IOException e) {
            System.out.println("tryAllCombos: Could not create file, what a noob...");
            System.exit(-1);
        }
    }//tryAllCombos


    /**
     * main
     *
     * helper methods (above) have been defined to do various things here.
     * Modify this method to call the one(s) you want.
     */
    public static void main(String [ ] args) {

        for(int i=0; i < NUM_RUNS; i++){
            //name our csv file after what run number we are currently on
            fileName = ("AIReportQuality1_SUS0_RANDOM1_"+i+".csv");
            SUS_CONSTANT = 0;
            RANDOM_SCORE = 8;
            COUNTING_CONSTANT = 4;
            ALIGNED_CONSTANT = 4;
            tryGenLearningCurves();
        }
        //tryAllCombos();
        System.out.println("Done.");
    }
}
