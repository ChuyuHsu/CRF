package org.postagging.crf;

import static org.postagging.crf.CrfUtilities.safeAdd;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.postagging.crf.features.CrfFeaturesAndFilters;
import org.postagging.function.DerivableFunction;
import org.postagging.utilities.PosTaggerException;
import org.postagging.utilities.TaggedToken;

/**
 * This function is CONCAVE, not convex!!!
 * 
 * @author Asher Stern
 * Date: Nov 9, 2014
 *
 * @param <K>
 * @param <G>
 */
public class CrfLogLikelihoodFunction<K,G> extends DerivableFunction
{
	public CrfLogLikelihoodFunction(Iterable<List<? extends TaggedToken<K, G>>> corpus, CrfTags<G> crfTags,
			CrfFeaturesAndFilters<K, G> features, boolean useRegularization,
			double sigmaSquare_inverseRegularizationFactor)
	{
		super();
		this.corpus = corpus;
		this.crfTags = crfTags;
		this.features = features;
		this.useRegularization = useRegularization;
		this.sigmaSquare_inverseRegularizationFactor = sigmaSquare_inverseRegularizationFactor;
	}


	@Override
	public double value(double[] point)
	{
		logger.debug("Calculating value");
		
		CrfModel<K, G> model = createModel(point);
		double regularization = useRegularization?calculateRegularizationFactor(point):0.0;
		logger.debug("Calculating sum weighted features");
		double sumWeightedFeatures = calculateSumWeightedFeatures(model);
		logger.debug("Calculating sum log normalizations");
		double sumOfLogNormalizations = calculateSumOfLogNormalizations(model);
		double ret = sumWeightedFeatures - sumOfLogNormalizations - regularization;
		logger.debug("Calculating value - done.");
		return ret;
	}
	

	@Override
	public double[] gradient(double[] point)
	{
		logger.debug("Calculating gradient");
		
		CrfModel<K, G> model = createModel(point);
		
		logger.debug("Calculating empirical feature values");
		CrfEmpiricalFeatureValueDistributionInCorpus<K,G> empiricalFeatureValue = new CrfEmpiricalFeatureValueDistributionInCorpus<K,G>(corpus.iterator(),model.getFeatures());
		empiricalFeatureValue.calculate();
		
		logger.debug("Calculating expected feature values by models");
		CrfFeatureValueExpectationByModel<K, G> featureValueExpectationsByModel = new CrfFeatureValueExpectationByModel<K, G>(corpus.iterator(),model);
		featureValueExpectationsByModel.calculate();
		
		logger.debug("Creating gradient array.");
		double[] ret = new double[point.length];
		for (int parameterIndex=0;parameterIndex<ret.length;++parameterIndex)
		{
			double regularizationDerivative = useRegularization?calculateRegularizationDerivative(point[parameterIndex]):0.0;
			ret[parameterIndex] = empiricalFeatureValue.getEmpiricalFeatureValue()[parameterIndex] - featureValueExpectationsByModel.getFeatureValueExpectation()[parameterIndex] - regularizationDerivative;
		}
		return ret;
	}


	@Override
	public int size()
	{
		return features.getFilteredFeatures().length;
	}

	
	
	
	private double calculateSumWeightedFeatures(CrfModel<K, G> model)
	{
		double sumWeightedFeatures = 0.0;
		for (List<? extends TaggedToken<K, G> > sentence : corpus)
		{
			K[] sentenceAsArray = CrfUtilities.extractSentence(sentence);
			G previousTag = null;
			int tokenIndex=0;
			for (TaggedToken<K, G> taggedToken : sentence)
			{
				double sumForThisToken = CrfUtilities.oneTokenSumWeightedFeatures(model,sentenceAsArray,tokenIndex,taggedToken.getTag(),previousTag);
				sumWeightedFeatures = safeAdd(sumWeightedFeatures, sumForThisToken);
				
				previousTag = taggedToken.getTag();
				++tokenIndex;
			}
			if (tokenIndex!=sentence.size()) {throw new PosTaggerException("BUG");}
		}
		
		return sumWeightedFeatures;
	}
	
	private double calculateSumOfLogNormalizations(CrfModel<K, G> model)
	{
		double sum = 0.0;
		for (List<? extends TaggedToken<K, G> > sentence : corpus)
		{
			K[] sentenceAsArray = CrfUtilities.extractSentence(sentence);
			CrfRememberActiveFeatures<K, G> activeFeaturesForSentence = CrfRememberActiveFeatures.findForSentence(features, crfTags, sentenceAsArray);
			CrfForwardBackward<K, G> forwardBackward = new CrfForwardBackward<K, G>(model,sentenceAsArray,activeFeaturesForSentence);
			//forwardBackward.calculateForwardAndBackward();
			forwardBackward.calculateOnlyNormalizationFactor();
			
			double normalizationFactor = forwardBackward.getCalculatedNormalizationFactor();
			double logNormalizationFactor = Math.log(normalizationFactor);
			sum = safeAdd(sum, logNormalizationFactor);
		}
		
		return sum;
	}
	
	private double calculateRegularizationFactor(double[] parameters)
	{
		return normSquare(parameters)/(2*sigmaSquare_inverseRegularizationFactor);
	}
	
	private double calculateRegularizationDerivative(double parameter)
	{
		return parameter/sigmaSquare_inverseRegularizationFactor;
	}

	
	private CrfModel<K, G> createModel(double[] point)
	{
		if (point.length!=features.getFilteredFeatures().length) {throw new PosTaggerException("Number of parameters differs from number of features.");}
		ArrayList<Double> parameters = new ArrayList<Double>(point.length);
		for (double parameter : point)
		{
			parameters.add(parameter);
		}
		return new CrfModel<K, G>(crfTags,features,parameters);
	}
	
	private double normSquare(double[] vector)
	{
		double ret = 0.0;
		for (double component : vector)
		{
			ret = safeAdd(ret, component*component);
		}
		return ret;
	}
	
//	private void buildActiveFeaturesWholeCorpus()
//	{
//		activeFeaturesWholeCorpus = new LinkedList<CrfRememberActiveFeatures<K,G>>();
//		for (List<? extends TaggedToken<K, G> > sentence : corpus)
//		{
//			K[] sentenceAsArray = CrfUtilities.extractSentence(sentence);
//			CrfRememberActiveFeatures<K,G> activeFeaturesOfSentence = CrfRememberActiveFeatures.findForSentence(features,tags, sentenceAsArray);
//			activeFeaturesWholeCorpus.add(activeFeaturesOfSentence);
//		}
//		logger.info(RuntimeUtilities.getUsedMemory());
//	}

	private final Iterable<List<? extends TaggedToken<K, G> >> corpus;
	private final CrfTags<G> crfTags;
	private final CrfFeaturesAndFilters<K, G> features;
	private final boolean useRegularization;
	private final double sigmaSquare_inverseRegularizationFactor;
	
	private static final Logger logger = Logger.getLogger(CrfLogLikelihoodFunction.class);
}
