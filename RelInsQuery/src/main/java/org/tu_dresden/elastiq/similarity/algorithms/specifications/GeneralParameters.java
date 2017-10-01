package org.tu_dresden.elastiq.similarity.algorithms.specifications;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
//import java.util.logging.Logger;

//import main.StaticValues;

public class GeneralParameters {

//	private static final Logger LOG = Logger.getLogger(StaticValues.LOGGER_NAME);
	
	public static final String SMALL_MODEL = "smallModel";
	public static final String NORMALIZING = "normalizing";
	public static final String DECIMAL_ACCURACY = "accuracy";
	public static final String LOG_LEVEL = "logLevel";
	public static final String OUT_DIR = "output";
	
	private Set<OutputType> m_outputs;
	
	private Map<String, Object> m_values;
	
	public GeneralParameters(){
		m_values = new HashMap<String, Object>();
		m_outputs = new HashSet<OutputType>();
		
		setDefaults();
	}
	
	private void setDefaults(){
		m_values.put(SMALL_MODEL, true);
		m_values.put(NORMALIZING, true);
		m_values.put(DECIMAL_ACCURACY, 10);
		m_values.put(LOG_LEVEL, Level.INFO);
		m_values.put(OUT_DIR, new File("./"));
	}
	
	public void enterValue(String key, Object value){
		m_values.put(key, value);
	}
	
	public void addOutput(OutputType type){
		m_outputs.add(type);
	}
	
	public Object getValue(String key){
		return m_values.get(key);
	}
	
	public Set<OutputType> getOutputs(){
		return m_outputs;
	}
	
}
