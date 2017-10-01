package similarity.measures;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import org.semanticweb.owlapi.model.OWLClass;

import interpretation.ds.PointedInterpretation;
import interpretation.ds.RoleConnection;
import main.StaticValues;
import similarity.algorithms.generalEL.GeneralELRelaxedInstancesAlgorithm;
import similarity.algorithms.generalEL.SimilarityValueFactory;
import similarity.algorithms.specifications.BasicInputSpecification;
import statistics.StatStore;
import util.SetMath;

public class PointedISM implements ISimilarityMeasure<PointedInterpretation> {

	private static final Logger LOG = Logger.getLogger(StaticValues.LOGGER_NAME);
	
	private BasicInputSpecification m_input;
	private GeneralELRelaxedInstancesAlgorithm m_algo;
	
	public PointedISM(BasicInputSpecification input, GeneralELRelaxedInstancesAlgorithm algo) {
		m_input = input;
		m_algo = algo;
	}
	
	@Override
	public double similarity(PointedInterpretation p, PointedInterpretation q) {
		RoleConnection[] pSucc = p.getElement().getSuccessorObjectsArray(p.getInterpretation());
		RoleConnection[] qSucc = q.getElement().getSuccessorObjectsArray(p.getInterpretation());
		
		// after this, qSucc contains the only sensible (yet variable) role successors
		qSucc = SetMath.getSuccessorBaseSmart(pSucc, qSucc);
		
		
		OWLClass[] pInst = p.getElement().getInstantiatorsArray();
		OWLClass[] qInst = q.getElement().getInstantiatorsArray();
		
		// here we separate the variable instantiators from the ones that will always/never benefit similarity
		OWLClass[][] dummy    = SetMath.getInstantiatorBasesSmart(pInst, qInst);
		OWLClass[] qInstMust  = dummy[0];
		OWLClass[] qInstMight = dummy[1];
		
		// calculate fixed set weights
		double fixWeight = 0.0;
		for(OWLClass pC : pInst){
			fixWeight += m_input.getWeight(pC);
		}
		for(OWLClass qC : qInstMust){
			fixWeight += m_input.getWeight(qC);
		}
		for(RoleConnection pS : pSucc){
			fixWeight += m_input.getWeight(pS.getProperty());
		}
		
		double maxsim = 0.0;
		
		LOG.info("Iterating through 2^" + qSucc.length + " * 2^" + qInstMight.length + " = " +
				(Math.pow(2, qSucc.length) * Math.pow(2, qInstMight.length))
				);
		StatStore.getInstance().enterValue("successor subset amount", Math.pow(2, qSucc.length));
		StatStore.getInstance().enterValue("instances subset amount", Math.pow(2, qInstMight.length));
		StatStore.getInstance().enterValue("successor subset amount 2^n", qSucc.length * 1.0);
		StatStore.getInstance().enterValue("instances fixed amount", qInstMust.length * 1.0);
		long start = System.currentTimeMillis();
		int workers = Runtime.getRuntime().availableProcessors();
		
		if(workers > Math.pow(2, qSucc.length)){
//			workers = 1;
//		}
			// sequential variant
			for(int sI = 0; sI < Math.pow(2, qSucc.length); sI++){
				for(int iI = 0; iI < Math.pow(2, qInstMight.length); iI++){
					double weight = fixWeight + getSubsetWeight(qInstMight, iI, qSucc, sI);
					
					maxsim = Math.max(maxsim,
							(simCN(pInst, qInstMust, qInstMight, iI) + simSC(pSucc, qSucc, sI))/weight);
				}
			}
		}else{
			// parallel variant
			double div = Math.pow(2, qSucc.length)/workers;
			List<SubsetIterator> iterators = new LinkedList<SubsetIterator>();
			for(int i=0; i<workers; i++){
				iterators.add(new SubsetIterator(pInst, qInstMust, qInstMight, pSucc, qSucc, fixWeight,
											m_input, i*Math.round(div), (i+1)*Math.round(div), m_algo.getCurrentIteration()));
			}
			
			ExecutorService executor = Executors.newFixedThreadPool(workers);
			List<Future<Double>> results = new LinkedList<Future<Double>>();
			
			try {
				results = executor.invokeAll(iterators);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			executor.shutdown();
			
			for(Future<Double> result : results){
				try {
//					LOG.info("Process iterated over " + result.get().toString() + " elements");
					maxsim = Math.max(maxsim, result.get());
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ExecutionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		StatStore.getInstance().enterValue("subset times for 2^"+qSucc.length, System.currentTimeMillis() - start * 1.0);
		LOG.info("msim = " + maxsim);
		return maxsim;
	}
	
	/**
	 * Computes both directions of simCN in one call.
	 * @param pInst
	 * @param qInstMust
	 * @param qInstMight
	 * @param selection
	 * @return
	 */
	private double simCN(OWLClass[] pInst, OWLClass[] qInstMust, OWLClass[] qInstMight, int selection){
		char[] bin = Integer.toBinaryString(selection).toCharArray();
		
		// simCN(p, q)
		
		double sumA = 0.0;
		for(OWLClass pC : pInst){
			double max = 0.0;
			// iterate selection of mightBase
			for(int j = 0; j < bin.length; j++){
				if(bin[j] == '1'){
					max = Math.max(max, 
								   m_input.getWeight(pC) *
								   m_input.getPrimitiveMeasure().similarity(pC, 
										   qInstMight[qInstMight.length - bin.length + j]));
				}
			}
			// always iterate selection of mustBase aswell (could be stored and computed only once?!)
			for(OWLClass qC : qInstMust){
				max = Math.max(max,
							   m_input.getWeight(pC) *
							   m_input.getPrimitiveMeasure().similarity(pC, qC));
			}
			
			sumA += max;
		}
		
		// simCN(q, p)
		
		double sumB = 0.0;
		// variable selection
		for(int j = 0; j < bin.length; j++){
			double max = 0.0;
			if(bin[j] == '1'){
				for(OWLClass pC : pInst){
					max = Math.max(max, 
								   m_input.getWeight(qInstMight[qInstMight.length - bin.length + j]) *
								   m_input.getPrimitiveMeasure().similarity(
										   qInstMight[qInstMight.length - bin.length + j], pC));
				}
				sumB += max;
			}
		}
		// must base
		for(OWLClass qC : qInstMust){
			double max = 0.0;
			for(OWLClass pC : pInst){
				max = Math.max(max,
							   m_input.getWeight(qC) *
							   m_input.getPrimitiveMeasure().similarity(qC, pC));
			}
			sumB += max;
		}
		
		return sumA + sumB;
	}
	
	private double simSC(RoleConnection[] pSucc, RoleConnection[] qSucc, int selection){
		char[] bin = Integer.toBinaryString(selection).toCharArray();
		
		double w = m_input.getDiscountingFactor();
		
		// simSC(p, q)
		double sumA = 0.0;
		for(RoleConnection pR : pSucc){
			double max = 0.0;
			for(int j = 0; j < bin.length; j++){
				if(bin[j] == '1'){
					int pos = qSucc.length - bin.length + j;
					double roleSim = m_input.getPrimitiveMeasure().similarity(
								pR.getProperty(), qSucc[pos].getProperty());
					
					if(roleSim > 0){
						max = Math.max(max, 
									   roleSim *
									   ((1 - w) + (w *
									   SimilarityValueFactory.getFactory()
									   .getSimilarityValue(
											   pR.getToPointedInterpretation(),
											   qSucc[pos].getToPointedInterpretation(),
											   m_algo.getCurrentIteration()-1)
									   			.getValue(m_algo.getCurrentIteration()-1))));
					}
				}
			}
			sumA += m_input.getWeight(pR.getProperty()) * max;
		}
		
		double sumB = 0.0;
		for(int j = 0; j < bin.length; j++){
			double max = 0.0;
			if(bin[j] == '1'){
				int pos = qSucc.length - bin.length + j;
				
				for(RoleConnection pR : pSucc){
					double roleSim = m_input.getPrimitiveMeasure().similarity(
							qSucc[pos].getProperty(), pR.getProperty());
				
					if(roleSim > 0){
						max = Math.max(max, 
									   roleSim *
									   ((1 - w) + (w *
									   SimilarityValueFactory.getFactory()
									   .getSimilarityValue(
											   qSucc[pos].getToPointedInterpretation(),
											   pR.getToPointedInterpretation(),
											   m_algo.getCurrentIteration()-1)
									   			.getValue(m_algo.getCurrentIteration()-1))));
					}
				}
				sumB += m_input.getWeight(qSucc[pos].getProperty()) * max;
			}
		}
		
		return sumA + sumB;
	}

	private double getSubsetWeight(OWLClass[] qInst, int instSelection, RoleConnection[] qSucc, int succSelection){
		char[] instBin = Integer.toBinaryString(instSelection).toCharArray();
		char[] succBin = Integer.toBinaryString(succSelection).toCharArray();
		
		double weight = 0.0;
		for(int j = 0; j < instBin.length; j++){
			if(instBin[j] == '1'){
				weight += m_input.getWeight(qInst[qInst.length - instBin.length + j]);
			}
		}
		
		for(int j = 0; j < succBin.length; j++){
			if(succBin[j] == '1'){
				weight += m_input.getWeight(qSucc[qSucc.length - succBin.length + j].getProperty());
			}
		}
		
		return weight;
	}
}
