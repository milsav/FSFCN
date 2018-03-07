package svc.fsfcn;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import jsc.independentsamples.MannWhitneyTest;

/**
 * FSFCN -- feature selection based on feature correlation networks (main class)
 * The class uses the python igraph-based script for community detection
 * 
 * @author svc (svc@dmi.uns.ac.rs)
 */
public class FSFCN {
	// feature correlation network file
	private static final String FCN_FILENAME = "fcn.net";
	
	// files containing features selected by different FSFCN variants
	private static final String WT_OUT = "wt.cfg";
	private static final String FG_OUT = "fg.cfg";
	private static final String LV_OUT = "lv.cfg";
	private static final String IM_OUT = "im.cfg";
	
	// files containing partitions of feature correlation networks for different community detection techniques
	private static final String WT_INF = "wt.cfg.cl";
	private static final String FG_INF = "fg.cfg.cl";
	private static final String LV_INF = "lv.cfg.cl";
	private static final String IM_INF = "im.cfg.cl";
	
	// feature correlation network
	private FCNetwork fcn; 
	
	// selected features
	private int[] wt, fg, lv, im;
	
	public FSFCN(Dataset dataset, double featureRelevanceThreshold, boolean printInfo) {
		fcn = new FCNetwork(dataset, featureRelevanceThreshold);
		fcn.createConnected();
		if (printInfo)
			fcn.info();
	}
	
	public FSFCN(Dataset dataset, boolean printInfo) {
		fcn = new FCNetwork(dataset);
		fcn.createConnected();
		if (printInfo)
			fcn.info();
	}
	
	public FSFCN(Dataset dataset, double featureRelevanceThreshold) {
		this(dataset, featureRelevanceThreshold, false);
	}
	
	public FSFCN(Dataset dataset) {
		this(dataset, false);
	}
	
	public FSFCN(FCNetwork fcn) {
		this.fcn = fcn;
	}
	
	public void selectFeatures() 
		throws IOException 
	{
		if (fcn == null)
			throw new NullPointerException("[FSFCN error, selectFeatures], feature correlation network == null");
		
		File f = new File(FCN_FILENAME);
		if (f.exists()) {
			boolean deleted = f.delete();
			if (!deleted) {
				String msg = "[FSFCN error, select features], unable to delete " + f.getAbsolutePath();
				throw new IOException(msg);
			}
		}
		
		fcn.save(FCN_FILENAME);
		
		boolean ok = invokePythonScript();
		if (!ok) throw new IOException("Invoke python script error");
		
		wt = processOutFile(WT_OUT);
		fg = processOutFile(FG_OUT);
		lv = processOutFile(LV_OUT);
		im = processOutFile(IM_OUT);
	}
	
	private boolean invokePythonScript() {
		Runtime rt = Runtime.getRuntime();
		try {
			Process p = rt.exec("python clusterer.py");
			int status = p.waitFor();
			if (status != 0) {
				System.out.println("[FSFCN, invoke python script error] process staus != ok] status = " + status);
				return false;
			}
			
			InputStream is = p.getInputStream();
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String scriptOut = br.readLine();
			if (!scriptOut.equals("OK")) {
				System.out.println("[FSFCN, invoke python script error] script output != ok]");
				return false;
			}
		} catch (IOException ioe) {
			System.out.println("[FSFCN, invoke python script error, IOException]" + ioe.getMessage());
			return false;
		} catch (InterruptedException ie) {
			System.out.println("[FSFCN, invoke python script error, InterruptedException]" + ie.getMessage());
			return false;
		}
		
		return true;
	}
	
	private int[] processOutFile(String fileName) 
		throws IOException
	{
		ArrayList<Integer> features = new ArrayList<Integer>();
		
		BufferedReader br = new BufferedReader(new FileReader(fileName));
		String line = null;
		while ((line = br.readLine()) != null) {
			String[] toks = line.split(",");
			if (toks.length != 2) {
				br.close();
				throw new IOException("[FSFCN, script output file format error] " + fileName + ", " + line);
			}
			
			int feature = -1;
			try {
				feature = Integer.parseInt(toks[0]);
			} catch (NumberFormatException nfe) {
				br.close();
				throw new IOException("[FSFCN, script output file format error] " + fileName + ", " + line);
			}
			
			feature = feature + 1;    // class attribute has index 0
			
			features.add(feature);
		}
		
		// add class attribute to selected attributes
		features.add(0);
		
		int[] fsRes = new int[features.size()];
		for (int i = 0; i < features.size(); i++)
			fsRes[i] = features.get(i);
				
		br.close();
		
		return fsRes;
	}
	
	public int[] featuresWT() {
		return wt;
	}
	
	public int[] featuresFG() {
		return fg;
	}
	
	public int[] featuresIM() {
		return im;
	}
	
	public int[] featuresLV() {
		return lv;
	}
	
	public String info() {
		StringBuilder sb = new StringBuilder();
		sb.append("WT: ").append(Arrays.toString(wt)).append("\n");
		sb.append("FG: ").append(Arrays.toString(fg)).append("\n");
		sb.append("IM: ").append(Arrays.toString(im)).append("\n");
		sb.append("LV: ").append(Arrays.toString(lv)).append("\n");
			
		return sb.toString();
	}
	
	public static class ClusteringQuality {
		int numClusters;
		double q;
		int intraClusterLinks;
		int interClusterLinks;
		double intraClusterLinksW;
		double interClusterLinksW;
		int numRW;
		int numRS;
		double largest;
		boolean sdif;
		double ps;
		
		public String toString() {
			return numClusters + "," + ps + "," + sdif;
		}
		
		public double q() {
			return q;
		}
		
		public int numC()      { return numClusters; }
		public int RS()        { return numRS; }
		public int RW()        { return numRW; }
		public double intraw() { return intraClusterLinksW; }
		public double interw() { return interClusterLinksW; }
		
		public double qualityValue() {
			double fIntraW = 0.0;
			if (numClusters != 1)
				fIntraW = (double) intraClusterLinksW / (double) (intraClusterLinksW + interClusterLinksW);
			
			return q * fIntraW;
		}
	}
	
	public ClusteringQuality wtQuality() 
		throws Exception
	{
		return determineClusteringQuality(WT_INF);
	}
	
	public ClusteringQuality fgQuality() 
		throws Exception 
	{
		return determineClusteringQuality(FG_INF);
	}
	
	public ClusteringQuality lvQuality() 
		throws Exception
	{
		return determineClusteringQuality(LV_INF);
	}
	
	public ClusteringQuality imQuality() 
		throws Exception
	{
		return determineClusteringQuality(IM_INF);
	}
	
	private ClusteringQuality determineClusteringQuality(String fileName) 
		throws Exception
	{
		ClusteringQuality cq = new ClusteringQuality();
		
		HashMap<String, String> clusterMap = new HashMap<String, String>();
		BufferedReader br = new BufferedReader(new FileReader(fileName));
		
		HashMap<String, ArrayList<Attribute>> clusters = 
			new HashMap<String, ArrayList<Attribute>>();
		
		String line = null;
		
		cq.q = Double.parseDouble(br.readLine());
		cq.numClusters = Integer.parseInt(br.readLine());
		
		cq.intraClusterLinksW = 0;
		cq.interClusterLinksW = 0;
		
		while ((line = br.readLine()) != null) {
			String[] toks = line.split(",");
			if (toks.length != 3) {
				br.close();
				throw new IOException("[ERR, script output file format error] " + fileName + ", " + line);
			}
				
			String fName = toks[1];
			String cl = toks[2];
			clusterMap.put(fName, cl);
		}
		br.close();
				
		UndirectedSparseGraph<Attribute, AttributePair> g = fcn.getGraph();
		Iterator<Attribute> ait = g.getVertices().iterator();
		while (ait.hasNext()) {
			Attribute a = ait.next();
			String aName = a.getName();
			String clusterId = clusterMap.get(aName);
			ArrayList<Attribute> cluster = clusters.get(clusterId);
			if (cluster == null) {
				cluster = new ArrayList<Attribute>();
				clusters.put(clusterId, cluster);
			} 
			cluster.add(a);
		} 
		
		DescriptiveStatistics intrads = new DescriptiveStatistics();
		DescriptiveStatistics interds = new DescriptiveStatistics();
		
		Iterator<AttributePair> it = g.getEdges().iterator();
		while (it.hasNext()) {
			AttributePair ap = it.next();
			double w = ap.getR();
			String atr1Name = ap.getAtr1().getName();
			String atr2Name = ap.getAtr2().getName();
			String atr1cl = clusterMap.get(atr1Name);
			String atr2cl = clusterMap.get(atr2Name);
				
			if (atr1cl.equals(atr2cl)) { 
				cq.intraClusterLinksW += Math.abs(w);
				cq.intraClusterLinks++;
				intrads.addValue(Math.abs(w));
			} else {
				cq.interClusterLinksW += Math.abs(w);
				cq.interClusterLinks++;
				interds.addValue(Math.abs(w));
			}
		}
		
		compareWeights(intrads, interds, cq);
		examineClusters(g, clusters, clusterMap, cq);
		return cq;
	}
	
	private void compareWeights(DescriptiveStatistics intrads, DescriptiveStatistics interds, ClusteringQuality cq) {
		double[] intra = intrads.getValues();
		double[] inter = interds.getValues();
		
		int psCount = 0;
		for (int i = 0; i < intra.length; i++)
			for (int j = 0; j < inter.length; j++)
				if (intra[i] >= inter[j])
					psCount++;
		
		cq.ps = (double) psCount / (double) (intra.length * inter.length);
		
		if (intra.length < 2 || inter.length < 2) {
			cq.sdif = false;
			return;
		}
			
		MannWhitneyTest mwu = new MannWhitneyTest(intra, inter);
		double sp = mwu.getSP();
		cq.sdif = sp < 0.05;
	}
	
	private void examineClusters(
		UndirectedSparseGraph<Attribute, AttributePair> g, 
		HashMap<String, ArrayList<Attribute>> clusters, 
		HashMap<String, String> clusterMap,
		ClusteringQuality cq
	) 
	{
		cq.numRS = 0;
		cq.numRW = 0;
		
		int maxSize = 0;
		
		Iterator<Entry<String, ArrayList<Attribute>>> it = clusters.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, ArrayList<Attribute>> entry = it.next();
			ArrayList<Attribute> cl = entry.getValue();
			
			double totalIntraWeight = 0.0;
			double totalInterWeight = 0.0;
	
			boolean rs = true;
			if (cl.size() > maxSize)
				maxSize = cl.size();
			
			for (int i = 0; i < cl.size(); i++) {
				Attribute node = cl.get(i);
				String nodeCLID = clusterMap.get(node.getName());
				Iterator<Attribute> neiit = g.getNeighbors(node).iterator();
				
				double localIntraW = 0.0;
				double localInterW = 0.0;
				
				while (neiit.hasNext()) {
					Attribute nei = neiit.next();
					String neiCLID = clusterMap.get(nei.getName());
					AttributePair link = g.findEdge(node,  nei);
					
					if (nodeCLID.equals(neiCLID)) {
						// same cluster
						totalIntraWeight += Math.abs(link.getR());
						localIntraW += Math.abs(link.getR());
					} else {
						// different clusters
						totalInterWeight += Math.abs(link.getR());
						localInterW += Math.abs(link.getR());
					}
				}
				
				if (localInterW > localIntraW)
					rs = false;
			}
			
			boolean rw = totalIntraWeight > totalInterWeight;
			
			if (rw) cq.numRW++;
			if (rs) cq.numRS++;
		}
		
		cq.largest = (double) maxSize / (double) g.getVertexCount();
	}
}
