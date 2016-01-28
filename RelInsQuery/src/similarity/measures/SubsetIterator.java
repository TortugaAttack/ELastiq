package similarity.measures;

import java.util.concurrent.Callable;

import org.semanticweb.owlapi.model.OWLClass;

import interpretation.ds.RoleConnection;
import similarity.algorithms.generalEL.SimilarityValueFactory;
import similarity.algorithms.specifications.BasicInputSpecification;

public class SubsetIterator implements Callable<Double> {

	
	private OWLClass[] pInst;
	private OWLClass[] qInstMust;
	private OWLClass[] qInstMight;
	private RoleConnection[] pSucc;
	private RoleConnection[] qSucc;
	
	private double fixWeight;
	
	private long from;
	private long to;
	
	private BasicInputSpecification m_input;
	private int currentIteration;
	
	public SubsetIterator(OWLClass[] pInst, OWLClass[] qInstMust, OWLClass[] qInstMight,
						RoleConnection[] pSucc, RoleConnection[] qSucc, double fixWeight,
						BasicInputSpecification input, long from, long to, int currentIteration) {
		 this.pInst = pInst;
		 this.qInstMust = qInstMust;
		 this.qInstMight = qInstMight;
		 this.pSucc = pSucc;
		 this.qSucc = qSucc;
		 
		 this.fixWeight = fixWeight;
		 
		 this.from = from;
		 this.to = to;
		 
		 m_input = input;
		 this.currentIteration = currentIteration;
	}
	
	@Override
	public Double call() throws Exception {
		double maxsim = 0.0;
		
		// so far only parallelized outer loop
		for(long sI = from; sI < to; sI++){
			for(int iI = 0; iI < Math.pow(2, qInstMight.length); iI++){ 
				double weight = fixWeight + getSubsetWeight(qInstMight, iI, qSucc, sI);
				
				maxsim = Math.max(maxsim,
						(simCN(pInst, qInstMust, qInstMight, iI) + simSC(pSucc, qSucc, sI))/weight);
			}
		}
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
	
	private double simSC(RoleConnection[] pSucc, RoleConnection[] qSucc, long selection){
		char[] bin = Long.toBinaryString(selection).toCharArray();
		
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
											   currentIteration-1)
									   			.getValue(currentIteration-1))));
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
											   currentIteration-1)
									   			.getValue(currentIteration-1))));
					}
				}
				sumB += m_input.getWeight(qSucc[pos].getProperty()) * max;
			}
		}
		
		return sumA + sumB;
	}

	private double getSubsetWeight(OWLClass[] qInst, long instSelection, RoleConnection[] qSucc, long succSelection){
		char[] instBin = Long.toBinaryString(instSelection).toCharArray();
		char[] succBin = Long.toBinaryString(succSelection).toCharArray();
		
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
