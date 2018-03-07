package svc.fsfcn;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import edu.uci.ics.jung.graph.util.EdgeType;

/**
 * Feature correlation network
 * 
 * @author svc (svc@dmi.uns.ac.rs)
 */
public class FCNetwork {
	private UndirectedSparseGraph<Attribute, AttributePair> g = 
		new UndirectedSparseGraph<Attribute, AttributePair>();
	
	private Attribute[] attribute;
	private ArrayList<AttributePair> attributePairs;
	private double minAttrCorrelation;
	
	private double relevantFeatureThreshold;
	
	public FCNetwork(Dataset ds) {
		this(ds, 0.05); // the default value of the feature relevance threshold
	}
	
	public FCNetwork(Dataset ds, double relevantFeatureThreshold) {
		this.relevantFeatureThreshold = relevantFeatureThreshold;
		attribute = ds.getAttributes();
		attributePairs = ds.getAttributePairs();
	
		for (int i = 0; i < attribute.length; i++) {
			if (Math.abs(attribute[i].getClassCorrelation()) > relevantFeatureThreshold)
				g.addVertex(attribute[i]);
		}
	}
	
	public void createConnected() {
		for (int i = 0; i < attributePairs.size(); i++) {
			AttributePair p = attributePairs.get(i);
			Attribute src = p.getAtr1();
			Attribute dst = p.getAtr2();
			if (g.containsVertex(src) && g.containsVertex(dst)) {
				g.addEdge(p, src, dst, EdgeType.UNDIRECTED);
				if (connected()) {
					minAttrCorrelation = p.getR();
					break;
				}
			}
		}
	}
	
	private boolean connected() {
		LinkedList<Attribute> q = new LinkedList<Attribute>();
		HashSet<String> visited = new HashSet<String>();
		Attribute start = g.getVertices().iterator().next();
		q.add(start);
		visited.add(start.getName());
		
		while (!q.isEmpty()) {
			Attribute current = q.removeFirst();
			Iterator<Attribute> nit = g.getNeighbors(current).iterator();
			while (nit.hasNext()) {
				Attribute n = nit.next();
				if (!visited.contains(n.getName())) {
					visited.add(n.getName());
					q.addLast(n);
				}
			}
		}
		
		return visited.size() == g.getVertexCount();
	}
	
	public void createThreshold(double t) {
		for (int i = 0; i < attributePairs.size(); i++) {
			AttributePair p = attributePairs.get(i);
			Attribute src = p.getAtr1();
			Attribute dst = p.getAtr2();
			double r = p.getR();
			if (Math.abs(r) >= t) {
				g.addEdge(p, src, dst, EdgeType.UNDIRECTED);
				minAttrCorrelation = p.getR();
			}
			else
				break;
		}
	}
	
	public void info() {
		double avgDeg = 2.0 * g.getEdgeCount() / (double) g.getVertexCount();
		System.out.println(
			"#nodes = " + g.getVertexCount() + 
			", #links = " + g.getEdgeCount() + 
			", avgdeg = " + avgDeg + 
			", minR = " + minAttrCorrelation + 
			", maxR = " + attributePairs.get(0).getR() + 
			", maxAttrClassCorrelation = " + attribute[0].getClassCorrelation()
		);
	}
	
	public UndirectedSparseGraph<Attribute, AttributePair> getGraph() {
		return g;
	}
	
	public void printNodes() {
		System.out.println(g.getVertices());
	}
	
	public int[] selectWithoutClustering() {
		ArrayList<Attribute> nodeList = new ArrayList<Attribute>();
		Iterator<Attribute> it = g.getVertices().iterator();
		while (it.hasNext()) {
			Attribute node = it.next();
			nodeList.add(node);	
		}
		
		Collections.sort(nodeList);
		ArrayList<Attribute> selection = new ArrayList<Attribute>();
		
		while (!nodeList.isEmpty()) {
			Attribute selected = nodeList.get(0);
			selection.add(selected);
			ArrayList<Attribute> toRemove = new ArrayList<Attribute>();
			toRemove.add(selected);
			toRemove.addAll(g.getNeighbors(selected));
			
			for (int i = 0; i < toRemove.size(); i++) {
				g.removeVertex(toRemove.get(i));
			}
	
			for (int i = 0; i < toRemove.size(); i++) {
				nodeList.remove(toRemove.get(i));
			}
		}
		
		int[] ret = new int[selection.size() + 1];
		for (int i = 0; i < selection.size(); i++) {
			ret[i] = selection.get(i).getId() + 1;   // IMPORTANT: class attribute has index 0
		}
		ret[selection.size()] = 0; // add class attribute to selected attributes
		return ret;
	}
	
	public void save(String outFile) 
		throws IOException
	{
		PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(outFile)));
		
		Iterator<Attribute> nit = g.getVertices().iterator();
		while (nit.hasNext()) {
			Attribute a = nit.next();
			pw.println(a.getId() + "," + a.getName() + "," + a.getClassCorrelation());
		}
		
		pw.println("links");
		Iterator<AttributePair> lit = g.getEdges().iterator();
		while (lit.hasNext()) {
			AttributePair ap = lit.next();
			pw.println(ap.getAtr1().getId() + "," + ap.getAtr2().getId() + "," + ap.getR());
		}
		
		pw.close();
	}
	
	public double getRelevantFeatureThreshold() {
		return relevantFeatureThreshold;
	}
}
