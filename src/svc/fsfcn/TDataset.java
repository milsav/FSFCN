package svc.fsfcn;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * Transposed data matrix
 * 
 * @author svc (svc@dmi.uns.ac.rs)
 */
public class TDataset extends Dataset {

	public TDataset(String inputFile) 
		throws IOException 
	{
		super(inputFile);
	}

	@Override
	protected void load() 
		throws IOException 
	{
		BufferedReader br = new BufferedReader(new FileReader(inputFile));
		br.readLine();
		while (br.readLine() != null)
			super.numAttr++;
		br.close();
		super.attribute = new Attribute[super.numAttr];
		
		br = new BufferedReader(new FileReader(inputFile));
		
		// parse header
		String header = br.readLine();
		String[] hToks = header.split(",");
		super.numInst = hToks.length - 1;
		super.classAttr = new String[numInst];
		for (int i = 1; i < hToks.length; i++) {
			super.classAttr[i - 1] = hToks[i];
		}
		
		// initialize data matrix
		super.data = new double[numInst][numAttr];
		
		// read attribute values
		String line = null;
		int noAttr = 0;
		while ((line = br.readLine()) != null) {
			String[] tok = line.split(",");
			if (tok.length != super.numInst + 1) {
				br.close();
				throw new IOException("IOERR [class: TDataset, constructor]");
			}
			
			super.attribute[noAttr] = new Attribute(noAttr, tok[0]);
			for (int i = 1; i < tok.length; i++) {
				super.data[i - 1][noAttr] = Double.parseDouble(tok[i]);
			}
			
			++noAttr;
		}
		
	}
}
