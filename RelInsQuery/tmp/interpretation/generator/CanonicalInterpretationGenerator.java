package interpretation.generator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
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
import owl.transform.flatten.OWLAxiomFlatteningTransformer;
import statistics.StatStore;
import tracker.BlockOutputMode;
import tracker.TimeTracker;

public class CanonicalInterpretationGenerator implements IInterpretationGenerator {

	private static final String LOCAL_LOGGER_NAME = "CanonicalModel-Logger";
	private static final TimeTracker TRACKER = TimeTracker.getInstance();
	private static final StatStore STAT = StatStore.getInstance();
	
	OWLClassExpression m_referenceExpression;
	OWLClass m_referenceClass;
	
	OntologyOperator m_ontologyOperator;
	
	private Map<OWLClass, OWLNamedIndividual> m_classAssociations;
	
	CanonicalDomain m_domain;
	
	boolean m_keepSmall;
	
	boolean m_normalize;
	
	protected Map<OWLClassExpression, Set<OWLClass>> m_superClassBuffer;
	protected Map<OWLClassExpression, Set<OWLClass>> m_equivalentClassBuffer;
	protected Map<OWLClassExpression, Set<OWLClass>> m_subClassBuffer;
	
	protected boolean m_useBuffer;
	
	Logger LOG;
	
	/**
	 * @deprecated you should use the iterative generators
	 */
	public CanonicalInterpretationGenerator() {
		this(true);
	}
	
	/**
	 * @deprecated you should use the iterative generators
	 */
	public CanonicalInterpretationGenerator(OWLClassExpression expr){
		this(expr, true);
	}
	
	/**
	 * @deprecated you should use the iterative generators
	 */
	public CanonicalInterpretationGenerator(boolean normalizing){
		this(null, normalizing);
	}
	
	/**
	 * @deprecated you should use the iterative generators
	 */
	public CanonicalInterpretationGenerator(OWLClassExpression expr, boolean normalizing) {
		this.m_referenceExpression = expr;
		this.m_classAssociations = new HashMap<OWLClass, OWLNamedIndividual>();
		
		m_keepSmall = true;
		
		m_normalize = normalizing;
		
		LOG = Logger.getLogger(LOCAL_LOGGER_NAME);
		
		m_useBuffer = false;
		m_superClassBuffer = new HashMap<OWLClassExpression, Set<OWLClass>>();
		m_subClassBuffer = new HashMap<OWLClassExpression, Set<OWLClass>>();
		m_equivalentClassBuffer = new HashMap<OWLClassExpression, Set<OWLClass>>();
	}
	
	@Override
	public CanonicalInterpretation generate(OWLOntology ontology) {
		CanonicalInterpretation canonInterpretation = new CanonicalInterpretation();
		m_domain = new CanonicalDomain();
		canonInterpretation.initDomain(m_domain);
		
		m_ontologyOperator = OntologyOperator.getOntologyOperator(ontology);
		IDomainElementGenerator elemGen;
		if(isKBMode()){
			elemGen = new KBDomainElementGenerator();
		}else{
			elemGen = new TBoxDomainElementGenerator();
		}
		
		if(!isKBMode()){ // TBox + Query mode
			// define class Q as equivalence to the reference expression (query)
			m_referenceClass = getFreshQueryClass("Q");
			insertQueryAxiom(m_referenceClass);
			((TBoxDomainElementGenerator)elemGen).registerConcept(m_referenceClass);
		}
		
		OWLAxiomFlatteningTransformer exRestStore = m_ontologyOperator.getFlatteningTransformer(); // flattens here
		m_ontologyOperator.getReasoner(isKBMode()); // precomputes inferences
		
		// the element generator creates all necessary domain elements and adds their instantiators
		elemGen.generate(this, m_keepSmall);
		LOG.info("a total of " + m_classAssociations.size() + " classes are mapped to individuals");
		
		TRACKER.start(StaticValues.TIME_DOMAIN_SUCCESSORS);
		if(!isKBMode()){ // TBox + Query mode
			
			// add all successor relations by iterating all known existential restrictions
			for(OWLObjectSomeValuesFrom some : exRestStore.getRestrictions()){
				addEntailedTBoxSuccessors(some, m_normalize);
			}
		}else{ // KB mode (ABox + TBox)
			// add all ABox property assertion successors
			for(OWLAxiom ax : m_ontologyOperator.getOntology().getABoxAxioms(true)){
				if(ax instanceof OWLObjectPropertyAssertionAxiom){
//					LOG.fine("Adding role assertion successor: " + ax);
					OWLObjectPropertyAssertionAxiom pAx = (OWLObjectPropertyAssertionAxiom)ax;
					getDomainElement(pAx.getSubject()).addSuccessor(
							(OWLObjectProperty)pAx.getProperty(),
							getDomainElement(pAx.getObject()));
				}
			}
			
			// add all successor relations by iterating all known existential restrictions
			int exR = 1;
			for(OWLObjectSomeValuesFrom some : m_ontologyOperator.getFlatteningTransformer().getRestrictions()){
				if(exR % 1000 == 0){
					System.out.println(exR + " restrictions handled");
				}
				addEntailedKBSuccessors(some);
				exR++;
			}
			
			LOG.info("TOTAL TIME FOR INSTANCE RETRIEVAL: " + sum + " ms");
			
			// normalize later
			// if(m_normalize){ startSimulationComputation(); }
			
		}
		TRACKER.stop(StaticValues.TIME_DOMAIN_SUCCESSORS);
		
		
		/* ************* TEST SPECIFIC STUFF ************** */
//		LOG.info("Start checking for useless domain nodes ...");
//		Map<OWLClassExpression, DomainNode<OWLClassExpression>> conceptDomainNodes = canonInterpretation.getDomain().getConceptElements();
//		Set<DomainNode<OWLClassExpression>> noPredecessors = new HashSet<DomainNode<OWLClassExpression>>();
//		noPredecessors.addAll(conceptDomainNodes.values());
//		for(OWLClassExpression ce : conceptDomainNodes.keySet()){
//			for(OWLObjectProperty r : conceptDomainNodes.get(ce).getSuccessorRoles()){
//				noPredecessors.removeAll(conceptDomainNodes.get(ce).getSuccessors(r));
//			}
//			
//			NodeSet<OWLClass> subClasses = m_ontologyOperator.getReasoner().getSubClasses(ce.asOWLClass(), false);
//			Node<OWLClass> eqClasses = m_ontologyOperator.getReasoner().getEquivalentClasses(ce.asOWLClass());
//			for(OWLClassExpression ce2 : conceptDomainNodes.keySet()){
//				if(!ce.equals(ce2)){
//					if(subClasses.containsEntity(ce2.asOWLClass())){
//						LOG.info("Domain node " + conceptDomainNodes.get(ce2) + " represents something more specific than " + conceptDomainNodes.get(ce));
//					}else if(eqClasses.contains(ce2.asOWLClass())){
//						LOG.info("Domain node " + conceptDomainNodes.get(ce2) + " is equivalent to " + conceptDomainNodes.get(ce));
//					}
//				}
//			}
//		}
//		for(DomainNode<OWLClassExpression> d : noPredecessors){
//			LOG.info(d + " does not have any predecessors.");
//			if(d.getSuccessorObjects().isEmpty())
//				LOG.info(d + " is not connected to the model at all!");
//		}
		/* ************ END TEST ***************** */
		
		return canonInterpretation;
	}
	
	protected NodeSet<OWLClass> currentIdSubClasses;
	protected NodeSet<OWLClass> currentIdSuperClasses;
	protected Node<OWLClass> currentIdEqClasses;
	private void addEntailedTBoxSuccessors(OWLObjectSomeValuesFrom some, boolean doNormalizing){
//		StatStore.getInstance().enterValue("lookup " + some.getFiller(), 1.0);
//		StatStore.getInstance().enterValue("restrictions handled", 1.0);
		TRACKER.start(StaticValues.TIME_TBOX_SUCCESSORS, BlockOutputMode.COMPLETE, true);
//		TRACKER.start("fetch domain element and intermediary", BlockOutputMode.COMPLETE, true);
		DomainNode<?> toNode = getDomainElement(some.getFiller());
		// the super class, intermediary stands for (some r B)
		OWLClass superClass = m_ontologyOperator.getFlatteningTransformer().getIntermediary(some);
//		TRACKER.stop("fetch domain element and intermediary");
		// add successors from all
//		TRACKER.start("query elk", BlockOutputMode.COMPLETE, true);
//		StatStore.getInstance().enterValue("subclass accessing", 1.0);
		NodeSet<OWLClass> classes = m_ontologyOperator.getReasoner().getSubClasses(superClass, false);
		
		// fill current class relations from Id
//		StatStore.getInstance().enterValue("subclass accessing", 1.0);
		currentIdSubClasses = m_ontologyOperator.getReasoner().getSubClasses(some.getFiller(), false);
//		StatStore.getInstance().enterValue("superclass accessing", 1.0);
		currentIdSuperClasses = m_ontologyOperator.getReasoner().getSuperClasses(some.getFiller(), false);
//		StatStore.getInstance().enterValue("equivalent classes accessing", 1.0);
		currentIdEqClasses = m_ontologyOperator.getReasoner().getEquivalentClasses(some.getFiller());
		
		
//		TRACKER.stop("query elk");
//		LOG.fine(" creating successors to " + some + ": " + classes.toString());
		Iterator<Node<OWLClass>> nodeIt = classes.iterator();
		while(nodeIt.hasNext()){
			Iterator<OWLClass> it = nodeIt.next().iterator();
			while(it.hasNext()){
				DomainNode<?> node = m_domain.getDomainNode(it.next());
				// if it.next() yields no domain node, there exists an ABox identity for it
				// and this case is covered by addEntailedKBSuccessors
				if(node != null){
					if(doNormalizing){
//						if(!isSuccessorRepresented(node, (OWLObjectProperty)some.getProperty(), some.getFiller())){
						TRACKER.start("normalizing and adding", BlockOutputMode.COMPLETE, true);
						if(!isSuccessorRepresented(node, toNode, (OWLObjectProperty)some.getProperty())){
							removeIncludedSuccessors((OWLObjectProperty)some.getProperty(), node, some.getFiller());
							
							node.addSuccessor((OWLObjectProperty)some.getProperty(), toNode);
						}
						TRACKER.stop("normalizing and adding");
					}else{
						node.addSuccessor((OWLObjectProperty)some.getProperty(), toNode);
					}
				}
			}
		}
		// add all equivalent class successors
//		TRACKER.start("query elk", BlockOutputMode.COMPLETE, true);
		Iterator<OWLClass> cIt = m_ontologyOperator.getReasoner().getEquivalentClasses(superClass).iterator();
//		TRACKER.stop("query elk");
		while(cIt.hasNext()){
			DomainNode<?> node = m_domain.getDomainNode(cIt.next());
			if(node != null){
				if(doNormalizing){
//					if(!isSuccessorRepresented(node, (OWLObjectProperty)some.getProperty(), some.getFiller())){
					TRACKER.start("normalizing and adding", BlockOutputMode.COMPLETE, true);
					if(!isSuccessorRepresented(node, toNode, (OWLObjectProperty)some.getProperty())){
						removeIncludedSuccessors((OWLObjectProperty)some.getProperty(), node, some.getFiller());
						
						node.addSuccessor((OWLObjectProperty)some.getProperty(), toNode);
					}
					TRACKER.stop("normalizing and adding");
				}else{
					node.addSuccessor((OWLObjectProperty)some.getProperty(), toNode);
				}
			}
		}
		TRACKER.stop(StaticValues.TIME_TBOX_SUCCESSORS);
	}
	
	private long sum = 0;
	private void addEntailedKBSuccessors(OWLObjectSomeValuesFrom some){
		TRACKER.start(StaticValues.TIME_KB_SUCCESSORS, BlockOutputMode.COMPLETE, true);
		// add all role-successors entailed by the TBox
		addEntailedTBoxSuccessors(some, m_normalize); // for KB mode only normalize TBox contained roles
		
		DomainNode<?> toNode = getDomainElement(some.getFiller());
		
		OWLClass someClass = m_ontologyOperator.getFlatteningTransformer().getIntermediary(some);
		
		long start = System.currentTimeMillis();
//		StatStore.getInstance().enterValue("instance accessing", 1.0);
		NodeSet<OWLNamedIndividual> instances = m_ontologyOperator.getReasoner().getInstances(someClass, false);
		sum += System.currentTimeMillis() - start;
//		NodeSet<OWLNamedIndividual> instances = m_ontologyOperator.getReasoner().getInstances(getClassRepresentation(some), false);
//		LOG.fine(" creating successors to " + some + " from instances: " + instances.toString());
		Iterator<Node<OWLNamedIndividual>> it1 = instances.iterator();
		while(it1.hasNext()){
			Iterator<OWLNamedIndividual> it2 = it1.next().iterator();
			while(it2.hasNext()){
				DomainNode<?> from = m_domain.getDomainNode(it2.next());
				if(from != null){
//					if(!isSuccessorRepresented(from, getDomainElement(some.getFiller()), (OWLObjectProperty)some.getProperty())){
						from.addSuccessor((OWLObjectProperty)some.getProperty(), toNode);
//					}
				}
			}
		}
		TRACKER.stop(StaticValues.TIME_KB_SUCCESSORS);
	}
	
	protected boolean isSuccessorRepresented(DomainNode<?> from, DomainNode<?> to, OWLObjectProperty property){
		if(from.getId() instanceof OWLClassExpression && to.getId() instanceof OWLClassExpression){
			Set<DomainNode<?>> successors = from.getSuccessors(property);
//			StatStore.getInstance().enterValue("subclass accessing", 1.0);
//			NodeSet<OWLClass> subClasses = m_ontologyOperator.getReasoner().getSubClasses((OWLClass)to.getId(), false);
			NodeSet<OWLClass> subClasses = currentIdSubClasses;
			Node<OWLClass> eqClasses = currentIdEqClasses;
			for(DomainNode<?> succ : successors){
				// only compare successors to other class domain elements
				if(succ.getId() instanceof OWLClass){
//					if(succ.getInstantiators().containsAll(to.getInstantiators())){ // could be done with reasoner
					if(m_useBuffer){
						if(m_subClassBuffer.get((OWLClass)to.getId()).contains((OWLClass)succ.getId())
								|| m_equivalentClassBuffer.get((OWLClass)to.getId()).contains((OWLClass)succ.getId())){
							return true;
						}
					}else{
						if(subClasses.containsEntity((OWLClass)succ.getId())
								|| eqClasses.contains((OWLClass)succ.getId())){ // if there exists a successors more (or equally) specific than the new one
							return true;
						}
					}
				}
			}
		}
		return false;
	}
	/*
	private boolean isSuccessorRepresented(DomainNode<?> d, OWLObjectProperty property, OWLClassExpression filler) {
		Set<DomainNode<?>> successors = d.getSuccessors(property);
		if(successors == null) return false;
		for(DomainNode<?> succ : successors){
			if(succ.getId() instanceof OWLClassExpression){
				if(m_ontologyOperator.getReasoner().getSubClasses(filler, false)
						.containsEntity(getClassRepresentation((OWLClassExpression)succ.getId()))
					|| m_ontologyOperator.getReasoner().getEquivalentClasses(filler)
						.contains(getClassRepresentation((OWLClassExpression)succ.getId()))
						){
					return true;
				}
			}
		}
		
		return false;
	}*/
	
	protected Set<DomainNode<OWLClassExpression>> removeIncludedSuccessors(OWLObjectProperty property, DomainNode<?> d, OWLClassExpression newSucc){
		Set<DomainNode<OWLClassExpression>> removed = new HashSet<DomainNode<OWLClassExpression>>();
		Set<DomainNode<?>> successors = d.getSuccessors(property);
//		StatStore.getInstance().enterValue("superclass accessing", 1.0);
//		NodeSet<OWLClass> superClasses = m_ontologyOperator.getReasoner().getSuperClasses(newSucc, false);
//		Node<OWLClass> eqClasses = m_ontologyOperator.getReasoner().getEquivalentClasses(newSucc);
		NodeSet<OWLClass> superClasses = currentIdSuperClasses;
		if(successors != null){
//			Set<DomainNode<?>> mark_removed = new HashSet<DomainNode<?>>();
			Iterator<DomainNode<?>> it = successors.iterator();
//			for(DomainNode<?> succ : successors){
			while(it.hasNext()){
				DomainNode<?> succ = it.next();
				if(succ.getId() instanceof OWLClassExpression){ // only inspect class to class relations
					OWLClass succClass = getClassRepresentation((OWLClassExpression)succ.getId());
					// if newSucc is more specific than succ, remove succ
					if(m_useBuffer){
						if(m_superClassBuffer.get(newSucc).contains(succClass)){
							it.remove();
							removed.add(m_domain.getDomainNode((OWLClassExpression)succ.getId()));
						}
					}else{
						if(superClasses.containsEntity(succClass)){
							it.remove();
							removed.add(m_domain.getDomainNode((OWLClassExpression)succ.getId()));
	//						mark_removed.add(succ);
						}
					}
				}// else what about individual domain elements
			}
//			successors.removeAll(mark_removed);
		}
		return removed;
	}
	
	protected OWLClass getFreshQueryClass(String base){
		long cnt = 0;
		String iriString = base;
		while(m_ontologyOperator.getOntology().containsClassInSignature(IRI.create(iriString))){
			iriString = base + (cnt++); // not guaranteed to succeed.. however |long| amount of possibilities
		}
		return OWLManager.getOWLDataFactory().getOWLClass(IRI.create(iriString));
	}
	
	protected void insertQueryAxiom(OWLClass queryClass){
//		Main.getOntologyManager().addAxiom(m_ontologyOperator.getOntology(),
//				OWLManager.getOWLDataFactory().getOWLEquivalentClassesAxiom(queryClass, m_referenceExpression));
		
		m_ontologyOperator.getOntology().getOWLOntologyManager().addAxiom(m_ontologyOperator.getOntology(),
//				OWLManager.getOWLDataFactory().getOWLEquivalentClassesAxiom(
				OWLManager.getOWLDataFactory().getOWLSubClassOfAxiom(
					queryClass,
					m_referenceExpression.accept(
							m_ontologyOperator.getFlatteningTransformer().getVisitor()
					)
		));
		m_ontologyOperator.getOntology().getOWLOntologyManager()
		.applyChanges(m_ontologyOperator.getFlatteningTransformer().getVisitor().getChanges());
		m_ontologyOperator.getFlatteningTransformer().getVisitor().resetChangeList();
		m_ontologyOperator.ontologyChanged();
	}
	
	public OWLClass getClassRepresentation(OWLClassExpression ex){
		if(ex != null && ex.equals(m_referenceExpression)) // before OWLClass check, in case referenceExpression is also a class
			return m_referenceClass;
		
		if(ex instanceof OWLClass){
			return (OWLClass)ex;
		}

		return m_ontologyOperator.getFlatteningTransformer().getIntermediary(ex);
	}
	
	public DomainNode<?> getDomainElement(Object o){
		Object searchFor = o;
		if(o instanceof OWLClassExpression){
			OWLClass c = getClassRepresentation((OWLClassExpression)o);
			STAT.enterValue(StaticValues.STAT_ASSOCIATION_CONTAINS_CHECKS, 1.0);
			searchFor = m_classAssociations.get(c);
			if(searchFor == null) searchFor = o;
		}
		return m_domain.getDomainNode(searchFor);
	}
	
	public boolean isKBMode(){
		return m_referenceExpression == null;
	}
	
	public boolean isRestrictedInstantiator(OWLClass c){
		return c == null
				|| c.isTopEntity()
				|| m_ontologyOperator.getFlatteningTransformer().isIntermediary(c)
				|| c.equals(m_referenceClass);
	}
	
	public CanonicalDomain getDomain() {
		return m_domain;
	}
	
	public OntologyOperator getOntologyOperator() {
		return m_ontologyOperator;
	}
	
	public void addAssociation(OWLClass c, OWLNamedIndividual i){
		m_classAssociations.put(c, i);
	}
	
	/**
	 * If set to true, the generator attempts to associate OWLClass domain nodes
	 * with OWLNamedIndividual domain nodes if they are an instance of the class,
	 * because they would behave exactly the same. This keeps the model smaller.
	 * @param keepSmall
	 */
	public void setSmallCreationFlag(boolean keepSmall){
		this.m_keepSmall = keepSmall;
	}
	
	/**
	 * If set to true, the generator will normalize role successors as they are 
	 * generated. This consists of two extra steps:<br />
	 * 1. check if the new role is already 'represented' by another role, if not do 2. + 3.<br />
	 * 2. remove all roles that will be 'represented' by the new one<br />
	 * 3. add the role<br />
	 * This has the effect of directly creating a normalized canonical interpretation, however when
	 * removing property assertions from the ABox, the interpretation may not be a model anymore. 
	 * @param normalize
	 */
	public void setNormalizingFlag(boolean normalize){
		this.m_normalize = normalize;
	}
	
	public void setUseBuffer(boolean buffer){
		m_useBuffer = buffer;
	}
	
	/**
	 * Initialize your own logger for the generator.
	 * If not actively specified, default logger will be used.
	 * Call with null to disable logging for the generator. 
	 * @param log
	 */
	public void setLogger(Logger log){
		if(log == null){
			this.LOG.setLevel(Level.OFF);
		}else{
			this.LOG = log;
		}
	}
	
	public Logger getLogger() {
		return LOG;
	}
}
