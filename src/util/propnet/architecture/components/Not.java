package util.propnet.architecture.components;

import util.propnet.architecture.Component;

/**
 * The Not class is designed to represent logical NOT gates.
 */
@SuppressWarnings("serial")
public final class Not extends Component
{
	/**
	 * Returns the inverse of the input to the not.
	 * 
	 * @see util.propnet.architecture.Component#getValue()
	 */
	@Override
	public boolean getValue()
	{
		return !getSingleInput().getValue();
	}

	/**
	 * @see util.propnet.architecture.Component#toString()
	 */
	@Override
	public String toString()
	{
		return toDot("invtriangle", "grey", "NOT");
	}

	@Override
	public Component copy_noCon() {
		// TODO Auto-generated method stub
		return new Not();
	}
}