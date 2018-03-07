package svc.fsfcn.evaluation;

/**
 * Classifier performance record
 * 
 * @author svc (svc@dmi.uns.ac.rs)
 */
public class ClassifierEvaluation {
	private double precision;
	private double recall;
	private double accuracy;
	private int numSelectedFeatures;
	
	public ClassifierEvaluation(double accuracy, double precision, double recall, int numSelectedFeatures) {
		this.precision = precision;
		this.recall = recall;
		this.accuracy = accuracy;
		this.numSelectedFeatures = numSelectedFeatures;
	}
	
	public double getP() {
		return precision;
	}
	
	public double getR() {
		return recall;
	}
	
	public double getA() {
		return accuracy;
	}
	
	public int getF() {
		return numSelectedFeatures;
	}
}
