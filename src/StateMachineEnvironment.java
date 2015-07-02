
import java.util.ArrayList;
import java.util.Random;

/**
 * <!-- class StateMachineEnvironment -->
 * 
 * An environment in which the agent can make moves
 * in an attempt to change it's state and reach the 
 * goal state. The agent will make moves and if a chosen
 * move brings the agent to a new state, it's sensor will 
 * be true, if not it will be false. The agent is also able
 * to sense if it has reached the desired goal state.
 *
 * @author Kirkland Spector
 * @author Chandler Underwood
 *
 * based off of code from:
 * @author Hailee Kenney
 * @author Preben Ingvaldsen
 * 
 * @version February 26 2015
 *
 */
public class StateMachineEnvironment {
	
	// Instance variables
	public static int NUM_STATES = 6;
	public static int GOAL_STATE = NUM_STATES - 1;
	public static int ALPHABET_SIZE = 4;  //this must be in the range [2,26]

	 //These are used as indexes into the the sensor array
	private static final int IS_NEW_STATE = 0;
	private static final int IS_GOAL = 1;


	private int[][] transition;  //transition table
	private char[] alphabet;
	private String[] paths;  //the shortest path from each state to goal
	public int currentState;

    //this will be useful
    private Random random = new Random();
	
	//DEBUG
	private boolean debug = false;

    /**
     * StatMachineEnvironment
     *
     * Constructor to build an environment using the the defined sizes set above
     */
	public StateMachineEnvironment() {
		paths = new String[NUM_STATES];
		paths[GOAL_STATE] = "";
		fillAlphabet();
		currentState = 0;
		generateStateMachine();
		if(debug){
			printStateMachine();
		}
		
		findShortestPaths();
		if (debug) {
			System.out.println("Shortest Path: " + paths[0]);
		}
	}
	
	/**
	 * A constructor which allows us to hard code state machine transitions
	 * for testing purposes 
	 */
	public StateMachineEnvironment(int[][] transitions, int alphaSize) {
		NUM_STATES = transitions.length;
		GOAL_STATE = NUM_STATES - 1;
		ALPHABET_SIZE = alphaSize;
		
		paths = new String[NUM_STATES];
		paths[GOAL_STATE] = "";
		fillAlphabet();
		currentState = 0;
		transition = transitions;
		
		if(debug) {
			printStateMachine();
		}
		
		findShortestPaths();
		if(debug) {
			System.out.println("Shortest Path: " + paths[0]);
		}
	}

    /**
     * fills the alphabet array with ALPHABET_SIZE characters
     *
     * In the future, it'd be nice to handle sizes greater than 26.  Right now
     * that's the max.
     */
    void fillAlphabet() {
        alphabet = new char[ALPHABET_SIZE];
        for(int i = 0; i < alphabet.length; ++i) {
            char next = (char)('a' + i);
            alphabet[i] = next;
        }
    }

    /**
     * initializes the list of shortest paths to nulls again so that they can be
     * recalcualted by findShortestPath
     */
    private void initPaths()
    {
        for(int i = 0; i < paths.length-1; ++i)
        {
            paths[i] = null;
        }
        paths[paths.length - 1] = "";
    }
    
	/**
	 * A helper method to generate a random state machine environment
	 */
	private void generateStateMachine() {
		
		//Create the transition table for our state machine. Each state has a
		//numerical designation. We index into the array using the number of the
		//state we are transitioning from, then the numerical index of the
		//alphabetical character being read
		transition = new int[NUM_STATES][alphabet.length];
		int charToTransition;

        	// //DEBUG
		// System.out.println(transition[0][0]);
		
		//Initialize all the values to -1 so we can tell if there's a transition
		//there or not (since 0 is a valid state to transition to, and the array
		//will initially consist of all 0s)
		for (int i = 0; i < NUM_STATES; i++) {
			for (int j = 0; j < transition[i].length; j++) {
				transition[i][j] = -1;
			}
		}
		
		//Iterate through each row of the Transition Table so we can set the
		//transitions out of each state in the state machine
		for (int i = 0; i < NUM_STATES; i++) {
			
			//Generate a random number of transitions
            int numTransitions = random.nextInt(alphabet.length) + 1;
			for (int j = 0; j < numTransitions; j++) {
				
				//Randomly generate a character to transition on
				charToTransition = random.nextInt(transition[i].length);
				
				//if there is already a transition for that character, generate a new character to transition on
				if (transition[i][charToTransition] != -1) {
					j--;
					continue;
				}
				
				//Randomly select a state to transition to that is not the
				//current state, then set the transition from the current state
				//on the randomly generated character to the randomly generated
				//state
				int nextState = random.nextInt(NUM_STATES);
				
				while(nextState == i) {
					nextState = random.nextInt(NUM_STATES);
				}
				
				transition[i][charToTransition] = nextState;
				
			}
			
			//For all characters for the current state that do not have a
			//transition, set the transition equal to current state
			for(int j = 0; j < transition[i].length; j++) {
				if(transition[i][j] == -1){
					transition[i][j] = i;
				}
			}
		}		
	}
	
	 /**
     * A method which iterates through and prints out
     * the two-dimension array that represents the state machine
     */
    public void printStateMachine() {
        System.out.print("     ");
        for(int i = 0; i < ALPHABET_SIZE; ++i) {
            System.out.printf("%3c", alphabet[i]);
        }
        System.out.println();

        for (int i = 0; i < NUM_STATES; i++) {
            System.out.printf("%3d: ", i);

            for (int j = 0; j < alphabet.length; j++) {
                System.out.printf("%3d", transition[i][j]);
            }
            System.out.println();
        }

        System.out.print("     ");
        for(int i = 0; i < ALPHABET_SIZE; ++i) {
            System.out.printf("%3c", alphabet[i]);
        }
        System.out.println();
    }

	 /**
     * A method which prints a .dot file (Graphviz) for visualizing a state machine
     */
    public void printStateMachineGraph() {
        System.out.println("digraph finite_state_machine {");
        System.out.println("node [shape = doublecircle]; Goal;");
        System.out.println("node [shape = circle];     ");

        //for each possible source state (skipping goal state)
        for (int i = 0; i < NUM_STATES - 1; i++) {
            String src = "S" + i;

            //for each possible destination state
            for (int j = 0; j < NUM_STATES; j++) {
                String dest = "S" + j; 
                String actions = "";

                //find all actions that lead from source to dest
                for (int k = 0; k < alphabet.length; k++) {
                    if (transition[i][k] == j)
                    {
                        actions = actions + alphabet[k] + ",";
                    }
                }

                //if no actions found, skip
                if (actions.length() == 0) continue;

                //remove the trailing command on the actions list
                actions = actions.substring(0,actions.length() - 1);

                //if the destination is the goal, call it such
                if (j == GOAL_STATE)
                {
                    dest = "Goal";
                }
                    
                System.out.println("    " + src + " -> " + dest + " [ label = \"" + actions + "\" ];");
            }//for
        }//for

        System.out.println("}");
    }//printStateMachineGraph
	
	/**
	 * Resets the current state back to a state not the goal
	 */
	private void reset() {
        Random randoSquew = new Random();
        int randoState = randoSquew.nextInt(NUM_STATES - 1);
		currentState = randoState;
	}
	
	/**
	 * A method which takes in a move from the agent and updates
	 * the current state and the agent's sensors if needed.
	 * 
	 * @param move
	 * 		The move the agent is making
	 * @return
	 * 		The agent's updated sensors
	 */
	public boolean[] tick(char move) {
		// An array of booleans to keep track of the agents
		// two sensors. The first represents if he is in a new
		// state and the second represents if he is at the goal
		boolean[] sensors = {false, false};
		int newState = transition[currentState][findAlphabetIndex(move)];
		
		// If the attempted letter brings us to a new state
		// update the current state and the new state sensor
		if(newState != currentState){
			currentState = newState;
			sensors[IS_NEW_STATE] = true;
		}
		
		// If we have reached the goal, update the goal sensor
		if(newState == GOAL_STATE){
			sensors[IS_GOAL] = true;
			reset();
		}
		
		return sensors;
	}
	
	/**
	 * A helper method which determines a given letter's
	 * location in the alphabet for the tick method
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
	 * A helper method which checks if one state has a transition to another
	 * @param fromState The state to transition from
	 * @param toState The state to transition to
	 * @return The index into the alphabet array of the character fromState reads to transition to toState,
	 * 			or -1 if no such character exists
	 */
	private int hasTransition(int fromState, int toState) {
		for (int i = 0; i < transition[fromState].length; i++) {
			if (transition[fromState][i] == toState) {
				return i;
			}
		}
		return -1;
	}
	
	/**
	 * A helper method that generates the shortest path to the goal from each
	 * state using Dijkstra's algorithm
	 */
	private void findShortestPaths() {

        initPaths();
        
		//Create a queue and add the Goal State to the queue
		ArrayList<Integer> queue = new ArrayList<Integer>();
		queue.add(GOAL_STATE);
		int currState;
		int transitionChar;
		
		
		while (!queue.isEmpty()) {
			//Grab the element at the front of the queue
			currState = queue.get(0);
			queue.remove(0);
			
			//Move through each state that doesn't have a path yet. Find the
			//transition from that state to the current state.
			for (int i = 0; i < NUM_STATES; i++) {

                //skip the ones that have a path
                if (paths[i] != null) continue;
                
				transitionChar = hasTransition(i, currState);
				
				//If state i has a transition to the current state and has no
				//path, set the path for state i equal to the transition
				//character from state i to the current state added to the front
				//of the shortest path to the current state, and add state i
				//onto the queue.
				if (transitionChar != -1) {
					paths[i] = alphabet[transitionChar] + paths[currState];
					queue.add(i);
				}
			}

        		 //detect if any state does not have a path to the goal
			boolean noPath = false;
			for(int i = 0; i < paths.length; i++){
				if(paths[i] == null){
					noPath = true;
				}
			}

            		//If there is a state with no path to the goal, we have a bum state
            		//machine. Regenerate and try again
			if(queue.size() == 0 && noPath){
				generateStateMachine();
				findShortestPaths();  //recurse
				return;
			}
		}

        	//DEBUG
		// if (debug) {
		// 	printPaths();
		// }
	}
	
	/**
	 * A helper method that prints the shortest path from each state to the goal.
	 */
	public void printPaths() {
		System.out.println("Paths: ");
		for (String path : paths) {
			System.out.println(path);
		}
	}
	
	/**
	 * Calculates which state the agent would be in if it followed a given path
	 * from a given starting state.
     *
     * CAVEAT:  caller is responsible for providing a valid path
     *
     * @param begin state the agent starts in
     * @param path  path the agent follows
     *
     * @return the id of the result state or -1 for invalid path
	 */
	public int pathResult(int begin, String path) {
        if (path == null) return -1;

        //Step through each step of the path
        int currState = begin;
        for(int i = 0; i < path.length(); ++i) {
            char action = path.charAt(i);
            currState = transition[currState][findAlphabetIndex(action)];
            if (currState == -1) return -1;
        }//for

        return currState;
	}//pathResult

	 /**
      * A recursive helper method for {@link #shortPathToGoal}
      *
      * @param soFar       the path so far (the agent will find a shortest path
      *                    from this point)
      * @param currStates  Each entry in this array specifies what state I'd be
      *                    in if I'd followed the given "soFar" path from the state
      *                    corresponding to the array index
      *
      * @return the shortest path from the given "so far" point
      */
    private String spHelper(String soFar, int[] currStates) {

        //Each entry in this array specifies shortest path to the goal from the
        //corresponding position in currStates
        String[] currPaths = new String[NUM_STATES];
        for(int i = 0; i < NUM_STATES; ++i) {
            currPaths[i] = this.paths[currStates[i]];
        }//for

        //We can't explore all possible paths (NP-hard) so examine the shortest
        //path from each state and select the one(s) that also reach the goal
        //for the most other states.   For example, the shortest path from S4 to
        //the goal might be "bcdd".  By following "bcdd" from S7 and S11, you
        //will also reach the goal.
        int shortLen = NUM_STATES; //effective infinity
        ArrayList<String> shortest = new ArrayList<String>();
        int bestSolveTotal = 0;
        for(int i = 0; i < NUM_STATES; ++i) {
            if (currPaths[i].length() == 0) continue;

            //find out how many states this path will reach a goal for
            int solveTotal = 0;
            for(int j = 0; j < NUM_STATES; ++j) {
                if (currPaths[j].length() == 0) continue;

                if (pathResult(currStates[j], currPaths[i]) == GOAL_STATE) {
                    solveTotal++;
                }
            }

            //If we find a new best, reset the list
            if (solveTotal > bestSolveTotal) {
                shortest.clear();
                bestSolveTotal = solveTotal;
            }

            //Add the new best to the list
            if ( (solveTotal >=  bestSolveTotal)
                 && (! shortest.contains(currPaths[i])) ) {
                shortest.add(currPaths[i]);
            }
        }//for

        //If all paths are zero length, then we're done
        if (shortest.isEmpty()) return soFar;

        //Make a recursive call for each shortest path to find the shortest
        //final path
        String shortestFinal = null;
        for(String path : shortest) {
            //Create a copy of currStates that reflects the application of this
            //shortest path
            int[] newCurrStates = new int[NUM_STATES];
            for(int i = 0; i < NUM_STATES; ++i) {
                newCurrStates[i] = currStates[i];
                if (newCurrStates[i] != GOAL_STATE) {
                    newCurrStates[i] = pathResult(newCurrStates[i], path);
                }
            }

            //recurse
            String cand = spHelper(soFar + path, newCurrStates);
            if ( (shortestFinal == null) || (shortestFinal.length() < cand.length()) ) {
                shortestFinal = cand;
            }
        }//for
                
        return shortestFinal;
        
    }//spHelper
    
    /**
     * Calculates a short path to the goal if the agent has a perfect model of
     * the environment but does not know what state it has started in.
     *
     * Note: Finding the actual shortest path is likely NP-hard and will take
     * far too long to calculate for anything but small FSMs.  This method uses
     * a greedy approach that will yield the shortest path much of the time for
     * small FSMs.
     */
    public String shortPathToGoal() {
        //
        int[] currStates = new int[NUM_STATES];
        for(int i = 0; i < NUM_STATES; ++i) {
            currStates[i] = i;
        }

        return spHelper("", currStates);
    }//shortPathToGoal
    
	
	public String[] getPaths() {
		return paths;
	}

    public char[] getAlphabet() {
        return alphabet;
    }

	public int[][] getTransition() {
		return transition;
	}

}
