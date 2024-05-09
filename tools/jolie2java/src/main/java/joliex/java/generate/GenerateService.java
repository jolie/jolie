package joliex.java.generate;

public enum GenerateService {
	IF_ABSENT,
	ALWAYS,
	NEVER;

	public static GenerateService create( Integer i ) {
		return switch ( i ) {
			case 1 -> ALWAYS;
			case 2 -> NEVER;
			default -> IF_ABSENT;
		};
	}
}
