import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.Collections;

/**
 * class NSMAgent
 *
 * This agent is an implementation of Andrew McCallum's Nearest Sequence Memory
 * algorithm.  
 *
 * @author: Andrew Nuxoll (with many thanks to Zach Faltersack for his original
 * implementation in C)
 *
 */
public class NSMAgent extends StateMachineAgent {

    /**
     * ************************************************************************************
     * INNER CLASSES
     * ************************************************************************************
     */
    /** 
     * class QEpisode
     *
     * This class extends Episode to have a q-value and a reward
     */
    public class QEpisode extends Episode {
        public double qValue = 0.0;
        public double reward = 0.0;

        public QEpisode(char cmd, int sensor) {
            super(cmd, sensor);
        }
    }//class QEpisode
    
    /**
     * class NBor
     *
     * describes a "neighbor", specifically a sequence that matches the current
     * sequence ending with the last episode (which represents the present
     * moment) presuming that a specific action will be taken next.
     */
    public class NBor implements Comparable<NBor> {
        int end;  // index of the last episode of the sequence
        int begin; // index of the first episode of the sequence
        int len;  //length of the sequence

        public NBor(int initEnd, int initLen) {
            this.begin = initEnd - initLen;
            this.end = initEnd;
            this.len = initLen;
        }

        /** this allows a collection of NBors to be sorted on length */
        public int compareTo(NBor other) {
            return this.len - other.len;
        }
    }//class NBor
    
    /**
     * class NHood
     *
     * is a public container class for defining a "neighborhood" in episodic
     * memory.  Specifically this a set of k sequences that have the longest
     * match to the current sequene ending with the last episode (which
     * represents the present moment) presuming that a specific action will be
     * taken next.
     */
    public class NHood {
        public final int K_NEAREST = 8;  //max allowed size of neighborhood
        
        public char command;           // action associated with this neighborhood
        public ArrayList<NBor> nbors;  // neigbhors in the hood
        public int shortest = 0;       //length of shortest neighbor

        public NHood(char initCmd) {
            this.command = initCmd;
            nbors = new ArrayList<NBor>();
        }

        /** adds a new neighbor to the neighborhood.
         * CAVEAT:  Caller is responsible for checking the neighbor is long
         * enough to belong. */
        public void addNBor(NBor newGuy) {
            //if the nhood is full, drop the shorest neighbor to make room
            while(nbors.size() >= K_NEAREST) {
                this.nbors.remove(0);
            }

            this.nbors.add(newGuy);

            //update the shortest
            Collections.sort(nbors);
            this.shortest = nbors.get(0).len;
        }//addNBor

    /**
     * calculates a neighborhood's total Q value.  This is the average of the
     * expected future discounted rewards of all the neighbors in the
     * neighborhood
     *
     */
    double calculateQValue()
    {
        //Don't calculate for empty neighborhoods
        if (nbors.size() == 0) return 0.0;

        // sum the q-values of each neighbor
        double total = 0.0;
        for(NBor nbor : nbors)
        {
            QEpisode qep = (QEpisode)episodicMemory.get(nbor.end);
            total += qep.qValue;
        }

        // return the average
        return (total / (double)nbors.size());
    }//calculateQValue

        
    }//class NHood

    /**
     * ************************************************************************************
     * VARIABLES
     * ************************************************************************************
     */
    // Defines for Q-Learning algorithm
    public static double DISCOUNT         =  0.8;
    public static double LEARNING_RATE    =  0.85;
    public static double REWARD_SUCCESS   =  1.0;
    public static double REWARD_FAIL      = -0.1;
    public static double INIT_RAND_CHANCE =  0.7;
    public static double RAND_DECREASE    =  0.7;
    
    protected NHood[] nhoods;
//%%%    protected ArrayList<QEpisode> episodicMemory = new ArrayList<QEpisode>();
    protected double randChance = INIT_RAND_CHANCE;  //how frequently the agent
                                                     //make a random move

    /**
     * ************************************************************************************
     * METHODS 
     * ************************************************************************************
     */

    /**
	 * The constructor for the agent simply initializes it's instance variables
	 */
	public NSMAgent() {
        env = new StateMachineEnvironment();
		alphabet = env.getAlphabet();
        nhoods = new NHood[alphabet.length];
        episodicMemory.clear();
	}//NSMAgent ctor

    /**
     * populateNHoods
     *
     * creates a neighborhood of k-nearest NBors for each action.  The NHoods
     * must be regenerated each time that that a new episode is added to the
     * store.
     */
    public void populateNHoods() {
        QEpisode ep = (QEpisode)episodicMemory.get(0);

        //Create a new neighborhood for each command
        for(int c = 0; c < alphabet.length; ++c)
        {
            nhoods[c] = new NHood(alphabet[c]);
            
            //temporarily set the to-be-issued command to this value
            ep.command = alphabet[c];

            //find the kNN 
            for(int i = 0; i <= episodicMemory.size() - 2; ++i) {
                int matchLen = matchedMemoryStringLength(i);
                if ( (matchLen >0) &&
                    ( (nhoods[c].shortest <= matchLen)
                      || (nhoods[c].nbors.size() < nhoods[c].K_NEAREST) ) ) {
                    nhoods[c].addNBor(new NBor(i, matchLen));
                }
            }//for
        }//for
    }//populateNHoods

    /**
     * setNewLittleQ
     *
     * This functions takes an episode and the current utility and updates the episode's
     * expected future discounted reward. 
     *
     * @param ep A pointer to an episode to update
     * @param utility A double that contains the current state's utility used to update
     *              the episodes that voted for the most recent action
     */
    public void setNewLittleQ(QEpisode ep, double utility)
    {
        // Set the new q value for the episode
        //if(!g_statsMode) printf("Calculating and setting new expected future discounted reward\n");
        ep.qValue = (1.0 - LEARNING_RATE) * (ep.qValue)
            + LEARNING_RATE * (ep.reward + DISCOUNT * utility);
    }//setNewLittleQ
    
    /**
     * updateAllLittleQ
     *
     * This function will update the expected future discounted rewards for the
     * action that was most recently executed. We cannot guarantee that the
     * chosen action was not selected randomly because of the exploration
     * rate. To account for this we will index into the vector of neighborhoods
     * and update the neighborhood relevant to the executed action.
     *
     * @arg ep A pointer to the episode containing the most recently executed action
     */
    public void updateAllLittleQ(QEpisode ep)
    {
        // Recalculate the Q value of the neighborhood associated with the
        // episode's action
        NHood nhood = nhoods[ep.command];
        double utility = nhood.calculateQValue();

        // Update the q values for each of the voting episodes for the most
        // recent action
        for(int i = 0; i < nhood.nbors.size(); ++i) {
            //Update the root episode
            NBor nbor = nhood.nbors.get(i);
            QEpisode rootEp = (QEpisode)episodicMemory.get(nbor.end - i);
            setNewLittleQ(rootEp, utility);
            double prevUtility = utility;

            //Update all the root's predecessors that participated in the match
            for(int j = 1; j < nbor.len; ++j)
            {
                QEpisode prevEp = (QEpisode)episodicMemory.get(episodicMemory.size() - j);
                setNewLittleQ(prevEp, prevUtility);
                prevUtility = prevEp.qValue;
            }
        }//for

        // Update the given (most recent) episode's Q value 
        setNewLittleQ(ep, utility);

    }//updateAllLittleQ

        //%%%DEBUG: REMOVE
        int lastSuccess = 0;


    /**
     * exploreEnvironment
     *
     * Main Driver Method of Program
     *
     */
    @Override
    public void exploreEnvironment() {
        int prevSensors = 0; //what was sensed last time
        
        while (episodicMemory.size() < MAX_EPISODES) { 
            //add an episode to represent the current moment
            char cmd = alphabet[random.nextInt(alphabet.length)];  //default is random for now
            QEpisode nowEp = new QEpisode(cmd, prevSensors);
			episodicMemory.add(nowEp);
            
            // We can't use NSM until we've found the goal at least once
            if(currentSuccesses > 0) {
                populateNHoods();

                // (if not using random action) select the action that has the
                // neighborhood with the highest Q-value
                if (random.nextDouble() >= this.randChance) {
                    double bestQ = nhoods[0].calculateQValue();
                    cmd = nhoods[0].command;
                    for(NHood nhood : nhoods) {
                        double qVal = nhood.calculateQValue();
                        if (nhood.calculateQValue() > bestQ) {
                            bestQ = qVal;
                            cmd = nhood.command;
                        }
                    }//for
                }//if
            }//if
            
            //execute the command
            nowEp.command = cmd;
            boolean[] sensors = env.tick(cmd);

            //Setup for next iteration
            prevSensors = encodeSensors(sensors);
            if (sensors[IS_GOAL]){
                currentSuccesses++;

                //%%%DEBUG: REMOVE
                System.out.print(episodicMemory.size() - lastSuccess);
                System.out.print(",");
                lastSuccess = episodicMemory.size();
            }
            
        }//while
    }//exploreEnvironment

	/**
	 * main
     *
	 */
	public static void main(String [ ] args) {
        for(int i = 0; i < 100; ++i) {
            NSMAgent skipper = new NSMAgent();
            skipper.exploreEnvironment();
            System.out.println();
        }
	}

}//class NSMAgent
