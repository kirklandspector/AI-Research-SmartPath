
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.Vector;


public class StateMachineAgent {

	// Instance variables
	private StateMachineEnvironment env;
	private char[] alphabet;
	private ArrayList<Episode> episodicMemory;

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
    private int susScore;
    boolean[] sequenceLengthsFound = new boolean[20];//just picked 20 as a guess
    private ArrayList<String> sequencesNotPerformed;
    private int SUS_CONSTANT; //will become final after testing to find values

    /**
     * The LMS (lama) is the longest matching sequence that matching with what the agent
     * has just executed. A score will be built related to the length of the sequence
     * and the amount of moves to execute to "get to the goal." The idea behind this
     * is that eventually there will be long matching strings that occur frequently
     * and regularly get the agent to the goal... cross your fingers
     */
    //variables related to the LMS
    private int lmsScore;
    private int LMS_CONSTANT; //will become final after testing

    private int RANDOM_SCORE; //will become final after testing

    //chance that a duplicate cmd is allowed if a random action is necessary
    double DUPLICATE_FORGIVENESS = .10; //10% chance a duplicate is permitted

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
		int[][] testTransitions = new int[][]{{0,1},{1,2},{2,2}};
		//int[][] testTransitions = new int[][]{{0,1},{1,1}};
		env = new StateMachineEnvironment(testTransitions, 3, 2);
		alphabet = env.getAlphabet();
		episodicMemory = new ArrayList<Episode>();
		//Need a first episode for makeMove
		episodicMemory.add(new Episode(' ', NO_TRANSITION));//the space cmd means unknown cmd for first memory
	}

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

			if (sensors[IS_GOAL] && i == pathToTry.size()-1) { //if at goal and last cmd return true
				return true;
			}
            else if (sensors[IS_GOAL]) { //if we hit the goal "early" stop and return false
                return false;
            }
		}
		// If we make it through the entire loop, the path was unsuccessful
		return false;
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
        } while (possibleCmd != lastCommand || dupPermitted); //different cmd or dups allowed

		return possibleCmd;
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
	 * Finds the ending index of the longest substring in episodic memory before
	 * the previous goal matching the final string of actions the agent has
	 * taken
	 *
	 * @return The ending index of the longest substring matching the final string of actions
	 *         the agent has taken
	 */
	private int maxMatchedStringIndex() {
		int lastGoalIndex = findLastGoal(episodicMemory.size());
		if (lastGoalIndex == -1) {
			return -1;
		}

		//If we've just reached the goal, then there is nothing to match
		if (lastGoalIndex == episodicMemory.size() - 1)
		{
			return -1;
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
			return 0;
		}
		else {
			return maxStringIndex;
		}
	}//maxMatchedStringIndex

	/**
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
			char currCmd = episodicMemory.get(indexOfMatchingAction - 1).command;
			int currSensors = episodicMemory.get(indexOfMatchingAction).sensorValue;
			char prevCmd = episodicMemory.get(i).command;
			int prevSensors = episodicMemory.get(i+1).sensorValue;

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
		StateMachineAgent gilligan;
		gilligan = new StateMachineAgent();
		System.out.println("ENVIRONMENT INFO:");
		gilligan.env.printStateMachine();
		gilligan.env.printPaths();

        //call driver method
	}

	protected StateMachineEnvironment getEnv() {
		return env;
	}

}//class StateMachineAgent
