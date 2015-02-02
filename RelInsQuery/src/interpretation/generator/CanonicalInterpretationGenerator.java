package interpretation.generator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import interpretation.ds.CanonicalDomain;
import interpretation.ds.DomainNode;
import interpretation.ds.IDomain;
import interpretation.ds.CanonicalInterpretation;

import main.Main;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnonymousIndividual;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;


import owl.IOWLOntologyExtension;
import owl.OntologyOperator;
import owl.transform.flatten.OWLAxiomFlatteningTransformer;

public class CanonicalInterpretationGenerator implements IInterpretationGenerator {

	private OWLClassExpression m_referenceExpression;
	private OWLClass m_referenceClass;
	
	private OntologyOperator m_ontologyOperator;
	
	private Map<OWLClass, OWLNamedIndividual> m_classAssociations;
	
	private CanonicalDomain m_domain;
	
	private boolean m_keepSmall;
	
	private boolean m_normalize;
	
	public CanonicalInterpretationGenerator() {
		this(null);
	}
	
	public CanonicalInterpretationGenerator(OWLClassExpression expr) {
		this.m_referenceExpression = expr;
		this.m_classAssociations = new HashMap<OWLClass, OWLNamedIndividual>();
		
		m_keepSmall = true;
		
		m_normalize = true;
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
		
		OWLAxiomFlatteningTransformer exRestStore = m_ontologyOperator.getExistentialRestrictionStore(); // flattens here
		OWLReasoner reasoner = m_ontologyOperator.getReasoner(isKBMode()); // precomputes inferences
		
		// the element generator creates all necessary domain elements and adds their instantiators
		elemGen.generate(this, m_keepSmall);
		
		if(!isKBMode()){ // TBox + Query mode
			// add all successor relations by iterating all known existential restrictions
			for(OWLObjectSomeValuesFrom some : exRestStore.getRestrictions()){
				addEntailedTBoxSuccessors(some, m_normalize);
			}
		}else{ // KB mode (ABox + TBox)
			// add all ABox property assertion successors
			for(OWLAxiom ax : m_ontologyOperator.getOntology().getABoxAxioms(true)){
				if(ax instanceof OWLObjectPropertyAssertionAxiom){
					OWLObjectPropertyAssertionAxiom pAx = (OWLObjectPropertyAssertionAxiom)ax;
					getDomainElement(pAx.getSubject()).addSuccessor(
							(OWLObjectProperty)pAx.getProperty(),
							getDomainElement(pAx.getObject()));
				}
			}
			
			// add all successor relations by iterating all known existential restrictions
			for(OWLObjectSomeValuesFrom some : m_ontologyOperator.getExistentialRestrictionStore().getRestrictions()){
				addEntailedKBSuccessors(some);
			}
			// normalize later
			// if(m_normalize){ startSimulationComputation(); }
			
		}
		
		return canonInterpretation;
	}
	
	private void addEntailedTBoxSuccessors(OWLObjectSomeValuesFrom some, boolean doNormalizing){
		// the super class, intermediary stands for (some r B)
		OWLClass superClass = m_ontologyOperator.getExistentialRestrictionStore().getIntermediary(some);
		// add successors from all
		NodeSet<OWLClass> classes = m_ontologyOperator.getReasoner().getSubClasses(superClass, false);
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
						if(!isSuccessorRepresented(node, getDomainElement(some.getFiller()), (OWLObjectProperty)some.getProperty())){
							removeIncludedSuccessors((OWLObjectProperty)some.getProperty(), node, some.getFiller());
							
							node.addSuccessor((OWLObjectProperty)some.getProperty(), getDomainElement(some.getFiller()));
						}
					}else{
						node.addSuccessor((OWLObjectProperty)some.getProperty(), getDomainElement(some.getFiller()));
					}
				}
			}
		}
		// add all equivalent class successors
		Iterator<OWLClass> cIt = m_ontologyOperator.getReasoner().getEquivalentClasses(superClass).iterator();
		while(cIt.hasNext()){
			DomainNode<?> node = m_domain.getDomainNode(cIt.next());
			if(node != null){
				if(doNormalizing){
//					if(!isSuccessorRepresented(node, (OWLObjectProperty)some.getProperty(), some.getFiller())){
					if(!isSuccessorRepresented(node, getDomainElement(some.getFiller()), (OWLObjectProperty)some.getProperty())){
						removeIncludedSuccessors((OWLObjectProperty)some.getProperty(), node, some.getFiller());
						
						node.addSuccessor((OWLObjectProperty)some.getProperty(), getDomainElement(some.getFiller()));
					}
				}else{
					node.addSuccessor((OWLObjectProperty)some.getProperty(), getDomainElement(some.getFiller()));
				}
			}
		}
	}
	
	private void addEntailedKBSuccessors(OWLObjectSomeValuesFrom some){
		// add all role-successors entailed by the TBox
		addEntailedTBoxSuccessors(some, true); // for KB mode only normalize TBox contained roles
		
		NodeSet<OWLNamedIndividual> instances = m_ontologyOperator.getReasoner().getInstances(getClassRepresentation(some), false);
		Iterator<Node<OWLNamedIndividual>> it1 = instances.iterator();
		while(it1.hasNext()){
			Iterator<OWLNamedIndividual> it2 = it1.next().iterator();
			while(it2.hasNext()){
				DomainNode<?> from = m_domain.getDomainNode(it2.next());
				if(from != null){
//					if(!isSuccessorRepresented(from, getDomainElement(some.getFiller()), (OWLObjectProperty)some.getProperty())){
						from.addSuccessor((OWLObjectProperty)some.getProperty(), getDomainElement(some.getFiller()));
//					}
				}
			}
		}
	}
	
	private boolean isSuccessorRepresented(DomainNode<?> from, DomainNode<?> to, OWLObjectProperty property){
		if(from.getId() instanceof OWLClassExpression && to.getId() instanceof OWLClassExpression){
			Set<DomainNode<?>> successors = from.getSuccessors(property);
			for(DomainNode<?> succ : successors){
				// only compare successors to other class domain elements
				if(succ.getId() instanceof OWLClassExpression){
					if(succ.getInstantiators().containsAll(to.getInstantiators())){ // could be done with reasoner
						return true;
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
	
	private void removeIncludedSuccessors(OWLObjectProperty r, DomainNode<?> d, OWLClassExpression newSucc){
		Set<DomainNode<?>> successors = d.getSuccessors(r);
		if(successors != null){
			Set<DomainNode<?>> mark_removed = new HashSet<DomainNode<?>>();
			for(DomainNode<?> succ : successors){
				if(succ.getId() instanceof OWLClassExpression){ // only inspect class to class relations
					// if newSucc is more specific than succ, remove succ
					if(m_ontologyOperator.getReasoner().getSuperClasses(newSucc, false)
							.containsEntity(getClassRepresentation((OWLClassExpression)succ.getId()))
						|| m_ontologyOperator.getReasoner().getEquivalentClasses(newSucc)
							.contains(getClassRepresentation((OWLClassExpression)succ.getId()))
							){
						mark_removed.add(succ);
					}
				}// else what about individual domain elements
			}
			successors.removeAll(mark_removed);
		}
	}
	
	private OWLClass getFreshQueryClass(String base){
		long cnt = 0;
		String iriString = base;
		while(m_ontologyOperator.getOntology().containsClassInSignature(IRI.create(iriString))){
			iriString = base + (cnt++); // not guaranteed to succeed.. however |long| amount of possibilities
		}
		return OWLManager.getOWLDataFactory().getOWLClass(IRI.create(iriString));
	}
	
	private void insertQueryAxiom(OWLClass queryClass){
		Main.getOntologyManager().addAxiom(m_ontologyOperator.getOntology(),
				OWLManager.getOWLDataFactory().getOWLEquivalentClassesAxiom(queryClass, m_referenceExpression));
		m_ontologyOperator.ontologyChanged();
	}
	
	public OWLClass getClassRepresentation(OWLClassExpression ex){
		if(ex instanceof OWLClass){
			return (OWLClass)ex;
		}
		if(ex != null && ex.equals(m_referenceExpression))
			return m_referenceClass;

		return m_ontologyOperator.getExistentialRestrictionStore().getIntermediary(ex);
	}
	
	public DomainNode<?> getDomainElement(Object o){
		Object searchFor = o;
		if(o instanceof OWLClassExpression){
			OWLClass c = getClassRepresentation((OWLClassExpression)o);
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
				|| m_ontologyOperator.getExistentialRestrictionStore().isIntermediary(c)
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
}
