package it.unitn.disi.smatch.oracles.ukc;

import it.unitn.disi.smatch.data.ling.ISense;
import it.unitn.disi.smatch.data.ling.Sense;
import it.unitn.disi.smatch.oracles.LinguisticOracleException;
import it.unitn.disi.sweb.core.kb.model.vocabularies.PartOfSpeech;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Ahmed on 6/6/14.
 */
public class UKCSense extends Sense {

    private long conceptid;
    private IUKCService component;

     /**
     * Constructs an instance linked to a synset.
     *
     * //@param concept concept
     */
    public UKCSense(long conceptid,long synsetid, String language, IUKCService component) {
        super(synsetid, language);
        this.conceptid = conceptid;
        this.component = component;
    }

    public String getGloss(UKCService component, String language) {

        return component.getGloss(conceptid, language);
        //return concept.getSynset(vocabulary).getGloss();
    }

    public List<String> getLemmas() {
        List<String> out = new ArrayList<String>();
        out = component.getlemmas(id);
//        for (int i = 0; i < concept.getSynset(vocabulary).getWords().size(); i++) {
//            out.add(concept.getSynset(vocabulary).getWords().get(i).getLemma());
//        }

        return out;
    }

    public List<ISense> getParents() throws LinguisticOracleException {
        return getParents(1);
    }

    public List<ISense> getParents(int depth) throws LinguisticOracleException {
        List<ISense> out = new ArrayList<ISense>();
        out = component.getParents(conceptid, language, depth);
        return out;
    }

    public List<ISense> getChildren() throws LinguisticOracleException {
        return getChildren(1);
    }

    public List<ISense> getChildren(int depth) throws LinguisticOracleException {
        List<ISense> out = new ArrayList<ISense>();
        out = component.getChildren(conceptid, language, depth);
        return out;
    }

    public PartOfSpeech getPOS() {
        //return component.
        //return concept.getSynset(vocabulary).getPartOfSpeech();
        return component.getPOS(id);
    }

    /*public long getOffset() {
        return synset.getOffset();
    }*/

    public String getlanguage()
    {
        return language;
    }

    public long getConceptID() {
        return conceptid;
    }

    public long getSynsetID() {
        return id;
    }
}
