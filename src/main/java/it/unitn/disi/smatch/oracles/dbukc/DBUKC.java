package it.unitn.disi.smatch.oracles.dbukc;

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

import com.cybozu.labs.langdetect.Detector;
import com.cybozu.labs.langdetect.DetectorFactory;
import com.cybozu.labs.langdetect.LangDetectException;

import it.unitn.disi.smatch.data.ling.ISense;
import it.unitn.disi.smatch.data.mappings.IMappingElement;
import it.unitn.disi.smatch.data.trees.Context;
import it.unitn.disi.smatch.data.trees.IContext;
import it.unitn.disi.smatch.data.trees.INode;
import it.unitn.disi.smatch.oracles.ILinguisticOracle;
import it.unitn.disi.smatch.oracles.ISenseMatcher;
import it.unitn.disi.smatch.oracles.LinguisticOracleException;
import it.unitn.disi.smatch.oracles.SenseMatcherException;

public class DBUKC implements ILinguisticOracle,ISenseMatcher {

	HashMap<String, HashMap<String, ArrayList<ArrayList<String>>>> multiwords = new HashMap<String, HashMap<String, ArrayList<ArrayList<String>>>>();
	Connection c;
    
	public DBUKC() {
        try {
        	Class.forName("org.postgresql.Driver");
			c = DriverManager.getConnection("jdbc:postgresql://localhost:5432/postgres",
			   "postgres", "postgres");
		} catch (Exception e) {
			e.printStackTrace();
		}
        multiwords = null;
	}
	
	@Override
	public boolean isEqual(String str1, String str2, String language)
			throws LinguisticOracleException {
		try
        {
            str1 = str1.toLowerCase(new Locale(language));
            str2 = str2.toLowerCase(new Locale(language));
        }
        catch (Exception e)
        {
            str1 = str1.toLowerCase();
            str2 = str2.toLowerCase();
        }
		
		Set<String> lemmas1 = new HashSet<String>();
		Set<String> lemmas2 = new HashSet<String>();
		Statement stmt = null;
		ResultSet rs;
		
		try {
			stmt = c.createStatement();
			rs = stmt.executeQuery("SELECT form, lemma FROM vocabulary_word_forms wf, vocabulary_words w "
					+ "WHERE (form = '"+str1.replace("'", "''")+"' or form = '"+str2.replace("'", "''")+"') "
					+ "and word_id = w.id and wf.vocabulary_id = "
					+ "(SELECT id FROM vocabularies WHERE language_code = '"+language+"');");
			while (rs.next()){
				String form = rs.getString("form");
				String lemma = rs.getString("lemma");
				if (str1.equals(form))
					lemmas1.add(lemma);
				else
					lemmas2.add(lemma);
			}
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		lemmas1.removeAll(lemmas2);
		if (lemmas1.size() == 0) //they have exactly the same set of lemmas 
			return true;
		return false;
	}

	@Override
	public List<ISense> getSenses(String word, String language)
			throws LinguisticOracleException {
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
			ResultSet rsWord = stmt.executeQuery("SELECT vsen.id vid, sense_frequency, word_sense_rank, cased_lemma, gloss, pos, concept_id "+
												"FROM vocabulary_senses vsen, vocabulary_synsets vsyn "+
												"WHERE word_id = "+word_id+" and vsyn.id = synset_id and vsen.vocabulary_id = "+lang_id+";");
			while (rsWord.next()){
				senseId = rsWord.getString("vid");
				sense = new DBUKCSense(senseId,language);
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
	public List<String> getBaseForms(String derivation, String language)
			throws LinguisticOracleException {
		try
        {
			derivation = derivation.toLowerCase(new Locale(language));
        }catch (Exception e){
        	derivation = derivation.toLowerCase();
        }
		
		List<String> lemmas = new ArrayList<String>();
		Statement stmt = null;
		ResultSet rs;
		
		try {
			stmt = c.createStatement();
			//check if the lang exists
			rs = stmt.executeQuery("SELECT form, lemma FROM vocabulary_word_forms wf, vocabulary_words w "
					+ "WHERE form = '"+derivation.replace("'", "''")+"' "
					+ "and word_id = w.id and wf.vocabulary_id = "
					+ "(SELECT id FROM vocabularies WHERE language_code = '"+language+"');");
			while (rs.next()){
				String lemma = rs.getString("lemma");
				lemmas.add(lemma);
			}
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return lemmas;
	}

	@Override
	public ISense createSense(String id, String language)
			throws LinguisticOracleException {
		Statement stmt = null;
		ResultSet rs;
		ISense sense=null;
		
		try {
			stmt = c.createStatement();
			//check if the lang exists
			rs = stmt.executeQuery("select id from vocabulary_senses "
					+ "where id = "+id+" and vocabulary_id = "
					+ "(SELECT id FROM vocabularies WHERE language_code = '"+language+"');");
			if (rs.next()){
				sense = new DBUKCSense(id,language);
			}
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return sense;
	}

	@Override
	public ArrayList<ArrayList<String>> getMultiwords(String beginning,
			String language) throws LinguisticOracleException {
		
		Statement stmt = null;
		ResultSet rs;
		String multiword;
		ArrayList<ArrayList<String>> multiwords = new ArrayList<ArrayList<String>>();
		try {
			stmt = c.createStatement();
			rs = stmt.executeQuery("SELECT lemma FROM vocabulary_words WHERE lemma LIKE '"+beginning.replace("'", "''")+" %' and vocabulary_id = "
					+ "(SELECT id FROM vocabularies WHERE language_code = '"+language+"');");
			if (rs.next()){
				multiword = rs.getString("lemma");
				ArrayList<String> separated = new ArrayList<>();
				for (String m : Arrays.asList(multiword.split(" "))){
					separated.add(m);
				}
				multiwords.add(separated);
			}
				
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return multiwords;
	}

	@Override
	public String detectLanguage(IContext context) {
		String lang = new String();
        try {

                Detector detector = DetectorFactory.create();
                HashMap<String,Double> priorMap = new HashMap<>();
                priorMap.put("en", new Double(0.1));
                priorMap.put("it", new Double(0.1));
                priorMap.put("es", new Double(0.1));
                detector.setPriorMap(priorMap);

                Iterator<INode> nodes = context.nodeIterator();

                String content = nodes.next().nodeData().getName();
                while(nodes.hasNext())
                {
                    content += " " + nodes.next().nodeData().getName();
                }

                detector.append(content);
                lang = detector.detect();

            }
            catch (LangDetectException e) {
                e.printStackTrace();
            }

        return lang;
	}

	@Override
	public void readMultiwords(String language) {
		if (this.multiwords != null) return;
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
				multiwords.add((ArrayList<String>) Arrays.asList(multiword.split(" ")));
			}
			HashMap<String, ArrayList<ArrayList<String>>> map = new HashMap<String, ArrayList<ArrayList<String>>>();
			for (ArrayList<String> mw : multiwords){
				String firstWord = mw.get(0);
				if (map.get(firstWord) == null || map.get(firstWord).size() == 0)
					map.put(firstWord, new ArrayList<ArrayList<String>>());
				map.get(firstWord).add(mw);
			}
			this.multiwords.put(language, map);
			
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	public char getRelation(List<ISense> sourceSenses, List<ISense> targetSenses)
			throws SenseMatcherException {
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
	public boolean isSourceLessGeneralThanTarget(ISense source, ISense target)
			throws SenseMatcherException {
		return isSourceMoreGeneralThanTarget(target,source);
	}

	@Override
	public boolean isSourceMoreGeneralThanTarget(ISense source, ISense target)
			throws SenseMatcherException {
		if (!(source instanceof DBUKCSense) || !(target instanceof DBUKCSense)) {
            return false;
        }
		DBUKCSense sourceSyn = (DBUKCSense) source;
        DBUKCSense targetSyn = (DBUKCSense) target;
        if ("2".equals(sourceSyn.getPOS()) && "2".equals(targetSyn.getPOS()) ||
        		"1".equals(sourceSyn.getPOS()) && "1".equals(targetSyn.getPOS())){//TODO hand-coded POS
        	if (source.getId().equals(target.getId())) {
                return false;
            }
        	String sourceConceptId = sourceSyn.getConceptID();
        	String targetConceptId = targetSyn.getConceptID();
        	List<String> ancestorsOfTarget = targetSyn.getConceptAncestorsIds();
        	System.out.println(sourceConceptId);
        	System.out.println(ancestorsOfTarget);
        	if(ancestorsOfTarget.contains(sourceConceptId)){
        		return true;
        	}else{
        		return !traverseTree(ancestorsOfTarget, sourceSyn);
        	}
        	
        }
		return false;
	}

	@Override
	public boolean isSourceOppositeToTarget(ISense source, ISense target)
			throws SenseMatcherException {
		if (!(source instanceof DBUKCSense) || !(target instanceof DBUKCSense)) {
            return false;
        }
		DBUKCSense sourceSyn = (DBUKCSense) source;
        DBUKCSense targetSyn = (DBUKCSense) target;
        if(sourceSyn.getLanguage().equals(targetSyn.getLanguage())){
        	if ("3".equals(sourceSyn.getPOS()) || "3".equals(targetSyn.getPOS())) {//TODO hand-coded POS tag
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
	
	private boolean traverseTree(List<String> targetAncestors, DBUKCSense sourceSyn) {
        String sourceConcept = sourceSyn.getConceptID();
        for(String targetAncestor : targetAncestors){
            if(spreadSearch(sourceConcept,targetAncestor)){
                return true;
            }
        }
        return false;
    }
	
	private boolean spreadSearch(String sourceId, String targetId)
    {
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
        
        for(String target : targets)
        {
            spreadSearch(sourceId, target);
        }

        return false;
    }

	@Override
	public boolean isSourceSynonymTarget(ISense source, ISense target)
			throws SenseMatcherException {
		if (!(source instanceof DBUKCSense) || !(target instanceof DBUKCSense)) {
            return false;
        }
        DBUKCSense sourceSyn = (DBUKCSense) source;
        DBUKCSense targetSyn = (DBUKCSense) target;
        if(sourceSyn.getLanguage().equals(targetSyn.getLanguage()))
        {
            if (source.equals(target)) {
                return true;
            }
            Statement stmt = null;
    		ResultSet rs;
    		try {
    			stmt = c.createStatement();
    			rs = stmt.executeQuery("select id from vocabulary_synset_relations "
    					+ "where src_synset_id=(select synset_id ssynid from vocabulary_senses where id="+source.getId()+") "
    					+ "and trg_synset_id=(select synset_id ssynid from vocabulary_senses where id="+target.getId()+") "
    					+ "and relation_type=35;");
    			if (rs.next()){
    				return !(("3".equals(sourceSyn.getPOS()) || ("3".equals(targetSyn.getPOS()))));//TODO hand-coded POS
    			}
    				
    			rs.close();
    		} catch (SQLException e) {
    			e.printStackTrace();
    		}
        }
        else {
            if(sourceSyn.getConceptID() == targetSyn.getConceptID())
            {
               return true;
            }
            if (source.equals(target)) {
                return true;
            }
            else
            {
                return false;
            }
        }
        return false;
	}
	
	public static void main(String[] args) throws Exception{
		DBUKC ukc = new DBUKC();
		System.out.println(ukc.isEqual("stopped", "stopping", "en"));
		System.out.println(ukc.getSenses("word", "en"));
		System.out.println(ukc.getBaseForms("stopped", "en"));
		System.out.println(ukc.createSense("65530", "en"));
		System.out.println(ukc.getMultiwords("cut", "en"));
//		IContext context = new Context();
//		INode root = context.createRoot("Course");
//        INode node = root.createChild("College of Arts and Sciences");
//        node.createChild("English");
//		System.out.println(ukc.detectLanguage(context));
//		System.out.println(ukc.getRelation(ukc.getSenses("word", "en"), ukc.getSenses("statement", "en")));
		System.out.println(ukc.isSourceSynonymTarget(ukc.createSense("129611", "en"), ukc.createSense("129612", "en")));
		System.out.println(ukc.isSourceOppositeToTarget(ukc.createSense("129611", "en"), ukc.createSense("129612", "en")));
		System.out.println(ukc.isSourceOppositeToTarget(new DBUKCSense("1853", "en"), new DBUKCSense("66908", "en")));
		System.out.println(ukc.isSourceMoreGeneralThanTarget(ukc.createSense("288622", "en"), ukc.createSense("3224", "en")));
	}

}
