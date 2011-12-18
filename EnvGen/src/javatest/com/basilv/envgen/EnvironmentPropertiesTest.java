// Copyright 2007 by Basil Vandegriend.  All rights reserved.
package com.basilv.envgen;

import java.util.Map;

import junit.framework.TestCase;

public class EnvironmentPropertiesTest extends TestCase {

	private EnvironmentProperties envProps;
	
	public EnvironmentPropertiesTest(String arg0) {
		super(arg0);
	}

	
	protected void setUp() {
		envProps = new EnvironmentProperties(3);
	}


	public void testAddEnvironment() {
		envProps.addProperty(0, "env", "devl");
		Map propertyMap = envProps.getPropertiesForEnvironment(0);
		assertEquals("devl", propertyMap.get("env"));
	}
	
}
