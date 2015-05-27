package it.unitn.disi.smatch.oracles.dbukc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import it.unitn.disi.smatch.data.ling.ISense;
import it.unitn.disi.smatch.oracles.LinguisticOracleException;

public class DBUKCSense implements ISense {
	
	private String id;
	private String language;
	private String gloss;
	private List<String> lemmas;
	private String conceptID;
	private String pos;
	private String synsetID;

	private static Connection c;
	static {
		try {
//        	Class.forName("org.postgresql.Driver");
			c = DriverManager.getConnection("jdbc:postgresql://localhost:5432/postgres",
			   "postgres", "postgres");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public DBUKCSense(String id, String language) {
		this.id = id;
		this.language = language;
		this.gloss = null;
		this.lemmas = null;
		this.conceptID = null;
		this.pos = "";
		this.synsetID=null;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public String getGloss() {
		if (this.gloss != null)
			return this.gloss;
		Statement stmt = null;
		ResultSet rs;
		String gloss = null;
		try {
			stmt = c.createStatement();
			//check if the lang exists
			rs = stmt.executeQuery("select gloss from vocabulary_senses vsen, vocabulary_synsets vsyn "
					+ "where vsen.id = "+this.id+" and synset_id = vsyn.id and vsen.vocabulary_id = "
							+ "(SELECT id FROM vocabularies WHERE language_code = '"+this.language+"');");
			if (rs.next()){
				gloss = rs.getString("gloss");
			}
			this.gloss = gloss;
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return gloss;
	}

	@Override
	public List<String> getLemmas() {
		//each sense references a single lemma; but many lemma share the same synset
		if (this.gloss != null)
			return this.lemmas;
		Statement stmt = null;
		ResultSet rs;
		List<String> lemmas = new ArrayList<String>();
		try {
			stmt = c.createStatement();
			//check if the lang exists
			rs = stmt.executeQuery("select lemma from vocabulary_senses vsen, vocabulary_words vw"+
								"where synset_id = (select synset_id from vocabulary_senses where id = "+id+") "
								+ "and vsen.vocabulary_id = (SELECT id FROM vocabularies WHERE language_code = '"+this.language+"')"
										+ " and vw.id = word_id;");
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
	public List<ISense> getParents() throws LinguisticOracleException {
		Statement stmt = null;
		ResultSet rs;
		List<ISense> senses = new ArrayList<ISense>();
		try {
			stmt = c.createStatement();
			rs = stmt.executeQuery("SELECT src_sense_id FROM vocabulary_sense_relations "
							+ "WHERE trg_sense_id = "+id+" and "
							+ "vocabulary_id = (SELECT id FROM vocabularies WHERE language_code = '"+language+"');");
			while (rs.next()){
				senses.add(new DBUKCSense(rs.getString("src_sense_id"),language));
			}
			
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return senses;
	}

	@Override
	public List<ISense> getParents(int depth) throws LinguisticOracleException {
		List<ISense> senses= new ArrayList<ISense>();
		ascendants(id,senses,depth);
		return senses;
	}
	private void ascendants(String senseId, List<ISense> senses, int depth){
		if (depth == 0) return;
		Statement stmt = null;
		ResultSet rs;
		try {
			stmt = c.createStatement();
			rs = stmt.executeQuery("SELECT src_sense_id FROM vocabulary_sense_relations "
							+ "WHERE trg_sense_id = "+senseId+" and "
							+ "vocabulary_id = (SELECT id FROM vocabularies WHERE language_code = '"+language+"');");
			while (rs.next()){
				String newid = rs.getString("src_sense_id");
				senses.add(new DBUKCSense(newid,language));
				ascendants(newid,senses,depth-1);
			}
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	public List<ISense> getChildren() throws LinguisticOracleException {
		Statement stmt = null;
		ResultSet rs;
		List<ISense> senses = new ArrayList<ISense>();
		try {
			stmt = c.createStatement();
			rs = stmt.executeQuery("SELECT trg_sense_id FROM vocabulary_sense_relations "
							+ "WHERE src_sense_id = "+id+" and "
							+ "vocabulary_id = (SELECT id FROM vocabularies WHERE language_code = '"+language+"');");
			while (rs.next()){
				senses.add(new DBUKCSense(rs.getString("trg_sense_id"),language));
			}
			
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return senses;
	}

	@Override
	public List<ISense> getChildren(int depth) throws LinguisticOracleException {
		List<ISense> senses= new ArrayList<ISense>();
		descendants(id,senses,depth);
		return senses;
	}
	
	private void descendants(String senseId, List<ISense> senses, int depth){
		if (depth == 0) return;
		Statement stmt = null;
		ResultSet rs;
		try {
			stmt = c.createStatement();
			rs = stmt.executeQuery("SELECT trg_sense_id FROM vocabulary_sense_relations "
							+ "WHERE src_sense_id = "+senseId+" and "
							+ "vocabulary_id = (SELECT id FROM vocabularies WHERE language_code = '"+language+"');");
			while (rs.next()){
				String newid = rs.getString("trg_sense_id");
				senses.add(new DBUKCSense(newid,language));
				descendants(newid,senses,depth-1);
			}
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String getLanguage() {
		return language;
	}
	
	//additional methods
	public String getConceptID(){
		if (conceptID != null)
			return conceptID;
		Statement stmt = null;
		ResultSet rs;
		String id = null;
		try {
			stmt = c.createStatement();
			rs = stmt.executeQuery("select concept_id from vocabulary_senses vsen, vocabulary_synsets vsyn "
					+ "where vsen.id = "+this.id+" and synset_id = vsyn.id and vsen.vocabulary_id = "
							+ "(SELECT id FROM vocabularies WHERE language_code = '"+this.language+"');");
			if (rs.next()){
				id = rs.getString("concept_id");
			}
			this.conceptID = id;
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return conceptID;
	}
	
	public String getPOS(){
		if (!pos.isEmpty())
			return this.pos;
		Statement stmt = null;
		ResultSet rs;
		String pos = "";
		try {
			stmt = c.createStatement();
			//check if the lang exists
			rs = stmt.executeQuery("select pos from vocabulary_senses vsen, vocabulary_synsets vsyn "
					+ "where vsen.id = "+this.id+" and synset_id = vsyn.id and vsen.vocabulary_id = "
							+ "(SELECT id FROM vocabularies WHERE language_code = '"+this.language+"');");
			if (rs.next()){
				pos = rs.getString("pos");
			}
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return pos;
	}
	
	public String getSynsetID(){
		if (synsetID != null)
			return synsetID;
		Statement stmt = null;
		ResultSet rs;
		String synsetID=null;
		try {
			stmt = c.createStatement();
			rs = stmt.executeQuery("select synset_id from vocabulary_senses "
					+ "where id = "+this.id+" and vocabulary_id="
					+ "(SELECT id FROM vocabularies WHERE language_code ='"+this.language+"');");
			if (rs.next()){
				synsetID = rs.getString("synset_id");
			}
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		this.synsetID=synsetID;
		return this.synsetID;
	}
	
	//Concept-level methods
	public List<String> getConceptAncestorsIds(){
		List<String> ancestors = new ArrayList<>();
		ancestor(conceptID, ancestors);
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
	
	@Override
	public String toString(){
		return lemmas+"["+id+"]" + ": "+gloss;
	}

}
