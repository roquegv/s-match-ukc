package it.unitn.disi.smatch.oracles.ukc;

import it.unitn.disi.smatch.oracles.ILinguisticOracle;
import it.unitn.disi.smatch.oracles.LinguisticOracleException;
import it.unitn.disi.sweb.core.common.utils.ContextLoader;
import it.unitn.disi.sweb.core.kb.IConceptService;
import it.unitn.disi.sweb.core.kb.IKnowledgeBaseService;
import it.unitn.disi.sweb.core.kb.IVocabularyService;
import it.unitn.disi.sweb.core.kb.model.KnowledgeBase;
import it.unitn.disi.sweb.core.kb.model.vocabularies.Vocabulary;
import it.unitn.disi.sweb.core.kb.model.vocabularies.Word;
import it.unitn.disi.sweb.core.nlp.components.lemmatizers.ILemmatizer;
import it.unitn.disi.sweb.core.nlp.parameters.NLPParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Created by Ahmed on 6/12/14.
 */
@Component
public class Main implements IMain{
    @Autowired
    private IKnowledgeBaseService knowledgeBaseService;

    @Autowired
    private IConceptService conceptservice;

    @Autowired
    private IVocabularyService vocabularyservice;

    @Autowired
    @Qualifier("Lemmatizer")
    private ILemmatizer<NLPParameters> lemmatizer;

/*    @Autowired
    @Qualifier("LemmatizationPipeline")
    private INLPPipeline<NLPParameters> lemmatizationPipeline;*/

    public static void main(String[] args) throws LinguisticOracleException {
        ContextLoader cl = new ContextLoader("classpath:/META-INF/smatch-context.xml");
        cl.getApplicationContext().getBean(IMain.class).doSomething();

        //System.out.println(linguisticOracle.getSenses("eat", "en").size());
    }

    @Transactional
    public void doSomething()  throws LinguisticOracleException{
        /*
        HashMap<String, ArrayList<ArrayList<String>>> languageMultiwords = new HashMap<String, ArrayList<ArrayList<String>>>();
        ArrayList<String> multiwordEnd = new ArrayList<String>();
        ArrayList<String> multiwordEndBelow = new ArrayList<String>();
        KnowledgeBase kb = knowledgeBaseService.readKnowledgeBase("uk");
        Vocabulary voc = vocabularyservice.readVocabulary(kb,"en");
        List<Word> multiwordsList = vocabularyservice.readMultiWords(voc);

        for(int i = 0; i < multiwordsList.size(); i++)
        {
            ArrayList<ArrayList<String>> temp = new ArrayList<ArrayList<String>>();
            String [] tokens = multiwordsList.get(i).getLemma().split(" ");
            multiwordEnd = new ArrayList<String>(Arrays.asList(tokens));
            multiwordEnd.remove(0);
            temp.add(multiwordEnd);

            for (int j = i + 1; j < multiwordsList.size(); j++)
            {
                String [] tokens2 = multiwordsList.get(j).getLemma().split(" ");

                if(tokens[0].equals(tokens2[0]))
                {
                    //System.out.println(tokens[0] + " " + tokens2[0]);

                    multiwordEndBelow = new ArrayList<String>(Arrays.asList(tokens2));
                    multiwordEndBelow.remove(0);
                    temp.add(multiwordEndBelow);

                }
            }
            languageMultiwords.put(tokens[0],temp);

            for(int ii = 0; ii < languageMultiwords.get(tokens[0]).size(); ii ++)
            {
                System.out.println();
                for (int j = 0; j < languageMultiwords.get(tokens[0]).get(ii).size(); j++)
                {
                    System.out.print(" " + languageMultiwords.get(tokens[0]).get(ii).get(j).toString());
                }
            }
        }
        */
/*        List<String> lemmas = new ArrayList<String>();

         Map<String,Set<String>> alllemmas = lemmatizer.lemmatize("corsi", "it");
         for(String key : alllemmas.keySet())
         {
             lemmas.addAll(alllemmas.get(key));
         }
         alllemmas.clear();

        System.out.print(lemmas);
        */
    }
}
