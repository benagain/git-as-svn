/**
 * This file is part of git-as-svn. It is subject to the license terms
 * in the LICENSE file found in the top-level directory of this distribution
 * and at http://www.gnu.org/licenses/gpl-2.0.html. No part of git-as-svn,
 * including this file, may be copied, modified, propagated, or distributed
 * except according to the terms contained in the LICENSE file.
 */
package svnserver.auth.ldap;

import org.apache.directory.api.ldap.extras.extended.ads_impl.whoAmI.WhoAmIResponseDecorator;
import org.apache.directory.api.ldap.model.name.Dn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.naming.ldap.ExtendedResponse;

/**
 * @author Marat Radchenko <marat@slonopotamus.org>
 */
final class WhoAmIResponse implements ExtendedResponse {

  @Nullable
  private final String userId;
  @Nullable
  private final Dn dn;

  WhoAmIResponse(@NotNull WhoAmIResponseDecorator decorator) {
    this.userId = decorator.getUserId();
    this.dn = decorator.getDn();
  }

  @Nullable
  String getUserId() {
    return userId;
  }

  @Nullable
  Dn getDn() {
    return dn;
  }

  @Override
  @Nullable
  public String getID() {
    throw new UnsupportedOperationException();
  }

  @Override
  public byte[] getEncodedValue() {
    throw new UnsupportedOperationException();
  }
}
