/**
 * <p>Provides classes that implement a test framework for Java programs.</p>
 * <p>This framework is aimed at testing software like embedded systems, server daemons, or other software that has elements that are not directly
 * under the control of an operator.  These types of applications cannot be tested by an operator, because the operator (if there even <i>is</i> an
 * operator!) has no available mechanism to exercise all the possible code paths.</p>
 * <p>To address such scenarios, this framework implements a few simple ideas:</p>
 * <ul>
 *     <li><b>Permanently embedded test software</b> - snippets of code that, when enabled, cause some code path to be traversed, or some logic in
 *     the code to be exercised.  For example, one such snippet might throw an exception that emulates a communications failure, with the objective of
 *     testing that the system reacts appropriately.  Another might add noise to a sensor reading to test how well a filter rejects such noise.
 *     These are good examples of the kind of testing that an operator cannot do.  </li>
 *     <li><b>Test enablers</b> - these are deceptively simple: just a method that can be called to determine whether a snippet of embedded test
 *     software should be run.  For instance, consider a snippet of test code that throws an exception inside of a communication method, emulating
 *     loss of network connectivity.  In normal operation one would <i>never</i> want that snippet to run - so the test enabler, in production,
 *     always returns false (disabled).  In a test scenario you also likely would not want that exception to be throws every time the communication
 *     method was run.  Instead, you might want to wait until communications were established and had run for a minute, and then you might want
 *     exactly one exception thrown, at some random time.  The test enablers in this package contain many flexible (and programmable) ways to do
 *     things like this.</li>
 *     <li><b>Test properties</b> - configurable properties that test software can access, to precisely control the behavior of the test code.  For
 *     example, you might have some test code that (when enabled) addes noise to a sensor reading.  How <i>much</i> noise gets added could be
 *     controlled by a test property, making it easy to change (in a configuration file) without having to rebuild or redeploy the software.</li>
 * </ul>
 * <p>This framework provides a variety of {@link com.dilatush.util.test.TestEnabler} implementations.  Each of these has a
 * {@link com.dilatush.util.test.TestEnabler#isEnabled()} method that provides the "enabled" test for a particular code snippet.  Exactly how and
 * when this method returns <code>true</code> is completely configurable via a configuration file.  If a particular class had five different test
 * code snippets, there would be five corresponding test enablers instantiated to control when those snippets ran.  Generally these test enablers
 * are created when the class embedding them is instantiated.  Each of these test enablers also has a configurable set of named properties, along
 * with a convenient set of getters (see, for example, {@link com.dilatush.util.test.TestEnabler#getAsDouble(java.lang.String)}).</p>
 * <p>The class {@link com.dilatush.util.test.TestTest}, along with the Java configuration file <code>TestTest.java</code>, provide some simple
 * examples of how one might use the elements of this package.</p>
 * <p>The {@link com.dilatush.util.test.TestManager} provides the means to manage this test process at runtime.  One facility it provides is
 * particularly useful: test "scenarios".  A test scenario is a collection of test enabler configurations, for any number of test enablers.  A
 * given named scenario can be loaded and started at runtime.  Later, under program control, a different scenario can be loaded and run.  This
 * facility allows one to run potentially many different tests without having to reload or restart the software under test.</p>
 * <p>There are two quite special {@link com.dilatush.util.test.TestEnabler} implementations that should be explored to get maximum value from this
 * package:</p>
 * <ul>
 *     <li>{@link com.dilatush.util.test.CompositeTestEnabler} - allows composing multiple {@link com.dilatush.util.test.TestEnabler}s together such
 *     that the overall result of {@link com.dilatush.util.test.CompositeTestEnabler#isEnabled()} is the logical "and" of all the
 *     {@link com.dilatush.util.test.TestEnabler}s it contains.  This is quite a powerful and flexible way to control when a particular snippet of
 *     test code runs.  For instance, one could configure such a test enabler to wait for 90 seconds after the application starts, then on each
 *     subsequent {@link com.dilatush.util.test.CompositeTestEnabler#isEnabled()} call return enabled with a 3.5% probability, but only one time.
 *     This has the effect of running the test code snippet exactly one time, at some random time at least 90 seconds after the application starts.</li>
 * </ul>
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
package com.dilatush.util.test;
