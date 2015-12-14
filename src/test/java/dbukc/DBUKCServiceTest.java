package dbukc;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import it.unitn.disi.smatch.data.ling.ISense;
import it.unitn.disi.smatch.data.mappings.IMappingElement;
import it.unitn.disi.smatch.data.trees.Context;
import it.unitn.disi.smatch.data.trees.IContext;
import it.unitn.disi.smatch.data.trees.INode;
import it.unitn.disi.smatch.oracles.LinguisticOracleException;
import it.unitn.disi.smatch.oracles.SenseMatcherException;
import it.unitn.disi.smatch.oracles.dbukc.DBUKCSense;
import it.unitn.disi.smatch.oracles.dbukc.DBUKCService;
import it.unitn.disi.smatch.oracles.dbukc.PartOfSpeech;

import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class DBUKCServiceTest {

	DBUKCService ukcService;
	
	@Before
	public void init(){
		ApplicationContext applicationContext = 
				new ClassPathXmlApplicationContext("classpath:/META-INF/smatch-context.xml");
		ukcService = (DBUKCService)applicationContext.getBean("ukcService");
	}
	
	@Test
	public void testGetGloss() {
		String gloss="social or verbal interchange (usually followed by `with')";
		assertEquals(gloss, ukcService.getGloss(123, "en"));
		gloss="reir silenciosamente";
		assertEquals(gloss, ukcService.getGloss(236622, "es"));
	}

	@Test
	public void testGetSenses() {
		List<ISense> sensesEN = ukcService.getSenses("derogate", "en");
		assertEquals(true, sensesEN.size() == 1 && ((DBUKCSense)sensesEN.get(0)).getConceptID() == 96765L);
		List<ISense> sensesES = ukcService.getSenses("apelar", "es");
		assertEquals(true, sensesES.size() == 1 && ((DBUKCSense)sensesES.get(0)).getConceptID() == 104952L);
	}

	@Test
	public void testGetBaseForms() {
		List<String> lemmasEN=ukcService.getBaseForms("catting", "en");
		List<String> trueLemmasEN = Arrays.asList("cat");
		assertEquals(true, lemmasEN.containsAll(trueLemmasEN) && lemmasEN.size() == trueLemmasEN.size());
		//there is no word forms from the ukc database
		List<String> lemmasES=ukcService.getBaseForms("paradas", "es");
		List<String> trueLemmasES = Arrays.asList("parado", "parada", "parar");
		assertEquals(true, lemmasES.containsAll(trueLemmasES) && lemmasES.size() == trueLemmasES.size());
	}

	@Test
	public void testIsEqual() {
		assertEquals(true, ukcService.isEqual("step", "step", "en"));
		assertEquals(false,ukcService.isEqual("stopped", "jumping", "en"));
		assertEquals(true, ukcService.isEqual("saltaba", "saltaban", "es"));
		assertEquals(false,ukcService.isEqual("saltaba", "brincaba", "es"));
	}

	@Test
	public void testCreateSense() {
		ISense senseEN = ukcService.createSense("168682", "en");
		assertEquals(true, senseEN!=null && ((DBUKCSense)senseEN).getConceptID() == 96765L);
		ISense senseES = ukcService.createSense("259890", "es");
		assertEquals(true, senseES!=null && ((DBUKCSense)senseES).getConceptID() == 104952L);
	}

	@Test
	public void testGetMultiwords() {
		List<String> mwEN=ukcService.getMultiwords("en");
		assertEquals(55882,mwEN.size());
		List<String> mwES=ukcService.getMultiwords("es");
		assertEquals(10695,mwES.size());
	}

	@Test
	public void testGetRelation() {
		assertEquals(IMappingElement.EQUIVALENCE,
				ukcService.getRelation(ukcService.getSenses("animal", "en"), ukcService.getSenses("beast", "en")));
		assertEquals(IMappingElement.EQUIVALENCE,
				ukcService.getRelation(ukcService.getSenses("animal", "es"), ukcService.getSenses("bestia", "es")));
		
		assertEquals(IMappingElement.MORE_GENERAL,
				ukcService.getRelation(ukcService.getSenses("feline", "en"), ukcService.getSenses("cat", "en")));
		assertEquals(IMappingElement.MORE_GENERAL,
				ukcService.getRelation(ukcService.getSenses("felino", "es"), ukcService.getSenses("gato", "es")));
		
		assertEquals(IMappingElement.LESS_GENERAL,
				ukcService.getRelation(ukcService.getSenses("dog", "en"), ukcService.getSenses("canine", "en")));
		assertEquals(IMappingElement.LESS_GENERAL,
				ukcService.getRelation(ukcService.getSenses("perro", "es"), ukcService.getSenses("canino", "es")));
		
		assertEquals(IMappingElement.DISJOINT,
				ukcService.getRelation(ukcService.getSenses("brother", "en"), ukcService.getSenses("sister", "en")));
		assertEquals(IMappingElement.DISJOINT,
				ukcService.getRelation(ukcService.getSenses("vivir", "es"), ukcService.getSenses("morir", "es")));
	}

	@Test
	public void testIsSourceLessGeneralThanTarget() throws SenseMatcherException, LinguisticOracleException {
		assertEquals(IMappingElement.LESS_GENERAL,
				ukcService.getRelation(ukcService.getSenses("dog", "en"), ukcService.getSenses("canine", "en")));
		assertEquals(IMappingElement.LESS_GENERAL,
				ukcService.getRelation(ukcService.getSenses("perro", "es"), ukcService.getSenses("canino", "es")));
	}

	@Test
	public void testIsSourceMoreGeneralThanTarget() throws SenseMatcherException, LinguisticOracleException {
		assertEquals(IMappingElement.MORE_GENERAL,
				ukcService.getRelation(ukcService.getSenses("feline", "en"), ukcService.getSenses("cat", "en")));
		assertEquals(IMappingElement.MORE_GENERAL,
				ukcService.getRelation(ukcService.getSenses("felino", "es"), ukcService.getSenses("gato", "es")));
	}

	@Test
	public void testIsSourceOppositeToTarget() throws SenseMatcherException, LinguisticOracleException {
//		assertEquals(IMappingElement.DISJOINT,
//				ukcService.getRelation(ukcService.getSenses("brother", "en"), ukcService.getSenses("sister", "en")));
		boolean flag = false;
		for (ISense sourceSense : ukcService.getSenses("brother", "en")) {
            for (ISense targetSense : ukcService.getSenses("sister", "en")) {
                DBUKCSense sourceSyn = (DBUKCSense) sourceSense;
                DBUKCSense targetSyn = (DBUKCSense) targetSense;
                if (ukcService.isSourceOppositeToTarget(sourceSyn,targetSyn)) {
                    flag=true;
                    break;
                }
            }
            if (flag == true) break;
        }
		assertEquals(true,flag);
//		assertEquals(IMappingElement.DISJOINT,
//				ukcService.getRelation(ukcService.getSenses("vivir", "es"), ukcService.getSenses("morir", "es")));
		for (ISense sourceSense : ukcService.getSenses("vivir", "es")) {
            for (ISense targetSense : ukcService.getSenses("morir", "es")) {
                DBUKCSense sourceSyn = (DBUKCSense) sourceSense;
                DBUKCSense targetSyn = (DBUKCSense) targetSense;
                if (ukcService.isSourceOppositeToTarget(sourceSyn,targetSyn)) {
                    flag=true;
                    break;
                }
            }
            if (flag == true) break;
        }
		assertEquals(true,flag);
	}

	@Test
	public void testIsSourceSynonymTarget() throws SenseMatcherException, LinguisticOracleException {
		assertEquals(IMappingElement.EQUIVALENCE,
				ukcService.getRelation(ukcService.getSenses("animal", "en"), ukcService.getSenses("beast", "en")));
		assertEquals(IMappingElement.EQUIVALENCE,
				ukcService.getRelation(ukcService.getSenses("animal", "es"), ukcService.getSenses("bestia", "es")));
	}
	
	@Test
	public void testDetectLanguage(){
		IContext t = new Context();
		INode root = t.createRoot("Course");
		INode node = root.createChild("College of Arts and Sciences");
        node.createChild("English");
        node = root.createChild("College Engineering");
        node.createChild("Civil and Environmental Engineering");
        assertEquals("en",ukcService.detectLanguage(t));
        t = new Context();
        root = t.createRoot("Curso");
		node = root.createChild("Escuela de Artes y Ciencias");
        node.createChild("Inglés");
        node = root.createChild("Escuela de Ingeniería");
        node.createChild("Ingeniería Civil y Ambiental");
        assertEquals("es",ukcService.detectLanguage(t));
	}

	@Test
	public void testGetlemmas() {
		List<String> lemmasEN = Arrays.asList("credit","course credit");
		assertEquals(lemmasEN,ukcService.getlemmas(351));
		List<String> lemmasES = Arrays.asList("cosa","objeto","objeto físico","objeto inanimado");
		assertEquals(lemmasES,ukcService.getlemmas(236310));
	}

	@Test
	public void testGetParents() {
		List<Long> parents= Arrays.asList(1L,19L,22L);
		List<Long> actualParents = new ArrayList<>();
		for(ISense sense : ukcService.getParents(23, "en", 8)){
			actualParents.add(((DBUKCSense)sense).getConceptID());
		}
		assertEquals(true,parents.containsAll(actualParents) && parents.size() == actualParents.size());
	}

	@Test
	public void testGetChildren() {
		List<Long> children= Arrays.asList(19L,20L,24109L);
		List<Long> actualChildren = new ArrayList<>();
		for(ISense sense : ukcService.getParents(1, "en", 1)){
			actualChildren.add(((DBUKCSense)sense).getConceptID());
		}
		assertEquals(true,children.containsAll(actualChildren) && children.size() == actualChildren.size());
	}

	@Test
	public void testGetPOS() {
		assertEquals(PartOfSpeech.VERB,ukcService.getPOS(73882));//EN
		assertEquals(PartOfSpeech.ADJECTIVE,ukcService.getPOS(145539));//ES
	}

}
