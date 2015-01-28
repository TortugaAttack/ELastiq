package similarity.algorithms.specifications;

import java.io.File;
import java.util.Map;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLObjectProperty;

import similarity.measures.entities.IEntitySimilarityMeasure;

public class WeightedInputSpecification extends BasicInputSpecification {
	
	private double m_discountingFactor;
	
	private Map<OWLEntity, Double> m_weightingFunction;
	
	public WeightedInputSpecification() {
		super();
	}
	
	@Override
	public void read(File f) {
		// TODO Auto-generated method stub
		super.read(f);
		// read some more
	}
	
	@Override
	public boolean isValid() {
		if(m_primitiveMeasure == null){
			System.err.println("Primitive measure not specified.");
			return false;
		}
		
		if(m_discountingFactor < 0 || m_discountingFactor > 1){
			System.err.println("Discounting factor must be in [0,1]");
			
			return false;
		}
		
		if(m_defaultWeight < 0 || m_defaultWeight > 1){
			System.err.println("Default entity weight must be in [0,1]");
			return false;
		}
		
		return super.isValid();
	}
	
	public void setDiscountingFactor(double discount){
		this.m_discountingFactor = discount;
	}
	
	public void setPrimitiveMeasure(IEntitySimilarityMeasure measure){
		this.m_primitiveMeasure = measure;
	}
	
	public void setWeight(OWLEntity e, double weight){
		if((e instanceof OWLClass || e instanceof OWLObjectProperty) // only accept weights for those
				&& weight <=1 && weight >= 0){ // only accept weight from [0,1]
			m_weightingFunction.put(e, weight);
		}
	}
	
	public void setDefaultWeight(double weight){
		this.m_defaultWeight = weight;
	}
	
	@Override
	public Double getWeight(OWLEntity e) {
		if(m_weightingFunction.containsKey(e))
			return m_weightingFunction.get(e);
		return super.getWeight(e); // default weight
	}
	
}
