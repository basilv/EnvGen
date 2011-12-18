// Copyright 2007 by Basil Vandegriend.  All rights reserved.
package com.basilv.envgen;

import java.io.File;
import java.util.Map;

import junit.framework.TestCase;

public class EnvironmentPropertiesLoaderTest extends TestCase {

	private File projectRootDir;
	
	public EnvironmentPropertiesLoaderTest(String arg0) {
		super(arg0);
	}

	protected void setUp() throws Exception {

        projectRootDir = new File(".").getCanonicalFile();
        if (projectRootDir.getName().equals("build")) {
            projectRootDir = projectRootDir.getParentFile();
        }

	}
     
	// This is an integration test to verify the behavior of the entire task.
	public void testFullExecution() {

		EnvironmentProperties envProps = EnvironmentPropertiesLoader.load(
			projectRootDir.getAbsolutePath() + "/src/testResources/envProperties.csv");
		
		assertNotNull(envProps);
		assertEquals(3, envProps.getNumberOfEnvironments());
		
		Map propertyMapOne = envProps.getPropertiesForEnvironment(0);
		Map propertyMapTwo = envProps.getPropertiesForEnvironment(1);
		Map propertyMapThree = envProps.getPropertiesForEnvironment(2);
		
		assertEquals("devl", propertyMapOne.get("env"));
		assertEquals("test", propertyMapTwo.get("env"));
		assertEquals("prod", propertyMapThree.get("env"));
		
		assertEquals("/d", propertyMapOne.get("dir"));
		assertEquals("/t", propertyMapTwo.get("dir"));
		assertEquals("/p", propertyMapThree.get("dir"));
		
		// Test that blank entries work.
		assertEquals("notprod", propertyMapOne.get("blankEntry"));
		assertEquals("notprod", propertyMapTwo.get("blankEntry"));
		assertEquals("", propertyMapThree.get("blankEntry"));
		
	}
	
	public void testIsComment() {
		assertTrue(EnvironmentPropertiesLoader.isComment(""));
		assertTrue(EnvironmentPropertiesLoader.isComment(null));
		
		assertTrue(EnvironmentPropertiesLoader.isComment("# My comment"));
		assertTrue(EnvironmentPropertiesLoader.isComment("// Another comment"));
		
		assertFalse(EnvironmentPropertiesLoader.isComment("property.name"));
	}
	
}
