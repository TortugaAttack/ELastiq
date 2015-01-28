package similarity.algorithms;

import java.util.Set;

import org.semanticweb.owlapi.model.OWLNamedIndividual;

import similarity.algorithms.specifications.IInputSpecification;


public interface IRelaxedInstancesAlgorithm<T extends IInputSpecification> {

	public Set<OWLNamedIndividual> relaxedInstances(T specification);
}
