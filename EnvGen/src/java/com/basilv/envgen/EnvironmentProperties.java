// Copyright 2006 by Basil Vandegriend.  All rights reserved.

package com.basilv.envgen;

import java.util.*;

import com.basilv.core.Assert;

/**
 * Holds a set of properties (as a map) for each environment.
 *
 */
public class EnvironmentProperties
{

	private List envPropertiesList = new ArrayList();

	public EnvironmentProperties(int environmentCount) {
		Assert.greaterThanZero("environmentCount", environmentCount);
		
		for (int i = 0; i < environmentCount; i++) {
			Map propertyMap = new HashMap();
			envPropertiesList.add(propertyMap);			
		}
	}
	
	public void addProperty(int environment, String property, String value) {
		Assert.notNegative("environment", environment);

		Map envProperties = getPropertiesForEnvironment(environment);
		envProperties.put(property, value);
		
	}

	public int getNumberOfEnvironments() {
		return envPropertiesList.size();
	}

	public List getEnvPropertiesList() {
		return envPropertiesList;
	}
	
	public Map getPropertiesForEnvironment(int environment) {
		Assert.notNegative("environment", environment);

		Map envProperties = (Map) envPropertiesList.get(environment);
		return envProperties;
	}
	
}
