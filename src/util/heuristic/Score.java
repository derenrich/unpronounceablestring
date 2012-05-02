package util.heuristic;

public class Score implements Comparable<Score>{
	public float stateScore = -1f;
	public float heuristicScore = -1f;
	public int depth = -1;

	
	public String toString(){
		return stateScore + "," + heuristicScore+","+depth;
	}
	
	@Override
	public int compareTo(Score s2) {
		if (stateScore >= 0f && s2.stateScore >= 0f){
			if (stateScore == s2.stateScore){
				if (stateScore == 0) {
					if(depth == s2.depth) {
						return 0;
					} else return depth > s2.depth ? 1 : -1;
				} else {
					if(depth == s2.depth) {
						return 0;
					} else return depth < s2.depth ? 1 : -1;					
				}
			} else {
				return Float.compare(stateScore, s2.stateScore);
			}
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
			if(heuristicScore == s2.heuristicScore){
				if(depth == s2.depth) {
					return 0;
				} else return depth < s2.depth ? 1 : -1;
			} else return Float.compare(heuristicScore, s2.heuristicScore);			
		}
		// we will not get here
		assert(false);
		return 0;
	}
}
