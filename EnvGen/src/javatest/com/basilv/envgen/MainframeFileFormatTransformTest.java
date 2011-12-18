// Copyright 2007 by Basil Vandegriend.  All rights reserved.
package com.basilv.envgen;

import java.io.IOException;

import junit.framework.TestCase;

public class MainframeFileFormatTransformTest extends TestCase {

	
	public MainframeFileFormatTransformTest(String arg0) {
		super(arg0);
	}

	protected void setUp() throws Exception {

	}


	public void testTransformLine() {

		// Identity
		verifyTransformLine("", "");
		verifyTransformLine("my test", "my test");
		
		// Tab Conversion
		verifyTransformLine("\tHello", "    Hello");
		verifyTransformLine("\tHello\t\tTest", "    Hello        Test");
		
		// Trim whitespace at end 
		verifyTransformLine("  ", "");
		verifyTransformLine("   \n", "");
		verifyTransformLine("test this   \t", "test this");
		
		// Line too long
		StringBuffer tooLongBuffer = new StringBuffer(MainframeFileFormatTransform.MAX_LINE_LENGTH);
		for (int i = 0; i < MainframeFileFormatTransform.MAX_LINE_LENGTH + 1; i++) {
			tooLongBuffer.append("a");
		}
		try {
			verifyTransformLine(tooLongBuffer.toString(), tooLongBuffer.toString());
			fail("Expected exception because line was too long.");
		} catch (RuntimeException e) {
			// Expected case
		}
		
	}
	
	private void verifyTransformLine(String inputLine, String expectedOutput) {
		String actualOutput = MainframeFileFormatTransform.transformLine(inputLine, 1);
		assertEquals(expectedOutput, actualOutput);
	}
	
	
	public void testTransform() throws IOException {

		StringBuffer input = new StringBuffer();
		StringBuffer expectedOutput = new StringBuffer();
		
		input.append("Hello.\n");
		expectedOutput.append("Hello.\n");
		
		input.append("\tSome tabs\t here.\n");
		expectedOutput.append("    Some tabs     here.\n");
		
		input.append("Trailing whitespace\t  \n");
		expectedOutput.append("Trailing whitespace\n");
		
		StringBuffer actualOutput = MainframeFileFormatTransform.transform(input);
		
		assertEquals(expectedOutput.toString(), actualOutput.toString());
	}
	
}
