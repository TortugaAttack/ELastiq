package similarity.measures.entities;

import org.semanticweb.owlapi.model.OWLEntity;

public class DefaultEntitySimilarityMeasure implements IEntitySimilarityMeasure {

	@Override
	public double similarity(OWLEntity obj1, OWLEntity obj2) {
		if(obj1 != null && obj1.equals(obj2)){
			return 1;
		}
		return 0;
	}

}
