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
import svnserver.parser.SvnServerParser;
import svnserver.parser.SvnServerWriter;

import javax.security.auth.callback.CallbackHandler;
import javax.security.sasl.SaslClient;
import javax.security.sasl.SaslClientFactory;
import javax.security.sasl.SaslException;
import java.security.Provider;
import java.security.Security;
import java.util.Map;

/**
 * @author Marat Radchenko <marat@slonopotamus.org>
 */
public final class ProxyDigestMD5SaslClientFactory implements SaslClientFactory {

  @NotNull
  static final String proxyAuthMethod = "PROXY-" + ProxyDigestMD5SaslClient.realAuthMethod;

  @NotNull
  public static final String[] emptyStrings = {};

  private static boolean dummy;

  static {
    Security.addProvider(new Provider("GIT-AS-SVN", 1, "") {
      {
        put(SaslClientFactory.class.getSimpleName() + '.' + proxyAuthMethod, ProxyDigestMD5SaslClientFactory.class.getName());
      }
    });
  }

  public static void init() {
    dummy = !dummy;
  }

  @Override
  public SaslClient createSaslClient(@NotNull String[] mechanisms,
                                     @Nullable String authorizationId,
                                     @NotNull String protocol,
                                     @NotNull String serverName,
                                     @Nullable Map<String, ?> props,
                                     @Nullable CallbackHandler cbh) throws SaslException {
    if (props == null)
      throw new SaslException();

    return new ProxyDigestMD5SaslClient(
        (SvnServerParser) props.get(SvnServerParser.class.getName()),
        (SvnServerWriter) props.get(SvnServerWriter.class.getName())
    );
  }

  @Override
  public String[] getMechanismNames(@Nullable Map<String, ?> props) {
    return emptyStrings;
  }
}
