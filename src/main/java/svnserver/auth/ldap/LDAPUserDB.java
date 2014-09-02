/**
 * This file is part of git-as-svn. It is subject to the license terms
 * in the LICENSE file found in the top-level directory of this distribution
 * and at http://www.gnu.org/licenses/gpl-2.0.html. No part of git-as-svn,
 * including this file, may be copied, modified, propagated, or distributed
 * except according to the terms contained in the LICENSE file.
 */
package svnserver.auth.ldap;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.tmatesoft.svn.core.SVNException;
import svnserver.auth.Authenticator;
import svnserver.auth.PasswordChecker;
import svnserver.auth.User;
import svnserver.auth.UserDB;
import svnserver.config.LDAPUserDBConfig;

import javax.naming.Context;
import java.util.Collection;
import java.util.Collections;
import java.util.Hashtable;

/**
 * Authenticates a user by binding to the directory with the DN of the entry for that user and the password
 * presented by the user. If this simple bind succeeds the user is considered to be authenticated.
 *
 * @author Marat Radchenko <marat@slonopotamus.org>
 */
public final class LDAPUserDB implements UserDB, PasswordChecker {

  @NotNull
  private final Collection<Authenticator> authenticators;
  @NotNull
  private final LDAPUserDBConfig config;

  public LDAPUserDB(@NotNull LDAPUserDBConfig config) {
    this.config = config;
    authenticators = Collections.singleton(config.getAuthentication().create(config, this));
  }

  @NotNull
  @Override
  public Collection<Authenticator> authenticators() {
    return authenticators;
  }

  @Nullable
  @Override
  public User check(@NotNull String username, @NotNull String password) throws SVNException {
    final Hashtable<String, Object> env = new Hashtable<>();
    env.put(Context.SECURITY_PRINCIPAL, username);
    env.put(Context.SECURITY_CREDENTIALS, password);
    env.put(Context.SECURITY_AUTHENTICATION, ProxyDigestMD5SaslClient.realAuthMethod);
    return LDAPHelper.bind(config, env);
  }
}
