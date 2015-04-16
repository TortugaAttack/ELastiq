package interpretation.generator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import interpretation.ds.CanonicalDomain;
import interpretation.ds.CanonicalInterpretation;
import interpretation.ds.DomainNode;

import main.StaticValues;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;

import owl.OntologyOperator;
import owl.transform.flatten.OWLAxiomFlatteningTransformer;
import statistics.StatStore;
import tracker.BlockOutputMode;
import tracker.TimeTracker;

public class IterativeKBInterpretationGenerator extends CanonicalInterpretationGenerator{

	private static final TimeTracker TRACKER = TimeTracker.getInstance();
	
	private IterativeQTBoxModelGenerator m_tbGenerator;
	
	public IterativeKBInterpretationGenerator() {
		this(true);
	}
	
	public IterativeKBInterpretationGenerator(boolean normalizing) {
		m_tbGenerator = new IterativeQTBoxModelGenerator(null, normalizing);
	}
	
	@Override
	public CanonicalInterpretation generate(OWLOntology ontology) {
		// by default create the model over all ABox individuals
		return generate(ontology, ontology.getIndividualsInSignature());
	}
	
	public CanonicalInterpretation generate(OWLOntology ontology, Set<OWLNamedIndividual> individuals) {
		LOG.info("starting KB model generation");
		m_ontologyOperator = OntologyOperator.getOntologyOperator(ontology);
		
		/* add domain elements for all individuals (and add instantiators)
		 * for all individual domain elements d_a {
		 *   add all explicit successors by iterating role assertions r(a, b) (for all r and b)
		 *   for all concepts A s.t. \exists r. A (a) is entailed {
		 *     if(NOT exists b s.t. r(a,b) and A(b)){
		 *       if(NOT exists B s.t. d_a has r-successor to d_B and B subclassof A){
		 *         remove all B' s.t. d_a has r-successor to d_B' and A subclassof B'
		 *         
		 *         add d_A as domain element and successor of d_a
		 *       }
		 *     }
		 *   }
		 *   for all added d_A{
		 *     recursively add successors as done in TBox mode
		 *     (DO NOT USE ASSOCIATIONS, ALWAYS INTRODUCE NEW ELEMENTS)
		 *   }
		*/
		
		CanonicalInterpretation interpretation = new CanonicalInterpretation();
		m_domain = new CanonicalDomain();
		interpretation.initDomain(m_domain);
		
		m_tbGenerator.useDomain(m_domain); // needed to build upon the current domain
		m_tbGenerator.useOntology(ontology);
		m_tbGenerator.setUseBuffer(true);
		
		OWLAxiomFlatteningTransformer restrictions = m_ontologyOperator.getFlatteningTransformer(); // flattens
		
		LOG.info("creating " + ontology.getIndividualsInSignature().size() + " individual domain elements");
		// can be improved by not iterating all individual domain elements twice
		for(OWLNamedIndividual ind : ontology.getIndividualsInSignature()){
			DomainNode<OWLNamedIndividual> node = m_domain.addDomainElement(ind);
			addInstantiators(node);
		}
		
		LOG.info("adding direct successors to all domain elements");
		Map<DomainNode<OWLClassExpression>, Integer> introducedTBoxNodes = new HashMap<DomainNode<OWLClassExpression>, Integer>();
		for(OWLNamedIndividual ind : individuals){
			DomainNode<OWLNamedIndividual> node = m_domain.getDomainNode(ind);
			
			TRACKER.start(StaticValues.TIME_EXPLICIT_SUCCESSORS, BlockOutputMode.COMPLETE, true);
			for(OWLObjectPropertyAssertionAxiom ax : ontology.getObjectPropertyAssertionAxioms(ind)){
				node.addSuccessor(ax.getProperty().asOWLObjectProperty(),
								  m_domain.getDomainNode(ax.getObject()));
			}
			TRACKER.stop(StaticValues.TIME_EXPLICIT_SUCCESSORS);
			
			TRACKER.start(StaticValues.TIME_DIRECT_TBOX_SUCCESSORS, BlockOutputMode.COMPLETE, true);
			Set<OWLClass> removeInstantiators = new HashSet<OWLClass>();
			for(OWLClass intermediary : node.getInstantiators()){
				if(restrictions.isIntermediary(intermediary)){
					removeInstantiators.add(intermediary); // mark intermediaries for removal
					OWLClassExpression ce = m_ontologyOperator.getDefinition(intermediary);
					if(ce != null && ce instanceof OWLObjectSomeValuesFrom){
						OWLObjectSomeValuesFrom some = (OWLObjectSomeValuesFrom)ce;
						
						// check for an appropriate individual to use instead of some.filler
						NodeSet<OWLNamedIndividual> instances = m_ontologyOperator.getReasoner(true).getInstances(some.getFiller(), false);
						NodeSet<OWLClass> subClasses = null;
						Node<OWLClass> eqClasses = null; // will only be initialized and used when m_normalize is true
						if(m_normalize){
							subClasses = m_ontologyOperator.getReasoner().getSubClasses(some.getFiller(), false);
							eqClasses = m_ontologyOperator.getReasoner().getEquivalentClasses(some.getFiller());
						}
						boolean addSuccessor = true;
						for(DomainNode<?> n : node.getSuccessors(some.getProperty().asOWLObjectProperty())){
							// check if an individual exists that can represent the current class
							if(n.getId() instanceof OWLNamedIndividual){
								if(instances.containsEntity((OWLNamedIndividual)n.getId())){
									addSuccessor = false;
									break;
								}
							}
							// check if another more specific class expression can represent the current class
							if(m_normalize && n.getId() instanceof OWLClassExpression){
								OWLClass fillerClass = getClassRepresentation((OWLClassExpression)n.getId());
								if(subClasses.containsEntity(fillerClass)
										|| eqClasses.contains(fillerClass)){
									addSuccessor = false;
									break;
								}
							}
						}
						// nothing represents the some.filler, thus create new element and add as successor
						if(addSuccessor){
							if(m_normalize){
								// also remove all more general nodes than this one as successor
								currentIdSuperClasses = m_ontologyOperator.getReasoner().getSuperClasses(some.getFiller(), false);
								Set<DomainNode<OWLClassExpression>> removed = removeIncludedSuccessors(some.getProperty().asOWLObjectProperty(), node, some.getFiller());
								for(DomainNode<OWLClassExpression> rem : removed){
									if(introducedTBoxNodes.containsKey(rem)){
										// reduce number of predecessors
										introducedTBoxNodes.put(rem, introducedTBoxNodes.get(rem) - 1);
										if(introducedTBoxNodes.get(rem) < 0){
											LOG.severe("A created domain node has less than 0 predecessors!");
										}
									}
								}
							}
							
							DomainNode<OWLClassExpression> succ = m_domain.addDomainElement(some.getFiller());
							if(!introducedTBoxNodes.containsKey(succ)){ // freshly created
								introducedTBoxNodes.put(succ, 0);
								m_tbGenerator.addInstantiators(succ);
							}
							// increase number of predecessors
							introducedTBoxNodes.put(succ, introducedTBoxNodes.get(succ) + 1);
							node.addSuccessor(some.getProperty().asOWLObjectProperty(), succ);
						}
					}
				}else if(isRestrictedInstantiator(intermediary)){ // still remove all other restricted instantiators
					removeInstantiators.add(intermediary);
				}
			}
			node.removeInstantiators(removeInstantiators);
			TRACKER.stop(StaticValues.TIME_DIRECT_TBOX_SUCCESSORS);
		}
		/* at this point all individuals have a domain element which is connected to other individual
		 * domain elements via the explicit role-assertions and to all necessary (i.e. no appropriate instance existed) 
		 * TBox "filler"-concepts.
		 * Now saturate those introduced filler-concept domain nodes with TBox successors recursively.
		 */
		TRACKER.start(StaticValues.TIME_TBOX_SUCCESSORS, BlockOutputMode.COMPLETE, true);
		LOG.info("unraveling " + introducedTBoxNodes.size() + " TBox domain nodes");
		Set<DomainNode<OWLClassExpression>> unraveled = new HashSet<DomainNode<OWLClassExpression>>();
		int skipping = 0;
		for(DomainNode<OWLClassExpression> node : introducedTBoxNodes.keySet()){
			// it must be reachable so far
			if(introducedTBoxNodes.get(node) > 0 && !unraveled.contains(node)){
				unraveled.add(node);
				m_tbGenerator.unravelTBoxNodeSuccessors(node, unraveled);
			}else{
				skipping++;
			}
		}
		StatStore.getInstance().enterValue("skipped direct TBox successors for unraveling", skipping*1.0);
		StatStore.getInstance().enterValue("needless elk accesses (redundand)", m_tbGenerator.getNeedlessAccesses()*1.0);
		TRACKER.stop(StaticValues.TIME_TBOX_SUCCESSORS);
		
		StatStore.getInstance().enterValue(StaticValues.STAT_KB_MODEL_SIZE, m_domain.size()*1.0);
		
		return interpretation;
	}
	
	private void addInstantiators(DomainNode<OWLNamedIndividual> node){
		TRACKER.start(StaticValues.TIME_ADD_INSTANTIATORS, BlockOutputMode.COMPLETE, true);
		for(Node<OWLClass> nodes : m_ontologyOperator.getReasoner(true).getTypes(node.getId(), false)){
			for(OWLClass clazz : nodes){
//				if(!isRestrictedInstantiator(clazz)){
					node.addInstantiator(clazz); // also add intermediaries, for later use (and removal)
//				}
			}
		}
		TRACKER.stop(StaticValues.TIME_ADD_INSTANTIATORS);
	}

}
