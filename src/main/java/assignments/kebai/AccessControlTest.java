package assignments.kebai;

import org.apache.jackrabbit.api.security.user.User;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.AccessControlList;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.Privilege;

import java.util.NoSuchElementException;

import static org.junit.Assert.*;

/**
 * Single unit test case that shows how: 
 * (1) a user Alice can create a document, 
 * (2) grant it enough permission, so that (3) user Bob can access it.
 * But then (4) she can create a 2nd document, grant it different permissions 
 * and then (5) Bob would not be able to access it.
 */
public class AccessControlTest extends AbstractAssignmentTest {
	private User user;
	
	/**
    * Create a new instance of this class.
    * @param user User user
    */
	public AccessControlTest(User user) {
		this.user = user;
	}
	
	protected User getUser() {
		return user;
	}
   
	public void testAll() throws RepositoryException {
		testAccessWithPermission("testFile.txt");
		testAccessWithoutPermission("testFile.txt");
	}
   
	public void testAccessWithPermission(String fileName) throws RepositoryException {
		try {
			setUp();
			Node testFile = createFile(testRootNode.getPath().substring(1), fileName);
			assertFalse(hasAccess(user, testFile));
			// Authorize user "Bob" access to the node
			authorizeAccess(user, testFile, Privilege.JCR_ALL);
			assertTrue(hasAccess(user, testFile));
		} finally {
			cleanUp();
		}
	}
   
	public void testAccessWithoutPermission(String fileName) throws RepositoryException {
		try {
			setUp();
			Node testFile = createFile(testRootNode.getPath().substring(1), fileName);
			// Without permission, user Bob should not have access to the node
			assertFalse(hasAccess(user, testFile));
		} finally {
			cleanUp();
		}	   
	}
    
	/**
	 * Create test file.
	 * @param fileName file name
	 * @param fileContent file data
	 * @return a file node
	 * @throws RepositoryException
	 * @throws NoSuchNodeTypeException 
	 * @throws PathNotFoundException 
	 * @throws ItemExistsException 
	 */
	protected Node createFile(String path, String fileName, String fileContent)
			throws ItemExistsException, PathNotFoundException, 
			NoSuchNodeTypeException, RepositoryException {
		Node folder = testRootNode.getParent().getNode(path);
		Node testFile = folder.addNode(fileName, "nt:file");
		//create the mandatory child node - jcr:content
		Node resNode = testFile.addNode ("jcr:content", "nt:resource");
		resNode.setProperty ("jcr:data", fileContent);
		testSession.save();
		return testFile;
	}

	/**
	 * Create test file.
	 * @param fileName file name
	 * @return a file node
	 * @throws RepositoryException
	 * @throws NoSuchNodeTypeException 
	 * @throws PathNotFoundException 
	 * @throws ItemExistsException 
	 */
	protected Node createFile(String path, String fileName)
			throws ItemExistsException, PathNotFoundException, 
			NoSuchNodeTypeException, RepositoryException {
		Node folder = testRootNode.getParent().getNode(path);
		Node testFile = folder.addNode(fileName, "nt:file");
		//create the mandatory child node - jcr:content
		Node resNode = testFile.addNode ("jcr:content", "nt:resource");
		resNode.setProperty ("jcr:data", "It's in file " + fileName);
		testSession.save();
		return testFile;
	}
	
	/**
	 * Authorize user the access to node
	 * @param user
	 * @param node
	 */
	protected void authorizeAccess(User user, Node node, String privilegeName) {
		try {
			String path = node.getPath();
			AccessControlManager aMgr = testSession.getAccessControlManager();
			// create a privilege set with privilegeName
			Privilege[] privileges = new Privilege[] { aMgr.privilegeFromName(privilegeName) };
			AccessControlPolicy acl = aMgr.getApplicablePolicies(path).nextAccessControlPolicy();
			// add a new one for the "user" principal
			((AccessControlList) acl).addAccessControlEntry(user.getPrincipal(), privileges);
			// the policy must be re-set
			aMgr.setPolicy(path, acl);
			testSession.save();
		} catch (Exception e) {
			log.error("Failed to authorize user the access to node");
			e.printStackTrace();
		}
	}
   
	/**
	 * @param node
	 * @return access control list for node
	 * @throws PathNotFoundException 
	 * @throws AccessDeniedException 
	 * @throws RepositoryException
	 */
	private AccessControlPolicy getACL(Node node)
			throws AccessDeniedException, PathNotFoundException, RepositoryException  {
		AccessControlPolicy acl = null;
		AccessControlManager aMgr = null;
		try {
			aMgr = testSession.getAccessControlManager();
			// get first applicable policy (for nodes w/o a policy)
			acl = aMgr.getApplicablePolicies(node.getPath()).nextAccessControlPolicy();
		} catch (NoSuchElementException e) {
			// else node already has a policy, get that one
			acl = aMgr.getPolicies(node.getPath())[0];
		} catch (RepositoryException e) {
			log.error("Unable to get access control manager");
			e.printStackTrace();
		}
		return acl;
	}
   
	/**
	 * @param user
	 * @param node
	 * @return boolean to show whether user has access to node or not
	 * @throws RepositoryException 
	 * @throws PathNotFoundException 
	 * @throws AccessDeniedException 
	 */
	protected boolean hasAccess(User user, Node node)
			throws AccessDeniedException, PathNotFoundException, RepositoryException {
		AccessControlPolicy acl = getACL(node);
		for (AccessControlEntry e : ((AccessControlList) acl).getAccessControlEntries()) {
			if (e.getPrincipal().equals(user.getPrincipal())) return true;
		}
		return false;
	}

}
