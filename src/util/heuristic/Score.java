package util.heuristic;

public class Score implements Comparable<Score>{
	public float stateScore = -1f;
	public float heuristicScore = -1f;
	
	void setHeuristicScore(float score){
		heuristicScore = score;
	}
	void setStateScore(float score){
		stateScore = score;
	}
	
	@Override
	public int compareTo(Score s2) {
		if (stateScore >= 0f && s2.stateScore >= 0f){
			return Float.compare(stateScore, s2.stateScore);			
		} else if (s2.stateScore == -1f) {
			if( stateScore > 0f ){ 
				return 1;
			} else if (stateScore == 0f) {
				return -1;
			}
		} else if (stateScore == -1f) {
			if(s2.stateScore > 0f) { 
				return -1;
			} else if (s2.stateScore == 0f) {
				return 1;
			}
		} else {
			return Float.compare(heuristicScore, s2.heuristicScore);			
		}
		// we will not get here
		assert(false);
		return 0;
	}
}
