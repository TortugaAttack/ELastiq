package interpretation.generator;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import interpretation.ds.CanonicalDomain;
import interpretation.ds.CanonicalInterpretation;
import interpretation.ds.DomainNode;

import main.Main;
import main.StaticValues;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owl.OntologyOperator;
import statistics.StatStore;
import tracker.BlockOutputMode;
import tracker.TimeTracker;

public class IterativeQTBoxModelGenerator extends CanonicalInterpretationGenerator{
	
	private static final TimeTracker TRACKER = TimeTracker.getInstance();
	private static final String LOCAL_LOGGER_NAME = "QTBox-Generator-Logger";
	
	public IterativeQTBoxModelGenerator(OWLClassExpression concept){
		this(concept, true);
	}
	
	public IterativeQTBoxModelGenerator(OWLClassExpression concept, boolean normalize) {
		super(concept, normalize);
	}
	
	@Override
	public CanonicalInterpretation generate(OWLOntology ontology) {
		CanonicalInterpretation canonInterpretation = new CanonicalInterpretation();
		m_domain = new CanonicalDomain();
		canonInterpretation.initDomain(m_domain);
		
		m_ontologyOperator = OntologyOperator.getOntologyOperator(ontology);
		
		m_ontologyOperator.getExistentialRestrictionStore(); // flattens
		
		m_referenceClass = getFreshQueryClass("Q");
		insertQueryAxiom(m_referenceClass);
		
		// build canonical model starting from d_Q
		DomainNode<OWLClassExpression> root = m_domain.addDomainElement(m_referenceClass);
		addInstantiators(root);
		Set<DomainNode<OWLClassExpression>> unraveled = new HashSet<DomainNode<OWLClassExpression>>();
		unraveled.add(root);
		TRACKER.start(StaticValues.TIME_TBOX_SUCCESSORS, BlockOutputMode.COMPLETE, true);
		unravelTBoxNodeSuccessors(root, unraveled);
		TRACKER.stop(StaticValues.TIME_TBOX_SUCCESSORS);
		
		StatStore.getInstance().enterValue(StaticValues.STAT_QT_MODEL_SIZE, m_domain.size()*1.0);
		
		return canonInterpretation;
	}
	/**
	 * Recursively creates (or just uses) domain node successors and takes care of successor domain nodes.
	 * Take care of cyclic node succesoors.
	 * @param node
	 */
	protected void unravelTBoxNodeSuccessors(DomainNode<OWLClassExpression> node, Set<DomainNode<OWLClassExpression>> unraveled){
		OWLReasoner reasoner = m_ontologyOperator.getReasoner();
		for(Node<OWLClass> nodes : reasoner.getSuperClasses(node.getId(), false)){
			for(OWLClass superclass : nodes.getEntities()){
				if(m_ontologyOperator.getExistentialRestrictionStore().isIntermediary(superclass)){
					OWLClassExpression ce = m_ontologyOperator.getDefinition(superclass);
					if(ce != null && ce instanceof OWLObjectSomeValuesFrom){
						// now ce is subsuming node.id, thus suggesting a successor relation

						OWLObjectSomeValuesFrom some = (OWLObjectSomeValuesFrom)ce;
						DomainNode<OWLClassExpression> newNode = m_domain.getDomainNode(some.getFiller());
						boolean nodeJustCreated = false;
						if(newNode == null){
							newNode = m_domain.addDomainElement(some.getFiller());
							nodeJustCreated = true;
						}
						
						// these things are needed to check for subsumption relations to successor nodes
						currentIdSubClasses = m_ontologyOperator.getReasoner().getSubClasses(some.getFiller(), false);
						currentIdSuperClasses = m_ontologyOperator.getReasoner().getSuperClasses(some.getFiller(), false);
						currentIdEqClasses = m_ontologyOperator.getReasoner().getEquivalentClasses(some.getFiller());
						
						if(!isSuccessorRepresented(node, newNode, some.getProperty().asOWLObjectProperty())){
							removeIncludedSuccessors(some.getProperty().asOWLObjectProperty(), node, some.getFiller());
							
							if(nodeJustCreated) // only invoke instantiator procedure when node is actually added
								addInstantiators(newNode);
							
							node.addSuccessor(some.getProperty().asOWLObjectProperty(), newNode);
						}else{
							m_domain.removeDomainNode(newNode);
						}
					}
				}
			}
		}
		
		for(OWLClass equivClass : reasoner.getEquivalentClasses(node.getId())){
			if(m_ontologyOperator.getExistentialRestrictionStore().isIntermediary(equivClass)){
				OWLClassExpression ce = m_ontologyOperator.getDefinition(equivClass);
				if(ce != null && ce instanceof OWLObjectSomeValuesFrom){
					// now ce is subsuming node.id, thus suggesting a successor relation

					OWLObjectSomeValuesFrom some = (OWLObjectSomeValuesFrom)ce;
					DomainNode<OWLClassExpression> newNode = m_domain.getDomainNode(some.getFiller());
					if(newNode == null){
						newNode = m_domain.addDomainElement(some.getFiller());
						addInstantiators(newNode);
					}
					
					// these things are needed to check for subsumption relations to successor nodes
					currentIdSubClasses = m_ontologyOperator.getReasoner().getSubClasses(some.getFiller(), false);
					currentIdSuperClasses = m_ontologyOperator.getReasoner().getSuperClasses(some.getFiller(), false);
					currentIdEqClasses = m_ontologyOperator.getReasoner().getEquivalentClasses(some.getFiller());
					
					if(!isSuccessorRepresented(node, newNode, some.getProperty().asOWLObjectProperty())){
						removeIncludedSuccessors(some.getProperty().asOWLObjectProperty(), node, some.getFiller());
						
						node.addSuccessor(some.getProperty().asOWLObjectProperty(), newNode);
					}
				}
			}
		}
		
		// only after all successors are determined, unravel them
		for(OWLObjectProperty r : node.getSuccessorRoles()){
			for(DomainNode<?> nextNode : node.getSuccessors(r)){
				DomainNode<OWLClassExpression> nN = (DomainNode<OWLClassExpression>)nextNode;
				if(!unraveled.contains(nN)){
					unraveled.add(nN);
					unravelTBoxNodeSuccessors(nN, unraveled);
				}
			}
		}
	}
	
	protected void addInstantiators(DomainNode<OWLClassExpression> node){
		TRACKER.start(StaticValues.TIME_ADD_INSTANTIATORS, BlockOutputMode.COMPLETE, true);
		OWLClass classRep = getClassRepresentation(node.getId());
		if(classRep != null){
			OWLReasoner reasoner = m_ontologyOperator.getReasoner();
			// add all super class instantiators
			NodeSet<OWLClass> classes = reasoner.getSuperClasses(classRep, false);
			Iterator<Node<OWLClass>> nodeIt = classes.iterator();
			while(nodeIt.hasNext()){
				Iterator<OWLClass> it = nodeIt.next().iterator();
				while(it.hasNext()){
					OWLClass inst = it.next();
					if(!isRestrictedInstantiator(inst))
						node.addInstantiator(inst);
				}
			}
			// add all equivalent class instantiators
			Iterator<OWLClass> cIt = reasoner.getEquivalentClasses(node.getId()).iterator();
			while(cIt.hasNext()){
				OWLClass inst = cIt.next();
				if(!isRestrictedInstantiator(inst))
					node.addInstantiator(inst);
			}
		}
		TRACKER.stop(StaticValues.TIME_ADD_INSTANTIATORS);
	}
	
	protected void useDomain(CanonicalDomain domain){
		m_domain = domain;
	}
	protected void useOntology(OWLOntology o){
		m_ontologyOperator = OntologyOperator.getOntologyOperator(o);
	}
}