package similarity.algorithms.generalEL;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import main.Main;
import main.StaticValues;

import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import similarity.algorithms.specifications.BasicInputSpecification;
import similarity.algorithms.specifications.GeneralParameters;
import similarity.algorithms.specifications.OutputType;
import statistics.StatStore;
import tracker.TimeTracker;
import util.EasyMath;

/**
 * Supplies different rendering strategies for {@link OutputType} and writing them to 
 * a specified file.
 * @author Maximilian Pensel - maximilian.pensel@gmx.de
 *
 */
public class GeneralELOutputGenerator {

	private GeneralELRelaxedInstancesAlgorithm m_algorithm;
	
	private BasicInputSpecification m_specification;
	
	public GeneralELOutputGenerator(GeneralELRelaxedInstancesAlgorithm algo, BasicInputSpecification spec){
		m_algorithm = algo;
		m_specification = spec;
	}
	
	public void storeOutputs(File dir){
		String path = dir.getAbsolutePath();
		if(!path.endsWith("/")) path += "/";
		for(OutputType t : m_specification.getParameters().getOutputs()){
			if(m_algorithm == null && t.requiresAlgorithmResult()) continue; // enable output storage in the middle of the computation
			
			File f = new File(path + StaticValues.getDefaultOutputFile(t));
			try{
				if(!f.exists()) f.createNewFile();
				FileWriter fw = new FileWriter(f);
				fw.write(renderOutput(t));
				fw.close();
			}catch(IOException e){
				e.printStackTrace();
			}
			
		}
	}
	
	public String renderOutput(OutputType type){
		switch(type){
		case ASCII      : return renderASCIITable();
		case CSV        : return renderCSVTable();
		case INSTANCES  : return renderInstanceList();
		case STATISTICS : return StatStore.getInstance().getCSVString();
		case TIMES      : return TimeTracker.getInstance().createEvaluation();
		default         : return "WARNING: output type not supported";
		}
	}
	
	public String renderInstanceList(){
		StringBuilder sb = new StringBuilder();
		List<OWLClassExpression> queries = m_specification.getQueries();
		for(int query : m_algorithm.getAnswers().keySet()){
			sb.append("Query " + query + ": " + queries.get(query-1) + "\n");
			sb.append(renderInstanceList(m_algorithm.getAnswers().get(query)));
		}
		return sb.toString();
	}
	
	public String renderInstanceList(int query){
		return renderInstanceList(m_algorithm.getAnswers().get(query));
	}
	
	public String renderInstanceList(final Map<OWLNamedIndividual, Double> instances){
		return renderInstanceList(instances, false);
	}
	
	public String renderInstanceList(final Map<OWLNamedIndividual, Double> instances, boolean ascending){
		List<OWLNamedIndividual> sorted = getSortedAnswers(instances, ascending);
		StringBuilder sb = new StringBuilder();
		sb.append(sorted.size() + " answers");
		if(m_specification.isTopK() && m_specification.getThreshold() < sorted.size())
			sb.append(" (additional results with the same similarity as answer "+(int)m_specification.getThreshold()+" are included)");
		sb.append(":\n");
		for(OWLNamedIndividual ind : sorted){
			sb.append(ind.toString() + "\t" + "similarity: " + EasyMath.round(instances.get(ind), 
					(Integer)m_specification.getParameters().getValue(GeneralParameters.DECIMAL_ACCURACY)) + "\n");
		}
		return sb.toString();
	}
	
	public String renderCSVTable(){
		StringBuilder sb = new StringBuilder();
		List<OWLClassExpression> queries = m_specification.getQueries();
		for(int query : m_algorithm.getAnswers().keySet()){
			sb.append("Query " + query + ": " + queries.get(query-1) + "\n");
			sb.append(renderCSVTable(query));
		}
		return sb.toString();
	}
	
	public String renderCSVTable(int query){
		return renderCSVTable(query, false);
	}
	
	public String renderCSVTable(int query, boolean ascending){
		final String COL_SEP = "\t";
		final String LINE_SEP = ";\n";
		Map<Integer, Map<SimilarityValue, Double>> simiDevelopment = m_algorithm.getSimiDevelopment(query, false);
		StringBuilder sb = new StringBuilder();
		// table head
		sb.append("i");
		
		List<SimilarityValue> sorted = getSortedAnswers(simiDevelopment.get(simiDevelopment.size()-1), ascending);
		for(SimilarityValue v : sorted){
			sb.append(COL_SEP + getShortForm(v));
		}sb.append(LINE_SEP);
		
		// table body
		for(Integer i : simiDevelopment.keySet()){
			sb.append(i);
			for(SimilarityValue v : sorted){
				sb.append(COL_SEP + EasyMath.round(simiDevelopment.get(i).get(v),
						(Integer)m_specification.getParameters().getValue(GeneralParameters.DECIMAL_ACCURACY))
						);
			}sb.append(LINE_SEP);
		}
		return sb.toString();
	}
	
	public String renderASCIITable(){
		StringBuilder sb = new StringBuilder();
		List<OWLClassExpression> queries = m_specification.getQueries();
		for(int query : m_algorithm.getAnswers().keySet()){
			sb.append("Query " + query + ": " + queries.get(query-1) + "\n");
			sb.append(renderASCIITable(query));
		}
		return sb.toString();
	}
	
	public String renderASCIITable(int query){
		return renderASCIITable(query, false);
	}
	
	public String renderASCIITable(int query, boolean ascending){
		Map<Integer, Map<SimilarityValue, Double>> simiDevelopment = m_algorithm.getSimiDevelopment(query, false);
		if(simiDevelopment.keySet().size() == 0) return "";
		StringBuilder sb = new StringBuilder();
		int preLength = (simiDevelopment.keySet().size() + "").length();
		for(int i = 0; i<preLength-1; i++){
			sb.append(" ");
		}sb.append("i");
		List<SimilarityValue> sorted = getSortedAnswers(simiDevelopment.get(simiDevelopment.size()-1), ascending);
		
		// determine maximal width of any element per column
		Map<SimilarityValue, Integer> widths = new HashMap<SimilarityValue, Integer>();
		// init with table head
		for(SimilarityValue v : sorted){
			widths.put(v, getShortForm(v).length());
		}
		for(Integer i : simiDevelopment.keySet()){
			for(SimilarityValue v : sorted){
				double value = simiDevelopment.get(i).get(v);
				value = EasyMath.round(value, (Integer)m_specification.getParameters().getValue(GeneralParameters.DECIMAL_ACCURACY));
				widths.put(v, Math.max((value+"").length(), widths.get(v)));
			}
		}
		int totalWidth = preLength;
		for(SimilarityValue v : sorted){
			totalWidth += 1 + widths.get(v); // +1 for spacing
		}

		// continue table-head
		for(SimilarityValue v : sorted){
			int curElemLength = getShortForm(v).length();
			for(int i=0; i<=widths.get(v) - curElemLength; i++){
				sb.append(" ");
			}
			sb.append(getShortForm(v));
		}
		
		sb.append("\n");
		for(int i = 0; i<totalWidth; i++){
			sb.append("-");
		}
		
		sb.append("\n");
		for(int i = 0; i<simiDevelopment.keySet().size(); i++){
			for(int j=0; j<preLength-(""+i).length(); j++){
				sb.append(" ");
			}
			sb.append(i);
			
			for(SimilarityValue v : sorted){
				Double d = EasyMath.round(simiDevelopment.get(i).get(v),
						(Integer)m_specification.getParameters().getValue(GeneralParameters.DECIMAL_ACCURACY));
				for(int j=0; j<=widths.get(v)-(""+d).length(); j++){
					sb.append(" ");
				}
				sb.append(d);
			}
			
			
			sb.append("\n");
			
		}
		
		return sb.toString();
	}
	
	private String getShortForm(SimilarityValue v){
		return "(" + v.getP1().getElement().toShortString() + ", " + v.getP2().getElement().toShortString() + ")";
	}
	
	private <T> List<T> getSortedAnswers(final Map<T, Double> instances, boolean ascending){
		List<T> sorted = new ArrayList<T>();
		for(T ind : instances.keySet())
			sorted.add(ind);
		
		// sorts answers descending by their similarity
		Collections.sort(sorted, new Comparator<T>() {
			@Override
			public int compare(T o1, T o2) {
				if(instances.get(o1) > instances.get(o2))
					return -1;
				if(instances.get(o1) < instances.get(o2))
					return 1;
				return 0;
			}
		});
		
		if(ascending)
			Collections.reverse(sorted);
		
		if(m_specification.isTopK()){
			Iterator<T> it = sorted.iterator();
			int count = 0;
			double lastSim = 0;
			boolean removeNow = false;
			while(it.hasNext()){
				count++;
				T cur = it.next();
				if(removeNow || (count > m_specification.getThreshold() && lastSim > instances.get(cur))){
					removeNow = true;
					it.remove();
				}else{
					lastSim = instances.get(cur);
				}
			}
		}
		
		return sorted;
	}
}
