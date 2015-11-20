package assignments.kebai;

import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeTypeTemplate;
import javax.jcr.nodetype.PropertyDefinitionTemplate;
import javax.jcr.security.Privilege;

import org.apache.jackrabbit.api.security.user.User;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

/**
 * Set up the system to introduce a new node type of “room” (a subclass of 
 * “folder”), which has an extra property called “Security Level”, which is
 * an integer type. When security level is 0, the system behaves as normal, 
 * any document uploaded into a room can be download as normal. However if, 
 * the security level is non-zero, the download request will result in a 
 * fixed content (say: a text file with content “access denied”) for any 
 * document. Demonstrate this in the form of a unit test case.
 */
public class RoomTest extends AccessControlTest {
    /**
     * Absolute path to the test root node.
     */
	private static final String TEST_NODE = "TestRoom";
	private Integer securityLevel;

	public RoomTest (User user) {
		super(user);
		this.securityLevel = 1; // No access
	}
	
	public RoomTest (User user, Integer securityLevel) {
		super(user);
		this.securityLevel = securityLevel;
	}
	
	public Integer getSecurityLevel() {
		return securityLevel;
	}
	
	public void setSecurityLevel(Integer securityLevel) {
		this.securityLevel = securityLevel;
	}
    
    public void testAll() throws RepositoryException {
    	testRoomNormalSecurity();
    	testRoomDenySecurity();
    }
    
    /**
     * When security level is 0, the system behaves as normal.
     * Any document uploaded into a room can be download as normal.
     * @throws RepositoryException
     */
    public void testRoomNormalSecurity() throws RepositoryException {
    	try {
    		setUp();
    		User user = getUser();
            Node testRoom = testRootNode.addNode(TEST_NODE, "nt:Room");
            testRoom.setProperty("SecurityLevel", 0);
            testSession.save();
            Node testFile = createFile(testRoom.getPath().substring(1), "test.txt");
            assertTrue(testFile.equals(getRoomAccess(user, testRoom, testFile)));
            /* Create a room inside the room */
            Node subTestRoom = testRoom.addNode("Sub" + TEST_NODE, "nt:Room");
            subTestRoom.setProperty("SecurityLevel", 0);
            testSession.save();
            Node anotherFile = createFile(subTestRoom.getPath().substring(1), "testtest.txt");
            assertTrue(anotherFile.equals(getRoomAccess(user, subTestRoom, anotherFile)));
    	} catch (Exception e) {
        	e.printStackTrace();
        } finally {
        	cleanUp();
        }    	
    }

    /**
     * When the security level is non-zero, the download request will result in a
     * fixed content (say: a text file with content “access denied”) for any document
     * @throws RepositoryException
     */
    public void testRoomDenySecurity() throws RepositoryException {
    	try {
    		setUp();
    		User user = getUser();
            Node testRoom = testRootNode.addNode(TEST_NODE, "nt:Room");
            testRoom.setProperty("SecurityLevel", 1);
            testSession.save();
            Node testFile = createFile(testRoom.getPath().substring(1), "test.txt");
            assertFalse(testFile.equals(getRoomAccess(user, testRoom, testFile)));
            /* Create a room inside the room */
            Node subTestRoom = testRoom.addNode("Sub" + TEST_NODE, "nt:Room");
            subTestRoom.setProperty("SecurityLevel", 0);
            testSession.save();
            // change security level of sub room should not affect parent room
            assertFalse(testFile.equals(getRoomAccess(user, testRoom, testFile)));
            Node anotherFile = createFile(subTestRoom.getPath().substring(1), "testtest.txt");
            assertTrue(anotherFile.equals(getRoomAccess(user, subTestRoom, anotherFile)));
            subTestRoom.setProperty("SecurityLevel", 1);
            testSession.save();
            assertFalse(anotherFile.equals(getRoomAccess(user, subTestRoom, anotherFile)));
            // change security level of parent room
            testRoom.setProperty("SecurityLevel", 0);
            testSession.save();
            // actually should be assertTrue if the change in the upper Room applies to subRoom
            assertFalse(anotherFile.equals(getRoomAccess(user, subTestRoom, anotherFile)));
            testRoom.setProperty("SecurityLevel", 1);
            testSession.save();
            assertFalse(anotherFile.equals(getRoomAccess(user, subTestRoom, anotherFile)));
    	} catch (Exception e) {
        	e.printStackTrace();
        } finally {
        	cleanUp();
        }   	
    }
 
    protected void RegisterRoomNodeType(Session session) {
    	try {
	    	NodeTypeManager nodeTypeManager = session.getWorkspace().getNodeTypeManager();
	    	// Create node type
	    	NodeTypeTemplate nodeType = nodeTypeManager.createNodeTypeTemplate();
	    	nodeType.setName("nt:Room");
	    	String[] superType = {"nt:folder"};
	    	nodeType.setDeclaredSuperTypeNames(superType);
	    	nodeType.setMixin(false);
	    	nodeType.setQueryable(true);
	    	nodeType.setOrderableChildNodes(false);
	    	// Create a new property
	    	PropertyDefinitionTemplate customProperty = nodeTypeManager.createPropertyDefinitionTemplate();
	    	customProperty.setName("SecurityLevel");
	    	customProperty.setRequiredType(PropertyType.LONG);
	    	Value defaultSecurityLevel = session.getValueFactory().createValue(1);
	    	customProperty.setDefaultValues(new Value[]{ defaultSecurityLevel });
	    	// Add property to node type  	
	    	nodeType.getPropertyDefinitionTemplates().add(customProperty);
	    	// Register node type	    	
	    	nodeTypeManager.registerNodeType(nodeType, true);
	    	session.save();
    	} catch (RepositoryException e) {
    		log.error("Failed to register node type: { Room } in workspace { " + session.getWorkspace().getName() + " }");
			e.printStackTrace();
		}
    }
    
    @Override
    protected void setUp() throws RepositoryException {
    	super.setUp();
    	RegisterRoomNodeType(testSession);
    }

    /**
     * When security level is 0, the system behaves as normal. However if, 
     * the security level is non-zero, the download request will result in a 
     * fixed content (say: a text file with content “access denied”) for any 
     * document.
     * @param user
     * @param room
     * @param fileInRoom the file to be accessed in room
     * @throws RepositoryException
     */
    private Node getRoomAccess(User user, Node room, Node fileInRoom)
    		throws RepositoryException {
    	assertTrue(room.isNodeType("nt:Room"));
    	assertTrue(room.hasProperty("SecurityLevel"));
    	assertTrue(room.hasNode(fileInRoom.getName()));
    	if (room.getProperty("SecurityLevel").getLong() == 0) {
    		authorizeAccess(user, fileInRoom, Privilege.JCR_ALL);
    		assertTrue(hasAccess(user, fileInRoom));
    		return fileInRoom;
    	} else {
    		withdrawAccess(user, fileInRoom);
    		assertFalse(hasAccess(user, fileInRoom));
    		if (room.hasNode("error.txt")) return room.getNode("error.txt");
    		else return createFile(room.getPath().substring(1), "error.txt", "access denied");
    	}
    }
}
