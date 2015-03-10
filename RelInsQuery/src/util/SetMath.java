package util;

import interpretation.ds.RoleAssertion;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import main.Main;
import main.StaticValues;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLEntity;

import similarity.algorithms.generalEL.GeneralELRelaxedInstancesAlgorithm;
import similarity.algorithms.specifications.WeightedInputSpecification;

public class SetMath {

	private static Logger LOG = Logger.getLogger(StaticValues.LOGGER_NAME);
	
	private static double totalSubsets = 0;
	private static double computeAmount = 0;
	
	public static double maxSubsets = 0;
	
	public static <T> Set<Set<T>> getAllSubsets(Set<T> set){
		if(set == null) return Collections.emptySet();
		Map<Integer, Set<Set<T>>> subsetsByCardinality = new HashMap<Integer, Set<Set<T>>>();
		Set<Set<T>> allSubsets = new HashSet<Set<T>>();
		
		int cardinality = set.size();
		
		subsetsByCardinality.put(cardinality, Collections.singleton(set));
		allSubsets.add(set);
		
		for(;cardinality > 0; cardinality--){
			Set<Set<T>> removeFromThese = subsetsByCardinality.get(cardinality);
			subsetsByCardinality.put(cardinality-1, new HashSet<Set<T>>());
			for(Set<T> removeFrom : removeFromThese){
				for(T obj : removeFrom){
					Set<T> newSet = new HashSet<T>(removeFrom);
					newSet.remove(obj);
					subsetsByCardinality.get(cardinality-1).add(newSet);
					allSubsets.add(newSet);
				}
			}
		}
		
		totalSubsets += allSubsets.size();
		maxSubsets = Math.max(maxSubsets, allSubsets.size());
		computeAmount++;
		return allSubsets;
	}
	
	public static Set<Set<OWLClass>> getAllClassSubsetsSmart(Set<OWLClass> reference, Set<OWLClass> set){
		Set<OWLClass> mustBase = new HashSet<OWLClass>();
		Set<OWLClass> variationBase = new HashSet<OWLClass>();
		WeightedInputSpecification spec = (WeightedInputSpecification)Main.getInputs();
		for(OWLClass ref : reference){
			for(OWLClass investigate : set){
				if(spec.getPrimitiveMeasure().similarity(ref, investigate) == 1){
					mustBase.add(investigate);
				}else if(spec.getPrimitiveMeasure().similarity(ref, investigate) > 0){
					variationBase.add(investigate);
				}// otherwise, leave definitely out
			}
		}
		Set<Set<OWLClass>> allSets = new HashSet<Set<OWLClass>>();
		for(Set<OWLClass> s : getAllSubsets(variationBase)){
			s.addAll(mustBase);
			allSets.add(s);
		}
		return allSets;
	}
	
	public static Set<Set<RoleAssertion>> getAllRoleSubsetsSmart(Set<RoleAssertion> reference, Set<RoleAssertion> set){
		Set<RoleAssertion> mustBase = new HashSet<RoleAssertion>();
		Set<RoleAssertion> variationBase = new HashSet<RoleAssertion>();
		WeightedInputSpecification spec = (WeightedInputSpecification)Main.getInputs();
		for(RoleAssertion ref : reference){
			for(RoleAssertion investigate : set){
				if(spec.getPrimitiveMeasure().similarity(ref.getProperty(), investigate.getProperty()) == 1){
					mustBase.add(investigate);
				}else if(spec.getPrimitiveMeasure().similarity(ref.getProperty(), investigate.getProperty()) > 0){
					variationBase.add(investigate);
				}// otherwise, leave definitely out
			}
		}
		Set<Set<RoleAssertion>> allSets = new HashSet<Set<RoleAssertion>>();
		for(Set<RoleAssertion> s : getAllSubsets(variationBase)){
			s.addAll(mustBase);
			allSets.add(s);
		}
		return allSets;
	}
	
	public static double getAvgSubsets(){
		return totalSubsets/computeAmount;
	}
}
