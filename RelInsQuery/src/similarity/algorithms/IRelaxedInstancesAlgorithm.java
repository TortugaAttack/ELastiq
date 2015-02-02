package similarity.algorithms;

import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLNamedIndividual;

import similarity.algorithms.specifications.IInputSpecification;


public interface IRelaxedInstancesAlgorithm<T extends IInputSpecification> {

	public Map<OWLNamedIndividual, Double> relaxedInstances(T specification);
}
