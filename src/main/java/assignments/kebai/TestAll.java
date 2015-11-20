package assignments.kebai;

import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.commons.JcrUtils;

public class TestAll {
	public static void main(String[] args) throws Exception {
		Session s = null;
		try {
			// Create a test user 
			Repository repository = JcrUtils.getRepository();
			s = repository.login(new SimpleCredentials("anonymous",
					"anonymous".toCharArray()));
			User bob = (User) ((JackrabbitSession) s).getUserManager()
					.getAuthorizable(s.getUserID());
			
			// Run access control test
			AccessControlTest accessTest = new AccessControlTest(bob);
			accessTest.testAll();
			System.out.println("Access control test is done!");
		} finally {
			if (s != null) {
				s.logout();
			}
		} 	
	}
}
