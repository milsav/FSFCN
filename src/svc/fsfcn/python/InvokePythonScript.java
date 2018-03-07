package svc.fsfcn.python;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Invoke python community detection script
 * 
 * @author svc (svc@dmi.uns.ac.rs)
 */
public class InvokePythonScript {
	public static void main(String[] args) 
		throws IOException, InterruptedException
	{
		Runtime rt = Runtime.getRuntime();
		Process p = rt.exec("python clusterer.py");
		int status = p.waitFor();
		System.out.println("Process p finished, status = " + status);
		
		InputStream is = p.getInputStream();
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(isr);
		String line;

		System.out.println("Output:");
		while ((line = br.readLine()) != null) {
		  System.out.println(line);
		}
	}
}
