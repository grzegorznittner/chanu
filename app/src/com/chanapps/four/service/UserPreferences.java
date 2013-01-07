/**
 * 
 */
package com.chanapps.four.service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author "Grzegorz Nittner" <grzegorz.nittner@gmail.com>
 *
 */
public class UserPreferences {
	public Map<String, Integer> boardOpen = new HashMap<String, Integer>();
	public Map<String, Integer> boardThread = new HashMap<String, Integer>();
	public Map<String, Integer> boardFullImage = new HashMap<String, Integer>();
	
	public Date lastUpdate;
	public Date lastStored;
}
