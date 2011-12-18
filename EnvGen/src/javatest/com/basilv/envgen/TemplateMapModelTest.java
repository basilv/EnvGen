// Copyright 2007 by Basil Vandegriend.  All rights reserved.
package com.basilv.envgen;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

public class TemplateMapModelTest extends TestCase {

	public TemplateMapModelTest(String arg0) {
		super(arg0);
	}

	public void testCreation() {
		
		Map properties = new HashMap();
		properties.put("env", "devl");
		properties.put("dir", "/mydir");
		properties.put("test", "true");
		properties.put("test.dir", "/testing");
		properties.put("test.dir.type", "new");
		TemplateMapModel mainModel = new TemplateMapModel(properties);

		TemplateMapModel envModel = mainModel.getAsTemplateMapModel("env");
		assertTrue(envModel.isEmpty());
		
		assertEquals("devl", envModel.getAsString());
		assertEquals("/mydir", mainModel.getAsTemplateMapModel("dir").getAsString());
		
		TemplateMapModel testModel = mainModel.getAsTemplateMapModel("test");
		assertEquals("true", testModel.getAsString());
		
		TemplateMapModel testDirModel = testModel.getAsTemplateMapModel("dir");
		assertEquals("/testing", testDirModel.getAsString());
		assertEquals("new", testDirModel.getAsTemplateMapModel("type").getAsString());
		
	}
	
}
