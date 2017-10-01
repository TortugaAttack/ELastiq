package org.tu_dresden.elastiq.similarity.algorithms.generalEL;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.tu_dresden.elastiq.interpretation.ds.PointedInterpretation;
import org.tu_dresden.elastiq.main.StaticValues;

public class SimilarityValueFactory {
	
	private static final Logger LOG = Logger.getLogger(StaticValues.LOGGER_NAME);
	
	private Set<SimilarityValue> m_tasks;
	
	public int maxSimTasks;

	public static int countNextTaskCalls;
	
	private Map<PointedInterpretation, Map<PointedInterpretation, SimilarityValue>> m_pool;
	
	/**
	 * When the factory resets, only values of interest will be kept, e.g. (d_Q, d_a)
	 */
	private Map<PointedInterpretation, Map<PointedInterpretation, SimilarityValue>> m_valuesOfInterest;
	
	private static SimilarityValueFactory _instance;
	
	private SimilarityValueFactory() {
		m_pool = new HashMap<PointedInterpretation, Map<PointedInterpretation,SimilarityValue>>();
		m_tasks = new HashSet<SimilarityValue>();
		m_valuesOfInterest = new HashMap<PointedInterpretation, Map<PointedInterpretation,SimilarityValue>>();
	}
	
	public static SimilarityValueFactory getFactory(){
		if(_instance == null) _instance = new SimilarityValueFactory();
		return _instance;
	}
	
	public SimilarityValue initializeSimilarityValue(PointedInterpretation p, PointedInterpretation q){
		SimilarityValue v = new SimilarityValue(p, q);
		addTo(v, m_pool);
		m_tasks.add(v); // immediately let it calculate the task at some point
		return v;
	}
	
	public SimilarityValue getSimilarityValue(PointedInterpretation p, PointedInterpretation q, int i){
		// search task pool
		// one way
		if(m_pool.containsKey(p)){
			if(m_pool.get(p).containsKey(q)){
				return m_pool.get(p).get(q);
			}
		}
		// other way
		if(m_pool.containsKey(q)){
			if(m_pool.get(q).containsKey(p)){
				return m_pool.get(q).get(p);
			}
		}

		// nothing found in searchSpaces
		if(i == 0){ // create new value and put it in pool
			SimilarityValue ret = initializeSimilarityValue(p, q);
			maxSimTasks++;
			return ret;
		}
		return null;
	}
	
	private boolean addTo(SimilarityValue v, Map<PointedInterpretation, Map<PointedInterpretation, SimilarityValue>> coll){
		if(!coll.containsKey(v.getP1())){
			coll.put(v.getP1(), new HashMap<PointedInterpretation, SimilarityValue>());
		}
		if(!coll.get(v.getP1()).containsKey(v.getP2())){
			coll.get(v.getP1()).put(v.getP2(), v);
			return true; // successfully added
		}else{
			LOG.warning("Could not add " + v + " because for both " + v.getP1() + " and " + v.getP2() + " there " +
					"already exists the value " + coll.get(v.getP1()).get(v.getP2()) + ".");
		}
		return false;
	}
	
	public void nextIteration(){
		if(m_tasks.isEmpty()){
			for(PointedInterpretation p1 : m_pool.keySet()){
				for(PointedInterpretation p2 : m_pool.get(p1).keySet()){
					m_tasks.add(m_pool.get(p1).get(p2));
				}
			}
			countNextTaskCalls = 0;
		}
	}
	
	public Map<PointedInterpretation, Map<PointedInterpretation, SimilarityValue>> getValuesOfInterest() {
		return m_valuesOfInterest;
	}
	
	public String getStatus(){
		StringBuilder sb = new StringBuilder();
		sb.append("Current SimValue Pool:\n");
		for(PointedInterpretation p1 : m_pool.keySet()){
			for(PointedInterpretation p2 : m_pool.get(p1).keySet()){
				sb.append(m_pool.get(p1).get(p2) + "\n");
			}
		}
		
		return sb.toString();
	}
	
	public boolean isTaskSetEmpty(){
		return m_tasks.isEmpty();
	}
	
	public SimilarityValue getNextTask(){
		countNextTaskCalls++;

		if(!m_tasks.isEmpty()){
			Iterator<SimilarityValue> it = m_tasks.iterator();
			if(it.hasNext()){
				SimilarityValue ret = it.next();
				it.remove(); // immediately remove it as task
				return ret;
			}
		}
		
		LOG.warning("I returned null. (getNextTask)");
		return null;
	}
	
	public void registerInteresting(SimilarityValue v){
		addTo(v, m_valuesOfInterest);
	}
	
	public void resetFactory(){
		m_valuesOfInterest = new HashMap<PointedInterpretation, Map<PointedInterpretation,SimilarityValue>>();
		m_pool = new HashMap<PointedInterpretation, Map<PointedInterpretation,SimilarityValue>>();
		m_tasks = new HashSet<SimilarityValue>();
		maxSimTasks = 0;
		countNextTaskCalls = 0;
	}
	
	public int getOpenTaskAmount(){
		int size = 0;
		for(PointedInterpretation p : m_pool.keySet()){
			size += m_pool.get(p).size();
		}
		return size;
	}
}

