/**
 * This file is part of git-as-svn. It is subject to the license terms
 * in the LICENSE file found in the top-level directory of this distribution
 * and at http://www.gnu.org/licenses/gpl-2.0.html. No part of git-as-svn,
 * including this file, may be copied, modified, propagated, or distributed
 * except according to the terms contained in the LICENSE file.
 */
package svnserver.auth.ldap;

import org.apache.directory.api.asn1.DecoderException;
import org.apache.directory.api.ldap.extras.extended.ads_impl.whoAmI.WhoAmIResponseContainer;
import org.apache.directory.api.ldap.extras.extended.ads_impl.whoAmI.WhoAmIResponseDecoder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.naming.CommunicationException;
import javax.naming.NamingException;
import javax.naming.ldap.ExtendedRequest;
import javax.naming.ldap.ExtendedResponse;
import java.nio.ByteBuffer;

/**
 * @author Marat Radchenko <marat@slonopotamus.org>
 */
final class WhoAmIRequest implements ExtendedRequest {

  @NotNull
  public static final WhoAmIResponseDecoder responseDecoder = new WhoAmIResponseDecoder();
  @NotNull
  public static final String OID = "1.3.6.1.4.1.4203.1.11.3";

  @Override
  public String getID() {
    return OID;
  }

  @Override
  public byte[] getEncodedValue() {
    return null;
  }

  @Override
  public ExtendedResponse createExtendedResponse(@Nullable String id, @Nullable byte[] berValue, int offset, int length) throws NamingException {
    final WhoAmIResponseContainer container = new WhoAmIResponseContainer();

    try {
      responseDecoder.decode(ByteBuffer.wrap(berValue, offset, length), container);
    } catch (DecoderException e) {
      final CommunicationException ex = new CommunicationException(e.getMessage());
      ex.initCause(e);
      throw ex;
    }

    return new WhoAmIResponse(container.getWhoAmIResponse());
  }
}
