package it.unitn.disi.smatch.oracles.ukc;

import it.unitn.disi.smatch.data.ling.ISense;
import it.unitn.disi.smatch.data.trees.IContext;
import it.unitn.disi.sweb.core.kb.model.vocabularies.PartOfSpeech;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Ahmed on 6/24/14.
 */
public interface IUKCService {
    String getGloss(long conceptID, String language);
    List<ISense> getSenses(String word,String language);
    List<String> getBaseForms(String derivation,String language);
    boolean isEqual(String str1, String str2,String language);
    ISense createSense(String id, String language);
    List<String> getMultiwords(String language);
    char getRelation(List<ISense> sourceSenses, List<ISense> targetSenses);
    boolean isSourceMoreGeneralThanTarget(ISense source, ISense target);
    boolean isSourceLessGeneralThanTarget(ISense source, ISense target);
    boolean isSourceSynonymTarget(ISense source, ISense target);
    boolean isSourceOppositeToTarget(ISense source, ISense target);
    List<String> getlemmas(long synsetID);
    List<ISense> getParents(long conceptID, String language, int depth);
    List<ISense> getChildren(long conceptID, String language, int depth);
    PartOfSpeech getPOS(long synsetID);
    String detectLanguage(IContext context);
    HashMap<String, ArrayList<ArrayList<String>>> readMultiwords(String language);
}
