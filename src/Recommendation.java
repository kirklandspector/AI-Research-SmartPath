/**
 * An object of this class has three variables:
 * a double score
 * an int stepsToGoal
 * a char recommendedChar
 *
 * It also contains the compareTo method which is required by the Comparable interface
 */
public class Recommendation implements Comparable<Recommendation>
{
    public double score;
    public int stepsToGoal;
    public char recommendedAction;

    public Recommendation(double score, int stepsToGoal, char recommendedAction)
    {
        this.score = score;
        this.stepsToGoal = stepsToGoal;
        this.recommendedAction = recommendedAction;
    }

    public int compareTo(Recommendation other)
    {
        if(this.score>other.score)
        {
            return -1;
        }
        else if(this.score<other.score)
        {
            return 1;
        }
        else
        {
            return 0;
        }
    }



}
