package interpretation.generator;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import interpretation.ds.CanonicalDomain;
import interpretation.ds.CanonicalInterpretation;
import interpretation.ds.DomainNode;
import main.StaticValues;
import owl.OntologyOperator;
import statistics.StatStore;
import tracker.BlockOutputMode;
import tracker.TimeTracker;

public class IterativeQTBoxModelGenerator extends CanonicalInterpretationGenerator{
	
	private static final TimeTracker TRACKER = TimeTracker.getInstance();
	
	private Set<OWLClassExpression> multiAccessTracker;
	private int needlessAccesses;
	public int getNeedlessAccesses(){
		return needlessAccesses;
	}
	
	public IterativeQTBoxModelGenerator(OWLClassExpression concept){
		this(concept, true);
	}
	
	public IterativeQTBoxModelGenerator(OWLClassExpression concept, boolean normalize) {
		super(concept, normalize);
		
		multiAccessTracker = new HashSet<OWLClassExpression>();
		needlessAccesses = 0;
	}
	
	@Override
	public CanonicalInterpretation generate(OWLOntology ontology) {
		CanonicalInterpretation canonInterpretation = new CanonicalInterpretation();
		m_domain = new CanonicalDomain();
		canonInterpretation.initDomain(m_domain);
		
		m_ontologyOperator = OntologyOperator.getOntologyOperator(ontology);
		
		m_ontologyOperator.getFlatteningTransformer(); // flattens
		
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
		int supClassSum = 0;
		for(Node<OWLClass> nodes : reasoner.getSuperClasses(node.getId(), false)){
			for(OWLClass superclass : nodes.getEntities()){
				if(m_ontologyOperator.getFlatteningTransformer().isIntermediary(superclass)){
					OWLClassExpression ce = m_ontologyOperator.getDefinition(superclass);
					if(ce != null && ce instanceof OWLObjectSomeValuesFrom){
						supClassSum++;
						// now ce is subsuming node.id, thus suggesting a successor relation

						OWLObjectSomeValuesFrom some = (OWLObjectSomeValuesFrom)ce;
						DomainNode<OWLClassExpression> newNode = m_domain.getDomainNode(some.getFiller());
						boolean nodeJustCreated = false;
						if(newNode == null){
							newNode = m_domain.addDomainElement(some.getFiller());
							nodeJustCreated = true;
						}
						
						// these things are needed to check for subsumption relations to successor nodes
						if(!m_subClassBuffer.containsKey(some.getFiller())){
							if(multiAccessTracker.contains(some.getFiller())){
								needlessAccesses++;
							}else{
								multiAccessTracker.add(some.getFiller());
							}
							TRACKER.start("Elk access", BlockOutputMode.COMPLETE, true);
							currentIdSubClasses = reasoner.getSubClasses(some.getFiller(), false);
							currentIdSuperClasses = reasoner.getSuperClasses(some.getFiller(), false);
							currentIdEqClasses = reasoner.getEquivalentClasses(some.getFiller());
							TRACKER.stop("Elk access");
							if(m_useBuffer){
								m_subClassBuffer.put(some.getFiller(), currentIdSubClasses.getFlattened());
								m_superClassBuffer.put(some.getFiller(), currentIdSuperClasses.getFlattened());
								m_equivalentClassBuffer.put(some.getFiller(), currentIdEqClasses.getEntities());
							}
						}
						
						if(!isSuccessorRepresented(node, newNode, some.getProperty().asOWLObjectProperty())){
							StatStore.getInstance().enterValue("removed subsuming successors",
									removeIncludedSuccessors(some.getProperty().asOWLObjectProperty(), node, some.getFiller()).size()*1.0
							);
							
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
		StatStore.getInstance().enterValue("interesting super classes for unraveling", supClassSum*1.0);
		
		int eqClassSum = 0;
		for(OWLClass equivClass : reasoner.getEquivalentClasses(node.getId())){
			if(m_ontologyOperator.getFlatteningTransformer().isIntermediary(equivClass)){
				OWLClassExpression ce = m_ontologyOperator.getDefinition(equivClass);
				if(ce != null && ce instanceof OWLObjectSomeValuesFrom){
					eqClassSum++;
					// now ce is subsuming node.id, thus suggesting a successor relation

					OWLObjectSomeValuesFrom some = (OWLObjectSomeValuesFrom)ce;
					DomainNode<OWLClassExpression> newNode = m_domain.getDomainNode(some.getFiller());
					if(newNode == null){
						newNode = m_domain.addDomainElement(some.getFiller());
						addInstantiators(newNode);
					}
					
					// these things are needed to check for subsumption relations to successor nodes
					if(!m_subClassBuffer.containsKey(some.getFiller())){
						if(multiAccessTracker.contains(some.getFiller())){
							needlessAccesses++;
						}else{
							multiAccessTracker.add(some.getFiller());
						}
						TRACKER.start("Elk access", BlockOutputMode.COMPLETE, true);
						currentIdSubClasses = reasoner.getSubClasses(some.getFiller(), false);
						currentIdSuperClasses = reasoner.getSuperClasses(some.getFiller(), false);
						currentIdEqClasses = reasoner.getEquivalentClasses(some.getFiller());
						TRACKER.stop("Elk access");
						if(m_useBuffer){
							m_subClassBuffer.put(some.getFiller(), currentIdSubClasses.getFlattened());
							m_superClassBuffer.put(some.getFiller(), currentIdSuperClasses.getFlattened());
							m_equivalentClassBuffer.put(some.getFiller(), currentIdEqClasses.getEntities());
						}
					}
					
					if(!isSuccessorRepresented(node, newNode, some.getProperty().asOWLObjectProperty())){
						removeIncludedSuccessors(some.getProperty().asOWLObjectProperty(), node, some.getFiller());
						
						node.addSuccessor(some.getProperty().asOWLObjectProperty(), newNode);
					}
				}
			}
		}
		StatStore.getInstance().enterValue("interesting equivalent classes for unraveling", eqClassSum*1.0);
		
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