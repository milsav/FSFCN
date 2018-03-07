package svc.fsfcn.evaluation;

import java.text.DecimalFormat;
import java.util.Random;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import jsc.datastructures.PairedData;
import jsc.onesample.WilcoxonTest;
import weka.core.Instances;

/**
 * FSComparator: 10 runs of 10-cross-validation to compare FSFCN with 
 * other feature selection methods w.r.t. the accuracy of a given classifier
 * 
 * @author svc (svc@dmi.uns.ac.rs)
 */
public class FSComparator extends EvaluationBase {
	private static final int RUNS = 10;
	private static DecimalFormat df = new DecimalFormat("#.###");
	
	public FSComparator(String dsFileName, String classifierName) 
		throws Exception
	{
		super(dsFileName, classifierName);
	}
	
	public void compare(double thr) throws Exception {
		System.out.println("Classification model: " +  classifierName);
		
		DescriptiveStatistics full = new DescriptiveStatistics();
		DescriptiveStatistics cfs = new DescriptiveStatistics();
		DescriptiveStatistics[] fsfcn = new DescriptiveStatistics[4];
		for (int i = 0; i < fsfcn.length; i++)
			fsfcn[i] = new DescriptiveStatistics();
		DescriptiveStatistics[][] rank = new DescriptiveStatistics[4][EvaluationBase.NUM_RANKING_METHODS];
		for (int i = 0; i < 4; i++) 
			for (int j = 0; j < EvaluationBase.NUM_RANKING_METHODS; j++)
				rank[i][j] = new DescriptiveStatistics();
			
		for (int r = 0; r < RUNS; r++) {
			// randomize data
			int seed = r + 1;
			Random rand = new Random(seed);
			Instances randData = new Instances(dataset);
			randData.randomize(rand);
			randData.stratify(FOLDS);
				
			for (int n = 0; n < FOLDS; n++) {
				Instances train = randData.trainCV(FOLDS, n);
				Instances test = randData.testCV(FOLDS, n);
		        
				ClassifierEvaluation fullEval = evaluateWOFS(train, test);
				full.addValue(fullEval.getA());
		        
				ClassifierEvaluation cfsEval = evaluateCFS(train, test); 
				cfs.addValue(cfsEval.getA());
				
				ClassifierEvaluation[] fsfcnEval = evaluateFSFCN(train, test, thr);
				for (int i = 0; i < 4; i++) {
					fsfcn[i].addValue(fsfcnEval[i].getA());
						
					int attrSelected = fsfcnEval[i].getF();
					ClassifierEvaluation[] rankingEval = evaluateRankingMethods(train, test, attrSelected);
					for (int j = 0; j < rankingEval.length; j++) {
						rank[i][j].addValue(rankingEval[j].getA());
					}
				}
			}
		}
			
		// Statistical comparison and report
		for (int i = 0; i < 4; i++) {
			DescriptiveStatistics fsfcnInstance = fsfcn[i];
			String fsfcnName = EvaluationBase.getFSFCNVariant(i);
				
			// comparison with FULL
			String res = performTest(fsfcnInstance, full, thr, fsfcnName, "FULL");
			System.out.println(res);
				
			// comparison with CFS
			res = performTest(fsfcnInstance, cfs, thr, fsfcnName, "CFS");
			System.out.println(res);
				
			// comparison with RANKING methods
			for (int j = 0; j < EvaluationBase.NUM_RANKING_METHODS; j++) {
				DescriptiveStatistics dst = rank[i][j];
				String dstName = EvaluationBase.rankingMethodName(j);
				res = performTest(fsfcnInstance, dst, thr, fsfcnName, dstName);
				System.out.println(res);
			}
		}
	}
	
	
	private String performTest(DescriptiveStatistics src, DescriptiveStatistics alt, double frt, String srcName, String altName) {
		double[] srcv  = src.getValues();
		double[] altv = alt.getValues();
		WilcoxonTest wt = new WilcoxonTest(new PairedData(srcv, altv));
		double pVal = wt.getSP();
		double w = wt.getTestStatistic();
		String mark = "equal";
		boolean statDiff = pVal < 0.05;
		if (statDiff) {
			mark = "SDIFF"; 
			if (src.getMean() > alt.getMean())
				mark += "_FSFCN";
			else
				mark += "_ALT";
		}
		
		String ret = classifierName + "," + frt + "," + srcName + "," + altName + "," + df.format(src.getMean()) + "," + 
		                    df.format(alt.getMean()) + "," + mark + "," + df.format(w) + "," + df.format(pVal);
		return ret;
	}
	
	public static void main(String[] args) 
		throws Exception
	{	
		for (int i = 0; i < ClassifierFactory.clNames.length; i++) {
			FSComparator val = new FSComparator("JoinedSet.csv", ClassifierFactory.clNames[i]);
			val.compare(0.05);
		}
	}
}
