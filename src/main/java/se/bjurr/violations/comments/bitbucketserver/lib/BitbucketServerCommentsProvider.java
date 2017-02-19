package se.bjurr.violations.comments.bitbucketserver.lib;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Suppliers.memoizeWithExpiration;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static se.bjurr.violations.comments.bitbucketserver.lib.client.model.DIFFTYPE.ADDED;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Supplier;

import se.bjurr.violations.comments.bitbucketserver.lib.client.BitbucketServerClient;
import se.bjurr.violations.comments.bitbucketserver.lib.client.model.BitbucketServerComment;
import se.bjurr.violations.comments.bitbucketserver.lib.client.model.BitbucketServerDiff;
import se.bjurr.violations.comments.bitbucketserver.lib.client.model.BitbucketServerDiffResponse;
import se.bjurr.violations.comments.bitbucketserver.lib.client.model.DiffDestination;
import se.bjurr.violations.comments.bitbucketserver.lib.client.model.DiffHunk;
import se.bjurr.violations.comments.bitbucketserver.lib.client.model.Line;
import se.bjurr.violations.comments.bitbucketserver.lib.client.model.Segment;
import se.bjurr.violations.comments.lib.model.ChangedFile;
import se.bjurr.violations.comments.lib.model.Comment;
import se.bjurr.violations.comments.lib.model.CommentsProvider;
import se.bjurr.violations.lib.model.Violation;
import se.bjurr.violations.lib.util.Optional;

public class BitbucketServerCommentsProvider implements CommentsProvider {

 private static final Logger LOG = LoggerFactory.getLogger(BitbucketServerCommentsProvider.class);

 private final BitbucketServerClient client;

 private final Supplier<BitbucketServerDiffResponse> diffResponse = memoizeWithExpiration(
   new Supplier<BitbucketServerDiffResponse>() {
    @Override
    public BitbucketServerDiffResponse get() {
     return client.pullRequestDiff();
    }
   }, 10, SECONDS);

 private final ViolationCommentsToBitbucketServerApi violationCommentsToBitbucketApi;

 @VisibleForTesting
 BitbucketServerCommentsProvider() {
  client = null;
  violationCommentsToBitbucketApi = null;
 }

 public BitbucketServerCommentsProvider(ViolationCommentsToBitbucketServerApi violationCommentsToBitbucketApi) {
  String bitbucketServerBaseUrl = violationCommentsToBitbucketApi.getBitbucketServerUrl();
  String bitbucketServerProject = violationCommentsToBitbucketApi.getProjectKey();
  String bitbucketServerRepo = violationCommentsToBitbucketApi.getRepoSlug();
  Integer bitbucketServerPullRequestId = violationCommentsToBitbucketApi.getPullRequestId();
  String bitbucketServerUser = violationCommentsToBitbucketApi.getUsername();
  String bitbucketServerPassword = violationCommentsToBitbucketApi.getPassword();
  client = new BitbucketServerClient(bitbucketServerBaseUrl, bitbucketServerProject, bitbucketServerRepo,
    bitbucketServerPullRequestId, bitbucketServerUser, bitbucketServerPassword);
  this.violationCommentsToBitbucketApi = violationCommentsToBitbucketApi;
 }

 @Override
 public void createCommentWithAllSingleFileComments(String comment) {
  client.pullRequestComment(comment);
 }

 @Override
 public void createSingleFileComment(ChangedFile file, Integer line, String comment) {
  client.pullRequestComment(file.getFilename(), line, comment);
 }

 @Override
 public List<Comment> getComments() {
  List<Comment> comments = newArrayList();
  for (String changedFile : client.pullRequestChanges()) {
   List<BitbucketServerComment> bitbucketServerCommentsOnFile = client.pullRequestComments(changedFile);
   for (BitbucketServerComment fileComment : bitbucketServerCommentsOnFile) {
    List<String> specifics = newArrayList(fileComment.getVersion() + "", changedFile);
    comments.add(new Comment(fileComment.getId() + "", fileComment.getText(), null, specifics));
   }
  }

  return comments;
 }

 @Override
 public List<ChangedFile> getFiles() {
  List<ChangedFile> changedFiles = newArrayList();

  List<String> bitbucketServerChangedFiles = client.pullRequestChanges();

  for (String changedFile : bitbucketServerChangedFiles) {
   changedFiles.add(new ChangedFile(changedFile, new ArrayList<String>()));
  }

  return changedFiles;
 }

 @Override
 public void removeComments(List<Comment> comments) {
  for (Comment comment : comments) {
   Integer commentId = null;
   Integer commentVersion = null;
   try {
    commentId = Integer.valueOf(comment.getIdentifier());
    commentVersion = Integer.valueOf(comment.getSpecifics().get(0));
    client.pullRequestRemoveComment(commentId, commentVersion);
   } catch (Exception e) {
    LOG.warn("Was unable to remove comment " + commentId + " " + commentVersion, e);
   }
  }
 }

 @Override
 public boolean shouldComment(ChangedFile changedFile, Integer changedLine) {
  if (!violationCommentsToBitbucketApi.getCommentOnlyChangedContent()) {
   return true;
  }
  int context = violationCommentsToBitbucketApi.getCommentOnlyChangedContentContext();
  List<BitbucketServerDiff> diffs = diffResponse.get().getDiffs();
  return shouldComment(changedFile, changedLine, context, diffs);
 }

 @VisibleForTesting
 boolean shouldComment(ChangedFile changedFile, Integer changedLine, int context, List<BitbucketServerDiff> diffs) {
  for (BitbucketServerDiff diff : diffs) {
   DiffDestination destination = diff.getDestination();
   if (destination != null) {
    String destinationToString = destination.getToString();
    if (!isNullOrEmpty(destinationToString)) {
     if (destinationToString.equals(changedFile.getFilename())) {
      if (diff.getHunks() != null) {
       for (DiffHunk hunk : diff.getHunks()) {
        for (Segment segment : hunk.getSegments()) {
         if (segment.getType() == ADDED) {
          for (Line line : segment.getLines()) {
           if (line.getDestination() >= changedLine - context && line.getDestination() <= changedLine + context) {
            return true;
           }
          }
         }
        }
       }
      }
     }
    }
   }
  }
  return false;
 }

 @Override
 public boolean shouldCreateCommentWithAllSingleFileComments() {
  return violationCommentsToBitbucketApi.getCreateCommentWithAllSingleFileComments();
 }

 @Override
 public boolean shouldCreateSingleFileComment() {
  return violationCommentsToBitbucketApi.getCreateSingleFileComments();
 }

 @Override
 public Optional<String> findCommentFormat(ChangedFile changedFile, Violation violation) {
   return Optional.absent();
 }
}
