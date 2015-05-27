package it.unitn.disi.smatch.oracles.dbukc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import it.unitn.disi.smatch.data.ling.ISense;
import it.unitn.disi.smatch.data.mappings.IMappingElement;
import it.unitn.disi.smatch.data.trees.IContext;
import it.unitn.disi.smatch.data.trees.INode;
import it.unitn.disi.smatch.oracles.ILinguisticOracle;
import it.unitn.disi.smatch.oracles.ISenseMatcher;
import it.unitn.disi.smatch.oracles.LinguisticOracleException;
import it.unitn.disi.smatch.oracles.SenseMatcherException;
import it.unitn.disi.sweb.core.nlp.components.languagedetectors.ILanguageDetector;
import it.unitn.disi.sweb.core.nlp.components.lemmatizers.ILemmatizer;
import it.unitn.disi.sweb.core.nlp.parameters.NLPParameters;

public class DBUKC_scrollLemma implements ILinguisticOracle,ISenseMatcher {

	HashMap<String, HashMap<String, ArrayList<ArrayList<String>>>> multiwords = new HashMap<String, HashMap<String, ArrayList<ArrayList<String>>>>();
	Connection c;
	
	@Autowired
    @Qualifier("Lemmatizer")
    private ILemmatizer<NLPParameters> lemmatizer;

    @Autowired
    @Qualifier("NLPParameters")
    private NLPParameters parameters;
    
    @Autowired
    @Qualifier("LanguageDetector")
    private ILanguageDetector<NLPParameters> languageDetector;
    
	public DBUKC_scrollLemma() {
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
		Iterator<INode> nodes = context.nodeIterator();
        StringBuilder content = new StringBuilder(nodes.next().nodeData().getName());
        while(nodes.hasNext())
        {
            content.append(" ").append(nodes.next().nodeData().getName());
        }
		return languageDetector.detectLanguage(content.toString(), parameters);
	}

//	@Override
//	public String detectLanguage(IContext context) {
//		String lang = new String();
//		URL url=null;
//        try {
//        	List<String> urls = new ArrayList<>();
//			url = DetectorFactory.class.getClassLoader().getResource("profiles/es");
////			urls.add(DetectorFactory.class.getClassLoader().getResource("profiles/es").toURI().toString());
////			urls.add(DetectorFactory.class.getClassLoader().getResource("profiles/en").toURI().toString());
////			urls.add(DetectorFactory.class.getClassLoader().getResource("profiles/it").toURI().toString());
//			InputStream in = this.getClass().getResourceAsStream("profiles/");
//			System.out.println(in.available());
//			File file;
//			file = new File(url.getFile());
//			System.out.println(file.exists());
//			System.out.println(file.isDirectory());
//			System.out.println(file.listFiles());
//        	DetectorFactory.loadProfile(file);
//        	System.out.println("PATH: "+DetectorFactory.class.getCanonicalName());
//        	System.out.println("PATH: "+DetectorFactory.class.getName());
//        	System.out.println("PATH: "+url);
//            Detector detector = DetectorFactory.create();
//            HashMap<String,Double> priorMap = new HashMap<>();
//            priorMap.put("en", new Double(0.1));
//            priorMap.put("it", new Double(0.1));
//            priorMap.put("es", new Double(0.1));
//            detector.setPriorMap(priorMap);
//
//            Iterator<INode> nodes = context.nodeIterator();
//
//            String content = nodes.next().nodeData().getName();
//            while(nodes.hasNext())
//            {
//                content += " " + nodes.next().nodeData().getName();
//            }
//
//            detector.append(content);
//            lang = detector.detect();
//
//        }
//        catch (Exception e) {
//            e.printStackTrace();
////        } catch (URISyntaxException e) {
////			e.printStackTrace();
////		} catch (IOException e) {
////			// TODO Auto-generated catch block
////			e.printStackTrace();
//		}
//
//        return lang;
//	}

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
        	if (source.getId().equals(target.getId()) || 
        			sourceSyn.getSynsetID().equals(targetSyn.getSynsetID())) {
                return false;
            }
        	String sourceConceptId = sourceSyn.getConceptID();
        	String targetConceptId = targetSyn.getConceptID();
        	List<String> ancestorsOfTarget = targetSyn.getConceptAncestorsIds();
//        	System.out.println(ancestorsOfTarget);
//        	System.out.println(sourceConceptId);
        	if(ancestorsOfTarget.contains(sourceConceptId)){
        		return true;
        	}else{
//        		return !traverseTree(ancestorsOfTarget, sourceSyn);
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
//		System.out.println(targets);
//		System.out.println(sourceId+"_"+targetId);
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
            if (source.getId().equals(target.getId())) {
                return true;
            }
            if (sourceSyn.getSynsetID().equals(targetSyn.getSynsetID())){
            	return true;
            }
            Statement stmt = null;
    		ResultSet rs;
    		try {
    			stmt = c.createStatement();
//    			rs = stmt.executeQuery("select id from vocabulary_synset_relations "
//    					+ "where src_synset_id=(select synset_id ssynid from vocabulary_senses where id="+source.getId()+") "
//    					+ "and trg_synset_id=(select synset_id ssynid from vocabulary_senses where id="+target.getId()+") "
//    					+ "and relation_type=35;");
    			rs = stmt.executeQuery("select id from vocabulary_synset_relations "
    					+ "where src_synset_id="+sourceSyn.getSynsetID()
    					+ " and trg_synset_id="+targetSyn.getSynsetID()
    					+ " and relation_type=35;");
    			if (rs.next()){
    				return !(("3".equals(sourceSyn.getPOS()) || ("3".equals(targetSyn.getPOS()))));//TODO hand-coded POS
    			}
    				
    			rs.close();
    		} catch (SQLException e) {
    			e.printStackTrace();
    		}
        }
        else {
            if(sourceSyn.getConceptID().equals(targetSyn.getConceptID())){
               return true;
            }
            if (source.getId().equals(target.getId())) {
                return true;
            }else{
                return false;
            }
        }
        return false;
	}
	
	public static void main(String[] args) throws Exception{
		ApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:**/smatch-context.xml");
		System.out.println(Arrays.asList(applicationContext.getBeanDefinitionNames()));
		DBUKC_scrollLemma ukc = (DBUKC_scrollLemma) applicationContext.getBean("dbukcLemma");
//		IContext t = new Context();
//        INode root = t.createRoot("Course");
//        INode node = root.createChild("College of Arts and Sciences");
//        node.createChild("English");
//        System.out.println(ukc.isEqual("stopped", "stopping", "en"));
//        System.out.println(ukc.isEqual("saltaba", "saltando", "es"));
//        System.out.println(ukc.getSenses("derogate", "en"));
//        System.out.println(ukc.getSenses("apelar", "es"));
//        System.out.println(ukc.getBaseForms("catting", "en"));
//        System.out.println(ukc.getBaseForms("paradas", "es"));
//        System.out.println(ukc.getMultiwords("strange", "en"));
//        System.out.println(ukc.getMultiwords("a", "es"));
//		for (ISense sourceSense : ukc.getSenses("mother", "en")) {
//            for (ISense targetSense : ukc.getSenses("father", "en")) {
//                DBUKCSense sourceSyn = (DBUKCSense) sourceSense;
//                DBUKCSense targetSyn = (DBUKCSense) targetSense;
//                System.out.println("__"+sourceSyn.getId()+"__"+targetSyn.getId()+"__");
//                System.out.println("__"+sourceSyn.getSynsetID()+"__"+targetSyn.getSynsetID()+"__");
//                System.out.println("__"+sourceSyn.getConceptID()+"__"+targetSyn.getConceptID()+"__");
//                System.out.println("__"+sourceSyn.getPOS()+"__"+targetSyn.getPOS()+"__\n");
//                if (ukc.isSourceOppositeToTarget(sourceSyn,targetSyn)) {
//                    System.out.println(IMappingElement.DISJOINT);
//                }
//            }
//        }
		System.out.println(ukc.getRelation(ukc.getSenses("animal", "es"), ukc.getSenses("bestia", "es")));
//        System.out.println(ukc.getRelation(ukc.getSenses("large", "en"), ukc.getSenses("big", "en")));
//        System.out.println(ukc.getRelation(ukc.getSenses("being", "en"), ukc.getSenses("animal", "en")));
//        System.out.println(ukc.getRelation(ukc.getSenses("omnivore", "en"), ukc.getSenses("being", "en")));
//        System.out.println(ukc.getRelation(ukc.getSenses("brother", "en"), ukc.getSenses("sister", "en")));
        
	}

}
