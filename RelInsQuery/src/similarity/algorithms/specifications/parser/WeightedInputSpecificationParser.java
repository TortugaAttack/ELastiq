package similarity.algorithms.specifications.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import main.StaticValues;

import similarity.algorithms.specifications.OutputType;
import similarity.algorithms.specifications.WeightedInputSpecification;
import similarity.measures.entities.DefaultEntitySimilarityMeasure;
import similarity.measures.entities.PrimitiveEntitySimilarityMeasure;

public class WeightedInputSpecificationParser {

	private static final String COMMENT_PREFIX = "#";
	
	private static final Set<String> PRIMITIVE_MEASURE_IDS = new HashSet<String>();
	
	private static final Logger LOG = Logger.getLogger(StaticValues.LOGGER_NAME);
	
	private Map<Block, List<String>> m_blockContent;
	
	private File m_input;
	
	private WeightedInputSpecification m_result;
	
	private boolean m_fatal;
	
	private List<String> m_errors;
	
	public WeightedInputSpecificationParser(File f) {
		m_input = f;
		
		m_blockContent = new HashMap<Block, List<String>>();
		m_errors = new ArrayList<String>();
		m_fatal = false;
		
		PRIMITIVE_MEASURE_IDS.add("PRIMITIVE");
		PRIMITIVE_MEASURE_IDS.add("DEFAULT");
	}
	
	public WeightedInputSpecification parse() throws IOException{
		if(m_result != null) return m_result;
		m_result = new WeightedInputSpecification();
		
		searchBlock();
		
		for(String error : m_errors){
			LOG.warning(error);
		}
		if(m_fatal){
			LOG.severe("The above errors lead to an unacceptable input specification.");
			System.exit(1);
		}
		
		return m_result;
	}
	
	private void searchBlock() throws IOException{
		FileReader fr = new FileReader(m_input);
		BufferedReader br = new BufferedReader(fr);
		String line = br.readLine();
		Set<Block> blocksRead = new HashSet<Block>();
		while(line != null && !m_fatal){
			if(!line.startsWith(COMMENT_PREFIX)){
			
				if(isBlockId(line)){
					String blockName = line.replace("[", "");
					blockName = blockName.replace("]", "");
					Block block = Block.getBlock(blockName);
					if(blocksRead.contains(block)){
						m_fatal = true;
						m_errors.add("Found block " + line + " twice. FATAL");
					}else{
						blocksRead.add(block);
						line = readBlock(br, block);
						continue; // skips readLine, since line is already the next thing to look at
					}
				}
			}
			
			
			line = br.readLine();
			
		}
		
	}
	
	private String readBlock(BufferedReader br, Block block) throws IOException{
		String line = br.readLine();
		int cnt = 0; // counts the valid content lines
		while(line != null && !isBlockId(line)){
			if(!line.startsWith(COMMENT_PREFIX)){
			
				if(line.matches(block.getValueRegex())){
					cnt++;
					switch(block){
					case QUERY : 
						m_result.setQuery(line);
						return br.readLine();
					case ONTOLOGY : 
						m_result.setOntologyFile(line);
						return br.readLine();
					case DISCOUNTING :
						m_result.setDiscountingFactor(Double.parseDouble(line));
						return br.readLine();
					case WEIGHTS :
						m_result.setWeight(line.split(":")[0], Double.parseDouble(line.split(":")[1]));
						break;
					case MEASURE :
						if(cnt == 1){ // expect measure specifier
							if(line.equals("PRIMITIVE")){
								m_result.setPrimitiveMeasure(new PrimitiveEntitySimilarityMeasure());
								break;
							}else if(line.equals("DEFAULT")){
								m_result.setPrimitiveMeasure(new DefaultEntitySimilarityMeasure());
								return br.readLine(); // do not bother reading explicit settings if they exist
							}
						}else{
							if(!PRIMITIVE_MEASURE_IDS.contains(line)){
								m_result.setPrimitiveSimilarity(line.split(":")[0],
																line.split(":")[1], 
																Double.parseDouble(line.split(":")[2]));
							}
							break;
						}
					case THRESHOLD :
						m_result.setThreshold(Double.parseDouble(line));
						return br.readLine();
					case PARAMETERS :
						addParameter(line.split(":")[0], line.split(":")[1]);
					case OUTPUT :
						for(OutputType t : OutputType.values()){
							if(t.toString().equals(line)){
								m_result.getParameters().addOutput(t);
								break;
							}
						}
						break;
					default : break;
					}
					
				}else{
					m_errors.add("Ignoring line \"" + line + "\" in block " + block.getBlockString());
				}
			}
			line = br.readLine();
		}
		if(cnt == 0){
			if(block == Block.QUERY || block == Block.THRESHOLD || block == Block.ONTOLOGY 
					|| block == Block.MEASURE || block == Block.DISCOUNTING){
				m_fatal = true;
				m_errors.add("The [" + block.getBlockString() + "] block was empty. FATAL");
			}else{
				m_errors.add("The [" + block.getBlockString() + "] block was empty.");	
			}
			
		}
		
		return line;
	}
	
	/**
	 * Already distinguish and validate custom parameters.
	 * @param key
	 * @param value
	 */
	private void addParameter(String key, String value){
		
	}
	
	private boolean isBlockId(String line){
		return line.startsWith("[") && line.endsWith("]");
	}
}
