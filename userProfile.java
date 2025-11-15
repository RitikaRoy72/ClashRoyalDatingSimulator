

import java.util.ArrayList;
import java.util.List;

public class UserProfile {

	private double score;
	private ArrayList<Double> doubleList = new ArrayList<>(
		       List.of(1.0, 3.0, 4.0, 5.0, 6.0, 8.0)
		   );

	public void addScore(int index) throws Exception{
		if (index < 0 || index >= doubleList.size()) {
		       throw new IndexOutOfBoundsException("Invalid index");
		   }
		score += doubleList.get(index);
	}
	public double subtractScore(int index) throws Exception{
		if (index < 0 || index >= doubleList.size()) {
		       throw new IndexOutOfBoundsException("Invalid index");
		   }
		score -= doubleList.get(index);
		return score;
	}

	public boolean checkGoodOption() {
		if (score > 25){
			return true;
		}
		else {
			return false;
		}
	}
	public boolean checkBadOption() throws Exception{
		if (score < 0){
			return true;
		}
		else {
			return false;
		}
	}
}
