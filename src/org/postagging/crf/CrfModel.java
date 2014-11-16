package org.postagging.crf;

import java.util.ArrayList;

import org.postagging.crf.features.CrfFeaturesAndFilters;

/**
 * 
 * @author Asher Stern
 * Date: Nov 8, 2014
 *
 * @param <K> token type - must implement equals() and hashCode()
 * @param <G> tag type - must implement equals() and hashCode()
 */
public class CrfModel<K,G> // K = token, G = tag
{
	public CrfModel(CrfTags<G> crfTags, CrfFeaturesAndFilters<K, G> features, ArrayList<Double> parameters)
	{
		super();
		this.crfTags = crfTags;
		this.features = features;
		this.parameters = parameters;
	}
	
	
	
	public CrfTags<G> getCrfTags()
	{
		return crfTags;
	}
	public CrfFeaturesAndFilters<K, G> getFeatures()
	{
		return features;
	}
	public ArrayList<Double> getParameters()
	{
		return parameters;
	}



	private final CrfTags<G> crfTags;
	private final CrfFeaturesAndFilters<K, G> features;
	private final ArrayList<Double> parameters;
}
