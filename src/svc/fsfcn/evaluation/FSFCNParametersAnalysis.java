package svc.fsfcn.evaluation;

import java.text.DecimalFormat;
import java.util.Random;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import weka.core.Instances;

import java.io.*;

/**
 * This class determines the accuracy of a classifier trained after FSFCN for different 
 * feature relevance thresholds
 * 
 * @author svc (svc@dmi.uns.ac.rs)
 */
public class FSFCNParametersAnalysis extends EvaluationBase {
	private static DecimalFormat df = new DecimalFormat("#.###");
	
	public FSFCNParametersAnalysis(String dsFileName, String classifierName) 
		throws Exception
	{
		super(dsFileName, classifierName);
	}
	
	private class Configuration {
		double threshold, accuracy, precision, recall;
		double features;
	
		public Configuration(double threshold, double accuracy, double precision, double recall, double features) {
			this.threshold = threshold;
			this.accuracy = accuracy;
			this.precision = precision;
			this.recall = recall;
			this.features = features;
		}
		
		public String toString() {
			return df.format(threshold) + "," + df.format(accuracy) + "," + df.format(precision) + "," + df.format(recall) + "," + df.format(features);
		}
	}
	
	public void perform(double min, double max) 
		throws Exception
	{
		PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter("accuracylog_" + classifierName + ".csv")));
		Random rand = new Random(1);
		Instances randData = new Instances(dataset);
		randData.randomize(rand);
		randData.stratify(FOLDS);

		DescriptiveStatistics[] numFeatures = new DescriptiveStatistics[4];
		for (int i = 0; i < numFeatures.length; i++)
			numFeatures[i] = new DescriptiveStatistics();
		
		Configuration[] best = new Configuration[4];
		Configuration[] worst = new Configuration[4];
		
		double t = min;
		boolean stop = false;
		while (t <= max && !stop) {
			DescriptiveStatistics[] accuracy = new DescriptiveStatistics[4];
			DescriptiveStatistics[] precision = new DescriptiveStatistics[4];
			DescriptiveStatistics[] recall = new DescriptiveStatistics[4];
			DescriptiveStatistics[] nf = new DescriptiveStatistics[4];
			DescriptiveStatistics[] sf = new DescriptiveStatistics[4];
			
			for (int i = 0; i < accuracy.length; i++) {
				accuracy[i] = new DescriptiveStatistics();
				precision[i] = new DescriptiveStatistics();
				recall[i] = new DescriptiveStatistics();
				nf[i] = new DescriptiveStatistics();
				sf[i] = new DescriptiveStatistics();
			}
			
			for (int n = 0; n < FOLDS; n++) {
				Instances train = randData.trainCV(FOLDS, n);
				Instances test = randData.testCV(FOLDS, n);
				try {
					ClassifierEvaluation[] ce = evaluateFSFCN(train, test, t);
					for (int i = 0; i < ce.length; i++) {
		        			accuracy[i].addValue(ce[i].getA());
		        			precision[i].addValue(ce[i].getP());
		        			recall[i].addValue(ce[i].getR());
		        			numFeatures[i].addValue(ce[i].getF());
		        			nf[i].addValue(ce[i].getF());
		        			sf[i].addValue(ce[i].getF());
					}
				} catch (RuntimeException re) {
		        		stop = true;
				}
			}
			
			if (!stop) {		
				for (int i = 0; i < 4; i++) {
					if (best[i] == null) {
						best[i] = new Configuration(t, accuracy[i].getMean(), precision[i].getMean(), recall[i].getMean(), nf[i].getMean());
					} else if (accuracy[i].getMean() > best[i].accuracy) {
						best[i].threshold = t;
						best[i].accuracy = accuracy[i].getMean();
						best[i].precision = precision[i].getMean();
						best[i].recall = recall[i].getMean();
						best[i].features = nf[i].getMean();
					}
					
					if (worst[i] == null) {
						worst[i] = new Configuration(t, accuracy[i].getMean(), precision[i].getMean(), recall[i].getMean(), nf[i].getMean());
					} else if (accuracy[i].getMean() < worst[i].accuracy) {
						worst[i].threshold = t;
						worst[i].accuracy = accuracy[i].getMean();
						worst[i].precision = precision[i].getMean();
						worst[i].recall = recall[i].getMean();
						worst[i].features = nf[i].getMean();
					}
					
					pw.println(classifierName + "," + EvaluationBase.getFSFCNVariant(i) + "," + t + "," + accuracy[i].getMean() + "," + accuracy[i].getStandardDeviation());
				}
				
				System.out.println(classifierName + "," + t + "," + 
						sf[0].getMean()  + "," + sf[0].getMax() + "," + 
						sf[1].getMean() + "," + sf[1].getMax() + "," + 
						sf[2].getMean() + "," + sf[2].getMax() + "," + 
						sf[3].getMean() + "," + sf[3].getMax());
				
				t += 0.01;
			}
		}
		
		pw.close();
	}
	
	public static void main(String[] args) throws Exception {
		for (int i = 0; i < ClassifierFactory.clNames.length; i++) {
			FSFCNParametersAnalysis analysis = new FSFCNParametersAnalysis("JoinedSet.csv", ClassifierFactory.clNames[i]);
			analysis.perform(0, 1.0);
		}
	}
}
