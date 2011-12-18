// Copyright 2007 by Basil Vandegriend.  All rights reserved.
package com.basilv.envgen;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.util.Map;

import freemarker.template.TemplateModelException;
import freemarker.template.TemplateTransformModel;

/**
 * Template to format input into a form acceptable for mainframes.
 * The formatting changes that are made are:
 * <ul>
 * <li>Convert tabs to spaces (4 spaces per tab)</li>
 * <li>Trim whitespace from end of line</li>
 * <li>Fail with error message if a line is longer than 75 characters</li>
 * </ul>
 */
public class MainframeFileFormatTransform implements TemplateTransformModel {
// TODO: Change to TemplateDirectiveModel and update documentation to reference directives rather than transforms?
/* From FreeMarker release notes.
A new model interface, TemplateDirectiveModel provides an easier paradigm for implementing user-defined directives than TemplateTransformModel did previously. TemplateTransformModel will be deprecated.

 

The "transform" term is now removed from the documentation. Instead the more general "user-defined directive" term is used, which encompasses macros, TemplateTransformModel-s and the new TemplateDirectiveModel-s, which are just different ways of implementing user-defined directives.

 
 */
	static final int MAX_LINE_LENGTH = 75;

	/**
	 * Must have a default no-arg constructor.
	 */
    public MainframeFileFormatTransform() {
    }


    public Writer getWriter(final Writer out, Map args) throws TemplateModelException {

    	// If you want to handle arguments to the transform, this is where it is done.
    	// Any variables must be stored in the writer instance below and not in the main transform class.
    	
    	final StringBuffer buffer = new StringBuffer();
		return new Writer() {
			public void write(char cbuf[], int off, int len) {
		 		buffer.append(cbuf, off, len);
		 	}

			public void flush() throws IOException {
		 		out.flush();
		 	}
		 	
			public void close() throws IOException {
		 		StringBuffer transformedBuffer = transform(buffer);
		 		out.write(transformedBuffer.toString());
		 		out.flush();
			}

		};    	
    }
    
    // Non-private for testing
	static StringBuffer transform(StringBuffer inputBuffer) throws IOException {
		BufferedReader reader = new BufferedReader(new StringReader(inputBuffer.toString()));
		StringBuffer outputBuffer = new StringBuffer(inputBuffer.length());
		try {
			int lineNumber = 0;
			String line = reader.readLine();
			while (line != null) {
				lineNumber++;

				String transformedLine = transformLine(line, lineNumber);
				outputBuffer.append(transformedLine);
				outputBuffer.append("\n"); // since removed by readLine().

				line = reader.readLine();				
			}
			
		} finally {
			reader.close();
		}
		
		return outputBuffer;
	}


	// Non-private for testing.
    static String transformLine(String line, int lineNumber) {

		if (line.length() == 0) {
			return line;
		}

		// Convert tabs to spaces
		String newLine = line.replaceAll("\t", "    ");

    	// Trim ending whitespace
    	int lastNonWhitespaceIndex = newLine.length() - 1;
    	while (Character.isWhitespace(newLine.charAt(lastNonWhitespaceIndex))) {
    		lastNonWhitespaceIndex --;
    		if (lastNonWhitespaceIndex < 0) {
    			// Line is nothing but whitespace
    			return "";
    		}
    	}
    	newLine = newLine.substring(0, lastNonWhitespaceIndex+1);
    	
    	if (newLine.length() > MAX_LINE_LENGTH) {
    		throw new RuntimeException("Line #" + lineNumber + 
				" is longer than the maximum of " + MAX_LINE_LENGTH + 
				" characters. Line content = [" + line + "].");
    	}

        return newLine;
    }
    
}
