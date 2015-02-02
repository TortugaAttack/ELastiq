package similarity.algorithms.specifications;

import java.io.File;
import java.util.Map;
import java.util.logging.Logger;

import main.Main;
import main.StaticValues;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;


import owl.io.OWLOntologyLoader;
import owl.io.OWLQueryParser;

import similarity.measures.entities.DefaultEntitySimilarityMeasure;
import similarity.measures.entities.IEntitySimilarityMeasure;

public class BasicInputSpecification implements IInputSpecification {

	private static final Logger LOG = Logger.getLogger(StaticValues.LOGGER_NAME);
	
	private OWLClassExpression m_query;
	
	private OWLOntology m_ontology;
	
	private double m_threshold;
	
	protected double m_defaultWeight;

	protected IEntitySimilarityMeasure m_primitiveMeasure;
	
	private GeneralParameters m_parameters;
	
	public BasicInputSpecification() {
		m_primitiveMeasure = new DefaultEntitySimilarityMeasure();
		
		m_defaultWeight = 1.0;
		
		m_threshold = 1.0;
		
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
		
		if(m_query == null){
			System.err.println("No query specified.");
			return false;
		}
		
		if(m_threshold <= 0 || m_threshold > 1){
			System.err.println("Threshold must be in (0,1]");
			return false;
		}
		
		return true;
	}
	
	/* ************* setters *********** */
	public void setQuery(String classExpression){
		if(m_ontology == null){
			LOG.severe("Unable to parse query without specified ontology. Regard the order of your input specifications.");
			return;
		}
		OWLQueryParser parser = new OWLQueryParser(m_ontology);
		// just use default
		setQuery(parser.parse(classExpression));
	}
	
	public void setQuery(OWLClassExpression m_query) {
		this.m_query = m_query;
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
	
	/* ********* getters ******** */
	public IEntitySimilarityMeasure getPrimitiveMeasure(){
		return m_primitiveMeasure;
	}
	
	public OWLOntology getOntology(){
		return m_ontology;
	}
	
	public OWLClassExpression getQuery(){
		return m_query;
	}
	
	public double getThreshold(){
		return m_threshold;
	}
	
	public GeneralParameters getParameters(){
		return m_parameters;
	}
	
	// default getters
	public Double getWeight(OWLEntity e){
		return m_defaultWeight;
	}
	
	public Double getDiscountingFactor(){
		return 1.0;
	}
	
	public TerminationMethod getTerminationMethod(){
		return TerminationMethod.RELATIVE;
	}
	
	public double getTerminationValue(){
		return 0.05;
	}
	
}
