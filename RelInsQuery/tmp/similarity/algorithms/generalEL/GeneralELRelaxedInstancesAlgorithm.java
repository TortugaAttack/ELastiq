package similarity.algorithms.generalEL;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import interpretation.ds.CanonicalInterpretation;
import interpretation.ds.DomainNode;
import interpretation.ds.PointedInterpretation;
import interpretation.generator.IInterpretationGenerator;
import interpretation.generator.IterativeKBInterpretationGenerator;
import interpretation.generator.IterativeQTBoxModelGenerator;
import main.StaticValues;
import similarity.algorithms.IRelaxedInstancesAlgorithm;
import similarity.algorithms.specifications.BasicInputSpecification;
import similarity.algorithms.specifications.GeneralParameters;
import similarity.algorithms.specifications.OutputType;
import similarity.algorithms.specifications.TerminationMethod;
import similarity.measures.PointedISM;
import statistics.StatStore;
import tracker.BlockOutputMode;
import tracker.TimeTracker;
import util.ConsolePrinter;

public class GeneralELRelaxedInstancesAlgorithm implements
		IRelaxedInstancesAlgorithm<BasicInputSpecification> {

	private static final Logger LOG = Logger.getLogger(StaticValues.LOGGER_NAME);
	private static final TimeTracker TRACKER = TimeTracker.getInstance();
	
	private CanonicalInterpretation m_TBoxModel;
	
	private CanonicalInterpretation m_KBModel;
	
	private BasicInputSpecification m_currentSpec;
	
	private boolean m_specChanged;
	
	private Map<Integer, Map<Integer, Map<SimilarityValue, Double>>> m_simiDevelopment;
	
	private Map<Integer, Map<OWLNamedIndividual, Double>> m_answers;
	
	private int m_currentIteration;
	
	private SimilarityValueFactory m_factory;
	
	@Override
	public Map<Integer, Map<OWLNamedIndividual, Double>> relaxedInstances(
			BasicInputSpecification specification) {
		if(!specification.isValid()){
			LOG.severe("No valid input specification.");
//			System.exit(1);
			return Collections.emptyMap();
		}
		if(m_currentSpec == null || !m_currentSpec.equals(specification)){
			m_specChanged = true;
			m_currentSpec = specification;
		}
		
		PointedISM measure = new PointedISM(specification, this);
		
		m_simiDevelopment = new HashMap<Integer, Map<Integer, Map<SimilarityValue,Double>>>();
		m_answers = new HashMap<Integer, Map<OWLNamedIndividual,Double>>();
		
		int model_normalizing = (int)m_currentSpec.getParameters().getValue(GeneralParameters.NORMALIZING);
		
		TRACKER.start(StaticValues.TIME_MODEL_KB, BlockOutputMode.IN_TREE);
//		IInterpretationGenerator generator = new CanonicalInterpretationGenerator(2 == model_normalizing); // KB mode first
		IInterpretationGenerator generator = new IterativeKBInterpretationGenerator(2 == model_normalizing); // KB mode first
		generator.setSmallCreationFlag((boolean)m_currentSpec.getParameters().getValue(GeneralParameters.SMALL_MODEL));
		generator.setLogger(LOG);
		m_KBModel = generator.generate(specification.getOntology());
		m_KBModel.setName("I_KB");
		TRACKER.stop(StaticValues.TIME_MODEL_KB);
		
		if(LOG.getLevel().intValue() <= Level.FINE.intValue()){
			StringBuilder sb = new StringBuilder();
			sb.append("Intermediary datastructures after preprocessing:\n");
			sb.append(StaticValues.SEPERATOR + "\n");
			sb.append("The flattened input ontology:\n");
			sb.append(ConsolePrinter.getOntologyString(specification.getOntology()));
			sb.append(StaticValues.SEPERATOR + "\n");
			sb.append("The canonical model for the entire knowledge base:\n");
			sb.append(m_KBModel.toString());
			sb.append(StaticValues.SEPERATOR + "\n");
			LOG.fine(sb.toString());
		}
		
		TRACKER.stop(StaticValues.TIME_PREPROCESSING);
		
		/* ******************************************  */
		/* ******* ITERATING OVER ALL QUERIES ******** */
		/* ******************************************* */
		TRACKER.start(StaticValues.TIME_MAIN_ALGO, BlockOutputMode.COMPLETE);
		for(int queryIndex = 1; queryIndex <= m_currentSpec.getQueries().size(); queryIndex++){
			LOG.info("start processing query " + queryIndex + ": " + m_currentSpec.getQueries().get( queryIndex - 1 ));
			TRACKER.start(StaticValues.TIME_QUERY_BASE + queryIndex);
			OWLClassExpression query = m_currentSpec.getQueries().get( queryIndex - 1 );
			Map<Integer, Map<SimilarityValue,Double>> simiDev = new HashMap<Integer, Map<SimilarityValue,Double>>();
			
			
			TRACKER.start(StaticValues.TIME_MODEL_QT, BlockOutputMode.IN_TREE);
	//		generator = new CanonicalInterpretationGenerator(specification.getQuery(), 2 == model_normalizing); // TBox mode second, it alters the TBox
			generator = new IterativeQTBoxModelGenerator(query, 2 == model_normalizing);
			generator.setSmallCreationFlag((boolean)m_currentSpec.getParameters().getValue(GeneralParameters.SMALL_MODEL));
			generator.setLogger(LOG);
			m_TBoxModel = generator.generate(specification.getOntology());
			OWLClass queryClass = generator.getClassRepresentation(query);
			m_TBoxModel.setName("I_QT");
			TRACKER.stop(StaticValues.TIME_MODEL_QT);
			
			// show everything so far (incl. flattened ontology in order to know what the intermediaries are defined as)
			if(LOG.getLevel().intValue() <= Level.FINE.intValue()){
				StringBuilder sb = new StringBuilder();
				sb.append("\n" + StaticValues.SEPERATOR + "\n");
				sb.append("The canonical model for the TBox and the query concept:\n");
				sb.append(m_TBoxModel.toString());
				sb.append(StaticValues.SEPERATOR);
				LOG.fine(sb.toString());
			}
			if(m_currentSpec.isTopK()){
				LOG.info("Start finding the best "+m_currentSpec.getThreshold()+" relaxed instances for the query " + m_currentSpec.getQueries().get(queryIndex - 1));
			}else{
				LOG.info("Start finding the relaxed instances with greater similarity to " + m_currentSpec.getQueries().get(queryIndex - 1)
					+ " than " + m_currentSpec.getThreshold());
			}
			
			TRACKER.start(StaticValues.TIME_SIMFACTORY_PREP, BlockOutputMode.COMPLETE);
			// now compute relaxed instances
			m_factory = SimilarityValueFactory.getFactory();
			
			PointedInterpretation queryPointed = new PointedInterpretation(m_TBoxModel, m_TBoxModel.getDomain().getDomainNode(queryClass));
			// add the values of interest, by doing that, the objects will be created and stored in the task pool
			for(DomainNode<OWLNamedIndividual> indElement : m_KBModel.getDomain().getIndividualElements().values()){
//				SimilarityValue trackMe = m_factory.getSimilarityValue(queryPointed, new PointedInterpretation(m_KBModel, indElement), 0);
				SimilarityValue trackMe = m_factory.initializeSimilarityValue(queryPointed, new PointedInterpretation(m_KBModel, indElement));
				m_factory.registerInteresting(trackMe);
			}
			TRACKER.stop(StaticValues.TIME_SIMFACTORY_PREP);
			
			// store values before first iteration
			storeCurrentSimiDev(m_factory, 0, simiDev);
			m_currentIteration = 0;
			boolean storeDevelopment = m_currentSpec.getParameters().getOutputs().contains(OutputType.ASCII) ||
										m_currentSpec.getParameters().getOutputs().contains(OutputType.CSV);
			/* ********************************** */
			/* ** ITERATIVE MATRIX COMPUTATION ** */
			/* ********************************** */
			while(!isDone()){ // do until termination condition is met (terminating all iterations)
				TRACKER.start(StaticValues.TIME_ITERATION);
				m_currentIteration++;
				LOG.info("+++ iteration " + m_currentIteration);
				
				// compute similarity for all tasks for the current iteration
				// empty taskSet => this iteration is done
				int tasksDone = 0;
				while(!m_factory.isTaskSetEmpty()){ // empty the task pool
					SimilarityValue v = m_factory.getNextTask();

					v.setNewValue(measure.similarity(v.getP1(), v.getP2()));

					tasksDone++;
				} // end current iteration

				StatStore.getInstance().enterValue("tasks per iteration on query " + queryIndex, tasksDone*1.0);
				if(LOG.getLevel() == Level.FINE){
					StringBuilder sb = new StringBuilder();
					sb.append("Current status of the similarity pools at iteration " + m_currentIteration + ":\n");
					sb.append(m_factory.getStatus()); // multiple lines
					LOG.fine(sb.toString());
				}
				m_factory.nextIteration();
				
				if(storeDevelopment){
					storeCurrentSimiDev(m_factory, m_currentIteration, simiDev);
				}
				TRACKER.stop(StaticValues.TIME_ITERATION);
			} // end iterations for current query
			
			// main algorithm done, collect result sets (and print ?!)
			TRACKER.start(StaticValues.TIME_FINALIZING_RESULTS, BlockOutputMode.COMPLETE);
			Map<OWLNamedIndividual, Double> answers = new HashMap<OWLNamedIndividual, Double>();
			for(PointedInterpretation p : m_factory.getValuesOfInterest().keySet()){
				for(SimilarityValue v : m_factory.getValuesOfInterest().get(p).values()){
					if(m_currentSpec.isTopK() || v.getValue(m_currentIteration) >= m_currentSpec.getThreshold()){
						answers.put((OWLNamedIndividual)v.getP2().getElement().getId(), v.getValue(m_currentIteration));
					}
				}
			}

			m_answers.put(queryIndex, answers);
			
			m_simiDevelopment.put(queryIndex, simiDev);
			
			if(storeDevelopment && (LOG.getLevel().intValue() >= Level.FINE.intValue())){
				StringBuilder sb = new StringBuilder();
				sb.append("Development of interesting similarity values:\n");
				GeneralELOutputGenerator gen = new GeneralELOutputGenerator(this, m_currentSpec);
				sb.append(gen.renderASCIITable(queryIndex));
				LOG.info(sb.toString());
				
				LOG.info("All the individuals that are not listed in the table above are totally dissimilar. (sim = 0)");
			}
			
			// compile the reason for termination
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
			
			m_specChanged = false;
			
			m_factory.resetFactory();
			
			TRACKER.stop(StaticValues.TIME_FINALIZING_RESULTS);
			
			TRACKER.stop(StaticValues.TIME_QUERY_BASE + queryIndex);
		} // end query loop
		TRACKER.stop(StaticValues.TIME_MAIN_ALGO);
	
		return m_answers;
	}
	
	private boolean isDone(){
		double precisionTermination = m_currentSpec.getTerminationValue();
		switch(m_currentSpec.getTerminationMethod()){
		case ABSOLUTE :
			if(m_currentIteration >= m_currentSpec.getTerminationValue()) return true;
			// otherwise, continue checking whether no value changed:
			precisionTermination = 0;
		case TOPK : // done for a default precision (topk only crops answer set)
			if(m_currentSpec.getTerminationMethod() != TerminationMethod.ABSOLUTE)
				precisionTermination = StaticValues.DEFAULT_PRECISION;
		case RELATIVE :
			BigDecimal maxDiffPercent = new BigDecimal(-1); // red flag
			for(PointedInterpretation p : m_factory.getValuesOfInterest().keySet()){
				for(SimilarityValue v : m_factory.getValuesOfInterest().get(p).values()){

					BigDecimal oldV = new BigDecimal(v.getValue(m_currentIteration-1));
					BigDecimal newV = new BigDecimal(v.getValue(m_currentIteration));
					
					BigDecimal diff = new BigDecimal(0);
					if(newV.subtract(oldV).compareTo(BigDecimal.ZERO) > 0){ // if difference > 0 calculate diff
						if(oldV.equals(BigDecimal.ZERO) || oldV.equals(new BigDecimal(-1))){ // prevents division by 0 and stopping at iteration 0
							diff = BigDecimal.ONE;
						}else{ // now newV > oldV > 0  (by Banach's fixpoint)
							diff = newV.divide(oldV, RoundingMode.HALF_UP).subtract(BigDecimal.ONE);
						}
					}
					
					maxDiffPercent = diff.max(maxDiffPercent);
				}
			}
			if(m_currentIteration > 0 && m_currentIteration % 10 == 0)
				LOG.info("max diff: " + maxDiffPercent.toString());
			if(maxDiffPercent.compareTo(new BigDecimal(precisionTermination)) <= 0){
				return true;
			}
			break;
		default : break;
		}
		return false;
	}
	
	private void storeCurrentSimiDev(SimilarityValueFactory f, int i, Map<Integer, Map<SimilarityValue, Double>> simiDev){
		simiDev.put(i, new HashMap<SimilarityValue, Double>());
		for(PointedInterpretation p1 : f.getValuesOfInterest().keySet()){
			for(PointedInterpretation p2 : f.getValuesOfInterest().get(p1).keySet()){
				SimilarityValue v = f.getValuesOfInterest().get(p1).get(p2);
				simiDev.get(i).put(v, v.getValue(i));
			}
		}
	}
	
	public Map<Integer, Map<Integer, Map<SimilarityValue, Double>>> getSimiDevelopment(){
		return getSimiDevelopment(true);
	}
	
	public Map<Integer, Map<SimilarityValue, Double>> getSimiDevelopment(int query, boolean includeZeroResults){
		if(includeZeroResults){
			return m_simiDevelopment.get(query);
		}else{
			Map<SimilarityValue, Double> finalValues = m_simiDevelopment.get(query).get(m_simiDevelopment.get(query).keySet().size()-1);
			Map<Integer, Map<SimilarityValue, Double>> curReduced = new HashMap<Integer, Map<SimilarityValue,Double>>();
			for(Integer iteration : m_simiDevelopment.get(query).keySet()){
				curReduced.put(iteration, new HashMap<SimilarityValue, Double>());
				for(SimilarityValue v : m_simiDevelopment.get(query).get(iteration).keySet()){
					if(finalValues.get(v) > 0){
						curReduced.get(iteration).put(v, m_simiDevelopment.get(query).get(iteration).get(v));
					}
				}
			}
			
			return curReduced;
		}
	}
	
	public Map<Integer, Map<Integer, Map<SimilarityValue, Double>>> getSimiDevelopment(boolean includeZeroResults){
		if(includeZeroResults){
			return m_simiDevelopment;
		}else{
			Map<Integer, Map<Integer, Map<SimilarityValue, Double>>> reduced = new HashMap<Integer, Map<Integer, Map<SimilarityValue,Double>>>();
			for(Integer i : m_simiDevelopment.keySet()){
				reduced.put(i, getSimiDevelopment(i, includeZeroResults));
			}
			
			return reduced;
		}
	}
	
	public Map<Integer, Map<OWLNamedIndividual, Double>> getAnswers(){
		if(m_answers == null || m_specChanged){
			LOG.warning("Trying to retrieve answers that are not computed yet for the current specification.");
			return Collections.emptyMap();
		}
		return m_answers;
	}
	
	public int getCurrentIteration(){
		return m_currentIteration;
	}
}
