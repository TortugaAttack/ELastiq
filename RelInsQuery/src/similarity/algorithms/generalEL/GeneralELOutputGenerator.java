package similarity.algorithms.generalEL;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.semanticweb.owlapi.model.OWLNamedIndividual;

import similarity.algorithms.specifications.BasicInputSpecification;
import similarity.algorithms.specifications.GeneralParameters;
import similarity.algorithms.specifications.OutputType;
import util.EasyMath;

public class GeneralELOutputGenerator {

	private GeneralELRelaxedInstancesAlgorithm m_algorithm;
	
	private BasicInputSpecification m_specification;
	
	private static final Map<OutputType, String> DEFAULT_OUTPUT_FILES = new HashMap<OutputType, String>();
	
	public GeneralELOutputGenerator(GeneralELRelaxedInstancesAlgorithm algo, BasicInputSpecification spec){
		m_algorithm = algo;
		m_specification = spec;
		
		if(DEFAULT_OUTPUT_FILES.isEmpty()){
			DEFAULT_OUTPUT_FILES.put(OutputType.ASCII, "value_development.txt");
			DEFAULT_OUTPUT_FILES.put(OutputType.CSV, "value_development.csv");
			DEFAULT_OUTPUT_FILES.put(OutputType.INSTANCES, "answers.txt");
		}
	}
	
	public void storeOutputs(File dir){
		String path = dir.getAbsolutePath();
		if(!path.endsWith("/")) path += "/";
		for(OutputType t : m_specification.getParameters().getOutputs()){
			
			File f = new File(path + DEFAULT_OUTPUT_FILES.get(t));
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
		case ASCII     : return renderASCIITable();
		case CSV       : return renderCSVTable();
		case INSTANCES : return renderInstanceList();
		default        : return "WARNING: output type not supported";
		}
	}
	
	public String renderInstanceList(){
		Map<Integer, Map<SimilarityValue, Double>> simiDevelopment = m_algorithm.getSimiDevelopment();
		int maxIteration = 0;
		for(Integer i : simiDevelopment.keySet()){
			maxIteration = Math.max(maxIteration, i);
		}
		final Map<OWLNamedIndividual, Double> answers = new HashMap<OWLNamedIndividual, Double>();
		
		double threshold = m_specification.getThreshold();
		for(SimilarityValue v : simiDevelopment.get(maxIteration).keySet()){
			if(v.getValue(maxIteration) > threshold){
				answers.put((OWLNamedIndividual)v.getP2().getElement().getId(), v.getValue(maxIteration));
			}
		}
		
		return renderInstanceList(answers);
	}
	
	public String renderInstanceList(final Map<OWLNamedIndividual, Double> instances){
		List<OWLNamedIndividual> sorted = new ArrayList<OWLNamedIndividual>();
		for(OWLNamedIndividual ind : instances.keySet())
			sorted.add(ind);
		
		Collections.sort(sorted, new Comparator<OWLNamedIndividual>() {
			@Override
			public int compare(OWLNamedIndividual o1, OWLNamedIndividual o2) {
				if(instances.get(o1) > instances.get(o2))
					return -1;
				if(instances.get(o1) < instances.get(o2))
					return 1;
				return 0;
			}
		});
		StringBuilder sb = new StringBuilder();
		for(OWLNamedIndividual ind : sorted){
			sb.append(ind.toString() + "\t" + "similarity: " + EasyMath.round(instances.get(ind), 
					(Integer)m_specification.getParameters().getValue(GeneralParameters.DECIMAL_ACCURACY)) + "\n");
		}
		return sb.toString();
	}
	
	public String renderCSVTable(){
		String colSep = "\t";
		String lineSep = ";\n";
		Map<Integer, Map<SimilarityValue, Double>> simiDevelopment = m_algorithm.getSimiDevelopment();
		StringBuilder sb = new StringBuilder();
		// table head
		sb.append("i");
		List<SimilarityValue> values = new ArrayList<SimilarityValue>(); 
		for(SimilarityValue v : simiDevelopment.get((Integer)simiDevelopment.keySet().iterator().next()).keySet()){
			sb.append(colSep + getShortForm(v));
			values.add(v);
		}sb.append(lineSep);
		for(Integer i : simiDevelopment.keySet()){
			sb.append(i);
			for(SimilarityValue v : values){
				sb.append(colSep + EasyMath.round(simiDevelopment.get(i).get(v),
						(Integer)m_specification.getParameters().getValue(GeneralParameters.DECIMAL_ACCURACY))
						);
			}sb.append(lineSep);
		}
		return sb.toString();
	}
	
	public String renderASCIITable(){
		Map<Integer, Map<SimilarityValue, Double>> simiDevelopment = m_algorithm.getSimiDevelopment();
		if(simiDevelopment.keySet().size() == 0) return "";
		StringBuilder sb = new StringBuilder();
		int preLength = (simiDevelopment.keySet().size() + "").length();
		for(int i = 0; i<preLength-1; i++){
			sb.append(" ");
		}sb.append("i");
		List<SimilarityValue> values = new ArrayList<SimilarityValue>();
		
		// determine maximal width of any element per column
		Map<SimilarityValue, Integer> widths = new HashMap<SimilarityValue, Integer>();
		// init with table head
		for(SimilarityValue v : simiDevelopment.get(simiDevelopment.keySet().iterator().next()).keySet()){
			values.add(v);
			widths.put(v, getShortForm(v).length());
		}
		for(Integer i : simiDevelopment.keySet()){
			for(SimilarityValue v : values){
				double value = simiDevelopment.get(i).get(v);
				value = EasyMath.round(value, (Integer)m_specification.getParameters().getValue(GeneralParameters.DECIMAL_ACCURACY));
				widths.put(v, Math.max((value+"").length(), widths.get(v)));
			}
		}
		int totalWidth = preLength;
		for(SimilarityValue v : values){
			totalWidth += 1 + widths.get(v); // +1 for spacing
		}

		// continue table-head
		for(SimilarityValue v : values){
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
			
			for(SimilarityValue v : values){
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
}
