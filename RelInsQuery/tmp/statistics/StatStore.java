package statistics;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatStore {

	private Map<String, Double> m_maxValue;
	private Map<String, Double> m_minValue;
	private Map<String, Double> m_meanValue;
	private Map<String, Integer> m_valueAmount;
	
	private List<String> m_order;
	
	private StatStore() {
		m_maxValue = new HashMap<String, Double>();
		m_minValue = new HashMap<String, Double>();
		m_meanValue = new HashMap<String, Double>();
		m_valueAmount = new HashMap<String, Integer>();
		
		m_order = new ArrayList<String>();
	}
	
	private static StatStore _instance; 
	public static StatStore getInstance(){
		if(_instance == null) _instance = new StatStore();
		return _instance;
	}
	
	public void enterValue(String id, Double value){
		if(!m_order.contains(id)) initValue(id, value);
		
		m_valueAmount.put(id, m_valueAmount.get(id) + 1);
		
		updateMax(id, value);
		
		updateMin(id, value);
		
		updateMean(id, value);
	}
	
	private void initValue(String id, Double value){
		m_order.add(id);
		m_maxValue.put(id, value);
		m_minValue.put(id, value);
		m_meanValue.put(id, value);
		m_valueAmount.put(id, 0);
	}
	
	private void updateMax(String id, Double value){
		m_maxValue.put(id, Math.max(m_maxValue.get(id), value));
	}
	
	private void updateMin(String id, Double value){
		m_minValue.put(id, Math.min(m_minValue.get(id), value));
	}

	private void updateMean(String id, Double value){
		double oldM = m_meanValue.get(id);
		double newM = ((oldM * (m_valueAmount.get(id) - 1)) + value) / m_valueAmount.get(id);
		m_meanValue.put(id, newM);
	}
	
	public void store(File f){
		try {
			
			FileWriter fw = new FileWriter(f);
			
			fw.write(getCSVString());
			
			fw.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void reorder(Comparator<String> comp){
		Collections.sort(m_order, comp);
	}
	
	public String getCSVString(){
		StringBuilder sb = new StringBuilder();
		sb.append("value;min;max;mean;amount\n");
		
		for(String id : m_order){
			sb.append(id);
			sb.append(";");
			sb.append(m_minValue.get(id));
			sb.append(";");
			sb.append(m_maxValue.get(id));
			sb.append(";");
			sb.append(m_meanValue.get(id));
			sb.append(";");
			sb.append(m_valueAmount.get(id));
			sb.append("\n");
		}
		
		return sb.toString();
	}
}
