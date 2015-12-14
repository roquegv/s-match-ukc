package it.unitn.disi.smatch.oracles.ukc;

import java.util.HashMap;
import java.util.Map;

public enum PartOfSpeech {
	NOUN,
	VERB,
	ADJECTIVE,
	ADVERB;
	
//
//	private String code;
//	private String pos;
//	
	private static Map<String,PartOfSpeech> code2pos = new HashMap<>();
	static{
		code2pos.put("1", PartOfSpeech.VERB);
		code2pos.put("2", PartOfSpeech.NOUN);
		code2pos.put("3", PartOfSpeech.ADJECTIVE);
		code2pos.put("4", PartOfSpeech.ADVERB);
	}
	public static PartOfSpeech getPosFromCode(String code){
		return code2pos.get(code);
	}
////	
//	PartOfSpeech(String code){
//		this.code = code;
//		this.pos = code2pos.get(code);
//	}
//	
//	public boolean isValidCode(String code){
//		return code2pos.containsKey(code);
//	}
}
