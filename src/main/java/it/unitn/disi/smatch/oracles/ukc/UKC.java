package it.unitn.disi.smatch.oracles.ukc;

import it.unitn.disi.common.DISIException;
import it.unitn.disi.common.utils.MiscUtils;
import it.unitn.disi.smatch.SMatchException;
import it.unitn.disi.smatch.data.ling.ISense;
import it.unitn.disi.smatch.data.mappings.IMappingElement;
import it.unitn.disi.smatch.data.trees.IContext;
import it.unitn.disi.smatch.oracles.ILinguisticOracle;
import it.unitn.disi.smatch.oracles.ISenseMatcher;
import it.unitn.disi.smatch.oracles.LinguisticOracleException;
import it.unitn.disi.smatch.oracles.SenseMatcherException;
import it.unitn.disi.sweb.core.common.utils.ContextLoader;
import it.unitn.disi.sweb.core.kb.IConceptService;
import it.unitn.disi.sweb.core.kb.IKnowledgeBaseService;
import it.unitn.disi.sweb.core.kb.IVocabularyService;
import it.unitn.disi.sweb.core.kb.model.KnowledgeBase;
import it.unitn.disi.sweb.core.kb.model.vocabularies.*;
import it.unitn.disi.sweb.core.kb.model.vocabularies.Synset;
import it.unitn.disi.sweb.core.kb.model.vocabularies.Word;
import net.sf.extjwnl.JWNLException;
import net.sf.extjwnl.data.*;
import net.sf.extjwnl.data.list.PointerTargetNode;
import net.sf.extjwnl.data.list.PointerTargetNodeList;
import net.sf.extjwnl.data.list.PointerTargetTree;
import net.sf.extjwnl.data.relationship.AsymmetricRelationship;
import net.sf.extjwnl.data.relationship.RelationshipFinder;
import net.sf.extjwnl.data.relationship.RelationshipList;
import net.sf.extjwnl.dictionary.Dictionary;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by Ahmed on 6/3/14.
 */

public class UKC implements ILinguisticOracle,ISenseMatcher {

    private static IUKCService ukcService;
    private ContextLoader cl;

    HashMap<String, HashMap<String, ArrayList<ArrayList<String>>>> multiwords = new HashMap<String, HashMap<String, ArrayList<ArrayList<String>>>>();

    public UKC()
    {
        //cl = new ContextLoader("classpath:/META-INF/smatch-context.xml");
        //ukcService = cl.getApplicationContext().getBean(IUKCService.class);
    }

    @Override
    public boolean isEqual(String str1, String str2, String language) throws LinguisticOracleException {
        boolean Flag = false;
        try {
            Flag = ukcService.isEqual(str1, str2, language);
        } catch (Exception e) {
            throw new LinguisticOracleException(e.getClass().getSimpleName() + ": " + e.getMessage(), e);
        }

        return Flag;
    }

    @Override
    public List<ISense> getSenses(String word, String language) throws LinguisticOracleException {
        List<ISense> result;
        try {
            result = ukcService.getSenses(word, language);
            //KnowledgeBase o = knowledgeBaseService.readKnowledgeBase("uk");
            //Vocabulary vocabulary = vocabularyService.readVocabulary(o, "en");
            //List<String> lemmas = vocabularyService.readLemmas(vocabularyService.readSynset(vocabulary, conceptService.readConcept(o, word)), false);
            //if (null != lemmas && 0 < lemmas.size()) {
                //Looping on all words in lemmas
                //for (int i = 0; i < lemmas.toArray().length; i++) {
                      //Word lemma = vocabularyService.readWord(vocabulary, (String) lemmas.toArray()[i]);
                    //for (int j = 0; j < lemma.getSynsets().size(); j++) {
                        //Synset synset = lemma.getSynsets().get(j);

                        //result.add(new UKCSense(ukcService, 5 ,7));
                    //}
                //}
            //}
        } catch (Exception e) {
            throw new LinguisticOracleException(e.getClass().getSimpleName() + ": " + e.getMessage(), e);
        }
        return result;
    }

    @Override
    public List<String> getBaseForms(String derivation,String language) throws LinguisticOracleException {
        List<String> result;
        try {
            result = ukcService.getBaseForms(derivation, language);
        } catch (Exception e) {
            throw new LinguisticOracleException(e.getClass().getSimpleName() + ": " + e.getMessage(), e);
        }
        return result;
    }

    @Override
    public ISense createSense(String id, String language) throws LinguisticOracleException {
        try {
            return ukcService.createSense(id, language);
        } catch (Exception e) {
            throw new LinguisticOracleException(e.getClass().getSimpleName() + ": " + e.getMessage(), e);
        }
    }

    @Override
    public ArrayList<ArrayList<String>> getMultiwords(String beginning, String language) throws LinguisticOracleException {
        return multiwords.get(language).get(beginning);
    }

    public void loadContextLoader() {
        cl = new ContextLoader("classpath:/META-INF/smatch-context.xml");
        ukcService = cl.getApplicationContext().getBean(IUKCService.class);
    }

    @Override
    public String detectLanguage(IContext context) {
        return ukcService.detectLanguage(context);
    }

    @Override
    public void readMultiwords(String language) {
        if(! multiwords.containsKey(language))
        {
            HashMap<String, ArrayList<ArrayList<String>>> MultiWordsEnd = ukcService.readMultiwords(language);
            multiwords.put(language, MultiWordsEnd);
        }
    }

    @Override
    public char getRelation(List<ISense> sourceSenses, List<ISense> targetSenses) throws SenseMatcherException {
        return ukcService.getRelation(sourceSenses,targetSenses);
    }

    @Override
    public boolean isSourceMoreGeneralThanTarget(ISense source, ISense target) throws SenseMatcherException {
        return ukcService.isSourceMoreGeneralThanTarget(source,target);
    }

    @Override
    public boolean isSourceLessGeneralThanTarget(ISense source, ISense target) throws SenseMatcherException {
        return ukcService.isSourceLessGeneralThanTarget(source, target);
    }

    @Override
    public boolean isSourceSynonymTarget(ISense source, ISense target) throws SenseMatcherException {
        return ukcService.isSourceSynonymTarget(source, target);
    }

    @Override
    public boolean isSourceOppositeToTarget(ISense source, ISense target) throws SenseMatcherException {
        return ukcService.isSourceOppositeToTarget(source, target);
    }
}
