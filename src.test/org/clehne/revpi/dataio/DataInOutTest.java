package org.clehne.revpi.dataio;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

public class DataInOutTest {

    private static final String[] DATA_IN = {
        "I_1",
        "I_2",
        "I_3",
        "I_4",
        "I_5",
        "I_6",
        "I_7",
        "I_8",
        "I_9",
        "I_10",
        "I_11",
        "I_12",
        "I_13",
        "I_14",
        "I_15",
        "I_16"
    };

    private static final String[] DATA_OUT = {
        "O_1",
        "O_2",
        "O_3",
        "O_4",
        "O_5",
        "O_6",
        "O_7",
        "O_8",
        "O_9",
        "O_10",
        "O_11",
        "O_12",
        "O_13",
        "O_14",
        "O_15",
        "O_16"
    };


    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    @interface Test { /* EMPTY */ }

    public static void main(String[] args) throws IOException {
	startTests();
//	miscTests();
    }
    
    private static void startTests() {
        final DataInOutTest dummy = new DataInOutTest();
        boolean succeeded = true;
        for (Method testMethod : DataInOutTest.class.getMethods()) {
            if (testMethod.getAnnotation(Test.class) != null) {
                System.out.print("Test: " + testMethod.getName());
                try {
                    testMethod.invoke(dummy);
                    System.out.println(" OK");
                } catch (final Exception e) {
                    System.out.println(" FAILED");
                    e.printStackTrace();
                    succeeded = false;
                }
            }
        }
        if (!succeeded) {
            System.out.println("unit tests went wrong".toUpperCase());
            System.exit(-1);
        }
    }
    
    @Test
    public void testDataInOutRawCreate() throws IOException {
        new DataInOut().close();
    }
    

    @Test
    public void testGetDataIn() throws IOException {
    	try (final DataInOut dio = new DataInOut()) {
    	    System.out.println("");
    	    for (int i = 1; i < 17; i++) {
        	    System.out.println("Result from getDataIn(" + DATA_IN[i-1] + "): " + dio.getDataIn( DATA_IN[i-1] ) );
			}
    	}
    }

    @Test
    public void testGetDataOut() throws IOException {
    	try (final DataInOut dio = new DataInOut()) {
    	    System.out.println("");
    	    for (int i = 1; i < 17; i++) {
        	    System.out.println("Result from getDataOut(" + DATA_OUT[i-1] + "): " + dio.getDataOut( DATA_OUT[i-1]) );
			}
    	}
    }

    @Test
    public void testSetDataOut() throws IOException {
    	try (final DataInOut dio = new DataInOut()) {

    	    System.out.println("");
    	    for (int i = 1; i < 17; i++) {
        	    System.out.println("setDataOut(" + DATA_OUT[i-1] + ", 1)");
        	    dio.setDataOut(DATA_OUT[i-1], true);
			}
    	    System.out.println("pause");
    	    try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {}
    	    for (int i = 1; i < 17; i++) {
        	    System.out.println("setDataOut(" + DATA_OUT[i-1] + ", 0)");
        	    dio.setDataOut(DATA_OUT[i-1], false);
			}
    	}
    }

    
    
}
