package svc.fsfcn;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;

import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;

import JavaMI.MutualInformation;
import weka.core.Instances;

/**
 * Dataset
 * 
 * IMPORTANT: the class attribute should be the first attribute (the attribute with index 0) in a dataset 
 * 
 * @author svc (svc@dmi.uns.ac.rs)
 */
public class Dataset {
	protected String inputFile;
	
	//
	// data table
	//
	protected int numAttr;                       // the number of attributes in the dataset
	protected int numInst;                       // the number of instances in the dataset
	protected Attribute[] attrBeforeSort;        // the order of attributes before sorting
	protected Attribute[] attribute;             // attributes in the dataset
	protected String[] classAttr;                // the class attribute
	protected double[][] data;                   // the data matrix
	
	private SpearmansCorrelation sc = new SpearmansCorrelation();
	private double[][] attrCorrelation;
	private ArrayList<AttributePair> attrPairs = 
		new ArrayList<AttributePair>();
	
	public Dataset(Attribute[] attribute, ArrayList<Instance> insts) {
		this.attribute = attribute;
		this.numAttr = attribute.length;
		this.numInst = insts.size();
		this.inputFile = "Reduced dataset";
		
		this.data = new double[insts.size()][attribute.length];
		this.classAttr = new String[insts.size()];
		for (int i = 0; i < insts.size(); i++) {
			data[i] = insts.get(i).getAttrs();
			classAttr[i] = insts.get(i).getClassAttr();
		}
		
		init();
	}
	
	public Dataset(String inputFile) 
		throws IOException
	{
		this.inputFile = inputFile;
		load();
		init();
	}
	
	public Dataset(Instances wekaDataset) {
		numAttr = wekaDataset.numAttributes() - 1;
		numInst = wekaDataset.numInstances();
		
		data = new double[numInst][numAttr];
		classAttr = new String[numInst];
		
		attribute = new Attribute[numAttr];
		for (int i = 0; i < numAttr; i++) {
			//  class attribute has index 0
			attribute[i] = new Attribute(i, wekaDataset.attribute(i + 1).name());
		}
		
		Iterator<weka.core.Instance> it = wekaDataset.iterator();
		int noInst = 0;
		while (it.hasNext()) {
			weka.core.Instance inst = it.next();
			String st = inst.toString();
			String[] tok = st.split(",");
			if (tok.length != numAttr + 1) {
				System.out.println("ERR [class: Data, constructor, loading from weka dataset]");
			}
			
			classAttr[noInst] = tok[0];
			for (int i = 1; i < tok.length; i++) {
				// class attribute has index 0
				data[noInst][i - 1] = Double.parseDouble(tok[i]);
			}
			
			++noInst;
		}
		
		init();
	}
	
	public int getNumInstances() {
		return numInst;
	}
	
	public void desc() {
		System.out.println("Dataset -- " + inputFile);
		System.out.println("The number of attributes = " + numAttr);
		System.out.println("The number of instances  = " + numInst);
		
		class MInt {
			int i;
		}
		
		HashMap<String, MInt> map = new HashMap<String, MInt>();
		for (int i = 0; i < numInst; i++) {
			String c = classAttr[i]; 
			MInt m = map.get(c);
			if (m == null) {
				m = new MInt();
				m.i = 1;
				map.put(c, m);
			} else {
				m.i++;
			}
		}
		
		Iterator<Entry<String, MInt>> it = map.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, MInt> e = it.next();
			System.out.println("Class " + e.getKey() + ", no. instances = " + e.getValue().i);
		}
	}
	
	public void printData() {
		for (int i = 0; i < numInst; i++) {
			System.out.print(classAttr[i] + ": ");
			for (int j = 0; j < numAttr; j++) 
				System.out.print(data[i][j] + " ");
			System.out.println();
		}
	}
	
	protected void load() throws IOException {
		throw new IOException("Error, this method should be overriden");
	}
	
	private void init() {
		// compute correlations between attributes
		attrCorrelation = new double[numAttr][numAttr];
		for (int j = 1; j < numAttr; j++) {
			for (int i = 0; i < j; i++) {
				double c = __correlation(i, j);
				attrCorrelation[i][j] = c;
				attrCorrelation[j][i] = c;
				attrPairs.add(new AttributePair(attribute[i], attribute[j], c));
			}
		}
		
		Collections.sort(attrPairs);
		
		// compute correlations between each attribute and class
		for (int i = 0; i < numAttr; i++) {
			attribute[i].setClassCorrelation(__mi(i));
		}
		
		attrBeforeSort = new Attribute[attribute.length];
		for (int i = 0; i < attribute.length; i++) {
			attrBeforeSort[i] = attribute[i];
		}
		
		Arrays.sort(attribute, new Comparator<Attribute>() {
			public int compare(Attribute o1, Attribute o2) {
				if (Math.abs(o1.getClassCorrelation()) > Math.abs(o2.getClassCorrelation()))
					return -1;
				else if (Math.abs(o1.getClassCorrelation()) < Math.abs(o2.getClassCorrelation()))
					return 1;
				else 
					return 0;
			}			
		});
	}
	
	private double __correlation(int i, int j) {
		double[] src = new double[numInst];
		double[] dst = new double[numInst];
		
		for (int k = 0; k < numInst; k++) {
			src[k] = data[k][i];
			dst[k] = data[k][j];
		}
		
		return sc.correlation(src, dst);
	}
	
	/*
	private double __correlation(int attrIndex) {
		// compute Goodman-Kruskal index   (or G+ index)
		int numDistances = numInst * (numInst - 1) / 2;
		double[] dist = new double[numDistances];
		boolean[] sameCluster = new boolean[numDistances];
		int currentIndex = 0;
		
		for (int j = 1; j < numInst; j++) {
			for (int i = 0; i < j; i++) {
				double x = data[i][attrIndex];
				double y = data[j][attrIndex];
				double d = 0.0;
				if (x > y)
					d = x - y;
				else
					d = y - x;
				
				dist[currentIndex] = d;
				sameCluster[currentIndex] = classAttr[i].equals(classAttr[j]);		
				++currentIndex;
			}
		}
		
		double sPlus = 0.0;
		double sMinus = 0.0;
		for (int j = 1; j < dist.length; j++) {
			for (int i = 0; i < j; i++) {
				double d1 = dist[i];
				double d2 = dist[j];
				if (sameCluster[i] && !sameCluster[j]) {
					if (d1 < d2)
						sPlus += 1.0;
					else
						sMinus += 1.0;
				}
				else if (!sameCluster[i] && sameCluster[j]) {
					if (d2 < d1)
						sPlus += 1.0;
					else
						sMinus += 1.0;
				}
			}
		}
		
		// G- index
		//double dp = numDistances * (numDistances - 1) / 2;
		//return 1.0 - sMinus / dp;
		return (sPlus - sMinus) / (sPlus + sMinus);
	}
	*/
	
	private double __mi(int attrIndex) {
		double[] fv = new double[numInst];
		double[] sv = new double[numInst];
		for (int i = 0; i < numInst; i++) {
			fv[i] = data[i][attrIndex];
			sv[i] = classAttr[i].equals("AD") ? 0 : 1;
		}
		
		return MutualInformation.calculateMutualInformation(fv, sv);
	}
	
	public Attribute[] getAttributes() {
		return attribute;
	}
	
	public ArrayList<AttributePair> getAttributePairs() {
		return attrPairs;
	}
	
	public void subset(String configFile, String outFile) 
		throws IOException
	{
		System.out.println("Starting subdata extraction for " + configFile);
		BufferedReader br = new BufferedReader(new FileReader(configFile));
		LinkedList<String> selection = new LinkedList<String>();
		String line = null;
		while ((line = br.readLine()) != null) {
			line = line.trim();
			if (line.length() > 0)
				selection.add(line);
		}
		br.close();
		
		int numSelected = selection.size();
		System.out.println("The number of selected attributes: " + numSelected);
		
		double[][] subdata = new double[numInst][numSelected];
	
		for (int k = 0; k < selection.size(); k++) {
			String attr = selection.get(k);
			int attrIndex = findAttributeIndex(attr);
			if (attrIndex == -1)
				throw new IllegalArgumentException("Invalid attribute |" + attr + "|");
			
			for (int i = 0; i < numInst; i++) {
				subdata[i][k] = data[i][attrIndex];
			}
		}
		
		PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(outFile)));
		
		out.print("CLASS,");
		for (int k = 0; k < selection.size(); k++) {
			out.print(selection.get(k));
			if (k < selection.size() - 1)
				out.print(",");
		}
		out.println();
		for (int i = 0; i < numInst; i++) {
			out.print(classAttr[i] + ",");
			for (int j = 0; j < selection.size(); j++) {
				out.print(subdata[i][j]);
				if (j < selection.size() - 1)
					out.print(",");
			}
			out.println();
		}
		
		out.close();
		
	}
	
	private int findAttributeIndex(String attr) {
		for (int i = 0; i < attribute.length; i++) {
			if (attribute[i].getName().equals(attr))
				return i;
		}
		
		return -1;
	}
	
	private class Instance {
		private String classAttr;
		private double[] data;
		
		public Instance(String classAttr, double[] data) {
			this.classAttr = classAttr;
			this.data = data;
		}
		
		public double[] getAttrs() {
			return data;
		}
		
		public String getClassAttr() {
			return classAttr;
		}
	}
	
	public Dataset removeInstances(int numInstances) {
		ArrayList<Instance> dataSet = new ArrayList<Instance>(numInst);
		for (int k = 0; k < numInst; k++) {
			Instance inst = new Instance(classAttr[k], data[k]);
			dataSet.add(inst);
		}
		
		for (int k = 0; k < numInstances; k++) {
			int rndIndex = (int) (Math.random() * dataSet.size()); 
			dataSet.remove(rndIndex);
		}
		
		Attribute[] attr = new Attribute[attrBeforeSort.length];
		for (int i = 0; i < attrBeforeSort.length; i++) {
			attr[i] = new Attribute(attrBeforeSort[i].getId(), attrBeforeSort[i].getName());
		}
		
		Dataset d = new Dataset(attr, dataSet);
		return d;
	}
}
