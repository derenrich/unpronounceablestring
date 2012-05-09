package util.heuristic;

public class Score implements Comparable<Score>{
	public float stateScore = -1f;
	public float heuristicScore = -1f;
	public int depth = -1;

	
	public String toString(){
		return stateScore + "," + heuristicScore+","+depth;
	}
	
	private int stateCompare(Score s2) {
		if (stateScore == s2.stateScore){
			if (stateScore == 0) {
				return -depthCompare(s2);
			} else {
				return depthCompare(s2);
			}
		} else {
			return Float.compare(stateScore, s2.stateScore);
		}		
	}
	private int depthCompare(Score s2){
		if(depth == s2.depth) {
			return 0;
		} else {
			return depth < s2.depth ? 1 : -1;					
		}

	}
	@Override
	public int compareTo(Score s2) {
		if (stateScore >= 0f && s2.stateScore >= 0f){		
			return stateCompare(s2);
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
			} else {
				return Float.compare(heuristicScore, s2.heuristicScore);			
			}
		}
		// we will not get here
		assert(false);
		return 0;
	}
}
