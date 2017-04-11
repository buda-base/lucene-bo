package io.bdrc.lucene.bo;

import static org.junit.Assert.*;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Unit test for simple App.
 */
public class AppTest 
{
	@BeforeClass
	public static void init() {
	    System.out.println("before the test sequence");
	}
	
	@Test
    public void test1()
    {
		System.out.println("test 1");
		assertTrue(false);
    }
	
	@Test
    public void test2()
    {
		System.out.println("test 2");
		assertFalse(false);
    }
	
	@AfterClass
	public static void finish() {
	    System.out.println("after the test sequence");
	}
	
}
