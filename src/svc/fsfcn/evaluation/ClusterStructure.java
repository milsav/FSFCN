package svc.fsfcn.evaluation;


import svc.fsfcn.Dataset;
import svc.fsfcn.FCNetwork;
import svc.fsfcn.FSFCN;
import svc.fsfcn.NDataset;

/**
 * Examine cluster (community) structure in feature correlation networks
 * 
 * @author svc (svc@dmi.uns.ac.rs)
 */
public class ClusterStructure {
	private Dataset ds;
	
	public ClusterStructure(String dsName) 
		throws Exception
	{
		ds = new NDataset(dsName);
	}
	
	public void examine() 
		throws Exception
	{
		double t = 0.0;
		while (t <= 1.0) {
			FCNetwork fcn = new FCNetwork(ds, t);
			fcn.createConnected();
			if (fcn.getGraph().getEdgeCount() == 0)
				break;
			
			int numNodes = fcn.getGraph().getVertexCount();
			int numLinks = fcn.getGraph().getEdgeCount();
			FSFCN f = new FSFCN(fcn);
			f.selectFeatures();
			System.out.println(t + ", " + numNodes + ", " + numLinks + "," + f.wtQuality() + "," + f.fgQuality() + "," + f.lvQuality() + "," + f.imQuality());
			
			t += 0.01;
		}
	}
}