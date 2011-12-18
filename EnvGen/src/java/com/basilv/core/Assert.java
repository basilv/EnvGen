package com.basilv.core;


/**
 * Provides assertion support. A failed assertion will throw an AssertionError.
 * <P>
 * The methods are generally of the form: methodXXX(message, expression). If the
 * expression fails the assertion, then the AssertionException is created using
 * the specified message and then thrown.
 */
public class Assert
{
	/**
	 * Fails the assertion if the specified expression is false.
	 */
	public static void isTrue(String expressionDescription, boolean expression) {
		if (!expression) {
			throwException(expressionDescription + " is not true.");
		}
	}

	/**
	 * Fails the assertion if the specified expression is false.
	 */
	public static void isTrue(boolean expression) {
		isTrue("Expression asserted as true", expression);
	}

	/**
	 * Fails the assertion if the specified expression is true.
	 */
	public static void isFalse(String expressionDescription, boolean expression) {
		if (expression) {
			throwException(expressionDescription + " is not false.");
		}
	}

	/**
	 * Fails the assertion if the specified expression is true.
	 */
	public static void isFalse(boolean expression) {
		isFalse("Expression asserted as false", expression);
	}

	/**
	 * Fails the assertion if the specified object is null.
	 */
	public static void notNull(Object evalObj) {
		notNull("Object asserted as not null", evalObj);
	}

	/**
	 * Fails the assertion if the specified object is null.
	 */
	public static void notNull(String objDescription, Object evalObj) {
		if (evalObj == null) {
			throwException(objDescription + " is null.");
		}
	}

	/**
	 * Fails the assertion if the specified string is null or empty.
	 */
	public static void notNullOrEmpty(String evalString) {
		notNullOrEmpty("String asserted as not null or empty", evalString);
	}

	/**
	 * Fails the assertion if the specified string is null or empty.
	 * 
	 * @param stringDescription A description of the string being evaluated.
	 * @param evalString The string to evaluate.
	 */
	public static void notNullOrEmpty(String stringDescription, String evalString) {
		if (StringUtilities.isNullOrEmpty(evalString)) {
			throwException(stringDescription + " is null or empty.");
		}
	}

	/**
	 * Fails the assertion if the integer is less than zero.
	 * 
	 * @param evalInt The integer to evaluate.
	 */
	public static void notNegative(int evalInt) {
		notNegative("Integer value " + evalInt + " asserted as not negative", evalInt);
	}

	/**
	 * Fails the assertion if the integer is less than zero.
	 * 
	 * @param intDescription A description of the integer being evaluated.
	 * @param evalInt The integer to evaluate.
	 */
	public static void notNegative(String intDescription, int evalInt) {
		if (evalInt < 0) {
			throwException(intDescription + " is negative.");
		}
	}

	/**
	 * Fails the assertion if the integer is not greater than zero.
	 * 
	 * @param evalInt The integer to evaluate.
	 */
	public static void greaterThanZero(int evalInt) {
		greaterThanZero("Integer value " + evalInt + " asserted as greater than zero", evalInt);
	}

	/**
	 * Fails the assertion if the integer is not greater than zero.
	 * 
	 * @param intDescription A description of the integer being evaluated.
	 * @param evalInt The integer to evaluate.
	 */
	public static void greaterThanZero(String intDescription, int evalInt) {
		if (evalInt <= 0) {
			throwException(intDescription + " is not greater than zero.");
		}
	}

	/**
	 * Automatically throws an AssertionException.
	 */
	public static void shouldNotReach() {
		shouldNotReach("Failed assertion in shouldNotReach().");
	}

	/**
	 * Automatically throws an AssertionException using the specified message.
	 */
	public static void shouldNotReach(String message) {
		throwException(message);
	}

	private static void throwException(String message) {
		throw new AssertionError(message);
	}

} // class Assert
