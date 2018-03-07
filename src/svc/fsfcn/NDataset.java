package svc.fsfcn;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * Normal data matrix
 * 
 * @author svc (svc@dmi.uns.ac.rs)
 */
public class NDataset extends Dataset {

	public NDataset(String inputFile) 
		throws IOException
	{
		super(inputFile);
	}
	
	@Override
	public void load() 
		throws IOException
	{
		BufferedReader br = new BufferedReader(new FileReader(inputFile));
		br.readLine();
		while (br.readLine() != null)
			super.numInst++;
		br.close();
			
		br = new BufferedReader(new FileReader(inputFile));
			
		// parse header
		String header = br.readLine();
		String[] hToks = header.split(",");
		super.numAttr = hToks.length - 1;
		super.attribute = new Attribute[super.numAttr];
		for (int i = 1; i < hToks.length; i++) {
			super.attribute[i - 1] = new Attribute(i - 1, hToks[i]);
		}
		
		// initialize data matrix
		super.data = new double[numInst][numAttr];
		super.classAttr = new String[numInst];
		
		// read instances
		String line = null;
		int noInst = 0;
		while ((line = br.readLine()) != null) {
			String[] tok = line.split(",");
			if (tok.length != super.numAttr + 1) {
				br.close();
				throw new IOException("IOERR [class: NDataSet, constructor]");
			}
			
			super.classAttr[noInst] = tok[0];
			for (int i = 1; i < tok.length; i++) {
				super.data[noInst][i - 1] = Double.parseDouble(tok[i]);
			}
			
			++noInst;
		}
		
		br.close();
	}
}
