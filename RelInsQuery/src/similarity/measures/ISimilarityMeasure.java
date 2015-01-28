package similarity.measures;

public interface ISimilarityMeasure<T> {

	/**
	 * Computes the similarity between any two objects.
	 * @param obj1
	 * @param obj2
	 * @return
	 */
	public double similarity(T obj1, T obj2);
}
