package interpretation.generator;

import java.util.Iterator;

import interpretation.ds.CanonicalDomain;
import interpretation.ds.DomainNode;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

public class KBDomainElementGenerator implements IDomainElementGenerator{

	private CanonicalInterpretationGenerator m_generator;

	@Override
	public void generate(IInterpretationGenerator g, boolean small) {
		if(!(g instanceof CanonicalInterpretationGenerator)) return;
		m_generator = (CanonicalInterpretationGenerator)g;

		OWLReasoner reasoner = m_generator.getOntologyOperator().getReasoner(true);
		
		// add domain elements for all ABox individuals
		for(OWLNamedIndividual ind : m_generator.getOntologyOperator().getOntology().getIndividualsInSignature()){
			System.out.println(ind + ": " +reasoner.getTypes(ind, false));
			
			DomainNode<?> node = m_generator.getDomain().addDomainElement(ind);
			
			addInstantiators(node);
		}
		
		// add domain elements for all existential restriction filler concepts that don't have instances
		for(OWLObjectSomeValuesFrom some : m_generator.getOntologyOperator().getExistentialRestrictionStore().getRestrictions()){
			if(small){ // only when requested tries to find a mapping to individuals
				NodeSet<OWLNamedIndividual> instances = reasoner.getInstances(some.getFiller(), false);
				if(!instances.isEmpty()){
					Iterator<Node<OWLNamedIndividual>> it1 = instances.iterator();
					if(it1.hasNext()){
						Iterator<OWLNamedIndividual> it2 = it1.next().iterator();
						if(it2.hasNext()){
							m_generator.addAssociation(m_generator.getClassRepresentation(some.getFiller()), it2.next());
							continue;
						}
					}
				}
			}
			// only generates a new domain element if no instance of the filler exists yet
			DomainNode<?> node = m_generator.getDomain().addDomainElement(some.getFiller());
			
			addInstantiators(node);
		}
	}
	
	private void addInstantiators(DomainNode<?> node){
		if(node.getId() instanceof OWLClassExpression){
			addInstantiators((DomainNode<OWLClassExpression>)node, (OWLClassExpression)node.getId());
		}else if(node.getId() instanceof OWLIndividual){
			addInstantiators((DomainNode<OWLNamedIndividual>)node, (OWLNamedIndividual)node.getId());
		}
		// do not accept other id-types
	}
	
	private void addInstantiators(DomainNode<OWLNamedIndividual> node, OWLNamedIndividual individual){
		OWLReasoner reasoner = m_generator.getOntologyOperator().getReasoner();
		NodeSet<OWLClass> types = reasoner.getTypes(individual, false);
		Iterator<Node<OWLClass>> it1 = types.iterator();
		while(it1.hasNext()){
			Iterator<OWLClass> it2 = it1.next().iterator();
			while(it2.hasNext()){
				OWLClass inst = it2.next();
				if(!m_generator.isRestrictedInstantiator(inst))
					node.addInstantiator(inst);
			}
		}
	}
	
	private void addInstantiators(DomainNode<OWLClassExpression> node, OWLClassExpression individual){
		OWLClass classRep = m_generator.getClassRepresentation(node.getId());
		if(classRep != null){
			OWLReasoner reasoner = m_generator.getOntologyOperator().getReasoner();
			// add all super class instantiators
			NodeSet<OWLClass> classes = reasoner.getSuperClasses(classRep, false);
			Iterator<Node<OWLClass>> nodeIt = classes.iterator();
			while(nodeIt.hasNext()){
				Iterator<OWLClass> it = nodeIt.next().iterator();
				while(it.hasNext()){
					OWLClass inst = it.next();
					if(!m_generator.isRestrictedInstantiator(inst))
						node.addInstantiator(inst);
				}
			}
			// add all equivalent class instantiators
			Iterator<OWLClass> cIt = reasoner.getEquivalentClasses(node.getId()).iterator();
			while(cIt.hasNext()){
				OWLClass inst = cIt.next();
				if(!m_generator.isRestrictedInstantiator(inst))
					node.addInstantiator(inst);
			}
		}
	}
}
