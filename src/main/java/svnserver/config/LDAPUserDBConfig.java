/**
 * This file is part of git-as-svn. It is subject to the license terms
 * in the LICENSE file found in the top-level directory of this distribution
 * and at http://www.gnu.org/licenses/gpl-2.0.html. No part of git-as-svn,
 * including this file, may be copied, modified, propagated, or distributed
 * except according to the terms contained in the LICENSE file.
 */
package svnserver.config;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import svnserver.auth.Authenticator;
import svnserver.auth.PasswordChecker;
import svnserver.auth.PlainAuthenticator;
import svnserver.auth.UserDB;
import svnserver.auth.ldap.LDAPUserDB;
import svnserver.auth.ldap.ProxyDigestMD5Authenticator;
import svnserver.config.serializer.ConfigType;

/**
 * @author Marat Radchenko <marat@slonopotamus.org>
 */
@SuppressWarnings("FieldCanBeLocal")
@ConfigType("ldapUsers")
public final class LDAPUserDBConfig implements UserDBConfig {

  @NotNull
  private static final Logger log = LoggerFactory.getLogger(LDAPUserDBConfig.class);

  /**
   * This is a URL whose format is defined by the JNDI provider.
   * It is usually an LDAP URL that specifies the domain name of the directory server to connect to,
   * and optionally the port number and distinguished name (DN) of the required root naming context.
   */
  @NotNull
  private String connectionUrl = "ldap://localhost:389/ou=groups,dc=mycompany,dc=com";
  /**
   * The JNDI context factory used to acquire our InitialContext. By
   * default, assumes use of an LDAP server using the standard JNDI LDAP
   * provider.
   */
  @NotNull
  private String contextFactory = "com.sun.jndi.ldap.LdapCtxFactory";
  /**
   * The type of authentication to use.
   */
  @NotNull
  private AuthMode authentication = AuthMode.DigestMD5;
  /**
   * The search scope. Set to <code>true</code> if you wish to search the entire subtree rooted at the
   * <code>userBase</code> entry. The default value of <code>false</code> requests a single-level search
   * including only the top level.
   */
  private boolean userSubtree;
  /**
   * Pattern specifying the LDAP search filter to use after substitution of the username.
   */
  @NotNull
  private String userSearch = "(samaccountname={0})";
  /**
   * LDAP attribute, containing user name.
   */
  @NotNull
  private String nameAttribute = "name";
  /**
   * LDAP attribute, containing user email.
   */
  @NotNull
  private String emailAttribute = "mail";

  @NotNull
  public String getConnectionUrl() {
    return connectionUrl;
  }

  public void setConnectionUrl(@NotNull String connectionUrl) {
    this.connectionUrl = connectionUrl;
  }

  @NotNull
  public String getContextFactory() {
    return contextFactory;
  }

  @NotNull
  public AuthMode getAuthentication() {
    return authentication;
  }

  public void setAuthentication(@NotNull AuthMode authentication) {
    this.authentication = authentication;
  }

  public boolean isUserSubtree() {
    return userSubtree;
  }

  public void setUserSubtree(boolean userSubtree) {
    this.userSubtree = userSubtree;
  }

  @NotNull
  public String getUserSearch() {
    return userSearch;
  }

  public void setUserSearch(@NotNull String userSearch) {
    this.userSearch = userSearch;
  }

  @NotNull
  public String getNameAttribute() {
    return nameAttribute;
  }

  public void setNameAttribute(@NotNull String nameAttribute) {
    this.nameAttribute = nameAttribute;
  }

  @NotNull
  public String getEmailAttribute() {
    return emailAttribute;
  }

  @NotNull
  @Override
  public UserDB create() {
    return new LDAPUserDB(this);
  }

  public static enum AuthMode {
    DigestMD5 {
      @NotNull
      @Override
      public Authenticator create(@NotNull LDAPUserDBConfig config, @NotNull PasswordChecker passwordChecker) {
        return new ProxyDigestMD5Authenticator(config);
      }
    },
    PLAIN {
      @NotNull
      @Override
      public Authenticator create(@NotNull LDAPUserDBConfig config, @NotNull PasswordChecker passwordChecker) {
        log.warn("Warning, using unsafe PLAIN authentication, it transmits passwords in clear-text. Consider using " + DigestMD5.name());
        return new PlainAuthenticator(passwordChecker);
      }
    };

    @NotNull
    public abstract Authenticator create(@NotNull LDAPUserDBConfig config, @NotNull PasswordChecker passwordChecker);
  }
}
