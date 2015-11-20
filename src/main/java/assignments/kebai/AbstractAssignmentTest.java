package assignments.kebai;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.jackrabbit.commons.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Didn't use JUnitTest
 */
public abstract class AbstractAssignmentTest {	
	/**
     * Logger instance for test cases
     */
    protected final Logger log = LoggerFactory.getLogger(getClass());
    
    /**
     * The superuser session for the default workspace
     */
	protected Session testSession;
	
	/**
	 * Test root node.
	 */
	protected Node testRootNode;
	
    /**
     * Absolute path to the test root node.
     */
	private static final String TEST_ROOT = "test";
	
	/**
	 * Set up test session
	 * @throws RepositoryException
	 */
	protected void setUp() throws RepositoryException {
		Repository repository = JcrUtils.getRepository();
		testSession = repository.login(new SimpleCredentials("admin",
				"admin".toCharArray()));
		testRootNode = testSession.getRootNode().addNode(TEST_ROOT);
	}
	
	/**
	 * Close test session
	 * @throws RepositoryException
	 */
    protected void cleanUp() throws RepositoryException {
		if (testSession != null) {
			testRootNode.remove();
			testSession.save();
		   	testSession.logout();
		   	testSession = null;
		}
    }
}
