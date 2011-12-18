// Copyright 2007 by Basil Vandegriend.  All rights reserved.
package com.basilv.envgen;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

import freemarker.template.TemplateModelException;
import freemarker.template.TemplateTransformModel;

/**
 * Template to skip generation of the target file.
 * If the target file exists previously it is deleted.
 */
public class SkipGenerationTransform implements TemplateTransformModel {


	private static final ThreadLocal skipGenerationFlag = new ThreadLocal();

	public static boolean isSkipGenerationAndResetFlag() {
		boolean isSkipGen = Boolean.TRUE.equals(skipGenerationFlag.get());
		skipGenerationFlag.set(null);
		return isSkipGen;
	}
	
	
	/**
	 * Must have a default no-arg constructor.
	 */
    public SkipGenerationTransform() {
    }


    public Writer getWriter(Writer out, Map args) throws TemplateModelException {
    	SkipGenerationTransform.skipGenerationFlag.set(Boolean.TRUE);
    	// Return a do-nothing writer.
		return new Writer() {
			public void write(char cbuf[], int off, int len) {
			}

			public void flush() throws IOException {
			}

			public void close() throws IOException {
			}
		};

    }
    
    
}
