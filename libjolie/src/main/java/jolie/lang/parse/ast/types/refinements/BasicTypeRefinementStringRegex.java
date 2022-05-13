package jolie.lang.parse.ast.types.refinements;

import dk.brics.automaton.Automaton;
import dk.brics.automaton.RegExp;

import java.io.Serializable;

public class BasicTypeRefinementStringRegex implements Serializable, BasicTypeRefinement< String > {
	private final String regex;
	private final Automaton automaton;

	public BasicTypeRefinementStringRegex( String regex ) {
		this.regex = regex;
		this.automaton = new RegExp( regex ).toAutomaton();
	}

	@Override
	public boolean checkValue( String value ) {
		return automaton.run( value );
	}

	@Override
	public boolean checkEqualness( BasicTypeRefinement< ? > basicTypeRefinement ) {
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
	public String getDocumentation() {
		return "regex(\"" + regex + "\")";
	}

	public String getRegex() {
		return this.regex;
	}

}
