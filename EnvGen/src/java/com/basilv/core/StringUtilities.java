/* Copyright 2000 by Basil Vandegriend.  All rights reserved. */

package com.basilv.core;

/**
 * Provides utility methods for Strings.
 */
public class StringUtilities
{

	public static boolean isNullOrEmpty(String stringToTest) {
		return (stringToTest == null || stringToTest.length() == 0);
	}

}
