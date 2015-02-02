package similarity.algorithms.generalEL;

import interpretation.ds.PointedInterpretation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import main.StaticValues;

import org.omg.CORBA._PolicyStub;

public class SimilarityValueFactory {
	
	private static final Logger LOG = Logger.getLogger(StaticValues.LOGGER_NAME);

	private Map<PointedInterpretation, Map<PointedInterpretation, SimilarityValue>> m_pool;
	
	private Map<PointedInterpretation, Map<PointedInterpretation, SimilarityValue>> m_upToDate;
	

	/**
	 * When the factory resets, only values of interest will be kept, e.g. (d_Q, d_a)
	 */
	private Map<PointedInterpretation, Map<PointedInterpretation, SimilarityValue>> m_valuesOfInterest;
	
	
	private static SimilarityValueFactory _instance;
	
	private SimilarityValueFactory() {
		m_pool = new HashMap<PointedInterpretation, Map<PointedInterpretation,SimilarityValue>>();
		m_upToDate = new HashMap<PointedInterpretation, Map<PointedInterpretation,SimilarityValue>>();
		m_valuesOfInterest = new HashMap<PointedInterpretation, Map<PointedInterpretation,SimilarityValue>>();
	}
	
	public static SimilarityValueFactory getFactory(){
		if(_instance == null) _instance = new SimilarityValueFactory();
		return _instance;
	}
	
	public SimilarityValue getSimilarityValue(PointedInterpretation p, PointedInterpretation q, int i){
		Set<Map<PointedInterpretation, Map<PointedInterpretation, SimilarityValue>>> searchSpaces =
				new HashSet<Map<PointedInterpretation,Map<PointedInterpretation,SimilarityValue>>>();
		searchSpaces.add(m_pool);
		/*if(i >= 1)*/ searchSpaces.add(m_upToDate); // no values from iteration 0 can be in upToDate
		
		for(Map<PointedInterpretation, Map<PointedInterpretation, SimilarityValue>> searchSpace : searchSpaces){
			// one way
			if(searchSpace.containsKey(p)){
				if(searchSpace.get(p).containsKey(q))
					return searchSpace.get(p).get(q);
			}
			// other way
			if(searchSpace.containsKey(q)){
				if(searchSpace.get(q).containsKey(p))
					return searchSpace.get(q).get(p);
			}
		}
		// nothing found in searchSpaces
		if(i == 0){ // create new value and put it in pool
			SimilarityValue v = new SimilarityValue(p, q);
			LOG.fine("Adding similarity value " + v + " to the pool.");
			addTo(v, m_pool);
			return v;
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
	
	private boolean removeFrom(SimilarityValue v, Map<PointedInterpretation, Map<PointedInterpretation, SimilarityValue>> coll){
		boolean successfullyRemoved = false;
		if(coll.containsKey(v.getP1())){
			if(coll.get(v.getP1()).containsKey(v.getP2())){
				coll.get(v.getP1()).remove(v.getP2());
				successfullyRemoved = true;
			}
			if(coll.get(v.getP1()).isEmpty())
				coll.remove(v.getP1());
		}
		if(!successfullyRemoved)
			LOG.warning("Did not remove " + v + ", because it was not found.");
		return successfullyRemoved;
	}
	
	public boolean pushUpdate(SimilarityValue v){
		if(removeFrom(v, m_pool)){
			if(addTo(v, m_upToDate)){
				return true;
			}else{
				LOG.severe("Update push failed: adding " + v + " to upToDate pool failed.");
			}
		}else{
			LOG.warning("Can't push update of " + v + ": not in the task pool.");
		}
		return false;
	}
	
	public void nextIteration(){
		if(m_pool.isEmpty()){
			m_pool = m_upToDate;
			m_upToDate = new HashMap<PointedInterpretation, Map<PointedInterpretation,SimilarityValue>>();
		}else{
			LOG.severe("Can only increase current iteration if task pool is empty.");
		}
	}
	
	public Map<PointedInterpretation, Map<PointedInterpretation, SimilarityValue>> getValuesOfInterest() {
		return m_valuesOfInterest;
	}
	
	public String getStatus(){
		StringBuilder sb = new StringBuilder();
		sb.append("Current Task Pool:\n");
		for(PointedInterpretation p1 : m_pool.keySet()){
			for(PointedInterpretation p2 : m_pool.get(p1).keySet()){
				sb.append(m_pool.get(p1).get(p2) + "\n");
			}
		}
		
		sb.append("\nUpToDate Pool:\n");
		for(PointedInterpretation p1 : m_upToDate.keySet()){
			for(PointedInterpretation p2 : m_upToDate.get(p1).keySet()){
				sb.append(m_upToDate.get(p1).get(p2) + "\n");
			}
		}
		
		return sb.toString();
	}
	
	public boolean isPoolEmpty(){
		return m_pool.isEmpty();
	}
	
	public SimilarityValue getNextTask(){
		if(!isPoolEmpty()){
			Iterator<PointedInterpretation> it1 = m_pool.keySet().iterator();
			while(it1.hasNext()){
				PointedInterpretation picked = it1.next();
				Iterator<PointedInterpretation> it2 = m_pool.get(picked).keySet().iterator();
				if(it2.hasNext()){
					return m_pool.get(picked).get(it2.next());
				}
			}
		}
		System.err.println("No next task to compute.");
		return null; // not supposed to happen..
	}
	
	public void registerInteresting(SimilarityValue v){
		addTo(v, m_valuesOfInterest);
	}
}

