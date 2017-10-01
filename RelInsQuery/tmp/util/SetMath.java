package util;

import java.util.HashSet;
import java.util.Set;
//import java.util.logging.Logger;

import org.semanticweb.owlapi.model.OWLClass;

import interpretation.ds.RoleConnection;
import main.Main;
//import main.StaticValues;
import similarity.algorithms.specifications.WeightedInputSpecification;

public class SetMath {

//	private static Logger LOG = Logger.getLogger(StaticValues.LOGGER_NAME);
	
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
}
