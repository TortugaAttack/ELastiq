package org.tu_dresden.elastiq.similarity.algorithms.specifications;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.semanticweb.owlapi.io.OWLParserException;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;
import org.tu_dresden.elastiq.main.Main;
import org.tu_dresden.elastiq.main.StaticValues;
import org.tu_dresden.elastiq.owl.io.OWLOntologyLoader;
import org.tu_dresden.elastiq.owl.io.OWLQueryParser;

import similarity.EntityWeightingFunction;
import similarity.measures.entities.DefaultEntitySimilarityMeasure;
import similarity.measures.entities.SymmetricPrimitiveEntitySimilarityMeasure;

public class BasicInputSpecification implements IInputSpecification {

	private static final Logger LOG = Logger.getLogger(StaticValues.LOGGER_NAME);
	
	private List<OWLClassExpression> m_queries;
	
	private OWLOntology m_ontology;
	
	private double m_threshold;
	private boolean m_topFlag;
	
	protected EntityWeightingFunction m_weightingFunction;

	protected SymmetricPrimitiveEntitySimilarityMeasure m_primitiveMeasure;
	
	private GeneralParameters m_parameters;
	
	private OWLQueryParser m_queryParser;
	
	public BasicInputSpecification() {
		m_queries = new ArrayList<OWLClassExpression>();
		
		m_primitiveMeasure = new DefaultEntitySimilarityMeasure();
		
		m_weightingFunction = new EntityWeightingFunction();
		
		m_threshold = 1.0;
		m_topFlag = false;
		
		m_parameters = new GeneralParameters();
	}
	
	@Override
	public void read(File f) {
		// TODO Auto-generated method stub

	}

	/**
	 * Checks the specification for unusable inputs.
	 * @return true, if nothing unacceptable has been registered.
	 */
	public boolean isValid(){
		
		if(m_ontology == null){
			System.err.println("No ontology specified.");
			return false;
		}
		
		if(m_queries == null){
			System.err.println("No query specified.");
			return false;
		}
		
		if(m_threshold <= 0 || (m_threshold > 1 && !m_topFlag)){
			System.err.println("Threshold must be in (0,1] or TopK > 0");
			return false;
		}
		
		return true;
	}
	
	/* ************* setters *********** */
	public void addQuery(String classExpression){
		if(m_ontology == null){
			LOG.severe("Unable to parse query without specified ontology. Regard the order of your input specifications.");
			return;
		}
		if(m_queryParser == null)
			m_queryParser = new OWLQueryParser(m_ontology);
		// just use default
		try{
			addQuery(m_queryParser.parse(classExpression));
		}catch(OWLParserException ex){
			LOG.warning(ex.getMessage());
			// go on, just skip a failed query
		}
	}
	
	public void addQuery(OWLClassExpression query) {
		this.m_queries.add(query);
	}

	public void setOntologyFile(String path){
		setOntologyFile(new File(path));
	}
	
	public void setOntologyFile(File file){
		OWLOntologyLoader loader = new OWLOntologyLoader(Main.getOntologyManager());
		setOntology(loader.load(file));
	}
	
	public void setOntology(OWLOntology o){
		this.m_ontology = o;
	}
	
	public void setThreshold(double threshold) {
		this.m_threshold = threshold;
	}
	
	public void setTopFlag(boolean flag){
		this.m_topFlag = flag;
	}
	
	/* ********* getters ******** */
	public SymmetricPrimitiveEntitySimilarityMeasure getPrimitiveMeasure(){
		return m_primitiveMeasure;
	}
	
	public OWLOntology getOntology(){
		return m_ontology;
	}
	
	public List<OWLClassExpression> getQueries(){
		return m_queries;
	}
	
	public double getThreshold(){
		return m_threshold;
	}
	
	public boolean isTopK(){
		return m_topFlag;
	}
	
	public GeneralParameters getParameters(){
		return m_parameters;
	}
	
	// default getters
	public Double getWeight(OWLEntity e){
		return m_weightingFunction.weight(e); // should always be the default weight
	}
	
	public Double getDiscountingFactor(){
		return 1.0;
	}
	
	public TerminationMethod getTerminationMethod(){
		return TerminationMethod.RELATIVE;
	}
	
	public double getTerminationValue(){
		return 0.01;
	}
	
}
