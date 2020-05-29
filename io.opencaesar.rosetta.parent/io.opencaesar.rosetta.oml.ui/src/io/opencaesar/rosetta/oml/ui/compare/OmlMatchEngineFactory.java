package io.opencaesar.rosetta.oml.ui.compare;

import org.eclipse.emf.compare.match.IMatchEngine;
import org.eclipse.emf.compare.match.impl.MatchEngineFactoryImpl;
import org.eclipse.emf.compare.scope.IComparisonScope;

public class OmlMatchEngineFactory extends MatchEngineFactoryImpl {

	@Override
	public boolean isMatchEngineFactoryFor(IComparisonScope scope) {
		return true;
	}
	
	@Override
	public IMatchEngine getMatchEngine() {
		return new OmlMatchEngine();
	}
}
