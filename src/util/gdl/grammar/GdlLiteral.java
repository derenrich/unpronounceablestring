package util.gdl.grammar;

import java.util.Map;

@SuppressWarnings("serial")
public abstract class GdlLiteral extends Gdl
{

	@Override
	public abstract boolean isGround();

	@Override
	public abstract String toString();

	
	
	@Override
	public abstract GdlLiteral replace(Map<Gdl, Gdl> replacementMap);
	
}
