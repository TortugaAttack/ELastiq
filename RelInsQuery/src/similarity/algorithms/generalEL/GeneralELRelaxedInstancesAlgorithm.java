package similarity.algorithms.generalEL;

import interpretation.ds.CanonicalDomain;
import interpretation.ds.CanonicalInterpretation;
import interpretation.ds.DomainNode;
import interpretation.ds.PointedInterpretation;
import interpretation.ds.RoleAssertion;
import interpretation.generator.CanonicalInterpretationGenerator;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import main.StaticValues;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;


import similarity.algorithms.IRelaxedInstancesAlgorithm;
import similarity.algorithms.specifications.BasicInputSpecification;
import similarity.algorithms.specifications.GeneralParameters;
import similarity.algorithms.specifications.OutputType;
import similarity.algorithms.specifications.TerminationMethod;
import similarity.algorithms.specifications.WeightedInputSpecification;
import util.ConsolePrinter;
import util.EasyMath;

public class GeneralELRelaxedInstancesAlgorithm implements
		IRelaxedInstancesAlgorithm<BasicInputSpecification> {

	private static final Logger LOG = Logger.getLogger(StaticValues.LOGGER_NAME);
	
	private CanonicalInterpretation m_TBoxModel;
	
	private CanonicalInterpretation m_KBModel;
	
	private BasicInputSpecification m_currentSpec;
	
	private Map<Integer, Map<SimilarityValue, Double>> m_simiDevelopment;
	
	private int m_currentIteration;
	
	private SimilarityValueFactory m_factory;
	
	@Override
	public Map<OWLNamedIndividual, Double> relaxedInstances(
			BasicInputSpecification specification) {
		if(!specification.isValid()){
			LOG.severe("No valid input specification.");
//			System.exit(1);
			return Collections.emptyMap();
		}
		m_currentSpec = specification;
		m_simiDevelopment = new HashMap<Integer, Map<SimilarityValue,Double>>();
		
		CanonicalInterpretationGenerator generator = new CanonicalInterpretationGenerator(); // KB mode first
		generator.setSmallCreationFlag((boolean)m_currentSpec.getParameters().getValue(GeneralParameters.SMALL_MODEL));
		m_KBModel = generator.generate(specification.getOntology());
		m_KBModel.setName("I_KB");
		
		generator = new CanonicalInterpretationGenerator(specification.getQuery()); // TBox mode second, it alters the TBox
		generator.setSmallCreationFlag((boolean)m_currentSpec.getParameters().getValue(GeneralParameters.SMALL_MODEL));
		m_TBoxModel = generator.generate(specification.getOntology());
		OWLClass queryClass = generator.getClassRepresentation(specification.getQuery());
		m_TBoxModel.setName("I_QT");
		
		// show everything so far (incl. flattened ontology in order to know what the intermediaries are defined as)
		if(LOG.getLevel() == Level.FINE){
			StringBuilder sb = new StringBuilder();
			sb.append("Intermediary datastructures after preprocessing:\n");
			sb.append(StaticValues.SEPERATOR + "\n");
			sb.append("The flattened input ontology:\n");
			sb.append(ConsolePrinter.getOntologyString(specification.getOntology()));
			sb.append(StaticValues.SEPERATOR + "\n");
			sb.append("The canonical model for the entire knowledge base:\n");
			sb.append(m_KBModel.toString());
			sb.append(StaticValues.SEPERATOR + "\n");
			sb.append("The canonical model for the TBox and the query concept:\n");
			sb.append(m_TBoxModel.toString());
			sb.append(StaticValues.SEPERATOR);
			LOG.fine(sb.toString());
		}

		LOG.info("Start finding the relaxed instances with greater similarity to " + specification.getQuery()
				+ " than " + m_currentSpec.getThreshold());
		
		// TODO compute relaxed instances
		m_factory = SimilarityValueFactory.getFactory();
		
		PointedInterpretation queryPointed = new PointedInterpretation(m_TBoxModel, m_TBoxModel.getDomain().getDomainNode(queryClass));
		// add the values of interest, by doing that, the objects will be created and stored in the task pool
		for(DomainNode<OWLNamedIndividual> indElement : m_KBModel.getDomain().getIndividualElements().values()){
			SimilarityValue trackMe = m_factory.getSimilarityValue(queryPointed, new PointedInterpretation(m_KBModel, indElement), 0);
			m_factory.registerInteresting(trackMe);
		}
		
		// TEST
//		PointedInterpretation p1 = getFix1();
//		PointedInterpretation p2 = getFix2();
//		m_factory.registerInteresting(m_factory.getSimilarityValue(p1, p2, 0));
		// EOT
		
		// store values before first iteration
		storeCurrentSimiDev(m_factory, 0);
		m_currentIteration = 0;
		boolean storeDevelopment = m_currentSpec.getParameters().getOutputs().contains(OutputType.ASCII) ||
									m_currentSpec.getParameters().getOutputs().contains(OutputType.CSV);
		while(!isDone()){ // do until termination condition fires
			m_currentIteration++;
			while(!m_factory.isPoolEmpty()){ // empty the task pool
				SimilarityValue v = m_factory.getNextTask();
				v.setNewValue(similarity(v.getP1(), v.getP2(), m_currentIteration));
//					factory.printStats();
			}
			
			if(LOG.getLevel() == Level.FINE){
				StringBuilder sb = new StringBuilder();
				sb.append("Current status of the similarity pools at iteration " + m_currentIteration + ":\n");
				sb.append(m_factory.getStatus()); // multiple lines
				LOG.fine(sb.toString());
			}
			m_factory.nextIteration();
			
			if(storeDevelopment){
				storeCurrentSimiDev(m_factory, m_currentIteration);
			}
		}
		
		if(storeDevelopment && (LOG.getLevel() == Level.INFO || LOG.getLevel() == Level.FINE)){
			StringBuilder sb = new StringBuilder();
			sb.append("Development of interesting similarity values:\n");
			GeneralELOutputGenerator gen = new GeneralELOutputGenerator(this, m_currentSpec);
			sb.append(gen.renderASCIITable());
			LOG.info(sb.toString());
		}
		
		String termination_reason = "";
		if(m_currentSpec.getTerminationMethod() == TerminationMethod.ABSOLUTE){
			if(m_currentIteration < m_currentSpec.getTerminationValue()){
				termination_reason = " because through the last iteration no value has changed.";
			}else{
				termination_reason = " because a fix number of " + (int)m_currentSpec.getTerminationValue() + " iterations was specified.";
			}
		}else if(m_currentSpec.getTerminationMethod() == TerminationMethod.RELATIVE){
			termination_reason = " because through the last iteration no value changed by more than " + (m_currentSpec.getTerminationValue()*100) + "%.";
		}
		LOG.info("The computation stopped after " + m_currentIteration + " iterations" + termination_reason);
		
		Map<OWLNamedIndividual, Double> results = new HashMap<OWLNamedIndividual, Double>();
		
		for(PointedInterpretation p : m_factory.getValuesOfInterest().keySet()){
			for(SimilarityValue v : m_factory.getValuesOfInterest().get(p).values()){
				if(v.getValue(m_currentIteration) >= m_currentSpec.getThreshold()){
					results.put((OWLNamedIndividual)v.getP2().getElement().getId(), v.getValue(m_currentIteration));
				}
			}
		}
		
		return results;
	}
	
	private double similarity(PointedInterpretation p, PointedInterpretation q, int i){
		return (simCN(p, q) + simCN(q, p) + simSC(p, q, i) + simSC(q, p, i))
				/
				(weightedSumClasses(p.getElement().getInstantiators())
					+ weightedSumClasses(q.getElement().getInstantiators())
					+ weightedSumRoles(p.getElement().getSuccessorObjects())
					+ weightedSumRoles(q.getElement().getSuccessorObjects()));
	}
	
	private double simCN(PointedInterpretation p, PointedInterpretation q){
		double sum = 0.0;
		for(OWLClass a : p.getElement().getInstantiators()){
			double max = 0.0;
			for(OWLClass b : q.getElement().getInstantiators()){
				max = Math.max(max, m_currentSpec.getPrimitiveMeasure().similarity(a, b));
			}
			sum += m_currentSpec.getWeight(a) * max;
		}
//		System.out.println("Concept Sim: " + p + " simCN " + q + " = " + sum);
		return sum;
	}
	
	private double simSC(PointedInterpretation p, PointedInterpretation q, int iterations){
		double sum = 0.0;
		double w = m_currentSpec.getDiscountingFactor();
//		System.out.println(w + " = w -> (w + (1-w)*0) = " + (w + (1 - w) * 0));
		for(RoleAssertion r : p.getElement().getSuccessorObjects(p.getInterpretation())){
			double max = 0.0;
			for(RoleAssertion s : q.getElement().getSuccessorObjects(q.getInterpretation())){
				PointedInterpretation pn = new PointedInterpretation(p.getInterpretation(), r.getTo());
				PointedInterpretation qn = new PointedInterpretation(q.getInterpretation(), s.getTo());
				double v = m_currentSpec.getPrimitiveMeasure().similarity(r.getProperty(), s.getProperty());
				if(v > 0){ // do not create unnecessary tasks
					v = v * ((1 - w) + w * SimilarityValueFactory.getFactory().getSimilarityValue(pn, qn, iterations-1).getValue(iterations-1) ); // version of the example
//					v = v * (w + (1 - w) * SimilarityValueFactory.getFactory().getSimilarityValue(pn, qn, iterations-1).getValue(iterations-1) ); // version of the definition
				}
				max = Math.max(max, v);
			}
			sum += m_currentSpec.getWeight(r.getProperty()) * max;
		}
//		System.out.println("Successor Sim: " + p + " simSC " + q + " = " + sum);
		return sum;
	}
	
	private double weightedSumClasses(Set<? extends OWLEntity> entities){
		double sum = 0.0;
		for(OWLEntity e : entities){
			sum += m_currentSpec.getWeight(e);
		}
		return sum;
	}
	
	private double weightedSumRoles(Set<RoleAssertion> successors){
		double sum = 0.0;
		for(RoleAssertion r : successors){
			sum += m_currentSpec.getWeight(r.getProperty());
		}
		return sum;
	}
	
	private boolean isDone(){
		double precisionTermination = m_currentSpec.getTerminationValue();
		switch(m_currentSpec.getTerminationMethod()){
		case ABSOLUTE :
			if(m_currentIteration >= m_currentSpec.getTerminationValue()) return true;
			// otherwise, continue checking whether no value changed:
			precisionTermination = 0;
		case RELATIVE :
//			double maxDiffPercent = -1.0; // red flag
			BigDecimal maxDiffPercent = new BigDecimal(-1);
			for(PointedInterpretation p : m_factory.getValuesOfInterest().keySet()){
				for(SimilarityValue v : m_factory.getValuesOfInterest().get(p).values()){
//					double oldV = v.getValue(m_currentIteration-1);
//					double newV = v.getValue(m_currentIteration);
					// to avoid division "strangeness", round to 5 decimal places
//					double pot = Math.pow(10, StaticValues.DECIMAL_ACCURACY);
//					oldV = Math.round(oldV*pot)/pot;
//					newV = Math.round(newV*pot)/pot;
//					double diff = newV / oldV;
//					if(newV >= oldV){
//						diff -= 1;
//					}
//					if(diff < 0){
//						diff = 1; // if the old value did not exist yet (iteration -1), set to 100% difference
//					}
//					maxDiffPercent = Math.max(diff, maxDiffPercent);
					
					// big decimal version
					BigDecimal oldV = new BigDecimal(v.getValue(m_currentIteration-1));
					BigDecimal newV = new BigDecimal(v.getValue(m_currentIteration));
					
//					MathContext mc = new MathContext(StaticValues.DECIMAL_ACCURACY);
//					oldV = oldV.round(mc);
//					newV = newV.round(mc);
					
					BigDecimal diff = new BigDecimal(2); // will be reset to 1
					if(!oldV.equals(BigDecimal.ZERO)){ // if oldV is 0, set to 100% difference
						diff = newV.divide(oldV, RoundingMode.HALF_UP);
					}
					
					if(newV.compareTo(oldV) >= 0){ // geq
						diff = diff.subtract(BigDecimal.ONE);
					}
					
					if(diff.compareTo(new BigDecimal(0)) < 0){
						diff = BigDecimal.ONE;
					}
					
					maxDiffPercent = diff.max(maxDiffPercent);
				}
			}
//			if(maxDiffPercent <= m_currentSpec.getTerminationValue()){
//				return true;
//			}
			if(maxDiffPercent.compareTo(new BigDecimal(precisionTermination)) <= 0){
				return true;
			}
			break;
		default : break;
		}
		return false;
	}
	
	private void storeCurrentSimiDev(SimilarityValueFactory f, int i){
		m_simiDevelopment.put(i, new HashMap<SimilarityValue, Double>());
		for(PointedInterpretation p1 : f.getValuesOfInterest().keySet()){
			for(PointedInterpretation p2 : f.getValuesOfInterest().get(p1).keySet()){
				SimilarityValue v = f.getValuesOfInterest().get(p1).get(p2);
				m_simiDevelopment.get(i).put(v, v.getValue(i));
			}
		}
	}
	
	public Map<Integer, Map<SimilarityValue, Double>> getSimiDevelopment(){
		return m_simiDevelopment;
	}
	
	private static PointedInterpretation getFix1(){
		CanonicalInterpretation i = new CanonicalInterpretation();
		CanonicalDomain d = new CanonicalDomain();
		i.initDomain(d);
		
		OWLDataFactory f = OWLManager.getOWLDataFactory();
		String[] classesS = {"Amount", "Service", "Low", "Seekable", "VideoStreamService", "High", "Server", "Computer", "Medium", "DatabaseService", "SQL"};
		String[] propertiesS = {"hasLoad", "hasLatency", "hasFeature", "hasQuality", "provides", "queryLanguage"};
		String[] individualsS = {"d", "e"};
		
		Map<String, OWLClass> classes = new HashMap<String, OWLClass>();
		for(int j = 0; j < classesS.length; j++){
			classes.put(classesS[j], f.getOWLClass(IRI.create(classesS[j])));
		}
		Map<String, OWLObjectProperty> properties = new HashMap<String, OWLObjectProperty>();
		for(int j = 0; j < propertiesS.length; j++){
			properties.put(propertiesS[j], f.getOWLObjectProperty(IRI.create(propertiesS[j])));
		}
		Map<String, OWLNamedIndividual> individuals = new HashMap<String, OWLNamedIndividual>();
		for(int j = 0; j < individualsS.length; j++){
			individuals.put(individualsS[j], f.getOWLNamedIndividual(IRI.create(individualsS[j])));
		}
		
		// add classes and instantiators
		DomainNode<?> dA = d.addDomainElement(classes.get("Amount"));
		dA.addInstantiator(classes.get("Amount"));
		
		DomainNode<?> dAL = d.addDomainElement(f.getOWLObjectIntersectionOf(classes.get("Amount"), classes.get("Low")));
		dAL.addInstantiator(classes.get("Amount"));
		dAL.addInstantiator(classes.get("Low"));
		
		DomainNode<?> dSV = d.addDomainElement(f.getOWLObjectIntersectionOf(classes.get("Service"), classes.get("VideoStreamService")));
		dSV.addInstantiator(classes.get("Service"));
		dSV.addInstantiator(classes.get("VideoStreamService"));
		
		DomainNode<?> dAH = d.addDomainElement(f.getOWLObjectIntersectionOf(classes.get("Amount"), classes.get("High")));
		dAH.addInstantiator(classes.get("Amount"));
		dAH.addInstantiator(classes.get("High"));
		
		DomainNode<?> dS = d.addDomainElement(f.getOWLObjectIntersectionOf(classes.get("Seekable")));
		dS.addInstantiator(classes.get("Seekable"));
		
		DomainNode<?> dd = d.addDomainElement(individuals.get("d"));
		dd.addInstantiator(classes.get("Server"));
		dd.addInstantiator(classes.get("Computer"));
		
		// add role successors
		dd.addSuccessor(properties.get("hasLoad"), dA);
		dd.addSuccessor(properties.get("provides"), dSV);
		dd.addSuccessor(properties.get("hasLatency"), dAL);
		
		dSV.addSuccessor(properties.get("hasFeature"), dS);
		dSV.addSuccessor(properties.get("hasQuality"), dAH);
		
		PointedInterpretation p = new PointedInterpretation(i, dd);
		System.out.println("P1:\n" + p.getInterpretation());
		
		return p;
	}
	private static PointedInterpretation getFix2(){
		CanonicalInterpretation i = new CanonicalInterpretation();
		CanonicalDomain d = new CanonicalDomain();
		i.initDomain(d);
		
		OWLDataFactory f = OWLManager.getOWLDataFactory();
		String[] classesS = {"Amount", "Service", "Low", "Seekable", "VideoStreamService", "High", "Server", "Computer", "Medium", "DatabaseService", "SQL"};
		String[] propertiesS = {"hasLoad", "hasLatency", "hasFeature", "hasQuality", "provides", "queryLanguage"};
		String[] individualsS = {"d", "e"};
		
		Map<String, OWLClass> classes = new HashMap<String, OWLClass>();
		for(int j = 0; j < classesS.length; j++){
			classes.put(classesS[j], f.getOWLClass(IRI.create(classesS[j])));
		}
		Map<String, OWLObjectProperty> properties = new HashMap<String, OWLObjectProperty>();
		for(int j = 0; j < propertiesS.length; j++){
			properties.put(propertiesS[j], f.getOWLObjectProperty(IRI.create(propertiesS[j])));
		}
		Map<String, OWLNamedIndividual> individuals = new HashMap<String, OWLNamedIndividual>();
		for(int j = 0; j < individualsS.length; j++){
			individuals.put(individualsS[j], f.getOWLNamedIndividual(IRI.create(individualsS[j])));
		}
		
		// add classes and instantiators
		DomainNode<?> dAH = d.addDomainElement(f.getOWLObjectIntersectionOf(classes.get("Amount"), classes.get("High")));
		dAH.addInstantiator(classes.get("Amount"));
		dAH.addInstantiator(classes.get("High"));
		
		DomainNode<?> dAM = d.addDomainElement(f.getOWLObjectIntersectionOf(classes.get("Amount"), classes.get("Medium")));
		dAM.addInstantiator(classes.get("Amount"));
		dAM.addInstantiator(classes.get("Medium"));
		
		DomainNode<?> dSD = d.addDomainElement(f.getOWLObjectIntersectionOf(classes.get("Service"), classes.get("DatabaseService")));
		dSD.addInstantiator(classes.get("Service"));
		dSD.addInstantiator(classes.get("DatabaseService"));
		
		DomainNode<?> dSQL = d.addDomainElement(classes.get("SQL"));
		dSQL.addInstantiator(classes.get("SQL"));
		
		DomainNode<?> de = d.addDomainElement(individuals.get("e"));
		de.addInstantiator(classes.get("Server"));
		de.addInstantiator(classes.get("Computer"));
		
		// add role successors
		de.addSuccessor(properties.get("hasLoad"), dAH);
		de.addSuccessor(properties.get("provides"), dSD);
		de.addSuccessor(properties.get("hasLatency"), dAM);
		
		dSD.addSuccessor(properties.get("queryLanguage"), dSQL);
		
		PointedInterpretation p = new PointedInterpretation(i, de);
		System.out.println("P2:\n" + p.getInterpretation());
		
		return p;
	}	
}
