package util.depthcharger;

public class Score {
	private int reached;
	private double total;
	public Score(){
		reached = 0;
		total = 0;
	}
	public double value() {
		if(total == 0)
			return 0;
		return reached/total;
	}
	public void addScore(double s) {
		reached ++;
		total += s;
	}
}
