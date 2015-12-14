package it.unitn.disi.smatch.oracles.dbukc;

import it.unitn.disi.smatch.data.ling.ISense;
import it.unitn.disi.smatch.data.ling.Sense;
import it.unitn.disi.smatch.data.mappings.IMappingElement;
import it.unitn.disi.smatch.data.trees.IContext;
import it.unitn.disi.smatch.data.trees.INode;
import it.unitn.disi.smatch.oracles.SenseMatcherException;
import it.unitn.disi.smatch.oracles.dbukc.PartOfSpeech;
import it.unitn.disi.sweb.core.nlp.components.languagedetectors.ILanguageDetector;
import it.unitn.disi.sweb.core.nlp.components.lemmatizers.ILemmatizer;
import it.unitn.disi.sweb.core.nlp.parameters.NLPParameters;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

public class DBUKCService implements IUKCService{

	private static Connection c;
	HashMap<String, HashMap<String, ArrayList<ArrayList<String>>>> multiwords = 
			new HashMap<String, HashMap<String, ArrayList<ArrayList<String>>>>();
	
	@Autowired
    @Qualifier("Lemmatizer")
    private ILemmatizer<NLPParameters> lemmatizer;

    @Autowired
    @Qualifier("NLPParameters")
    private NLPParameters parameters;
    
    @Autowired
    @Qualifier("LanguageDetector")
    private ILanguageDetector<NLPParameters> languageDetector;
    
	public DBUKCService() {
		try {
//	        	Class.forName("org.postgresql.Driver");
			c = DriverManager.getConnection("jdbc:postgresql://localhost:5432/postgres",
			   "postgres", "postgres");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	@Override
	public String getGloss(long conceptID, String language) {
		Statement stmt = null;
		ResultSet rs;
		String gloss = null;
		try {
			stmt = c.createStatement();
			//check if the lang exists
			rs = stmt.executeQuery("select gloss from vocabulary_senses vsen, vocabulary_synsets vsyn "
					+ "where vsen.id = "+conceptID+" and synset_id = vsyn.id and vsen.vocabulary_id = "
							+ "(SELECT id FROM vocabularies WHERE language_code = '"+language+"');");
			if (rs.next()){
				gloss = rs.getString("gloss");
			}
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return gloss;
	}

	@Override
	public List<ISense> getSenses(String word, String language) {
		Statement stmt = null;
		ResultSet rs;
		ISense sense;
		ArrayList <ISense> senses = new ArrayList<ISense>();
		Long lang_id, word_id;
		String senseId;
		try {
			stmt = c.createStatement();
			//check if the lang exists
			rs = stmt.executeQuery( "(SELECT id FROM vocabularies WHERE language_code = '"+language+"');" );
			if (!rs.next())
				throw new SQLException("The vocabulary "+language+" does not exist!");
			else
				lang_id = rs.getLong("id");
			rs.close();

			//check if the lemma exists
			rs= stmt.executeQuery("SELECT id FROM vocabulary_words WHERE lemma = '"+word+"' and vocabulary_id = "+lang_id+";");
			if (!rs.next())
				throw new SQLException("The lemma "+word+" does not exist!");
			else
				word_id = rs.getLong("id");
			rs.close();
			//find the sense
//			ResultSet rsWord = stmt.executeQuery("SELECT id, synset_id, sense_frequency, word_sense_rank, cased_lemma "
//					+ "FROM vocabulary_senses WHERE word_id = "+word_id+";");
			ResultSet rsWord = stmt.executeQuery("SELECT concept_id, vsyn.id synsetid "
					+ "FROM vocabulary_senses vsen, vocabulary_synsets vsyn "
					+ "WHERE word_id = "+word_id+" and "
							+ "vsyn.id = synset_id and vsen.vocabulary_id = "+lang_id);
			while (rsWord.next()){
				Long conceptId = rsWord.getLong("concept_id");
				Long synsetId = rsWord.getLong("synsetid");
				sense = new DBUKCSense(conceptId,synsetId,language,this);
				senses.add(sense);
			}
			rsWord.close();
	        stmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return senses;
	}

	@Override
	public List<String> getBaseForms(String derivation, String language) {
		try{
			derivation = derivation.toLowerCase(new Locale(language));
        }catch (Exception e){
        	derivation = derivation.toLowerCase();
        }
		
		List<String> lemmas = new ArrayList<String>();
		Iterator<Set<String>> it = lemmatizer.lemmatize(derivation, language).values().iterator();
		while(it.hasNext())
			lemmas.addAll(it.next());
		return lemmas;
	}

	@Override
	public boolean isEqual(String str1, String str2, String language) {
		try{
            str1 = str1.toLowerCase(new Locale(language));
            str2 = str2.toLowerCase(new Locale(language));
        }catch (Exception e){
            str1 = str1.toLowerCase();
            str2 = str2.toLowerCase();
        }
		//TODO naive implementation
		ArrayList<String> lemmas1 = new ArrayList<>() ;
        ArrayList<String> lemmas2 = new ArrayList<>() ;
        Iterator<Set<String>> it;
        
		if(lemmatizer.isLemmaExists(str1, language))
			lemmas1.add(str1);
		else{
			it = lemmatizer.lemmatize(str1, language).values().iterator();
			while(it.hasNext())
				lemmas1.addAll(it.next());
		}
		if(lemmatizer.isLemmaExists(str2, language))
			lemmas2.add(str2);
		else{
			it = lemmatizer.lemmatize(str2, language).values().iterator();
			while(it.hasNext())
				lemmas2.addAll(it.next());
		}
		for(String lemma1 : lemmas1){
	        for(String lemma2 : lemmas2)
	            if(lemma1.equals(lemma2)){
	                return true;
	            }
        }
		return false;
	}

	@Override
	public ISense createSense(String id, String language) {
		Statement stmt = null;
		ResultSet rs;
		ISense sense=null;
		
		try {
			stmt = c.createStatement();
			//check if the lang exists
			rs = stmt.executeQuery("select syn.concept_id, syn.id "
					+ "from vocabulary_senses sen,vocabulary_synsets syn "
					+ "where sen.id = "+id+" and sen.vocabulary_id = "
					+ "(SELECT id FROM vocabularies WHERE language_code = '"+language+"') "
					+ "and syn.id = sen.synset_id");
			if (rs.next()){
				Long synsetId = rs.getLong("id");
				Long conceptId = rs.getLong("concept_id");
				sense = new DBUKCSense(conceptId,synsetId,language,this);
			}
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return sense;
	}

	@Override
	public List<String> getMultiwords(String language) {
		Statement stmt = null;
		ResultSet rs;
		String multiword;
		List<String> multiwords = new ArrayList<String>();
		try {
			stmt = c.createStatement();
			rs = stmt.executeQuery("SELECT lemma FROM vocabulary_words WHERE lemma LIKE '% %' and vocabulary_id = "
					+ "(SELECT id FROM vocabularies WHERE language_code = '"+language+"');");
			while (rs.next()){
				multiword = rs.getString("lemma");
				multiwords.add(multiword);
			}
				
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return multiwords;
	}

	@Override
	public char getRelation(List<ISense> sourceSenses, List<ISense> targetSenses){
		for (ISense sourceSense : sourceSenses) {
            for (ISense targetSense : targetSenses) {
                DBUKCSense sourceSyn = (DBUKCSense) sourceSense;
                DBUKCSense targetSyn = (DBUKCSense) targetSense;
                if (isSourceSynonymTarget(sourceSyn,targetSyn)) {
                    return IMappingElement.EQUIVALENCE;
                }
            }
        }
        for (ISense sourceSense : sourceSenses) {
            for (ISense targetSense : targetSenses) {
                DBUKCSense sourceSyn = (DBUKCSense) sourceSense;
                DBUKCSense targetSyn = (DBUKCSense) targetSense;
                if (isSourceLessGeneralThanTarget(sourceSyn,targetSyn)) {
                    return IMappingElement.LESS_GENERAL;
                }
            }
        }
        for (ISense sourceSense : sourceSenses) {
            for (ISense targetSense : targetSenses) {
                DBUKCSense sourceSyn = (DBUKCSense) sourceSense;
                DBUKCSense targetSyn = (DBUKCSense) targetSense;
                if (isSourceMoreGeneralThanTarget(sourceSyn,targetSyn)) {
                    return IMappingElement.MORE_GENERAL;
                }
            }
        }
        for (ISense sourceSense : sourceSenses) {
            for (ISense targetSense : targetSenses) {
                DBUKCSense sourceSyn = (DBUKCSense) sourceSense;
                DBUKCSense targetSyn = (DBUKCSense) targetSense;
                if (isSourceOppositeToTarget(sourceSyn,targetSyn)) {
                    return IMappingElement.DISJOINT;
                }
            }
        }
        return IMappingElement.IDK;
	}

	@Override
	public boolean isSourceLessGeneralThanTarget(ISense source, ISense target){
		return isSourceMoreGeneralThanTarget(target,source);
	}

	@Override
	public boolean isSourceMoreGeneralThanTarget(ISense source, ISense target){
		if (!(source instanceof DBUKCSense) || !(target instanceof DBUKCSense)) {
            return false;
        }
		DBUKCSense sourceSyn = (DBUKCSense) source;
        DBUKCSense targetSyn = (DBUKCSense) target;
        if (PartOfSpeech.NOUN == sourceSyn.getPOS() && PartOfSpeech.NOUN == targetSyn.getPOS() ||
        		PartOfSpeech.VERB == sourceSyn.getPOS() && PartOfSpeech.VERB == targetSyn.getPOS()){
        	if (source.getId().equals(target.getId())) {
                return false;
            }
        	Long sourceConceptId = sourceSyn.getConceptID();
        	Long targetConceptId = targetSyn.getConceptID();
        	List<String> ancestorsOfTarget = getConceptAncestorsIds(targetConceptId.toString());
        	if(ancestorsOfTarget.contains(sourceConceptId)){
        		return true;
        	}else{
        		return !traverseTree(ancestorsOfTarget, sourceSyn);
        	}
        	
        }
		return false;
	}

	@Override
	public boolean isSourceOppositeToTarget(ISense source, ISense target){
		if (!(source instanceof DBUKCSense) || !(target instanceof DBUKCSense)) {
            return false;
        }
		DBUKCSense sourceSyn = (DBUKCSense) source;
        DBUKCSense targetSyn = (DBUKCSense) target;
        if(sourceSyn.getLanguage().equals(targetSyn.getLanguage())){
        	if (PartOfSpeech.ADJECTIVE == sourceSyn.getPOS() || 
        			PartOfSpeech.ADJECTIVE == targetSyn.getPOS()) {
        		Statement stmt = null;
        		ResultSet rs;
        		try {
        			stmt = c.createStatement();
        			rs = stmt.executeQuery("select id from vocabulary_sense_relations vsr "
        					+ "where src_sense_id="+sourceSyn.getId()+" and trg_sense_id="+targetSyn.getId()+
        					" and relation_type=30;");
        			if (rs.next()){
        				return true;
        			}
        				
        			rs.close();
        		} catch (SQLException e) {
        			e.printStackTrace();
        		}
        	}
        }
        return false;
	}
	
	private List<String> getConceptAncestorsIds(String conceptId){
		List<String> ancestors = new ArrayList<>();
		ancestor(conceptId, ancestors);
		return ancestors;
	}
	
	private void ancestor(String conceptId, List<String> conceptsIds){
		Statement stmt = null;
		ResultSet rs;
		try {
			stmt = c.createStatement();
			rs = stmt.executeQuery("select src_con_id from concept_relations "
					+ "where trg_con_id="+conceptId+" and relation_type in (22,20,34,36,37);");
			while (rs.next()){
				String newid = rs.getString("src_con_id");
				if (!conceptsIds.contains(newid)){
					conceptsIds.add(newid);
					ancestor(newid,conceptsIds);
				}
			}
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	private boolean traverseTree(List<String> targetAncestors, DBUKCSense sourceSyn) {
        String sourceConcept = ((Long)sourceSyn.getConceptID()).toString();
        for(String targetAncestor : targetAncestors){
            if(spreadSearch(sourceConcept,targetAncestor)){
                return true;
            }
        }
        return false;
    }
	
	private boolean spreadSearch(String sourceId, String targetId){
		Statement stmt = null;
		ResultSet rs;
//		List<String> sources = new ArrayList<>();
		List<String> targets = new ArrayList<>();
		try {
			stmt = c.createStatement();
			rs = stmt.executeQuery("select src_con_id,trg_con_id from concept_relations "
					+ "where src_con_id="+targetId+" and relation_type in (22,36,37);");
			while (rs.next()){
//				sources.add(rs.getString("src_con_id"));
				targets.add(rs.getString("trg_con_id"));
			}
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		if (targets.contains(sourceId))
			return true;
        
        for(String target : targets){
            spreadSearch(sourceId, target);
        }

        return false;
    }

	@Override
	public boolean isSourceSynonymTarget(ISense source, ISense target){
		if (!(source instanceof DBUKCSense) || !(target instanceof DBUKCSense)) {
            return false;
        }
        DBUKCSense sourceSyn = (DBUKCSense) source;
        DBUKCSense targetSyn = (DBUKCSense) target;
        if(sourceSyn.getLanguage().equals(targetSyn.getLanguage())){
            if (source.equals(target))
                return true;
            Statement stmt = null;
    		ResultSet rs;
    		try {
    			stmt = c.createStatement();
    			rs = stmt.executeQuery("select id from vocabulary_synset_relations "
    					+ "where src_synset_id=(select synset_id ssynid from vocabulary_senses where id="+source.getId()+") "
    					+ "and trg_synset_id=(select synset_id ssynid from vocabulary_senses where id="+target.getId()+") "
    					+ "and relation_type=35;");
    			if (rs.next()){
    				return !(PartOfSpeech.ADJECTIVE == sourceSyn.getPOS() || 
    						PartOfSpeech.ADJECTIVE== targetSyn.getPOS());
    			}
    				
    			rs.close();
    		} catch (SQLException e) {
    			e.printStackTrace();
    		}
        }else {
            if(sourceSyn.getConceptID() == targetSyn.getConceptID())
               return true;
            if (source.equals(target)) {
                return true;
            }else{
                return false;
            }
        }
        return false;
	}

	@Override
	public List<String> getlemmas(long synsetID) {
		Statement stmt = null;
		ResultSet rs;
		List<String> lemmas = new ArrayList<String>();
		try {
			stmt = c.createStatement();
			rs = stmt.executeQuery("select lemma from vocabulary_senses vsen, vocabulary_words vw"+
								"where synset_id = (select synset_id from vocabulary_senses where id = "+synsetID+") "
								+ "and vw.id = word_id;");
			while (rs.next()){
				lemmas.add(rs.getString("lemma"));
			}
			
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return lemmas;
	}

	@Override
	public List<ISense> getParents(long conceptID, String language, int depth) {
		List<ISense> senses= new ArrayList<ISense>();
		ascendants(conceptID,senses,depth,language);
		return senses;
	}
	
	private void ascendants(long conceptID, List<ISense> senses, int depth,String language){
		if (depth == 0) return;
		Statement stmt = null;
		ResultSet rs;
		try {
			stmt = c.createStatement();
			rs = stmt.executeQuery("select src_con_id, vs.id "
					+ "from concept_relations cr, vocabulary_synsets vs "
					+ "where trg_con_id = "+conceptID+" and vs.concept_id=cr.src_con_id and vocabulary_id = "
					+ "(SELECT id FROM vocabularies WHERE language_code = '"+language+"')");
			while (rs.next()){
				Long conceptId = rs.getLong("src_con_id");
				Long synsetId = rs.getLong("vs.id");
				senses.add(new DBUKCSense(conceptId,synsetId,language,this));
				ascendants(conceptId,senses,depth-1,language);
			}
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	public List<ISense> getChildren(long conceptID, String language, int depth) {
		List<ISense> senses= new ArrayList<ISense>();
		descendants(conceptID,senses,depth,language);
		return senses;
	}
	
	private void descendants(long conceptID, List<ISense> senses, int depth,String language){
		if (depth == 0) return;
		Statement stmt = null;
		ResultSet rs;
		try {
			stmt = c.createStatement();
			rs = stmt.executeQuery("select trg_con_id, vs.id from concept_relations cr, vocabulary_synsets vs "
					+ "where src_con_id = "+conceptID+" and vs.concept_id=cr.trg_con_id and vocabulary_id = "
					+ "(SELECT id FROM vocabularies WHERE language_code = '"+language+"')");
			while (rs.next()){
				Long newConceptId = rs.getLong("trg_con_id");
				Long newSynsetId = rs.getLong("vs.id");
				senses.add(new DBUKCSense(newConceptId,newSynsetId,language,this));
				descendants(newConceptId,senses,depth-1,language);
			}
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String detectLanguage(IContext context) {
		Iterator<INode> nodes = context.nodeIterator();
        StringBuilder content = new StringBuilder(nodes.next().nodeData().getName());
        while(nodes.hasNext())
        {
            content.append(" ").append(nodes.next().nodeData().getName());
        }
		return languageDetector.detectLanguage(content.toString(), parameters);
	}

	@Override
	public HashMap<String, ArrayList<ArrayList<String>>> readMultiwords(
			String language) {
		if (this.multiwords != null && this.multiwords.containsKey(language))
			return multiwords.get(language);
		Statement stmt = null;
		ResultSet rs;
		String multiword;
		ArrayList<ArrayList<String>> multiwords = new ArrayList<ArrayList<String>>();
		try {
			stmt = c.createStatement();
			//check if the lang exists
			rs = stmt.executeQuery("SELECT lemma FROM vocabulary_words WHERE lemma LIKE '% %' "
					+ "and vocabulary_id = (SELECT id FROM vocabularies WHERE language_code = '"+language+"');");
			while (rs.next()){
				multiword = rs.getString("lemma");
				ArrayList<String> mws = new ArrayList<String>();
				for (String mw : multiword.split(" ")){
					mws.add(mw);
				}
//				multiwords.add((ArrayList<String>) Arrays.asList(multiword.split(" ")));
				multiwords.add(mws);
			}
			HashMap<String, ArrayList<ArrayList<String>>> map = new HashMap<String, ArrayList<ArrayList<String>>>();
			for (ArrayList<String> mw : multiwords){
				String firstWord = mw.get(0);
				if (map.get(firstWord) == null || map.get(firstWord).size() == 0)
					map.put(firstWord, new ArrayList<ArrayList<String>>());
				map.get(firstWord).add(mw);
			}
			this.multiwords = new HashMap<String, HashMap<String, ArrayList<ArrayList<String>>>>();
			this.multiwords.put(language, map);
			
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return this.multiwords.get(language);
	}
	
	@Override
	public PartOfSpeech getPOS(long synsetID) {
		Statement stmt = null;
		ResultSet rs;
		PartOfSpeech pos = null;
		try {
			stmt = c.createStatement();
			rs = stmt.executeQuery("select pos from vocabulary_synsets where id = "+synsetID);
			if (rs.next()){
				String code = rs.getString("pos");
				pos = PartOfSpeech.getPosFromCode(code);
			}
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return pos;
	}

}
