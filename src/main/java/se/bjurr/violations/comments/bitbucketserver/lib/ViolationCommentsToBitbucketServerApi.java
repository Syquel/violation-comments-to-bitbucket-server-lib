package se.bjurr.violations.comments.bitbucketserver.lib;

import static com.google.common.base.Preconditions.checkNotNull;
import static se.bjurr.violations.comments.lib.CommentsCreator.createComments;
import static se.bjurr.violations.lib.util.Utils.firstNonNull;
import static se.bjurr.violations.lib.util.Utils.isNullOrEmpty;

import java.util.List;
import se.bjurr.violations.comments.lib.model.CommentsProvider;
import se.bjurr.violations.lib.model.Violation;

public class ViolationCommentsToBitbucketServerApi {
  private static final Integer BITBUCKET_MAX_COMMENT_SIZE = 32767;
  public static final String DEFAULT_PROP_VIOLATIONS_PASSWORD = "VIOLATIONS_PASSWORD";
  public static final String DEFAULT_PROP_VIOLATIONS_USERNAME = "VIOLATIONS_USERNAME";
  public static final String DEFAULT_PROP_PERSONAL_ACCESS_TOKEN = "VIOLATIONS_PAT";

  public static ViolationCommentsToBitbucketServerApi violationCommentsToBitbucketServerApi() {
    return new ViolationCommentsToBitbucketServerApi();
  }

  private String bitbucketServerUrl = null;
  private boolean createCommentWithAllSingleFileComments = false;
  private boolean createSingleFileComments = true;
  private String password;
  private String projectKey;
  private String propPassword = DEFAULT_PROP_VIOLATIONS_PASSWORD;
  private String propUsername = DEFAULT_PROP_VIOLATIONS_USERNAME;
  private String propPersonalAccessToken = DEFAULT_PROP_PERSONAL_ACCESS_TOKEN;
  private int pullRequestId;
  private String repoSlug;
  private String username;
  private List<Violation> violations;
  private boolean commentOnlyChangedContent = false;
  private int commentOnlyChangedContentContext;
  private boolean shouldKeepOldComments;
  private String personalAccessToken;

  private ViolationCommentsToBitbucketServerApi() {}

  private void checkState() {
    final boolean noUsername = isNullOrEmpty(username) || isNullOrEmpty(password);
    final boolean noPat = isNullOrEmpty(personalAccessToken);
    if (noUsername && noPat) {
      throw new IllegalStateException(
          "User and Password, or personal access token, must be set! They can be set with the API or by setting properties.\n"
              + //
              "Username/password:\n"
              + //
              "-D"
              + DEFAULT_PROP_VIOLATIONS_USERNAME
              + "=theuser -D"
              + DEFAULT_PROP_VIOLATIONS_PASSWORD
              + "=thepassword"
              + //
              "\n\nPersonal access token:\n"
              + //
              "-D"
              + DEFAULT_PROP_PERSONAL_ACCESS_TOKEN
              + "=asdasd");
    }
    checkNotNull(bitbucketServerUrl, "BitbucketServerURL");
    checkNotNull(pullRequestId, "PullRequestId");
    checkNotNull(repoSlug, "repoSlug");
    checkNotNull(projectKey, "projectKey");
  }

  public String getBitbucketServerUrl() {
    return bitbucketServerUrl;
  }

  public boolean getCommentOnlyChangedContent() {
    return commentOnlyChangedContent;
  }

  public int getCommentOnlyChangedContentContext() {
    return commentOnlyChangedContentContext;
  }

  public boolean getCreateCommentWithAllSingleFileComments() {
    return createCommentWithAllSingleFileComments;
  }

  public boolean getCreateSingleFileComments() {
    return createSingleFileComments;
  }

  public String getPassword() {
    return password;
  }

  public String getProjectKey() {
    return projectKey;
  }

  public String getPropPassword() {
    return propPassword;
  }

  public String getPropUsername() {
    return propUsername;
  }

  public int getPullRequestId() {
    return pullRequestId;
  }

  public String getRepoSlug() {
    return repoSlug;
  }

  public String getUsername() {
    return username;
  }

  private void populateFromEnvironmentVariables() {
    if (System.getProperty(propUsername) != null) {
      username = firstNonNull(username, System.getProperty(propUsername));
    }
    if (System.getProperty(propPassword) != null) {
      password = firstNonNull(password, System.getProperty(propPassword));
    }
    if (System.getProperty(propPassword) != null) {
      personalAccessToken =
          firstNonNull(personalAccessToken, System.getProperty(propPersonalAccessToken));
    }
  }

  public void toPullRequest() throws Exception {
    populateFromEnvironmentVariables();
    checkState();
    final CommentsProvider commentsProvider = new BitbucketServerCommentsProvider(this);
    createComments(commentsProvider, violations, BITBUCKET_MAX_COMMENT_SIZE);
  }

  public ViolationCommentsToBitbucketServerApi withBitbucketServerUrl(
      final String bitbucketServerUrl) {
    this.bitbucketServerUrl = bitbucketServerUrl;
    return this;
  }

  public ViolationCommentsToBitbucketServerApi withCommentOnlyChangedContent(
      final boolean commentOnlyChangedContent) {
    this.commentOnlyChangedContent = commentOnlyChangedContent;
    return this;
  }

  public ViolationCommentsToBitbucketServerApi withCommentOnlyChangedContentContext(
      final int commentOnlyChangedContentContext) {
    this.commentOnlyChangedContentContext = commentOnlyChangedContentContext;
    return this;
  }

  public ViolationCommentsToBitbucketServerApi withCreateCommentWithAllSingleFileComments(
      final boolean createCommentWithAllSingleFileComments) {
    this.createCommentWithAllSingleFileComments = createCommentWithAllSingleFileComments;
    return this;
  }

  public ViolationCommentsToBitbucketServerApi withCreateSingleFileComments(
      final boolean createSingleFileComments) {
    this.createSingleFileComments = createSingleFileComments;
    return this;
  }

  public ViolationCommentsToBitbucketServerApi withPassword(final String password) {
    this.password = password;
    return this;
  }

  public ViolationCommentsToBitbucketServerApi withProjectKey(final String projectKey) {
    this.projectKey = projectKey;
    return this;
  }

  public void withPropPassword(final String envPassword) {
    propPassword = envPassword;
  }

  public void withPropUsername(final String envUsername) {
    propUsername = envUsername;
  }

  public ViolationCommentsToBitbucketServerApi withPropPersonalAccessToken(
      final String propPersonalAccessToken) {
    this.propPersonalAccessToken = propPersonalAccessToken;
    return this;
  }

  public ViolationCommentsToBitbucketServerApi withPullRequestId(final int pullRequestId) {
    this.pullRequestId = pullRequestId;
    return this;
  }

  public ViolationCommentsToBitbucketServerApi withRepoSlug(final String repoSlug) {
    this.repoSlug = repoSlug;
    return this;
  }

  public ViolationCommentsToBitbucketServerApi withUsername(final String username) {
    this.username = username;
    return this;
  }

  public ViolationCommentsToBitbucketServerApi withViolations(final List<Violation> violations) {
    this.violations = violations;
    return this;
  }

  public ViolationCommentsToBitbucketServerApi withShouldKeepOldComments(
      final boolean shouldKeepOldComments) {
    this.shouldKeepOldComments = shouldKeepOldComments;
    return this;
  }

  public boolean getShouldKeepOldComments() {
    return shouldKeepOldComments;
  }

  public String getPersonalAccessToken() {
    return personalAccessToken;
  }
}
