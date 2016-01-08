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
	
	private Set<SimilarityValue> m_tasks;
	
	public int maxSimTasks;

	public static int countNextTaskCalls;
	
	private Map<PointedInterpretation, Map<PointedInterpretation, SimilarityValue>> m_pool;
	
//	private Map<PointedInterpretation, Map<PointedInterpretation, SimilarityValue>> m_upToDate;
	

	/**
	 * When the factory resets, only values of interest will be kept, e.g. (d_Q, d_a)
	 */
	private Map<PointedInterpretation, Map<PointedInterpretation, SimilarityValue>> m_valuesOfInterest;
	
//	private Set<PointedInterpretation> workingKeys;
//	private Map<PointedInterpretation, Set<PointedInterpretation>> workingKeyPair;
	
	
	private static SimilarityValueFactory _instance;
	
	private SimilarityValueFactory() {
		m_pool = new HashMap<PointedInterpretation, Map<PointedInterpretation,SimilarityValue>>();
//		m_upToDate = new HashMap<PointedInterpretation, Map<PointedInterpretation,SimilarityValue>>();
		m_tasks = new HashSet<SimilarityValue>();
		m_valuesOfInterest = new HashMap<PointedInterpretation, Map<PointedInterpretation,SimilarityValue>>();
//		workingKeys = new HashSet<PointedInterpretation>();
//		workingKeyPair = new HashMap<PointedInterpretation, Set<PointedInterpretation>>();
	}
	
	public static SimilarityValueFactory getFactory(){
		if(_instance == null) _instance = new SimilarityValueFactory();
		return _instance;
	}
	
	public SimilarityValue initializeSimilarityValue(PointedInterpretation p, PointedInterpretation q){
		SimilarityValue v = new SimilarityValue(p, q);
//		LOG.fine("Adding similarity value " + v + " to the pool.");
		addTo(v, m_pool);
		m_tasks.add(v); // immediately let it calculate the task at some point
		return v;
	}
	
//	public int calls = 0;
//	public long time1 = 0;
//	public long time2 = 0;
//	public long time3 = 0;
	public SimilarityValue getSimilarityValue(PointedInterpretation p, PointedInterpretation q, int i){
		String str = "Asking for "+p+" with "+q+" in iter. "+i;
//		calls++;
//		long start = System.currentTimeMillis();
//		Set<Map<PointedInterpretation, Map<PointedInterpretation, SimilarityValue>>> searchSpaces 
//				= new HashSet<Map<PointedInterpretation,Map<PointedInterpretation,SimilarityValue>>>();
//		
//		searchSpaces.add(m_pool);
//		/*if(i >= 1)*/ searchSpaces.add(m_upToDate); // no values from iteration 0 can be in upToDate
//		time1 += System.currentTimeMillis() - start;
//		start = System.currentTimeMillis();
//		for(Map<PointedInterpretation, Map<PointedInterpretation, SimilarityValue>> searchSpace : searchSpaces){
			// search task pool first
			// one way
			if(m_pool.containsKey(p)){
				if(m_pool.get(p).containsKey(q)){
//					time2 += System.currentTimeMillis() - start;
//					LOG.info(str + " FOUND");
					return m_pool.get(p).get(q);
				}
			}
			// other way
			if(m_pool.containsKey(q)){
				if(m_pool.get(q).containsKey(p)){
//					time2 += System.currentTimeMillis() - start;
//					LOG.info(str + " FOUND");
					return m_pool.get(q).get(p);
				}
			}
			// other map -> uptoDate
			// one way
//			if(m_upToDate.containsKey(p)){
//				if(m_upToDate.get(p).containsKey(q)){
////					time2 += System.currentTimeMillis() - start;
//					return m_upToDate.get(p).get(q);
//				}
//			}
//			// other way
//			if(m_upToDate.containsKey(q)){
//				if(m_upToDate.get(q).containsKey(p)){
////					time2 += System.currentTimeMillis() - start;
//					return m_upToDate.get(q).get(p);
//				}
//			}
//		}
//		time2 += System.currentTimeMillis() - start;
//		start = System.currentTimeMillis();
//		if(calls >= 1000){
//			calls = 0;
//			System.out.println(time + " ms per 1000 contains checks");
//			time = 0;
//		}
		// nothing found in searchSpaces
		if(i == 0){ // create new value and put it in pool
			SimilarityValue ret = initializeSimilarityValue(p, q);
			maxSimTasks++;
//			LOG.info(str + " CREATED");
//			time3 += System.currentTimeMillis() - start;
			return ret;
		}
//		time3 += System.currentTimeMillis() - start;
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
	
//	private boolean removeFrom(SimilarityValue v, Map<PointedInterpretation, Map<PointedInterpretation, SimilarityValue>> coll){
//		boolean successfullyRemoved = false;
//		if(coll.containsKey(v.getP1())){
//			if(coll.get(v.getP1()).containsKey(v.getP2())){
//				coll.get(v.getP1()).remove(v.getP2());
//				successfullyRemoved = true;
//			}
//			if(coll.get(v.getP1()).isEmpty())
//				coll.remove(v.getP1());
//		}
//		if(!successfullyRemoved)
//			LOG.warning("Did not remove " + v + ", because it was not found.");
//		return successfullyRemoved;
//	}
	
//	public boolean pushUpdate(SimilarityValue v){
//		if(removeFrom(v, m_pool)){
//			if(addTo(v, m_upToDate)){
//				return true;
//			}else{
//				LOG.severe("Update push failed: adding " + v + " to upToDate pool failed.");
//			}
//		}else{
//			LOG.warning("Can't push update of " + v + ": not in the task pool.");
//		}
//		return false;
//	}
	
	public void nextIteration(){
//		if(m_pool.isEmpty()){
//			m_pool = m_upToDate;
//			m_upToDate = new HashMap<PointedInterpretation, Map<PointedInterpretation,SimilarityValue>>();
//			workingKeys = new HashSet<PointedInterpretation>();
//			workingKeyPair = new HashMap<PointedInterpretation, Set<PointedInterpretation>>();
//		}else{
//			LOG.severe("Can only increase current iteration if task pool is empty.");
//		}
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
		
//		sb.append("\nUpToDate Pool:\n");
//		for(PointedInterpretation p1 : m_upToDate.keySet()){
//			for(PointedInterpretation p2 : m_upToDate.get(p1).keySet()){
//				sb.append(m_upToDate.get(p1).get(p2) + "\n");
//			}
//		}
		
		return sb.toString();
	}
	
	public boolean isTaskSetEmpty(){
		return m_tasks.isEmpty();
	}
	
	public SimilarityValue getNextTask(){
//		if(!isPoolEmpty()){
//			Iterator<PointedInterpretation> it1 = m_pool.keySet().iterator();
//			while(it1.hasNext()){
//				PointedInterpretation picked = it1.next();
//				Iterator<PointedInterpretation> it2 = m_pool.get(picked).keySet().iterator();
//				while(it2.hasNext()){
//					SimilarityValue ret = m_pool.get(picked).get(it2.next());
//					if(workingKeyPair.containsKey(picked) && workingKeyPair.get(picked).contains(ret.getP2())){
//						// already investigating this
//						continue;
//					}
//					workingOn(ret.getP1(), ret.getP2());
////					removeFrom(ret, m_pool);
//					return ret;
//				}
//			}
//		}
//		System.err.println("No next task to compute.");
//		return null; // not supposed to happen..
		countNextTaskCalls++;
//		if(countNextTaskCalls % 1000 == 0){
//			System.out.print(m_tasks.size()+"/"+maxSimTasks+" tasks left");
//		}
		if(!m_tasks.isEmpty()){
			Iterator<SimilarityValue> it = m_tasks.iterator();
			if(it.hasNext()){
				SimilarityValue ret = it.next();
				it.remove(); // immediately remove it as task
//				System.out.println(ret);
//				System.out.println(" and after returning: "+m_tasks.size()+"/"+maxSimTasks+" tasks left");
				return ret;
			}
		}
		System.out.println("I returned null. (getNextTask)");
		return null;
	}
	
//	private void workingOn(PointedInterpretation key1, PointedInterpretation key2){
//		if(!workingKeyPair.containsKey(key1)){
//			workingKeyPair.put(key1, new HashSet<PointedInterpretation>());
//		}
//		workingKeyPair.get(key1).add(key2);
//	}
	
	
//	public Map<PointedInterpretation, SimilarityValue> getNextTasks(){
//		if(!isPoolEmpty()){
//			Iterator<PointedInterpretation> it = m_pool.keySet().iterator();
//			while(it.hasNext()){
//				PointedInterpretation picked = it.next();
//				if(workingKeys.contains(picked)){
//					continue;
//				}else{
//					workingKeys.add(picked);
//					return m_pool.get(picked);
//				}
//			}
//		}
//		return new HashMap<PointedInterpretation, SimilarityValue>();
//	}
	
	public void registerInteresting(SimilarityValue v){
		addTo(v, m_valuesOfInterest);
	}
	
	public void resetFactory(){
		m_valuesOfInterest = new HashMap<PointedInterpretation, Map<PointedInterpretation,SimilarityValue>>();
		m_pool = new HashMap<PointedInterpretation, Map<PointedInterpretation,SimilarityValue>>();
		m_tasks = new HashSet<SimilarityValue>();
		maxSimTasks = 0;
		countNextTaskCalls = 0;
//		m_upToDate = new HashMap<PointedInterpretation, Map<PointedInterpretation,SimilarityValue>>();
	}
	
	public int getOpenTaskAmount(){
		int size = 0;
		for(PointedInterpretation p : m_pool.keySet()){
			size += m_pool.get(p).size();
		}
		return size;
	}
}

