
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;


public class StateMachineAgent {

	// Instance variables
	private StateMachineEnvironment env;
	private char[] alphabet;
	private ArrayList<Episode> episodicMemory;
    private int currentSuccesses = 0;

	//These are used as indexes into the the sensor array
	private static final int IS_NEW_STATE = 0;
	private static final int IS_GOAL = 1;

	//Sensor values
	public static final int NO_TRANSITION = 0;
	public static final int TRANSITION_ONLY = 1;
	public static final int GOAL = 2;

    /**
     * The SUS is the shortest unique sequence that has not been performed yet.
     * A score will be made to evaluate if it will be chosen as the next path
     * to execute. The idea being that it will slowly help find the best string
     * of cmds to execute in tricky situations
     */
    //variables related to the SUS
    private double susScore = 0;
    private static final int MAX_SEQUENCE_SIZE = 10; //just picked 10 as a guess
    private ArrayList<ArrayList<String>> sequencesNotPerformed;
    private static int SUS_CONSTANT = 10; //will become final after testing to find values

    /**
     * The LMS (llama) is the longest matching sequence that matching with what the agent
     * has just executed. A score will be built related to the length of the sequence
     * and the amount of moves to execute to "get to the goal." The idea behind this
     * is that eventually there will be long matching strings that occur frequently
     * and regularly get the agent to the goal... cross your fingers
     */
    //variables related to the LMS
    private double lmsScore;
    private static int LMS_CONSTANT = 10; //will become final after testing
    public static final int MATCHED_INDEX = 0;
    public static final int MATCHED_LENGTH = 1;

    private static int RANDOM_SCORE = 1; //will become final after testing

    //chance that a duplicate cmd is allowed if a random action is necessary
    double DUPLICATE_FORGIVENESS = .25; //25% chance a duplicate is permitted (S.W.A.G.)

	// Turns debug printing on and off
	boolean debug = true;

	//specify path to take for testing if boolean is true
	ArrayList<Character> testPath = new ArrayList<Character>(Arrays.asList('b', 'b'));
	boolean useDefinedPath = false;

	/**
	 * The constructor for the agent simply initializes it's instance variables
	 */
	public StateMachineAgent() {
		//int[][] testTransitions = new int[][] {{2, 1, 0},{1, 0, 2},{2, 2, 2}};
		//int[][] testTransitions = new int[][]{{0,1},{1,2},{2,2}};
		//int[][] testTransitions = new int[][]{{0,1},{1,1}};
		//env = new StateMachineEnvironment(testTransitions, 2, 3);
        env = new StateMachineEnvironment();
		alphabet = env.getAlphabet();

		episodicMemory = new ArrayList<Episode>();
		//Need a first episode that is empty
		episodicMemory.add(new Episode(' ', NO_TRANSITION));//the space cmd means unknown cmd for first memory

        //build the permutations of all sequences that can be performed
        sequencesNotPerformed = new ArrayList<ArrayList<String>>();
        sequencesNotPerformed.add(0, null);//since a path of size 0 should be skipped (might not be necessary)

        //iterate over all path sizes and add the permutations to the sequencesNotPerformed arrays
        for(int lengthSize=1; lengthSize<=MAX_SEQUENCE_SIZE; lengthSize++){
            ArrayList<String> tempList = new ArrayList<String>();
            fillPermutations(alphabet, lengthSize, tempList);
            sequencesNotPerformed.add(lengthSize, tempList);
        }
	}

    /**
     * exploreEnvironment
     *
     * Main Driver Method of Program
     *
     * Sets the agent free into the wild allowing him to roam free. This means that he'll
     * use the different scores to decide how to navigate the environment giving him full
     * sentient capabilities...
     */
    public void exploreEnvironment() {
        while (episodicMemory.size() < 1000) { //perform 1000 cmds
            //Find sus and lms scores
            determineSusScore();
            String currentLms = determineLmsScore();

            String pathToAttempt;
            //pick larger score of the three
            if (RANDOM_SCORE > susScore && RANDOM_SCORE > lmsScore) {
                pathToAttempt = "" + generateSemiRandomAction();
            }
            else if (susScore > lmsScore) {
                pathToAttempt = getSus();
            }
            else if (lmsScore > susScore) {
                pathToAttempt = currentLms;
            }
            else {//if we tied, default to a rando to hopefully tweak them
                pathToAttempt = "" + generateSemiRandomAction();
            }

            //execute "the chosen one"
            Path finalPath = stringToPath(pathToAttempt);
            tryPath(finalPath);

            scanAndRemoveNewSequences(finalPath.size());
        }
    }

    /**
     * ************************************************************************************
     * METHODS FOR THE SUS
     * ************************************************************************************
     */

    /**
     * scanAndRemoveNewSequences
     *
     * Rips through the memory and finds all unique sequences that have been performed
     * and not yet removed from sequencesNotPerformed. Should be called after every
     * command the agent makes
     *
     * @param numCmdsExecuted the number of cmds commited by the last try path
     */
    public void scanAndRemoveNewSequences(int numCmdsExecuted){
        //iterate through all sizes of possible paths, if no unique paths exist, move on
        //otherwise scan mem and remove matching sequences from SNP arraylist
        for (int i=1; i<=MAX_SEQUENCE_SIZE; i++){
            //if there are no paths of this length, move on
            if (sequencesNotPerformed.get(i).isEmpty()){
                continue;
            }

            //make sure current path size is smaller than mem size, if not break out
            if (i > episodicMemory.size()-1) { //sub 1 because first " " command doesn't count
                break;
            }

            //otherwise we'll go through recently changed memory of this size and check against SNP
            //iterate over all commands - i to stop fence posting error
            //determine starting position
            int startPosition = episodicMemory.size() - 1;
            int changedMemory = MAX_SEQUENCE_SIZE + numCmdsExecuted;
            while (startPosition > 1 && startPosition >= episodicMemory.size() - changedMemory) {
                startPosition--;
            }
            for (int j=startPosition; j<=episodicMemory.size()-i; j++){
                String currentPath = ""; //path in memory to test
                for (int k=0; k<i; k++){ //iterate the size of the path through
                    currentPath += episodicMemory.get(j+k).command;
                }

                //test if this path is in SNP and remove if so, huzzah!
                if (sequencesNotPerformed.get(i).contains(currentPath)){
                    sequencesNotPerformed.get(i).remove(currentPath);
                }
            }
        }
    }

    /**
     * getSus
     *
     * Returns the sus by fishing through the SNP and getting a path of the smallest length
     *
     * @return a string if a sus is found or null if none found
     */
    public String getSus() {
        for (int i=1; i<sequencesNotPerformed.size(); i++) { //go through path sizes
            if (!sequencesNotPerformed.get(i).isEmpty()) { //if not empty, there's a victim inside
                return sequencesNotPerformed.get(i).remove(0);//returns and removes sus
            }
        }
        return null;
    }

    /**
     * determineSusScore
     *
     * set the susScore based on the summation equation and constant
     */
    public void determineSusScore() {
        //loop through mem to find length of sus
        int susLength=0;

        //get shortest length for sus
        for (int i=1; i<sequencesNotPerformed.size(); i++){
            if (!sequencesNotPerformed.get(i).isEmpty()) {
                susLength = i;
                break;
            }
        }

        //if the length is still 0 the sus has dried up, set to 0
        if (susLength == 0){
            susScore = 0;
            return;
        }

        //otherwise add up summation and multiply by constant
        double sum = 0;
        for (int i=susLength; i>0; i--) {
            sum+=i;
        }

        susScore = (1 / sum) * SUS_CONSTANT;
    }

    /**
     * fillPermutations
     *
     * driver method to generate all strings for the sequencesNotPerformed arraylist
     *
     * @param set set of chars that can be used to build strings (alphabet)
     * @param k length of string to build up to
     * @param permutations place to store the strings
     */
    public void fillPermutations(char set[], int k, ArrayList<String> permutations){
        int n = set.length;
        buildPermutations(set, "", n, k, permutations);
    }

    /**
     * buildPermutations
     *
     * helper method to actually build all the permutations of the strings and store them in the arraylist
     *
     * @param set set of chars that can be used to build strings (alphabet)
     * @param prefix used to slowly build up different permutations
     * @param n length of set (sort of clumsy way to do it right now)
     * @param k length of string to build up to
     * @param permutations place to store the strings
     */
    public void buildPermutations(char set[], String prefix, int n, int k, ArrayList<String> permutations) {
        // Base case: k is 0
        if (k == 0) {
            permutations.add(prefix);
            return;
        }

        // One by one add all characters from set and recursively
        // call for k equals to k-1
        for (int i = 0; i < n; ++i) {
            // Next character of input added
            String newPrefix = prefix + set[i];
            // k is decreased, because we have added a new character
            buildPermutations(set, newPrefix, n, k - 1, permutations);
        }
    }

    /**
     * ************************************************************************************
     * METHODS FOR THE LMS
     * ************************************************************************************
     */

    /**
     * maxMatchedString
     *
     * Finds the ending index of the longest substring in episodic memory before
     * the previous goal matching the final string of actions the agent has
     * taken and the length of the matched string
     *
     * @return The ending index of the longest substring matching the final string of actions
     *         the agent has taken and the length of the matched string
     */
    private int[] maxMatchedString() {
        int lastGoalIndex = findLastGoal(episodicMemory.size());
        int[] scoreInfo = {0,0};//var to be returned

        if (lastGoalIndex == -1) {
            return scoreInfo;//since init to 0's the ending score will be poor
        }

        //If we've just reached the goal, then there is nothing to match
        if (lastGoalIndex == episodicMemory.size() - 1)
        {
            return scoreInfo;//again, both are 0's for bad outcome
        }

        //Find the longest matching subsequence (LMS)
        int maxStringIndex = -1;
        int maxStringLength = 0;
        int currStringLength;
        for (int i = lastGoalIndex-1; i >= 0; i--) {
            currStringLength = matchedMemoryStringLength(i);
            if (currStringLength > maxStringLength) {
                maxStringLength = currStringLength;
                maxStringIndex = i+1;
            }
        }//for

        if (maxStringIndex < 0) {
            return scoreInfo;//bad score
        }

        else {
            scoreInfo[MATCHED_INDEX] = maxStringIndex;
            scoreInfo[MATCHED_LENGTH] = maxStringLength;
            return scoreInfo;
        }
    }//maxMatchedStringIndex

    /**
     * determineLmsScore
     *
     * Figures out the lms score and sets it using passed info from maxMatchedString
     *
     * @return pathToAttempt the string to exec if lms is chosen
     */
    private String determineLmsScore() {
        int[] matchedStringInfo = maxMatchedString();
        String pathToAttempt = stepsToGoal(matchedStringInfo[MATCHED_INDEX]);
        //calc score lengthMatched/numStepsToGoal
        double lengthMatched = matchedStringInfo[MATCHED_LENGTH];
        double numStepsToGoal = pathToAttempt.length();

        lmsScore = (lengthMatched / numStepsToGoal) * LMS_CONSTANT;
        return pathToAttempt;
    }

    /**
     * stepsToGoal
     *
     * takes an index and finds the path to reach the next goal
     *
     * @return steps the string to exec to "reach" goal
     */
    private String stepsToGoal(int idx) {
        //loop to next goal appending all chars
        String steps = "";
        if (idx ==0)//no mem to evaluate
            return " ";
        for (int i=idx; i<episodicMemory.size(); i++) {
            steps += episodicMemory.get(i).command;
            //break if at goal
            if (episodicMemory.get(i).sensorValue == GOAL){
                break;
            }
        }
        return steps;
    }

    /**
     * matchedMemoryStringLength
     *
     * Starts from a given index and the end of the Agent's episodic memory and moves backwards, returning
     * the number of consecutive matching characters
     * @param endOfStringIndex The index from which to start the backwards search
     * @return the number of consecutive matching characters
     */
    private int matchedMemoryStringLength(int endOfStringIndex) {
        int length = 0;
        int indexOfMatchingAction = episodicMemory.size() - 1;
        boolean match;
        for (int i = endOfStringIndex; i >= 0; i--) {
            //We want to compare the command from the prev episode and the
            //sensors from the "right now" episode to the sequence at the
            //index indicated by 'i'
            char currCmd = episodicMemory.get(indexOfMatchingAction).command;
            int currSensors = episodicMemory.get(indexOfMatchingAction).sensorValue;
            char prevCmd = episodicMemory.get(i).command;
            int prevSensors = episodicMemory.get(i).sensorValue;

            match = ( (currCmd == prevCmd) && (currSensors == prevSensors) );

            if (match) {
                length++;
                indexOfMatchingAction--;
            }
            else {
                return length;
            }
        }//for

        return length;
    }//matchedMemoryStringLength


    /**
     * findLastGoal
     *
     * Searches backwards through the list of move-result pairs from the given index
     * @param toStart The index from which to start the backwards search
     * @return The index of the previous goal
     */
    private int findLastGoal(int toStart) {
        for (int i = toStart - 1; i > 0; i --) {
            if (episodicMemory.get(i).sensorValue == GOAL) {
                return i;
            }
        }
        return -1;
    }

    /**
     * ************************************************************************************
     * METHODS FOR NAVIGATION
     * ************************************************************************************
     */

	/**
     * tryPath
     *
	 * Given a full string of moves, tryPath will enter the moves
	 * one by one and determine if the entered path is successful
     * A path is successful (returns true) only if it reaches the goal
     * on the last cmd, otherwise it will return false. If it reaches the
     * goal prematurely it will not execute anymore cmd's and return false
	 *
	 * @param pathToTry
	 * 		An ArrayList of Characters representing the path to try
	 * 
	 * @return
	 * 		A boolean which is true if the path was reached the goal and
	 * 		false if it did not
	 */
	public boolean tryPath(Path pathToTry) {
		boolean[] sensors;
		// Enter each character in the path
		for (int i = 0; i < pathToTry.size(); i++) {
			sensors = env.tick(pathToTry.get(i));
			int encodedSensorResult = encodeSensors(sensors);
			episodicMemory.add(new Episode(pathToTry.get(i), encodedSensorResult));

            if (sensors[IS_GOAL]){
                currentSuccesses++;
            }

			if (sensors[IS_GOAL] && i == pathToTry.size()-1) { //if at goal and last cmd return true
				return true;
			}
		}
		// If we make it through the entire loop, the path was unsuccessful
		return false;
	}

    /**
     * stringToPath
     *
     * Takes a string of chars and converts them into a path
     *
     * @param commands string to be converted
     */
    public Path stringToPath(String commands) {
        ArrayList<Character> generatedPath = new ArrayList<Character>();
        for (int i=0; i<commands.length(); i++) {
            generatedPath.add(i, commands.charAt(i));
        }
        return new Path(generatedPath);
    }

	/**
	 * getMostRecentPath
	 * 
	 * Gets the most recent path present in Episodic Memory
     *
	 * @return The most recent path in episodic memory
	 */
	public Path getMostRecentPath() {
		int lastGoal = findLastGoal(episodicMemory.size() - 2) + 1;
		ArrayList<Character> pathChars = new ArrayList<Character>();
		for (int i = lastGoal; i < episodicMemory.size(); i++) {
			pathChars.add(episodicMemory.get(i).command);
		}
		return new Path(pathChars);
	}

    //TODO: Take this code for later use
	/**
     * reset
     *
	 * Resets the agent by having it act randomly until it reaches the goal.
	 * This will be changed to a more intelligent scheme later on
	 */
	public void reset() {
		char toCheck;
		boolean[] sensors;
		int encodedSensorResult;

		//Currently, the agent will just move randomly until it reaches the goal
		//and magically resets itself
		do {
			toCheck = generateSemiRandomAction();
			sensors = env.tick(toCheck);
			encodedSensorResult = encodeSensors(sensors);
			episodicMemory.add(new Episode(toCheck, encodedSensorResult));
			/*if (episodicMemory.size() > 500000000) {
				System.exit(0);
			}*/

		} while (!sensors[IS_GOAL]); // Keep going until we've found the goal
	}

	/**
     * generateSemiRandomAction
     *
	 * Generates a semi random action for the Agent to take
     * There is a a 10% forgiveness to make the same move again since
     * prior research has shown duplicate commands are rarely successful
	 * 
	 * @return A random action for the Agent to take
	 */
	public char generateSemiRandomAction() {
        //decide if a dup command is acceptable
        double chanceForDup = Math.random();
        boolean dupPermitted = false;
        if (chanceForDup < DUPLICATE_FORGIVENESS) {
            dupPermitted = true;
        }

        //keep generating random moves till it is different from last or dups are allowed
        Random random;
        char possibleCmd;
        Episode lastEpisode = episodicMemory.get(episodicMemory.size() - 1);
        char lastCommand = lastEpisode.command;

        do {
            random = new Random();
            possibleCmd = alphabet[random.nextInt(alphabet.length)];
            if (dupPermitted)//if they are allowed we don't care to check for dup
                break;
        } while (possibleCmd == lastCommand); //same cmd, redo loop

		return possibleCmd;
	}

    /**
     * Takes in an agent's sensor data and encodes it into an integer
     * @param sensors The agent's sensor data
     * @return the integer encoding of that sensor data
     */
    private int encodeSensors(boolean[] sensors) {
        int encodedSensorResult;

        if (sensors[IS_GOAL]) {
            encodedSensorResult = GOAL;
        }

        else if (sensors[IS_NEW_STATE]) {
            encodedSensorResult = TRANSITION_ONLY;
        }

        else {
            encodedSensorResult = NO_TRANSITION;
        }

        return encodedSensorResult;
    }

	/**
	 * Uses testPath arraylist to start agent on a specific path for testing purposes
	 *
	 * @return next char in the testPath
	 */
	private char testDefinedPath() {
		//precaution to never send null char
		if (testPath.size() != 0) return testPath.remove(0);
		//if that wasn't hit something went wrong continue with a random action and alert user
		else {
			System.out.println("WARNING: your test path is out of commands and hasn't reached the goal, executing random action");
			return generateSemiRandomAction();
		}
	}

	/**
	 * Returns the index of the given character in the alphabet array
	 * @param toCheck the character to find the index of
	 * @return the index of toCheck
	 */
	private int indexOfCharacter(char toCheck) {
		for (int i = 0; i < alphabet.length; i++) {
			if (alphabet[i] == toCheck) {
				return i;
			}
		}

		return -1;
	}

	/**
	 * A helper method which determines a given letter's
	 * location in the alphabet
	 * 
	 * @param letter
	 * 		The letter who's index we wish to find
	 * @return
	 * 		The index of the given letter (or -1 if the letter was not found)
	 */
	private int findAlphabetIndex(char letter) {
		// Iterate the through the alphabet to find the index of letter
		for(int i = 0; i < alphabet.length; i++){
			if(alphabet[i] == letter)
				return i;
		}

		// Error if letter is not found
		return -1;
	}

	/**
	 * main
     *
     * sends the agent through many rounds of exploring the environment in hopes
     * to find a consistent way of navigating
	 */
	public static void main(String [ ] args)
	{
		StateMachineAgent gilligan = new StateMachineAgent();
        double[][][] successStats = new double[4][51][51];
        //constants loops
        for (int i=1; i<4; i++){//random loop
            gilligan.RANDOM_SCORE = i;
            System.out.println("Testing Random Constant: " + i);
            for (int j=10; j<50; j++){//sus loop
                gilligan.SUS_CONSTANT = j;
                System.out.println("Testing Sus Constant: " + j);
                for (int k=10; k<50; k++){//lms loop
                    gilligan.LMS_CONSTANT = k;
                    System.out.println("Testing Lms Constant: " + k);

                    double sum = 0;//total num successes
                    for (int l=0; l<100; l++){//number of machines
                        gilligan = new StateMachineAgent();
                        gilligan.exploreEnvironment();
                        sum += gilligan.currentSuccesses;
                    }
                    double averageSuccesses = sum / 1000;
                    successStats[i][j][k] = averageSuccesses;

                }//lms
            }//sus
        }//random
        System.out.println("debug");
	}

	protected StateMachineEnvironment getEnv() {
		return env;
	}

}//class StateMachineAgent
