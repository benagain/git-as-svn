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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import svnserver.auth.User;
import svnserver.config.LDAPUserDBConfig;

import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.Control;
import javax.naming.ldap.InitialLdapContext;
import java.text.MessageFormat;
import java.util.Hashtable;

/**
 * @author Marat Radchenko <marat@slonopotamus.org>
 */
public final class LDAPHelper {

  @NotNull
  private static final Logger log = LoggerFactory.getLogger(LDAPHelper.class);

  @NotNull
  private static final Control[] emptyControls = {};

  @Nullable
  public static User bind(@NotNull LDAPUserDBConfig config, @NotNull Hashtable<String, Object> env) throws SVNException {
    env.put(Context.INITIAL_CONTEXT_FACTORY, config.getContextFactory());
    env.put(Context.PROVIDER_URL, config.getConnectionUrl());

    InitialLdapContext context = null;
    try {
      context = new InitialLdapContext(env, emptyControls);

      final WhoAmIResponse whoAmI = (WhoAmIResponse) context.extendedOperation(new WhoAmIRequest());
      final String username = whoAmI.getUserId();

      if (username == null) {
        // TODO: whoAmI.getDn()
        throw new UnsupportedOperationException();
      }

      final SearchControls searchControls = new SearchControls();
      searchControls.setSearchScope(config.isUserSubtree() ? SearchControls.SUBTREE_SCOPE : SearchControls.ONELEVEL_SCOPE);
      searchControls.setReturningAttributes(new String[]{config.getNameAttribute(), config.getEmailAttribute()});
      searchControls.setCountLimit(2);

      final NamingEnumeration<SearchResult> search = context.search("", MessageFormat.format(config.getUserSearch(), username), searchControls);
      if (!search.hasMore()) {
        log.info("Failed to find LDAP entry for {}", username);
        return null;
      }

      final Attributes attributes = search.next().getAttributes();

      if (search.hasMore()) {
        log.error("Multiple LDAP entries found for {}", username);
        return null;
      }

      final String realName = String.valueOf(attributes.get(config.getNameAttribute()).get());
      final String email = String.valueOf(attributes.get(config.getEmailAttribute()).get());

      return new User(username, realName, email);
    } catch (AuthenticationException e) {
      return null;
    } catch (NamingException e) {
      throw new SVNException(SVNErrorMessage.create(SVNErrorCode.AUTHN_NO_PROVIDER, e.getMessage()), e);
    } finally {
      if (context != null)
        try {
          context.close();
        } catch (NamingException e) {
          log.error(e.getMessage(), e);
        }
    }
  }

}
