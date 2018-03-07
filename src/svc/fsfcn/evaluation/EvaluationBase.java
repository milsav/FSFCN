package svc.fsfcn.evaluation;

import svc.fsfcn.Dataset;
import svc.fsfcn.FCNetwork;
import svc.fsfcn.FSFCN;
import weka.attributeSelection.ASEvaluation;
import weka.attributeSelection.AttributeSelection;
import weka.attributeSelection.BestFirst;
import weka.attributeSelection.CfsSubsetEval;
import weka.attributeSelection.CorrelationAttributeEval;
import weka.attributeSelection.GainRatioAttributeEval;
import weka.attributeSelection.InfoGainAttributeEval;
import weka.attributeSelection.Ranker;
import weka.attributeSelection.ReliefFAttributeEval;
import weka.classifiers.Classifier;
import weka.classifiers.evaluation.Evaluation;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

/**
 * The base class for the evaluation of FSFCN and other feature selection methods
 * 
 * @author svc (svc@dmi.uns.ac.rs)
 */
public abstract class EvaluationBase {
	// different variants of the FSFCN method
	protected static final int WT = 0;
	protected static final int FG = 1;
	protected static final int LV = 2;
	protected static final int IM = 3;
	
	public static final int NUM_RANKING_METHODS = 4;
	
	// the number of folds in cross validation procedures
	protected static final int FOLDS = 10;	
	
	// attributes selected by FSFCN
	protected int[][] selectedAttributes = new int[4][];
	
	protected Instances dataset;
	protected  String classifierName;
	
	public EvaluationBase(String dsFileName, String classifierName) 
		throws Exception
	{
		this.classifierName = classifierName;
		DataSource source = new DataSource(dsFileName);
		dataset = source.getDataSet();
		dataset.setClassIndex(0); //  class attribute has index 0
	}
	
	public static String getFSFCNVariant(int i) {
		if (i == 0) return "WT";
		else if (i == 1) return "FG";
		else if (i == 2) return "LV";
		else if (i == 3) return "IM";
		else return "UNKNOWN";
	}
	
	public static String rankingMethodName(int i) {
		if (i == 0) return "RELIEF";
		else if (i == 1) return "GAINR";
		else if (i == 2) return "INFOG";
		else if (i == 3) return "CORR";
		else return "UNKNOWN";
	}
	
	protected Instances keepAttributes(int[] attrsToRemove, Instances data) 
		throws Exception
	{
		Remove remove = new Remove();
		remove.setAttributeIndicesArray(attrsToRemove);
		remove.setInvertSelection(true);
		remove.setInputFormat(data);
		return Filter.useFilter(data, remove);
	}
	
	protected ClassifierEvaluation evaluateClassifier(Instances train, Instances test, int numFeatures) 
		throws Exception
	{
		Classifier cls = ClassifierFactory.makeClassifier(classifierName);
		cls.buildClassifier(train);
		Evaluation eval = new Evaluation(train);
		eval.evaluateModel(cls, test);
		if (eval.unclassified() > 0)
			System.out.println("[EvaluationBase, evaluateClassifier WARNING] -- there are unclassified data points");
		
		double accuracy = eval.correct() / (double) test.numInstances();	
		double precision = 0.0;
		if (!Double.isFinite(eval.precision(0)) && !Double.isFinite(eval.precision(1))) precision = 0; 
		else if (!Double.isFinite(eval.precision(0))) precision = eval.precision(1);
		else if (!Double.isFinite(eval.precision(1))) precision = eval.precision(0);
		else precision = (eval.precision(0) + eval.precision(1)) / 2;
		
		double recall = 0.0;
		if (eval.recall(0) == Double.NaN && eval.recall(1) == Double.NaN) recall = 0;
		else if (eval.recall(0) == Double.NaN) recall = eval.recall(1);
		else if (eval.recall(1) == Double.NaN) recall = eval.recall(0);
		else recall = (eval.recall(0) + eval.recall(1)) / 2;
		
		return new ClassifierEvaluation(accuracy, precision, recall, numFeatures);
	}
	
	
	/**
	 * Without feature selection
	 */
	protected ClassifierEvaluation evaluateWOFS(Instances train, Instances test)
		throws Exception
	{
		return evaluateClassifier(train, test, test.numAttributes() - 1);
	}
	
	
	/**
	 * WEKA-CFS feature selection
	 */
	protected int[] cfsSelector(Instances data) throws Exception {
		AttributeSelection attsel = new AttributeSelection();
		CfsSubsetEval eval = new CfsSubsetEval();
		BestFirst search = new BestFirst();
		attsel.setEvaluator(eval);
		attsel.setSearch(search);
		attsel.SelectAttributes(data);
		int[] indices = attsel.selectedAttributes();    
		return indices;
	}
	
	protected ClassifierEvaluation evaluateCFS(Instances train, Instances test) 
		throws Exception
	{
		int[] cfsAttrs  = cfsSelector(train);
		Instances trainAfterSelection = keepAttributes(cfsAttrs, train);
		Instances testAfterSelection= keepAttributes(cfsAttrs, test);
		return evaluateClassifier(trainAfterSelection, testAfterSelection, trainAfterSelection.numAttributes() - 1);
	}
	
	
	/**
	 * FEATURE CORRELATION NETWORK WITHOUT CLUSTERING
	 */
	protected ClassifierEvaluation evaluateFCNWOC(Instances train, Instances test) 
		throws Exception
	{
		Dataset ds = new Dataset(train);
		FCNetwork fcn = new FCNetwork(ds);
		fcn.createConnected();
		int[] attr =  fcn.selectWithoutClustering();
		Instances trainAfterSelection = keepAttributes(attr, train);
		Instances testAfterSelection= keepAttributes(attr, test);
		return evaluateClassifier(trainAfterSelection, testAfterSelection, attr.length -1);
	}
	
	
	/**
	 * RANKING METHODS (relieff, gainratio, information gain, pearson correlation)
	 */
	protected ClassifierEvaluation[] evaluateRankingMethods(Instances train, Instances test, int numAttrsToSelect) 
		throws Exception
	{
		ASEvaluation[] eval = new ASEvaluation[NUM_RANKING_METHODS];
		eval[0] = new ReliefFAttributeEval();
		eval[1] = new GainRatioAttributeEval();
		eval[2] = new InfoGainAttributeEval();
		eval[3] = new CorrelationAttributeEval();
		
		ClassifierEvaluation[] res = new ClassifierEvaluation[NUM_RANKING_METHODS];
		
		for (int i = 0; i < eval.length; i++) {
			AttributeSelection attsel = new AttributeSelection();
			Ranker searchMethod = new Ranker();
			searchMethod.setNumToSelect(numAttrsToSelect);
			attsel.setSearch(searchMethod);
			attsel.setEvaluator(eval[i]);
			attsel.SelectAttributes(train);
			int[] indices = attsel.selectedAttributes();  
			Instances trainAfterSelection = keepAttributes(indices, train);
			Instances testAfterSelection= keepAttributes(indices, test);
			res[i] = evaluateClassifier(trainAfterSelection, testAfterSelection, indices.length - 1);
		}
		
		return res;
	}
	
	
	/**
	 *  FSFCN (feature selection based on feature correlation networks)
	 */
	public ClassifierEvaluation[] evaluateFSFCN(Instances train, Instances test, double featureRelevanceThreshold) 
		throws Exception
	{
		Dataset ds = new Dataset(train);
		FCNetwork fcn = new FCNetwork(ds, featureRelevanceThreshold);
		fcn.createConnected();
		
		if (fcn.getGraph().getEdgeCount() == 0)
			throw new RuntimeException("Empty FCN for relevance threshold " + featureRelevanceThreshold);
		
		FSFCN f = new FSFCN(fcn);
		f.selectFeatures();
		selectedAttributes[WT] = f.featuresWT();
		selectedAttributes[FG] = f.featuresFG();
		selectedAttributes[LV] = f.featuresLV();
		selectedAttributes[IM] = f.featuresIM();
		ClassifierEvaluation[] res = new ClassifierEvaluation[4];
		
		for (int i = 0; i < selectedAttributes.length; i++) {
			Instances trainAfterSelection = keepAttributes(selectedAttributes[i], train);
			Instances testAfterSelection= keepAttributes(selectedAttributes[i], test);
			res[i] = evaluateClassifier(trainAfterSelection, testAfterSelection, selectedAttributes[i].length - 1);
		}
		
		return res;
	}
	
	
	/**
	 * WT FSFCN variant
	 */
	public ClassifierEvaluation evaluateWT(Instances train, Instances test, double featureRelevanceThreshold) 
		throws Exception
	{
		Dataset ds = new Dataset(train);
		FCNetwork fcn = new FCNetwork(ds, featureRelevanceThreshold);
		fcn.createConnected();
		
		if (fcn.getGraph().getEdgeCount() == 0)
			throw new RuntimeException("Empty FCN for relevance threshold " + featureRelevanceThreshold);
		
		FSFCN f = new FSFCN(fcn);
		f.selectFeatures();
		selectedAttributes[WT] = f.featuresWT();
		Instances trainAfterSelection = keepAttributes(selectedAttributes[WT], train);
		Instances testAfterSelection= keepAttributes(selectedAttributes[WT], test);
		return evaluateClassifier(trainAfterSelection, testAfterSelection, selectedAttributes[WT].length - 1);
	}

	
	/**
	 * FG FSFCN variant
	 */
	public ClassifierEvaluation evaluateFG(Instances train, Instances test, double featureRelevanceThreshold) 
		throws Exception
	{
		Dataset ds = new Dataset(train);
		FCNetwork fcn = new FCNetwork(ds, featureRelevanceThreshold);
		fcn.createConnected();
		
		if (fcn.getGraph().getEdgeCount() == 0)
			throw new RuntimeException("Empty FCN for relevance threshold " + featureRelevanceThreshold);
		
		FSFCN f = new FSFCN(fcn);
		f.selectFeatures();
		selectedAttributes[FG] = f.featuresWT();
		Instances trainAfterSelection = keepAttributes(selectedAttributes[FG], train);
		Instances testAfterSelection= keepAttributes(selectedAttributes[FG], test);
		return evaluateClassifier(trainAfterSelection, testAfterSelection, selectedAttributes[FG].length - 1);
	}

	
	/**
	 * LV FSFCN variant
	 */
	public ClassifierEvaluation evaluateLV(Instances train, Instances test, double featureRelevanceThreshold) 
		throws Exception
	{
		Dataset ds = new Dataset(train);
		FCNetwork fcn = new FCNetwork(ds, featureRelevanceThreshold);
		fcn.createConnected();
		
		if (fcn.getGraph().getEdgeCount() == 0)
			throw new RuntimeException("Empty FCN for relevance threshold " + featureRelevanceThreshold);
		
		FSFCN f = new FSFCN(fcn);
		f.selectFeatures();
		selectedAttributes[LV] = f.featuresWT();
		Instances trainAfterSelection = keepAttributes(selectedAttributes[LV], train);
		Instances testAfterSelection= keepAttributes(selectedAttributes[LV], test);
		return evaluateClassifier(trainAfterSelection, testAfterSelection, selectedAttributes[LV].length - 1);
	}
	
	
	/**
	 * IM FSFCN variant
	 */
	public ClassifierEvaluation evaluateIM(Instances train, Instances test, double featureRelevanceThreshold) 
		throws Exception
	{
		Dataset ds = new Dataset(train);
		FCNetwork fcn = new FCNetwork(ds, featureRelevanceThreshold);
		fcn.createConnected();
		
		if (fcn.getGraph().getEdgeCount() == 0)
			throw new RuntimeException("Empty FCN for relevance threshold " + featureRelevanceThreshold);
		
		FSFCN f = new FSFCN(fcn);
		f.selectFeatures();
		selectedAttributes[IM] = f.featuresWT();
		Instances trainAfterSelection = keepAttributes(selectedAttributes[IM], train);
		Instances testAfterSelection= keepAttributes(selectedAttributes[IM], test);
		return evaluateClassifier(trainAfterSelection, testAfterSelection, selectedAttributes[IM].length - 1);
	}
}
