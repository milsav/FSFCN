package svc.fsfcn;

/**
 * One attribute in a dataset
 * 
 * @author svc (svc@dmi.uns.ac.rs)
 */
public class Attribute implements Comparable<Attribute> {
	private int id;
	private String name;
	private double classCorrelation;  // correlation with the class variable
	
	public Attribute(int id, String name) {
		this.name = name;
		this.id = id;
	}
	
	public String getName() {
		return name;
	}
	
	public String toString() {
		return name + ", class correlation = " + classCorrelation;
	}
	
	public void setClassCorrelation(double r) {
		this.classCorrelation = r;
	}
	
	public double getClassCorrelation() {
		return classCorrelation;
	}
	
	public int getId() {
		return id;
	}
	
	public int hashCode() {
		return name.hashCode();
	}
	
	public boolean equals(Object o) {
		if (this == o)
			return true;
		
		Attribute oa = (Attribute) o;
		return oa.name.equals(name);
	}

	@Override
	public int compareTo(Attribute arg0) {
		if (arg0.classCorrelation > this.classCorrelation)
			return 1;
		else if (arg0.classCorrelation < this.classCorrelation)
			return -1;
		else 
			return 0;
	}
}
