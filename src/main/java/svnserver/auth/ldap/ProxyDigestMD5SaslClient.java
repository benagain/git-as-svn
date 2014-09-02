/**
 * This file is part of git-as-svn. It is subject to the license terms
 * in the LICENSE file found in the top-level directory of this distribution
 * and at http://www.gnu.org/licenses/gpl-2.0.html. No part of git-as-svn,
 * including this file, may be copied, modified, propagated, or distributed
 * except according to the terms contained in the LICENSE file.
 */
package svnserver.auth.ldap;

import org.jetbrains.annotations.NotNull;
import org.tmatesoft.svn.core.internal.util.SVNBase64;
import svnserver.StringHelper;
import svnserver.parser.SvnServerParser;
import svnserver.parser.SvnServerWriter;

import javax.security.sasl.SaslClient;
import javax.security.sasl.SaslException;
import java.io.IOException;

/**
 * @author Marat Radchenko <marat@slonopotamus.org>
 */
final class ProxyDigestMD5SaslClient implements SaslClient {
  @NotNull
  public static final String realAuthMethod = "DIGEST-MD5";

  @NotNull
  private final SvnServerParser parser;
  @NotNull
  private final SvnServerWriter writer;

  private int step = 2;

  ProxyDigestMD5SaslClient(@NotNull SvnServerParser parser, @NotNull SvnServerWriter writer) {
    this.parser = parser;
    this.writer = writer;
  }

  @Override
  public String getMechanismName() {
    return realAuthMethod;
  }

  @Override
  public boolean hasInitialResponse() {
    return false;
  }

  @Override
  public byte[] evaluateChallenge(byte[] challenge) throws SaslException {
    System.out.println(new String(challenge));
    switch (step) {
      case 2:
        try {
          writer
              .listBegin()
              .word("step")
              .listBegin()
              .string(SVNBase64.byteArrayToBase64(challenge))
              .listEnd()
              .listEnd();

          final byte[] clientResponse = StringHelper.fromBase64(parser.readText());
          System.out.println(new String(clientResponse));
          System.out.println("======================");

          ++step;
          return clientResponse;
        } catch (IOException e) {
          step = 0;
          throw new SaslException(e.getMessage(), e);
        }

      case 3:
        try {
          writer
              .listBegin()
              .word("success")
              .listBegin()
              .string(SVNBase64.byteArrayToBase64(challenge))
              .listEnd()
              .listEnd();
          return null;
        } catch (IOException e) {
          throw new SaslException(e.getMessage(), e);
        } finally {
          step = 0;
        }

      default:
        throw new SaslException("Invalid state");
    }
  }

  @Override
  public boolean isComplete() {
    return step == 0;
  }

  @Override
  public byte[] unwrap(byte[] incoming, int offset, int len) throws SaslException {
    throw new UnsupportedOperationException();
  }

  @Override
  public byte[] wrap(byte[] outgoing, int offset, int len) throws SaslException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Object getNegotiatedProperty(String propName) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void dispose() throws SaslException {
    throw new UnsupportedOperationException();
  }
}
