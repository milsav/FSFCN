package svc.fsfcn;

/**
 * Tuple containing a pair of attributes and correlation between them 
 * 
 * @author svc (svc@dmi.uns.ac.rs)
 */
public class AttributePair implements Comparable<AttributePair> {
	private Attribute atr1, atr2;
	private double correlation;
	
	public AttributePair(Attribute atr1, Attribute atr2, double correlation) {
		this.atr1 = atr1;
		this.atr2 = atr2;
		this.correlation = correlation;
	}
	
	public Attribute getAtr1() {
		return atr1;
	}
	
	public Attribute getAtr2() {
		return atr2;
	}
	
	public double getR() {
		return correlation;
	}
	
	public String toString() {
		return atr1 + " -- " + atr2 + " (r = " + correlation + ")";
	}

	public int hashCode() {
		return atr1.hashCode() + 31 * atr2.hashCode();
	}
	
	public boolean equals(Object o) {
		if (this == o)
			return true;
		
		AttributePair ap = (AttributePair) o;
		return ap.atr1.equals(atr1) && ap.atr2.equals(atr2);
	}
	
	@Override
	public int compareTo(AttributePair arg0) {
		if (Math.abs(arg0.correlation) > Math.abs(this.correlation))
			return 1;
		else if (Math.abs(arg0.correlation) < Math.abs(this.correlation))
			return -1;
		else 
			return 0;
	}
}
