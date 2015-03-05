package interpretation.generator;

import java.util.Iterator;

import interpretation.ds.CanonicalDomain;
import interpretation.ds.DomainNode;

import main.StaticValues;

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

import tracker.BlockOutputMode;
import tracker.TimeTracker;
import util.EasyMath;
import util.EasyTimes;

public class KBDomainElementGenerator implements IDomainElementGenerator{

	private static final TimeTracker TRACKER = TimeTracker.getInstance();
	
	private CanonicalInterpretationGenerator m_generator;

	@Override
	public void generate(IInterpretationGenerator g, boolean small) {
		if(!(g instanceof CanonicalInterpretationGenerator)) return;
		TRACKER.start(StaticValues.TIME_DOMAIN_ELEMENTS_CREATION);
		m_generator = (CanonicalInterpretationGenerator)g;

		OWLReasoner reasoner = m_generator.getOntologyOperator().getReasoner(true);
		
		m_generator.getLogger().info(m_generator.getOntologyOperator().getOntology().getIndividualsInSignature().size() + " individual domain elements to create.");
		// add domain elements for all ABox individuals
		TRACKER.start(StaticValues.TIME_INDIVIDUAL_DOMAIN_ELEM);
		for(OWLNamedIndividual ind : m_generator.getOntologyOperator().getOntology().getIndividualsInSignature()){
			m_generator.getLogger().fine(ind + ": " +reasoner.getTypes(ind, false));
			
			DomainNode<?> node = m_generator.getDomain().addDomainElement(ind);
			
			addInstantiators(node);
		}
		TRACKER.stop(StaticValues.TIME_INDIVIDUAL_DOMAIN_ELEM);
		
		TRACKER.start(StaticValues.TIME_ER_DOMAIN_ELEM);
		m_generator.getLogger().info(m_generator.getOntologyOperator().getExistentialRestrictionStore().getRestrictions().size() + " Existential restrictions to generate domain elements from.");
//		System.exit(1);
		// add domain elements for all existential restriction filler concepts that don't have instances
		for(OWLObjectSomeValuesFrom some : m_generator.getOntologyOperator().getExistentialRestrictionStore().getRestrictions()){
			if(small){ // only when requested tries to find a mapping to individuals
				TRACKER.start(StaticValues.TIME_SMALL_MODEL, BlockOutputMode.COMPLETE, true);
				NodeSet<OWLNamedIndividual> instances = reasoner.getInstances(some.getFiller(), false);
				if(!instances.isEmpty()){
					Iterator<Node<OWLNamedIndividual>> it1 = instances.iterator();
					if(it1.hasNext()){
						Iterator<OWLNamedIndividual> it2 = it1.next().iterator();
						if(it2.hasNext()){
							OWLNamedIndividual ind = it2.next();
//							m_generator.getOntologyOperator().getOntology().getObjectPropertyAssertionAxioms(ind);
//							somehow determine whether we can find an individual domain element that can be used as the current filler
							// it may not contain "more" role-assertions than the concept
//							reasoner.getObjectPropertyValues(ind, some.getProperty());
							m_generator.addAssociation(m_generator.getClassRepresentation(some.getFiller()), ind);
							TRACKER.stop(StaticValues.TIME_SMALL_MODEL);
							continue;
						}
					}
				}
				TRACKER.stop(StaticValues.TIME_SMALL_MODEL);
			}
			// only generates a new domain element if no instance of the filler exists yet
			DomainNode<?> node = m_generator.getDomain().addDomainElement(some.getFiller());
//			m_generator.getLogger().fine(some + ": " + reasoner.getSuperClasses((OWLClassExpression)node.getId(), false));
			addInstantiators(node);
		}
		TRACKER.stop(StaticValues.TIME_ER_DOMAIN_ELEM);
		m_generator.getLogger().info("added a total of " + totalInstantiators + " instantiators (mean: " + EasyMath.round(totalInstantiators/domainElements, 3) + ") in " + EasyTimes.niceTime(totalAddInstTimeMs));
		TRACKER.stop(StaticValues.TIME_DOMAIN_ELEMENTS_CREATION);
	}
	
	private static int totalInstantiators = 0;
	private static int domainElements = 0;
	private static long totalAddInstTimeMs = 0;
	private void addInstantiators(DomainNode<?> node){
		long tStart = System.currentTimeMillis();
		TRACKER.start(StaticValues.TIME_ADD_INSTANTIATORS, BlockOutputMode.COMPLETE, true);
		domainElements++;
		if(node.getId() instanceof OWLClassExpression){
			addInstantiators((DomainNode<OWLClassExpression>)node, (OWLClassExpression)node.getId());
		}else if(node.getId() instanceof OWLIndividual){
			addInstantiators((DomainNode<OWLNamedIndividual>)node, (OWLNamedIndividual)node.getId());
		}
		// do not accept other id-types
		TRACKER.stop(StaticValues.TIME_ADD_INSTANTIATORS);
		totalAddInstTimeMs += System.currentTimeMillis() - tStart;
	}
	
	private void addInstantiators(DomainNode<OWLNamedIndividual> node, OWLNamedIndividual individual){
		OWLReasoner reasoner = m_generator.getOntologyOperator().getReasoner();
		NodeSet<OWLClass> types = reasoner.getTypes(individual, false);
		Iterator<Node<OWLClass>> it1 = types.iterator();
		int instantiators = 0;
		while(it1.hasNext()){
			Iterator<OWLClass> it2 = it1.next().iterator();
			while(it2.hasNext()){
				OWLClass inst = it2.next();
				if(!m_generator.isRestrictedInstantiator(inst)){
					node.addInstantiator(inst);
					instantiators++;
				}
			}
		}
		totalInstantiators += instantiators;
	}
	
	private void addInstantiators(DomainNode<OWLClassExpression> node, OWLClassExpression individual){
		OWLClass classRep = m_generator.getClassRepresentation(node.getId());
		if(classRep != null){
			OWLReasoner reasoner = m_generator.getOntologyOperator().getReasoner();
			// add all super class instantiators
			NodeSet<OWLClass> classes = reasoner.getSuperClasses(classRep, false);
			Iterator<Node<OWLClass>> nodeIt = classes.iterator();
			int instantiators = 0;
			while(nodeIt.hasNext()){
				Iterator<OWLClass> it = nodeIt.next().iterator();
				while(it.hasNext()){
					OWLClass inst = it.next();
					if(!m_generator.isRestrictedInstantiator(inst)){
						node.addInstantiator(inst);
						instantiators++;
					}
				}
			}
			// add all equivalent class instantiators
			Iterator<OWLClass> cIt = reasoner.getEquivalentClasses(node.getId()).iterator();
			while(cIt.hasNext()){
				OWLClass inst = cIt.next();
				if(!m_generator.isRestrictedInstantiator(inst)){
					node.addInstantiator(inst);
					instantiators++;
				}
			}
			totalInstantiators += instantiators;
		}
	}
}
