package util;

import interpretation.ds.RoleConnection;

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
import statistics.StatStore;

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
	
	/**
	 * Returns an array where ret[0] contains an array of OWLClass objects that must be contained in
	 * the similarity computation (always benefits) and ret[1] contains OWLClass objects that might 
	 * benefit the similarity computation and therefore need to be investigated with a subset iteration.
	 * 
	 * @param references the fixed OWLClass objects (no relaxing)
	 * @param classes the 'relaxible' OWLClass objects
	 * @return
	 */
	public static OWLClass[][] getInstantiatorBasesSmart(OWLClass[] references, OWLClass[] classes){
		Set<OWLClass> variationBase = new HashSet<OWLClass>();
		Set<OWLClass> mustBase = new HashSet<OWLClass>();
		WeightedInputSpecification spec = (WeightedInputSpecification)Main.getInputs();
		for(OWLClass c : classes){
			boolean var = false;
			for(OWLClass reference : references){
				double psim = spec.getPrimitiveMeasure().similarity(reference, c);
				if(psim == 1){
					mustBase.add(c);
					var = false;
					break;
				}else if(psim > 0){
					var = true;
				}
			}
			if(var){
				variationBase.add(c);
			}
		}
		
		OWLClass[] must = new OWLClass[mustBase.size()];
		OWLClass[] var = new OWLClass[variationBase.size()];
		OWLClass[][] ret = new OWLClass[2][];
		ret[0] = mustBase.toArray(must);
		ret[1] = variationBase.toArray(var);
		return ret;
	}
	
	public static Set<Set<RoleConnection>> getAllRoleSubsetsSmart(Set<RoleConnection> reference, Set<RoleConnection> set){
//		Set<RoleConnection> mustBase = new HashSet<RoleConnection>(); // never certain
		Set<RoleConnection> variationBase = new HashSet<RoleConnection>();
		WeightedInputSpecification spec = (WeightedInputSpecification)Main.getInputs();
		for(RoleConnection ref : reference){
			for(RoleConnection investigate : set){
				/*if(spec.getPrimitiveMeasure().similarity(ref.getProperty(), investigate.getProperty()) == 1){
					mustBase.add(investigate);
				}else */
				double pSim = spec.getPrimitiveMeasure().similarity(ref.getProperty(), investigate.getProperty());
				StatStore.getInstance().enterValue("primitive role similarity", pSim);
				if(pSim > 0){
					variationBase.add(investigate);
				}// otherwise, leave definitely out
			}
		}
//		System.out.print(" (|varbase|="+variationBase.size()+") ");
		Set<Set<RoleConnection>> allSets = new HashSet<Set<RoleConnection>>();
		for(Set<RoleConnection> s : getAllSubsets(variationBase)){
//			s.addAll(mustBase);
			allSets.add(s);
		}
		return allSets;
	}
	
	/**
	 * Different to the subset-base computation for instantiators, there is no way to be certain that
	 * some role successors *must* be included in the similarity computation, we can only exclude
	 * some role successors which is why only the variation base is returned here.
	 *  
	 * @param references
	 * @param successors
	 * @return
	 */
	public static RoleConnection[] getSuccessorBaseSmart(RoleConnection[] references, RoleConnection[] successors){
		Set<RoleConnection> base = new HashSet<RoleConnection>();
		WeightedInputSpecification spec = (WeightedInputSpecification)Main.getInputs();
		
		for(RoleConnection succ : successors){
			boolean add = false;
			for(RoleConnection reference : references){
				double pSim = spec.getPrimitiveMeasure().similarity(reference.getProperty(), succ.getProperty());
				if(pSim > 0){
					add = true;
					break;
				}
			}
			if(add)
				base.add(succ);
		}
		
		RoleConnection[] ret = new RoleConnection[base.size()];
		return base.toArray(ret);
	}
	
	public static double getAvgSubsets(){
		return totalSubsets/computeAmount;
	}
}
