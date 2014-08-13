package svnserver.server.command;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.delta.SVNDeltaReader;
import org.tmatesoft.svn.core.io.ISVNDeltaConsumer;
import org.tmatesoft.svn.core.io.diff.SVNDeltaProcessor;
import org.tmatesoft.svn.core.io.diff.SVNDiffWindow;
import svnserver.StringHelper;
import svnserver.SvnConstants;
import svnserver.parser.MessageParser;
import svnserver.parser.SvnServerParser;
import svnserver.parser.SvnServerWriter;
import svnserver.parser.token.ListBeginToken;
import svnserver.parser.token.ListEndToken;
import svnserver.repository.FileInfo;
import svnserver.server.SessionContext;
import svnserver.server.error.ClientErrorException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Commit client changes.
 * <p><pre>
 * get-dir
 * commit
 *    params:   ( logmsg:string ? ( ( lock-path:string lock-token:string ) ... )
 *    keep-locks:bool ? rev-props:proplist )
 *    response: ( )
 *    Upon receiving response, client switches to editor command set.
 *    Upon successful completion of edit, server sends auth-request.
 *    After auth exchange completes, server sends commit-info.
 *    If rev-props is present, logmsg is ignored.  Only the svn:log entry in
 *    rev-props (if any) will be used.
 *    commit-info: ( new-rev:number date:string author:string
 *    ? ( post-commit-err:string ) )
 *    NOTE: when revving this, make 'logmsg' optional, or delete that parameter
 *    and have the log message specified in 'rev-props'.
 * </pre>
 *
 * @author Artem V. Navrotskiy <bozaro@users.noreply.github.com>
 */

public class CommitCmd extends BaseCmd<CommitCmd.CommitParams> {
  public static class CommitParams {
    @NotNull
    private final String message;

    public CommitParams(@NotNull String message) {
      this.message = message;
    }
  }

  public static class NoParams {
  }

  public static class OpenRootParams {
    @NotNull
    private final int rev[];
    @NotNull
    private final String token;

    public OpenRootParams(@NotNull int[] rev, @NotNull String token) {
      this.rev = rev;
      this.token = token;
    }
  }

  public static class OpenParams {
    @NotNull
    private final String name;
    @NotNull
    private final String parentToken;
    @NotNull
    private final String token;
    @NotNull
    private final int rev[];

    public OpenParams(@NotNull String name, @NotNull String parentToken, @NotNull String token, @NotNull int[] rev) {
      this.name = name;
      this.parentToken = parentToken;
      this.token = token;
      this.rev = rev;
    }
  }

  public static class TokenParams {
    @NotNull
    private final String token;

    public TokenParams(@NotNull String token) {
      this.token = token;
    }
  }

  public static class DeltaApplyParams {
    @NotNull
    private final String token;
    @NotNull
    private final String[] checksum;

    public DeltaApplyParams(@NotNull String token, @NotNull String[] checksum) {
      this.token = token;
      this.checksum = checksum;
    }
  }

  public static class DeltaChunkParams {
    @NotNull
    private final String token;
    @NotNull
    private final byte[] chunk;

    public DeltaChunkParams(@NotNull String token, @NotNull byte[] chunk) {
      this.token = token;
      this.chunk = chunk;
    }
  }

  @NotNull
  private static final Logger log = LoggerFactory.getLogger(DeltaCmd.class);

  @NotNull
  @Override
  public Class<CommitParams> getArguments() {
    return CommitParams.class;
  }

  @Override
  protected void processCommand(@NotNull SessionContext context, @NotNull CommitParams args) throws IOException, ClientErrorException {
    final SvnServerWriter writer = context.getWriter();
    writer
        .listBegin()
        .word("success")
        .listBegin()
        .listEnd()
        .listEnd();
    log.info("Enter editor mode");
    EditorPipeline pipeline = new EditorPipeline(args);
    pipeline.editorCommand(context);
  }

  public static class CommitFile {
    @NotNull
    private final FileInfo fileInfo;
    @NotNull
    private final SVNDeltaProcessor window = new SVNDeltaProcessor();
    @NotNull
    private final SVNDeltaReader reader = new SVNDeltaReader();
    @NotNull
    private final ByteArrayOutputStream memory = new ByteArrayOutputStream();

    public CommitFile(@NotNull FileInfo fileInfo) {
      this.fileInfo = fileInfo;
    }
  }

  public static class EditorPipeline {
    @NotNull
    private final Map<String, BaseCmd<?>> commands;
    @NotNull
    private final String message;
    @NotNull
    private final Map<String, String> paths;
    @NotNull
    private final Map<String, CommitFile> files;

    public EditorPipeline(@NotNull CommitParams params) {
      this.message = params.message;
      paths = new HashMap<>();
      files = new HashMap<>();
      commands = new HashMap<>();
      commands.put("open-root", new LambdaCmd<>(OpenRootParams.class, this::openRoot));
      commands.put("open-dir", new LambdaCmd<>(OpenParams.class, this::openDir));
      commands.put("open-file", new LambdaCmd<>(OpenParams.class, this::openFile));
      commands.put("close-dir", new LambdaCmd<>(TokenParams.class, this::closeDir));
      commands.put("close-file", new LambdaCmd<>(NoParams.class, this::fake));
      commands.put("textdelta-chunk", new LambdaCmd<>(DeltaChunkParams.class, this::deltaChunk));
      commands.put("textdelta-end", new LambdaCmd<>(TokenParams.class, this::deltaEnd));
      commands.put("apply-textdelta", new LambdaCmd<>(DeltaApplyParams.class, this::deltaApply));
      commands.put("close-edit", new LambdaCmd<>(NoParams.class, this::closeEdit));
    }

    private void openRoot(@NotNull SessionContext context, @NotNull OpenRootParams args) throws ClientErrorException {
      context.push(this::editorCommand);
      paths.put(args.token, context.getRepositoryPath(""));
    }

    private void openDir(@NotNull SessionContext context, @NotNull OpenParams args) throws ClientErrorException {
      context.push(this::editorCommand);
      paths.put(args.token, getPath(args.parentToken, args.name));
    }

    private void openFile(@NotNull SessionContext context, @NotNull OpenParams args) throws ClientErrorException, IOException {
      context.push(this::editorCommand);
      final String path = getPath(args.parentToken, args.name);
      if (args.rev.length == 0) {
        throw new ClientErrorException(0, "File revision is not defined: " + path);
      }
      final int rev = args.rev[0];
      log.info("Modify file: {} (rev: {})", path, rev);
      final FileInfo fileInfo = context.getRepository().getRevisionInfo(rev).getFile(path);
      if (fileInfo == null) {
        throw new ClientErrorException(0, "File not found in revision: " + path + " (rev: " + rev + ")");
      }
      files.put(args.token, new CommitFile(fileInfo));
    }

    private void deltaApply(@NotNull SessionContext context, @NotNull DeltaApplyParams args) throws ClientErrorException, IOException {
      context.push(this::editorCommand);
      final CommitFile commitFile = getFile(args.token);
      SVNDeltaProcessor window = commitFile.window;
      window.applyTextDelta(commitFile.fileInfo.openStream(), commitFile.memory, true);
    }

    private void deltaChunk(@NotNull SessionContext context, @NotNull DeltaChunkParams args) throws ClientErrorException, IOException {
      context.push(this::editorCommand);
      final CommitFile commitFile = getFile(args.token);
      try {
        commitFile.reader.nextWindow(args.chunk, 0, args.chunk.length, "", new ISVNDeltaConsumer() {
          @Override
          public void applyTextDelta(String path, String baseChecksum) throws SVNException {

          }

          @Override
          public OutputStream textDeltaChunk(String path, SVNDiffWindow diffWindow) throws SVNException {
            commitFile.window.textDeltaChunk(diffWindow);
            return null;
          }

          @Override
          public void textDeltaEnd(String path) throws SVNException {
          }
        });
      } catch (SVNException e) {
        e.printStackTrace();
      }
    }

    private void deltaEnd(@NotNull SessionContext context, @NotNull TokenParams args) throws ClientErrorException, IOException {
      context.push(this::editorCommand);
      final CommitFile commitFile = getFile(args.token);
      String md5 = commitFile.window.textDeltaEnd();
      log.info("Delta end: {}, {}", md5, commitFile.memory.toByteArray());
    }

    @NotNull
    private CommitFile getFile(@NotNull String token) throws ClientErrorException {
      final CommitFile file = files.get(token);
      if (file == null) {
        throw new ClientErrorException(0, "Invalid file token: " + token);
      }
      return file;
    }

    @NotNull
    private String getPath(@NotNull String parentToken, @NotNull String name) throws ClientErrorException {
      final String path = paths.get(parentToken);
      if (path == null) {
        throw new ClientErrorException(0, "Invalid path token: " + parentToken);
      }
      return StringHelper.joinPath(path, name);
    }

    private void closeDir(@NotNull SessionContext context, @NotNull TokenParams args) throws ClientErrorException {
      context.push(this::editorCommand);
      paths.remove(args.token);
    }

    @Deprecated
    private void fake(@NotNull SessionContext context, @NotNull NoParams args) {
      context.push(this::editorCommand);
    }

    private void closeEdit(@NotNull SessionContext context, @NotNull NoParams args) throws IOException {
      final SvnServerWriter writer = context.getWriter();
      writer
          .listBegin()
          .word("success")
          .listBegin()
          .listEnd()
          .listEnd();
      sendError(writer, 0, "test");
      //context.push(new CheckPermissionStep(this::complete));
    }

    private void complete(@NotNull SessionContext context) throws IOException {
      final SvnServerWriter writer = context.getWriter();
      writer
          .listBegin()
          .number(42) // rev number
          .listBegin().string(StringHelper.formatDate(new Date().getTime())).listEnd() // date
          .listBegin().string("commit author").listEnd()
          .listBegin().listEnd()
          .listEnd();
    }

    private void editorCommand(@NotNull SessionContext context) throws IOException, ClientErrorException {
      final SvnServerParser parser = context.getParser();
      final SvnServerWriter writer = context.getWriter();
      parser.readToken(ListBeginToken.class);
      final String cmd = parser.readText();
      log.info("Editor command: {}", cmd);
      final BaseCmd command = commands.get(cmd);
      if (command != null) {
        Object param = MessageParser.parse(command.getArguments(), parser);
        parser.readToken(ListEndToken.class);
        //noinspection unchecked
        command.process(context, param);
      } else {
        log.error("Unsupported command: {}", cmd);
        BaseCmd.sendError(writer, SvnConstants.ERROR_UNIMPLEMENTED, "Unsupported command: " + cmd);
        parser.skipItems();
      }
    }
  }
}