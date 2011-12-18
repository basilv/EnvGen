// Copyright 2007 by Basil Vandegriend.  All rights reserved.
package com.basilv.envgen;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;


public class AllEnvGen14Tests {

    public static void main(java.lang.String[] args) {
        TestRunner.run(suite());
    }

	public static Test suite()
	{
		TestSuite suite = new TestSuite(AllEnvGen14Tests.class.getName());

		suite.addTestSuite(EnvGenTaskTest.class);
		suite.addTestSuite(EnvironmentPropertiesLoaderTest.class);
		suite.addTestSuite(EnvironmentPropertiesTest.class);
		suite.addTestSuite(MainframeFileFormatTransformTest.class);
		suite.addTestSuite(TemplateMapModelTest.class);
		
        return suite;
    }
    
}
