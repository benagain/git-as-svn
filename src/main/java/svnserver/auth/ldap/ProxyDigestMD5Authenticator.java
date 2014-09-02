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
import svnserver.auth.User;
import svnserver.config.LDAPUserDBConfig;
import svnserver.parser.SvnServerParser;
import svnserver.parser.SvnServerWriter;

import javax.naming.Context;
import java.io.IOException;
import java.util.Hashtable;

/**
 * @author Marat Radchenko <marat@slonopotamus.org>
 */
public final class ProxyDigestMD5Authenticator implements Authenticator {

  @NotNull
  private final LDAPUserDBConfig config;

  public ProxyDigestMD5Authenticator(@NotNull LDAPUserDBConfig config) {
    this.config = config;
    ProxyDigestMD5SaslClientFactory.init();
  }

  @NotNull
  @Override
  public String getMethodName() {
    return ProxyDigestMD5SaslClient.realAuthMethod;
  }

  @Nullable
  @Override
  public User authenticate(@NotNull SvnServerParser parser, @NotNull SvnServerWriter writer, @NotNull String token) throws IOException, SVNException {
    final Hashtable<String, Object> env = new Hashtable<>();
    env.put(SvnServerParser.class.getName(), parser);
    env.put(SvnServerWriter.class.getName(), writer);
    env.put(Context.SECURITY_AUTHENTICATION, ProxyDigestMD5SaslClientFactory.proxyAuthMethod);
    return LDAPHelper.bind(config, env);
  }
}
