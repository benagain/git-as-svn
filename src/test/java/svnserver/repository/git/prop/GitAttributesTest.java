/**
 * This file is part of git-as-svn. It is subject to the license terms
 * in the LICENSE file found in the top-level directory of this distribution
 * and at http://www.gnu.org/licenses/gpl-2.0.html. No part of git-as-svn,
 * including this file, may be copied, modified, propagated, or distributed
 * except according to the terms contained in the LICENSE file.
 */
package svnserver.repository.git.prop;

import org.testng.Assert;
import org.testng.annotations.Test;
import org.tmatesoft.svn.core.SVNProperty;

import java.util.HashMap;
import java.util.Map;

/**
 * Tests for GitAttributes.
 *
 * @author Artem V. Navrotskiy <bozaro@users.noreply.github.com>
 */
public class GitAttributesTest {
  @Test
  public void testParseAttributes() {
    final GitProperty attr = new GitAttributes(
        "# Gradle\n" +
            "*.gradle\t\ttext eol=native\n" +
            "*.jpg\tbinary\n" +
            "\n" +
            "# Java\n" +
            "\t*.java\t\t\teol=native  text\n" +
            "*.properties  text eol=native\n" +
            " *.py\t\t\ttext eol=native # Python\n" +
            "*.sh\t\t\ttext eol=lf"
    );
    final Map<String, String> props = new HashMap<>();
    attr.apply(props);
    Assert.assertEquals(props.size(), 1);
    Assert.assertEquals(props.get(SVNProperty.INHERITABLE_AUTO_PROPS),
        "*.gradle = svn:eol-style=native\n" +
            "*.jpg = svn:mime-type=application/octet-stream\n" +
            "*.java = svn:eol-style=native\n" +
            "*.properties = svn:eol-style=native\n" +
            "*.py = svn:eol-style=native\n" +
            "*.sh = svn:eol-style=LF\n"
    );
  }
}
