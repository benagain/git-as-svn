/**
 * This file is part of git-as-svn. It is subject to the license terms
 * in the LICENSE file found in the top-level directory of this distribution
 * and at http://www.gnu.org/licenses/gpl-2.0.html. No part of git-as-svn,
 * including this file, may be copied, modified, propagated, or distributed
 * except according to the terms contained in the LICENSE file.
 */
package svnserver.auth;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.tmatesoft.svn.core.SVNException;
import svnserver.StringHelper;
import svnserver.parser.SvnServerParser;
import svnserver.parser.SvnServerWriter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @author Marat Radchenko <marat@slonopotamus.org>
 */
public final class PlainAuthenticator implements Authenticator {

  @NotNull
  private final PasswordChecker passwordChecker;

  public PlainAuthenticator(@NotNull PasswordChecker passwordChecker) {
    this.passwordChecker = passwordChecker;
  }

  @NotNull
  @Override
  public String getMethodName() {
    return "PLAIN";
  }

  @Nullable
  @Override
  public User authenticate(@NotNull SvnServerParser parser, @NotNull SvnServerWriter writer, @NotNull String token) throws IOException, SVNException {
    final String[] credentials = new String(StringHelper.fromBase64(token), StandardCharsets.US_ASCII).split("\u0000");
    if (credentials.length < 3)
      return null;

    final String username = credentials[1];
    final String password = credentials[2];
    return passwordChecker.check(username, password);
  }
}
