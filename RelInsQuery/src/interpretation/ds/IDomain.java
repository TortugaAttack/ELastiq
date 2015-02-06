package interpretation.ds;
/**
 * Main interface for Domain representing objects.
 * 
 * @author Maximilian Pensel - maximilian.pensel@gmx.de
 *
 */
public interface IDomain {

	/**
	 * Gives the possibility to add generic {@link DomainNode} elements to the domain.
	 * @param id : The generic domain node identifier.
	 * @return The generated DomainNode element.
	 */
	public <T> DomainNode<T> addDomainElement(T id);
}
