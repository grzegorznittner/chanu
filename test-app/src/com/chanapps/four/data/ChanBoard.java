package com.chanapps.four.data;

import java.util.ArrayList;


public class ChanBoard {
	public enum Type {JAPANESE_CULTURE, INTERESTS, CREATIVE, ADULT, OTHER, MISC};
	
	public String name;
	public String link;
	
	public int iconId;
	
	public Type type;
	public boolean nsfw;
	public boolean workSafe;
	public boolean classic;
	public boolean textOnly;
	
	public String rules;
	
	public ArrayList<ChanThread> threads = new ArrayList<ChanThread>();
}
