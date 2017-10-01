package org.tu_dresden.elastiq.interpretation.generator;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.tu_dresden.elastiq.interpretation.ds.DomainNode;
import org.tu_dresden.elastiq.main.StaticValues;

import tracker.BlockOutputMode;
import tracker.TimeTracker;
/**
 * @deprecated unused by the iterative model generators
 * 
 * @author Maximilian Pensel - maximilian.pensel@gmx.de
 *
 */
public class TBoxDomainElementGenerator implements
		IDomainElementGenerator {

	private static final TimeTracker TRACKER = TimeTracker.getInstance();
	
	private CanonicalInterpretationGenerator m_generator;
	
	private Set<OWLClass> m_extraClasses;
	
	public TBoxDomainElementGenerator() {
		m_extraClasses = new HashSet<OWLClass>();
	}
	
	public void registerConcept(OWLClass clazz){
		m_extraClasses.add(clazz);
	}
	
	@Override
	public void generate(IInterpretationGenerator g, boolean small) { // small mode ignored here
		if(!(g instanceof CanonicalInterpretationGenerator)) return;
		TRACKER.start(StaticValues.TIME_DOMAIN_ELEMENTS_CREATION);
		m_generator = (CanonicalInterpretationGenerator)g;
		
		TRACKER.start(StaticValues.TIME_ER_DOMAIN_ELEM);
		// add all additionally specified elements and their instantiators
		for(OWLClass c : m_extraClasses){
			DomainNode<OWLClassExpression> node = (DomainNode<OWLClassExpression>)m_generator.getDomain().addDomainElement(c);
			
			addInstantiators(node);
		}
		
		m_generator.getLogger().info(m_generator.getOntologyOperator().getFlatteningTransformer().getRestrictions().size() + " Existential restrictions to generate domain elements from.");
		// add all relevant domain elements and their instantiators
		for(OWLObjectSomeValuesFrom some : m_generator.getOntologyOperator().getFlatteningTransformer().getRestrictions()){
			DomainNode<OWLClassExpression> node = (DomainNode<OWLClassExpression>)m_generator.getDomain().addDomainElement(some.getFiller());
			
			addInstantiators(node);
		}
		TRACKER.stop(StaticValues.TIME_ER_DOMAIN_ELEM);
		
		TRACKER.stop(StaticValues.TIME_DOMAIN_ELEMENTS_CREATION);
	}
	
	private void addInstantiators(DomainNode<OWLClassExpression> node){
		TRACKER.start(StaticValues.TIME_ADD_INSTANTIATORS, BlockOutputMode.COMPLETE, true);
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
		TRACKER.stop(StaticValues.TIME_ADD_INSTANTIATORS);
	}
}
