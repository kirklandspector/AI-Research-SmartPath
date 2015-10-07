/**
 * Episode
 *
 * Represents an episode in the agents episodic memory
 */
public class Episode { 
	
	public char command;     //what the agent did
	public int sensorValue;  //what the agent sensed

	public Episode(char cmd, int sensor) {
		command = cmd;
		sensorValue = sensor;

	}

    public String toString() {
        return "[Cmd: "+command+"| Sensor: "+sensorValue+"]";
    }
}
