package similarity.measures.entities;

import java.util.HashMap;
import java.util.Map;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLObjectProperty;

import similarity.measures.ISimilarityMeasure;

/**
 * This measure allows to customize similarity values between atomic properties and 
 * between atomic classes. If nothing specified, the default similarity is used.
 * @author max
 *
 */
public class PrimitiveEntitySimilarityMeasure extends DefaultEntitySimilarityMeasure {

	private Map<OWLObjectProperty, Map<OWLObjectProperty, Double>> m_propertySimilarities;
	
	private Map<OWLClass, Map<OWLClass, Double>> m_classSimilarities;
	
	public PrimitiveEntitySimilarityMeasure() {
		m_propertySimilarities = new HashMap<OWLObjectProperty, Map<OWLObjectProperty,Double>>();
		m_classSimilarities = new HashMap<OWLClass, Map<OWLClass,Double>>();
	}
	
	@Override
	public double similarity(OWLEntity obj1, OWLEntity obj2) {
		if(obj1 instanceof OWLObjectProperty && obj2 instanceof OWLObjectProperty){
			if(m_propertySimilarities.containsKey((OWLObjectProperty)obj1)
					&& m_propertySimilarities.get((OWLObjectProperty)obj1).containsKey((OWLObjectProperty)obj2)){
				return m_propertySimilarities.get((OWLObjectProperty)obj1).get((OWLObjectProperty)obj2);
			}else{
				return super.similarity(obj1, obj2);
			}
		}else if(obj1 instanceof OWLClass && obj2 instanceof OWLClass){
			if(m_classSimilarities.containsKey((OWLClass)obj1)
					&& m_classSimilarities.get((OWLClass)obj1).containsKey((OWLClass)obj2)){
				return m_classSimilarities.get((OWLClass)obj1).get((OWLClass)obj2);
			}else{
				return super.similarity(obj1, obj2);
			}
		}else{
			return 0; // if the object type is different, return totally dissimilar
		}
	}
	
	public void registerSimilarity(OWLObjectProperty p1, OWLObjectProperty p2, double sim){
		if(!m_propertySimilarities.containsKey(p1))
			m_propertySimilarities.put(p1, new HashMap<OWLObjectProperty, Double>());
		m_propertySimilarities.get(p1).put(p2, sim);
	}
	
	public void registerSimilarity(OWLClass c1, OWLClass c2, double sim){
		if(!m_classSimilarities.containsKey(c1))
			m_classSimilarities.put(c1, new HashMap<OWLClass, Double>());
		m_classSimilarities.get(c1).put(c2, sim);
	}
	
}
