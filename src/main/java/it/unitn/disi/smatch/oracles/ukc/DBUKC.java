package it.unitn.disi.smatch.oracles.ukc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import it.unitn.disi.smatch.data.ling.ISense;
import it.unitn.disi.smatch.data.trees.IContext;
import it.unitn.disi.smatch.oracles.ILinguisticOracle;
import it.unitn.disi.smatch.oracles.LinguisticOracleException;

public class DBUKC implements ILinguisticOracle {

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
			//check if the lang exists
			rs = stmt.executeQuery("SELECT form, lemma FROM vocabulary_word_forms wf, vocabulary_words w "
					+ "WHERE (form = '"+str1.replace("'", "''")+"' or form = '"+str2.replace("'", "''")+") "
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
			ResultSet rsWord = stmt.executeQuery("SELECT vsen.id vid, sense_frequency, word_sense_rank, cased_lemma, gloss, pos, concept_id"+
												"FROM vocabulary_senses vsen, vocabulary_synsets vsyn"+
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
			//check if the lang exists
			rs = stmt.executeQuery("SELECT lemma FROM vocabulary_words WHERE lemma LIKE '"+beginning.replace("'", "''")+" %' and vocabulary_id = "
					+ "(SELECT id FROM vocabularies WHERE language_code = '"+language+"');");
			if (rs.next()){
				multiword = rs.getString("lemma");
				multiwords.add((ArrayList<String>) Arrays.asList(multiword.split(" ")));
			}
				
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return multiwords;
	}

	@Override
	public String detectLanguage(IContext context) {
		// TODO Auto-generated method stub
		return null;
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
	
	public static void main(String[] argv) throws Exception{
		DBUKC main = new DBUKC();
		System.out.println(main.isEqual("stopped", "stopping", "en"));
	}

}
