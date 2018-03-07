package svc.fsfcn.evaluation;

import weka.classifiers.Classifier;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.functions.Logistic;
import weka.classifiers.functions.SMO;
import weka.classifiers.rules.JRip;
import weka.classifiers.trees.J48;
import weka.classifiers.trees.LMT;
import weka.classifiers.trees.RandomForest;

/**
 * Factory for instantiating classifiers
 * 
 * @author svc (svc@dmi.uns.ac.rs)
 */
public class ClassifierFactory {
	public static String[] clNames = {
		"RF",
		"J48",
		"LMT",
		"JRIP",
		"LOGR",
		"SMO",
		"NB"
	};
	
	public static Classifier makeClassifier(String classifierName) {
		if (classifierName.equals("RF")) return new RandomForest();
		else if (classifierName.equals("J48")) return new J48();
		else if (classifierName.equals("LMT")) return new LMT();
		else if (classifierName.equals("JRIP")) return new JRip();
		else if (classifierName.equals("LOGR")) return new Logistic();
		else if (classifierName.equals("SMO")) return new SMO();
		else if (classifierName.equals("NB")) return new NaiveBayes();
	
		System.out.println("Classifier factory error, unknown classifier: " + classifierName);
		return null;
	}
	
}
