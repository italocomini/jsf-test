/**
 * 
 */
package org.jboss.test.faces;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;
import junit.framework.TestResult;

/**
 * @author asmirnov
 * @author Nick Belaevski
 */
public abstract class AbstractThreadedTest extends TestCase {

    /** 
     * The threads that are executing.
     */
    private Thread threads[] = null;
    /**
     * The tests TestResult.*/
    private TestResult testResult = null;

    public void interruptThreads() {
        if(threads != null) {
            for(int i = 0;i < threads.length;i++) {
                threads[i].interrupt();
            }
        }
    }

    /**
     * Override run so we can squirrel away the test result.
     * 
     */
    @Override
    public void run(final TestResult result) {
        testResult = result;
        super.run(result);
        testResult = null;
    }

    /**
     * Create instances of classes and run threads with it.
     * @param clazz - class of test thread implementation.
     * @param numThreads - number of threads to run.
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    protected void runTestCaseThreads(Class<?> clazz, int numThreads)  {
    	TestCaseRunnable[] runnables = new TestCaseRunnable[numThreads];
    	for (int i = 0; i < runnables.length; i++) {
			try {
				runnables[i]= (TestCaseRunnable) clazz.newInstance();
			} catch (Exception e) {
				testResult.addError(this, e);
				return;
			}
		}
    	runTestCaseRunnables(runnables);
    }
    /**
     * Run the test case threads.
     * @param runnables - array with instances of {@link TestCaseRunnable} with concrete tests
     */
    protected void runTestCaseRunnables (final TestCaseRunnable[] runnables) {
        if(runnables == null) {
            throw new IllegalArgumentException("runnables is null");
        }
        threads = new Thread[runnables.length];
        for(int i = 0;i < threads.length;i++) {
            threads[i] = new Thread(runnables[i]);
        }
        for(int i = 0;i < threads.length;i++) {
            threads[i].start();
        }
        try {
            for(int i = 0;i < threads.length;i++) {
                threads[i].join();
            }
        }
        catch(InterruptedException ignore) {
            System.out.println("Thread join interrupted.");
        }
        threads = null;
    }
    
    /**
     * Handle an exception. Since multiple threads won't have their
     * exceptions caught the threads must manually catch them and call
     * <code>handleException ()</code>.
     * @param t Exception to handle.*/
    private void handleException(final Throwable t) {
        synchronized(testResult) {
            if(t instanceof AssertionFailedError) {
                testResult.addFailure(this, (AssertionFailedError)t);
            }
            else {
                testResult.addError(this, t);
            }
        }
    }
    
    /**
     * A test case thread. Override runTestCase () and define
     * behaviour of test in there.*/
    public abstract class TestCaseRunnable implements Runnable {
        /**
         * Override this to define the test*/
        
        public abstract void runTestCase()
                              throws Throwable;
        /**
         * Run the test in an environment where
         * we can handle the exceptions generated by the test method.*/
        
        public void run() {
            try {
                runTestCase();
            }
            catch(Throwable t) /* Any other exception we handle and then we interrupt the other threads.*/ {
                handleException(t);
                interruptThreads();
            }
        }
    }

}