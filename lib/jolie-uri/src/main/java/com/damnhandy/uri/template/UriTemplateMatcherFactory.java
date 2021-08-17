package com.damnhandy.uri.template;



import java.util.regex.Pattern;

public class UriTemplateMatcherFactory {
	public static Pattern getReverseMatchPattern( UriTemplate t ) {
		return t.getReverseMatchPattern();
	}
}
