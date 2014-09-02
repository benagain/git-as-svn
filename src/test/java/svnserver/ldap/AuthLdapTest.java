/**
 * This file is part of git-as-svn. It is subject to the license terms
 * in the LICENSE file found in the top-level directory of this distribution
 * and at http://www.gnu.org/licenses/gpl-2.0.html. No part of git-as-svn,
 * including this file, may be copied, modified, propagated, or distributed
 * except according to the terms contained in the LICENSE file.
 */
package svnserver.ldap;

import org.jetbrains.annotations.NotNull;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.tmatesoft.svn.core.SVNAuthenticationException;
import svnserver.SvnTestServer;
import svnserver.config.LDAPUserDBConfig;

import java.util.Arrays;

/**
 * LDAP authentication test.
 *
 * @author Artem V. Navrotskiy (bozaro at buzzsoft.ru)
 */
public class AuthLdapTest {

  @Test(dataProvider = "authModes")
  public void validUser(@NotNull LDAPUserDBConfig.AuthMode authMode) throws Throwable {
    checkUser(authMode, "ldapadmin", "ldapadmin");
  }

  @Test(expectedExceptions = SVNAuthenticationException.class, dataProvider = "authModes")
  public void invalidPassword(@NotNull LDAPUserDBConfig.AuthMode authMode) throws Throwable {
    checkUser(authMode, "ldapadmin", "ldapadmin2");
  }

  @Test(expectedExceptions = SVNAuthenticationException.class, dataProvider = "authModes")
  public void invalidUser(@NotNull LDAPUserDBConfig.AuthMode authMode) throws Throwable {
    checkUser(authMode, "ldapadmin2", "ldapadmin");
  }

  private void checkUser(@NotNull LDAPUserDBConfig.AuthMode authMode, @NotNull String login, @NotNull String password) throws Throwable {
    try (
        EmbeddedDirectoryServer ldap = EmbeddedDirectoryServer.create();
        SvnTestServer server = SvnTestServer.createEmpty(ldap.createUserConfig(authMode))
    ) {
      server.openSvnRepository(login, password).getLatestRevision();
    }
  }

  @NotNull
  @DataProvider
  public static Object[][] authModes() {
    return Arrays.stream(LDAPUserDBConfig.AuthMode.values()).map(authMode -> new Object[]{authMode}).toArray(Object[][]::new);
  }
}
