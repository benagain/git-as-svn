/**
 * This file is part of git-as-svn. It is subject to the license terms
 * in the LICENSE file found in the top-level directory of this distribution
 * and at http://www.gnu.org/licenses/gpl-2.0.html. No part of git-as-svn,
 * including this file, may be copied, modified, propagated, or distributed
 * except according to the terms contained in the LICENSE file.
 */
package svnserver;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.tmatesoft.svn.core.internal.util.SVNBase64;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Useful string utilites.
 *
 * @author Artem V. Navrotskiy <bozaro@users.noreply.github.com>
 */
public final class StringHelper {
  @NotNull
  private static final char[] DIGITS = "0123456789abcdef".toCharArray();
  @NotNull
  private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

  @SuppressWarnings("MagicNumber")
  public static String toHex(byte[] data) {
    final StringBuilder result = new StringBuilder();
    for (byte i : data) {
      result.append(DIGITS[(i >> 4) & 0x0F]);
      result.append(DIGITS[i & 0x0F]);
    }
    return result.toString();
  }

  @NotNull
  public static String formatDate(long time) {
    final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    df.setTimeZone(UTC);
    return df.format(new Date(time));
  }

  @NotNull
  public static String joinPath(@NotNull String path, @Nullable String localPath) {
    if (localPath == null || localPath.isEmpty()) {
      return normalize(path);
    }
    if (localPath.startsWith("/")) {
      return normalize(localPath);
    }
    return normalize(path + (path.endsWith("/") ? "" : "/") + localPath);
  }

  @NotNull
  public static String normalize(@NotNull String path) {
    if (path.isEmpty()) return "";
    String result = path;
    if (result.charAt(0) != '/') {
      result = "/" + result;
    } else if (result.length() == 1) {
      return "";
    }
    return result.endsWith("/") ? result.substring(0, result.length() - 1) : result;
  }

  @NotNull
  public static String parentDir(@NotNull String fullPath) {
    int index = fullPath.lastIndexOf('/');
    return index >= 0 ? fullPath.substring(0, index) : "";
  }

  @NotNull
  public static String baseName(@NotNull String fullPath) {
    return fullPath.substring(fullPath.lastIndexOf('/') + 1);
  }

  /**
   * Returns true, if parentPath is base path of childPath.
   *
   * @param parentPath Parent path.
   * @param childPath  Child path.
   * @return Returns true, if parentPath is base path of childPath.
   */
  public static boolean isParentPath(@NotNull String parentPath, @NotNull String childPath) {
    return childPath.equals(parentPath)
        || childPath.startsWith(parentPath) && (childPath.charAt(parentPath.length()) == '/');
  }

  /**
   * Taken from {@link org.tmatesoft.svn.core.internal.io.svn.sasl.SVNSaslAuthenticator#fromBase64(String)}.
   */
  public static byte[] fromBase64(@Nullable String src) {
    if (src == null) {
      return new byte[0];
    }
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    for (int i = 0; i < src.length(); i++) {
      char ch = src.charAt(i);
      if (!Character.isWhitespace(ch) && ch != '\n' && ch != '\r') {
        bos.write((byte) ch & 0xFF);
      }
    }
    byte[] cbytes = new byte[src.length()];
    src = new String(bos.toByteArray(), StandardCharsets.US_ASCII);
    int clength = SVNBase64.base64ToByteArray(new StringBuffer(src), cbytes);
    byte[] result = new byte[clength];
    // strip trailing -1s.
    for (int i = clength - 1; i >= 0; i--) {
      if (i == -1) {
        clength--;
      }
    }
    System.arraycopy(cbytes, 0, result, 0, clength);
    return result;
  }
}
