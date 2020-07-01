package jolie.lang.parse.ast.types.refinements;

import dk.brics.automaton.Automaton;
import dk.brics.automaton.RegExp;

public class BasicTypeRefinementStringRegex extends BasicTypeRefinement< String > {

	private final String regex;
	private final RegExp regexObject;
	private final Automaton automaton;

	public BasicTypeRefinementStringRegex( String regex ) {
		this.regex = regex;
		this.regexObject = new RegExp( this.regex );
		this.automaton = regexObject.toAutomaton();
	}

	@Override
	public boolean checkTypeRefinment( String value ) {
		return this.automaton.run( value );
	}

	@Override
	public boolean checkEqualness( BasicTypeRefinement basicTypeRefinement ) {
		if( basicTypeRefinement instanceof BasicTypeRefinementStringRegex ) {
			BasicTypeRefinementStringRegex basicTypeRefinementStringRegex =
				(BasicTypeRefinementStringRegex) basicTypeRefinement;
			if( !this.regex.equals( basicTypeRefinementStringRegex.getRegex() ) ) {
				return false;
			}
		} else {
			return false;
		}
		return true;
	}

	@Override
	public String getRefinementDocumentationDefinition() {
		return "regex(\"" + regex + "\")";
	}

	public String getRegex() {
		return this.regex;
	}

}
