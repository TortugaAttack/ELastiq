package similarity.algorithms.specifications;

public enum OutputType {
ASCII(true),
CSV(true),
INSTANCES(true),
STATISTICS(false),
TIMES(false);

	private boolean m_requiresAlgorithmResult;

	private OutputType(boolean requiresAlgoResult){
		m_requiresAlgorithmResult = requiresAlgoResult;
	}
	
	public boolean requiresAlgorithmResult(){
		return m_requiresAlgorithmResult;
	}
}
