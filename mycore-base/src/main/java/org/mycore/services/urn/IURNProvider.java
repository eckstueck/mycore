/**
 * 
 */
package org.mycore.services.urn;


/**
 * @author shermann
 *
 */
public interface IURNProvider {
    /** Generates a single URN */
    public URN generateURN();

    /**
     * Generates multiple urns
     * 
     * @param int the amount of urn to generate, must be &gt;= 1
     */
    public URN[] generateURN(int amount);

    /**
     * Generates multiple urns. The generated urns have the following structure
     * <code>&lt;base-urn&gt;-1</code> up to
     * <code>&lt;base-urn&gt;-amount</code>
     * 
     * @param int the amount of urn to generate, must be &gt;= 1
     * @param URN
     *            the base urn
     */
    public URN[] generateURN(int amount, URN base);

    /**
     * Generates multiple urns. The generated urns have the following structure
     * <code>&lt;base-urn&gt;-setId-1</code> up to
     * <code>&lt;base-urn&gt;-setId-amount</code>
     * 
     * @param amount
     *            the amount of urn to generate, must be &gt;= 1
     * @param base
     *            the base urn
     * @param setId
     *            must represent an integer &gt;= 0, e.g. 1, 001 or 00004
     * @return an Array of {@link URN} or <code>null</code> if the base urn is
     *         null or amount &lt;1 or the setID &lt;0
     */
    public URN[] generateURN(int amount, URN base, String setId);
}
