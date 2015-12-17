package dbukc;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import it.unitn.disi.smatch.data.ling.ISense;
import it.unitn.disi.smatch.data.mappings.IMappingElement;
import it.unitn.disi.smatch.data.trees.IContext;
import it.unitn.disi.smatch.data.trees.Context;
import it.unitn.disi.smatch.data.trees.INode;
import it.unitn.disi.smatch.oracles.LinguisticOracleException;
import it.unitn.disi.smatch.oracles.SenseMatcherException;
import it.unitn.disi.smatch.oracles.dbukc.DBUKC;

import org.hamcrest.core.IsEqual;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class DBUKC_lemmaTest {

	DBUKC ukc;
	
	@Before
	public void init(){
		ApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:**/smatch-context.xml");
		ukc = (DBUKC) applicationContext.getBean("ukcService");
	}
	@Test
	public void testIsEqual() throws LinguisticOracleException {
		assertEquals(true, ukc.isEqual("step", "step", "en"));
		assertEquals(false,ukc.isEqual("stopped", "jumping", "en"));
		assertEquals(true, ukc.isEqual("saltaba", "saltaban", "es"));
		assertEquals(false,ukc.isEqual("saltaba", "brincaba", "es"));
	}

	@Test
	public void testGetSenses() throws LinguisticOracleException {
		List<ISense> sensesEN = ukc.getSenses("derogate", "en");
		assertEquals(true, sensesEN.size() == 1 && sensesEN.get(0).getId().equals("168682"));
		List<ISense> sensesES = ukc.getSenses("apelar", "es");
		assertEquals(true, sensesES.size() == 1 && sensesES.get(0).getId().equals("259890"));
	}

	@Test
	public void testGetBaseForms() throws LinguisticOracleException {
		List<String> lemmasEN=ukc.getBaseForms("catting", "en");
		List<String> trueLemmasEN = Arrays.asList("cat");
		assertEquals(true, lemmasEN.containsAll(trueLemmasEN) && lemmasEN.size() == trueLemmasEN.size());
		List<String> lemmasES=ukc.getBaseForms("paradas", "es");
		List<String> trueLemmasES = Arrays.asList("parado", "parada", "parar");
		assertEquals(true, lemmasES.containsAll(trueLemmasES) && lemmasES.size() == trueLemmasES.size());
	}

	@Test
	public void testCreateSense() throws LinguisticOracleException {
		ISense senseEN = ukc.createSense("168682", "en");
		assertEquals(true, senseEN!=null && senseEN.getId()=="168682");
		ISense senseES = ukc.createSense("259890", "es");
		assertEquals(true, senseES!=null && senseES.getId()=="259890");
	}

	@Test
	public void testGetMultiwords() throws LinguisticOracleException {
		ArrayList<ArrayList<String>> mwEN=ukc.getMultiwords("strange", "en");
//		ArrayList<ArrayList<String>> trueMwEN = new ArrayList<>();
//		trueMwEN.add(new ArrayList<String>(Arrays.asList("strange", "attractor")));
		assertEquals(true, mwEN.get(0).containsAll(Arrays.asList("strange", "attractor")) && mwEN.size()==1);
		ArrayList<ArrayList<String>> mwES=ukc.getMultiwords("a", "es");
		assertEquals(true, mwES.get(0).containsAll(Arrays.asList("a", "capella")) && mwES.size()==1);
	}

//	@Test
//	public void testReadMultiwords() {
//		fail("Not yet implemented");
//	}

	@Test
	public void testGetRelation() throws SenseMatcherException, LinguisticOracleException {
		assertEquals(IMappingElement.EQUIVALENCE,
				ukc.getRelation(ukc.getSenses("animal", "en"), ukc.getSenses("beast", "en")));
		assertEquals(IMappingElement.EQUIVALENCE,
				ukc.getRelation(ukc.getSenses("animal", "es"), ukc.getSenses("bestia", "es")));
		
		assertEquals(IMappingElement.MORE_GENERAL,
				ukc.getRelation(ukc.getSenses("feline", "en"), ukc.getSenses("cat", "en")));
		assertEquals(IMappingElement.MORE_GENERAL,
				ukc.getRelation(ukc.getSenses("felino", "es"), ukc.getSenses("gato", "es")));
		
		assertEquals(IMappingElement.LESS_GENERAL,
				ukc.getRelation(ukc.getSenses("dog", "en"), ukc.getSenses("canine", "en")));
		assertEquals(IMappingElement.LESS_GENERAL,
				ukc.getRelation(ukc.getSenses("perro", "es"), ukc.getSenses("canino", "es")));
		
		assertEquals(IMappingElement.DISJOINT,
				ukc.getRelation(ukc.getSenses("brother", "en"), ukc.getSenses("sister", "en")));
		assertEquals(IMappingElement.DISJOINT,
				ukc.getRelation(ukc.getSenses("vivir", "es"), ukc.getSenses("morir", "es")));
		
	}

	@Test
	public void testIsSourceLessGeneralThanTarget() throws SenseMatcherException, LinguisticOracleException {
		assertEquals(IMappingElement.LESS_GENERAL,
				ukc.getRelation(ukc.getSenses("dog", "en"), ukc.getSenses("canine", "en")));
		assertEquals(IMappingElement.LESS_GENERAL,
				ukc.getRelation(ukc.getSenses("perro", "es"), ukc.getSenses("canino", "es")));
	}

	@Test
	public void testIsSourceMoreGeneralThanTarget() throws SenseMatcherException, LinguisticOracleException {
		assertEquals(IMappingElement.MORE_GENERAL,
				ukc.getRelation(ukc.getSenses("feline", "en"), ukc.getSenses("cat", "en")));
		assertEquals(IMappingElement.MORE_GENERAL,
				ukc.getRelation(ukc.getSenses("felino", "es"), ukc.getSenses("gato", "es")));
	}

	@Test
	public void testIsSourceOppositeToTarget() throws SenseMatcherException, LinguisticOracleException {
		assertEquals(IMappingElement.DISJOINT,
				ukc.getRelation(ukc.getSenses("brother", "en"), ukc.getSenses("sister", "en")));
		assertEquals(IMappingElement.DISJOINT,
				ukc.getRelation(ukc.getSenses("vivir", "es"), ukc.getSenses("morir", "es")));
	}

	@Test
	public void testIsSourceSynonymTarget() throws SenseMatcherException, LinguisticOracleException {
		assertEquals(IMappingElement.EQUIVALENCE,
				ukc.getRelation(ukc.getSenses("animal", "en"), ukc.getSenses("beast", "en")));
		assertEquals(IMappingElement.EQUIVALENCE,
				ukc.getRelation(ukc.getSenses("animal", "es"), ukc.getSenses("bestia", "es")));
	}
	
	@Test
	public void testDetectLanguage(){
		IContext t = new Context();
		INode root = t.createRoot("Course");
		INode node = root.createChild("College of Arts and Sciences");
        node.createChild("English");
        node = root.createChild("College Engineering");
        node.createChild("Civil and Environmental Engineering");
        assertEquals("en",ukc.detectLanguage(t));
        t = new Context();
        root = t.createRoot("Curso");
		node = root.createChild("Escuela de Artes y Ciencias");
        node.createChild("Inglés");
        node = root.createChild("Escuela de Ingeniería");
        node.createChild("Ingeniería Civil y Ambiental");
        assertEquals("es",ukc.detectLanguage(t));
	}

}
