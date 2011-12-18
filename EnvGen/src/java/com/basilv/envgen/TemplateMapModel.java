// Copyright 2007 by Basil Vandegriend.  All rights reserved.

package com.basilv.envgen;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.basilv.core.Assert;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateScalarModel;

/**
 * Adapts a Map of property names and values to Freemarker's TemplateModel interface.
 * Properties of the form "foo.bar" need special handling, as FreeMarker considers a period
 * to be a dereference operator, and thus first looks up property "foo", and then in the result looks up
 * property "bar". So we store these properties in the same fashion.
 * 
 * This class can represent both a simple string value for a given property, or a map of properties. 
 * This allows it to store the properties "foo.bar=one" and "foo.bar.baz=two" as follows. The property "foo" 
 * is mapped to instance #2 of this class. Instance #2 has property "bar", which maps to instance #3 
 * that holds the string value "one" and holds the property "baz" which maps to instance #4 that just holds
 * the string value "two". 
 */
public class TemplateMapModel implements TemplateHashModel, TemplateScalarModel
{

	private Map map = new HashMap();
	private String stringValue;

	private TemplateMapModel() {
	}

	public TemplateMapModel(Map properties) {
		Assert.notNull("properties", properties);
		
		for (Iterator i = properties.entrySet().iterator(); i.hasNext(); ) {
			Map.Entry entry = (Map.Entry) i.next();
			String key = (String) entry.getKey();
			Assert.notNullOrEmpty("key", key);
			put(key, (String)entry.getValue());

		}
	}
	
	public TemplateMapModel getAsTemplateMapModel(String key) {
		return (TemplateMapModel) map.get(key);
	}
	
	public TemplateModel get(String key) {
		return getAsTemplateMapModel(key);
	}

	public boolean isEmpty() {
		return map.isEmpty();
	}

	public String getAsString() {
		return stringValue;
	}
	
	private void put(String key, String value) {
		Assert.notNullOrEmpty("key", key);

		if (key.indexOf(".") != -1) {
			putDereferencingKey(key, value);						
		} else {
			putRegularKey(key, value);
		}
		
	}

	private void putDereferencingKey(String key, String value) {
		// Key is of the form foo.bar, which we want to split into the first 'foo' plus the remainder
		// The remainder may also contain a period, so this recursively calls put. 
		String[] parts = key.split("\\.", 2);
		Assert.isTrue("Key [" + key + "] was not properly split two parts.", parts.length==2);
		String firstKey = parts[0];
		String remainingKey = parts[1];
		
		TemplateMapModel modelForKey = getModelForKeyCreatingIfNotExists(firstKey);
		modelForKey.put(remainingKey, value);		
	}

	private void putRegularKey(String key, String value) {
		TemplateMapModel modelForKey = getModelForKeyCreatingIfNotExists(key);
		modelForKey.stringValue = value;
	}

	private TemplateMapModel getModelForKeyCreatingIfNotExists(String key) {
		
		TemplateMapModel modelForKey = (TemplateMapModel) map.get(key);
		
		if (modelForKey == null) {
			modelForKey = new TemplateMapModel();
			map.put(key, modelForKey);
		}
		return modelForKey;
	}

}
