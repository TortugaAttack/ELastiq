package similarity.algorithms.specifications.parser;

import java.util.HashMap;
import java.util.Map;

import main.StaticValues;

public enum Block {
	QUERY("query", "[a-zA-Z0-9 \\-_\\.\\(\\)]+"),
	ONTOLOGY("ontology", ".+"),
	DISCOUNTING("discount", StaticValues.DOUBLE_0_1_REGEX),
	WEIGHTS("weights", StaticValues.ENTITY_REGEX + ":[0-9]+(\\.[0-9]+){0,1}"),
	MEASURE("measure", "(DEFAULT|PRIMITIVE|"
					+ StaticValues.ENTITY_REGEX + ":"
					+ StaticValues.ENTITY_REGEX + ":" 
					+ StaticValues.DOUBLE_0_1_REGEX + ")"),
	THRESHOLD("threshold", StaticValues.DOUBLE_0_1_REGEX),
	PARAMETERS("parameters", "[^:]*:[^:]*"),
	OUTPUT("output", "(ASCII|CSV|INSTANCES|STATISTICS|TIMES)");
	
	
	
	private String m_strRep;
	private String m_valueRegEx;
	
	private static final Map<String, Block> lookup = new HashMap<String, Block>();
	static{
		for(Block b : Block.values()){
			lookup.put(b.getBlockString(), b);
		}
	}
	
	private Block(String str, String regex) {
		m_strRep = str;
		m_valueRegEx = regex;
	}
	
	public String getBlockString(){
		return this.m_strRep;
	}
	
	public String getValueRegex(){
		return m_valueRegEx;
	}
	
	public static Block getBlock(String str){
		return lookup.get(str);
	}
}
