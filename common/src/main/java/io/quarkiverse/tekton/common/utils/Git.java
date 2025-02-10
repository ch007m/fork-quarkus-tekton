package io.quarkiverse.tekton.common.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.logging.Logger;

public class Git {

    private static final Logger LOG = Logger.getLogger(Git.class);

    public static final String DOT_GIT = ".git";
    public static final String CONFIG = "config";
    public static final String ORIGIN = "origin";
    public static final String OB = "[";
    public static final String CB = "]";
    public static final String SLASH = "/";
    public static final String COLN = ":";
    public static final String EQUALS = "=";
    public static final String REMOTE = "remote";
    public static final String HEAD = "HEAD";
    public static final String URL = "url";
    public static final String REF = "ref";

    public static final String REMOTE_PATTERN = "^\\s*\\[remote\\s*\"([a-zA-Z0-9_-]+)\"\\s*\\]\\s*";

    private static final String GITHUB_PATTERN = "(?:git@|https://)github.com[:/](.*?)/(.*?)(?:.git)?$";
    private static final String GITLAB_PATTERN = "(?:git@|https://)gitlab.com[:/](.*?)/(.*?)(?:.git)?$";
    private static final String BITBUCKET_PATTERN = "(?:git@|https://)bitbucket.org[:/](.*?)/(.*?)(?:.git)?$";
    private static final String GITEA_PATTERN = "(?:git@|http://|https://)(.*?)/(.*?)/(.*?)(?:.git)?$";

    private static final String GITHUB_HOST = "github.com";
    private static final String GITLAB_HOST = "gitlab.com";
    private static final String BITBUCKET_HOST = "bitbucket.org";

    private static String username;
    private static String password;

    private Git() {
        //Utility class
    }

    public static Optional<Path> getScmRoot(Path dir) {
        while (dir != null && !dir.resolve(DOT_GIT).toFile().exists()) {
            dir = dir.getParent();
        }
        return Optional.ofNullable(dir).filter(p -> p.resolve(DOT_GIT).toFile().exists());
    }

    private static String buildUrl(String host, String user, String repo, String branch, Path path) {
        String pathStr = path.toString().replace("\\", "/"); // Ensure the path uses forward slashes
        return String.format("https://%s/%s/%s/blob/%s/%s", host, user, repo, branch, pathStr);
    }

    private static String buildGithubUrl(String host, String user, String repo, String branch, Path path) {
        String pathStr = path.toString().replace("\\", "/"); // Ensure the path uses forward slashes
        return String.format("https://%s/%s/%s/blob/%s/%s", host, user, repo, branch, pathStr);
    }

    private static String buildGiteaUrl(String host, String user, String repo, String branch, Path path) {
        String pathStr = path.toString().replace("\\", "/"); // Ensure the path uses forward slashes
        return String.format("%s/src/branch/%s/%s", host, branch, pathStr);
    }

    public static Optional<String> getUrl(String remoteName, String branch, Path path) {
        return getScmRoot()
                .flatMap(root -> getScmUrl(root, remoteName).flatMap(baseUrl -> getUrlFromBase(baseUrl, branch, path)));
    }

    public static Optional<String> getUrlFromBase(String baseUrl, String branch, Path path) {
        Pattern githubPattern = Pattern.compile(GITHUB_PATTERN);
        Pattern gitlabPattern = Pattern.compile(GITLAB_PATTERN);
        Pattern bitbucketPattern = Pattern.compile(BITBUCKET_PATTERN);
        Pattern giteaPattern = Pattern.compile(GITEA_PATTERN);

        Matcher matcher;

        if ((matcher = githubPattern.matcher(baseUrl)).matches()) {
            return Optional.of(buildGithubUrl(GITHUB_HOST, matcher.group(1), matcher.group(2), branch, path));
        } else if ((matcher = gitlabPattern.matcher(baseUrl)).matches()) {
            return Optional.of(buildUrl(GITLAB_HOST, matcher.group(1), matcher.group(2), branch, path));
        } else if ((matcher = bitbucketPattern.matcher(baseUrl)).matches()) {
            return Optional.of(buildUrl(BITBUCKET_HOST, matcher.group(1), matcher.group(2), branch, path));
        } else if ((matcher = giteaPattern.matcher(baseUrl)).matches()) {
            return Optional.of(buildGiteaUrl(matcher.group(0), matcher.group(2), matcher.group(3), branch, path));
        }

        // Return empty if the remote does not match any known patterns
        return Optional.empty();
    }

    public static Optional<String> getScmUrl() {
        return getScmRoot().flatMap(Git::getScmUrl);
    }

    public static Optional<String> getScmUrl(Path root) {
        return getScmUrl(root, ORIGIN);
    }

    public static Optional<String> getScmUrl(Path root, String remote) {
        return getScmUrl(root, remote, true);
    }

    public static Optional<String> getScmUrl(Path root, String remote, boolean httpsPreferred) {
        try {
            Optional<String> url = Files.lines(getConfig(root)).map(String::trim)
                    .filter(inRemote(remote, new AtomicBoolean()))
                    .filter(l -> l.startsWith(URL) && l.contains(EQUALS))
                    .map(s -> s.split(EQUALS)[1].trim())
                    .findAny();
            return httpsPreferred ? url.map(Git::sanitizeRemoteUrl) : url;
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static Optional<String> getScmBranch(Path path) {
        try {
            return Files.lines(getHead(path)).map(String::trim)
                    .filter(l -> l.startsWith(REF) && l.contains(SLASH))
                    .map(s -> s.substring(s.lastIndexOf(SLASH) + 1).trim())
                    .findAny();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static boolean isGithubSshUrl(String url) {
        return url.startsWith("git@github.com:");
    }

    /**
     * Get the git remote urls as a map.
     *
     * @param path the path to the git config.
     * @return A {@link Map} of urls per remote.
     */

    public static Optional<Path> getScmRoot() {
        Path dir = Paths.get("").toAbsolutePath();
        while (dir != null && !dir.resolve(DOT_GIT).toFile().exists()) {
            dir = dir.getParent();
        }
        return Optional.ofNullable(dir).filter(p -> p.resolve(DOT_GIT).toFile().exists());
    }

    /**
     * Get the git root.
     *
     * @param path Any path under the target git repo.
     * @return The {@link Path} to the git root.
     */
    public static Optional<Path> getRoot(Path path) {
        Path root = path;
        while (root != null && !root.resolve(Git.DOT_GIT).toFile().exists()) {
            root = root.toAbsolutePath().getParent();
        }
        return Optional.ofNullable(root);
    }

    /**
     * Get the git config.
     *
     * @param root the git root.
     * @return The {@link Path} to the git config.
     */
    public static Path getConfig(Path root) {
        return root.resolve(DOT_GIT).resolve(CONFIG);
    }

    public static Path getHead(Path root) {
        return root.resolve(DOT_GIT).resolve(HEAD);
    }

    /**
     * Get the git remote urls as a map.
     *
     * @param path the path to the git config.
     * @return A {@link Map} of urls per remote.
     */
    public static Map<String, String> getRemotes(Path path) {
        Map<String, String> result = new HashMap<String, String>();
        try {
            Iterator<String> linesIter = Files.lines(getConfig(path)).map(String::trim).iterator();
            while (linesIter.hasNext()) {
                remoteValue(linesIter.next()).ifPresent(remote -> {
                    while (linesIter.hasNext()) {
                        String remoteLine = linesIter.next();
                        if (remoteLine.startsWith(URL) && remoteLine.contains(EQUALS)) {
                            result.put(remote, remoteLine.split(EQUALS)[1].trim());
                            break;
                        }
                    }
                });
            }
            return result;
        } catch (Exception e) {
            return result;
        }
    }

    /**
     * Get the git remote url.
     *
     * @param path the path to the git config.
     * @param remote the remote.
     * @return The an {@link Optional} String with the URL of the specified remote.
     */
    public static Optional<String> getRemoteUrl(Path path, String remote) {
        return getRemoteUrl(path, remote, false);
    }

    public static Optional<String> getSafeRemoteUrl(Path path, String remote) {
        return getRemoteUrl(path, remote, true);
    }

    public static Optional<String> getRemoteUrl(Path path, String remote, boolean httpsPreferred) {
        try {
            Optional<String> url = Files.lines(getConfig(path)).map(String::trim)
                    .filter(inRemote(remote, new AtomicBoolean()))
                    .filter(l -> l.startsWith(URL) && l.contains(EQUALS))
                    .map(s -> s.split(EQUALS)[1].trim())
                    .findAny();
            return httpsPreferred ? url.map(Git::sanitizeRemoteUrl) : url;
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static String sanitizeRemoteUrl(String remoteUrl) {
        final int atSign = remoteUrl.indexOf('@');
        if (atSign > 0) {
            remoteUrl = remoteUrl.substring(atSign + 1);
            remoteUrl = remoteUrl.replaceFirst(":", "/");
            remoteUrl = "https://" + remoteUrl;
        }
        if (!remoteUrl.endsWith(".git")) {
            remoteUrl += ".git";
        }
        return remoteUrl;
    }

    /**
     * Get the git branch.
     *
     * @param path the path to the git config.
     * @return The an {@link Optional} String with the branch.
     */
    public static Optional<String> getBranch(Path path) {
        try {
            return Files.lines(getHead(path)).map(String::trim)
                    .filter(l -> l.startsWith(REF) && l.contains(SLASH))
                    .map(s -> s.substring(s.lastIndexOf(SLASH) + 1).trim())
                    .findAny();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Get the git branch.
     *
     * @param path the path to the git config.
     * @return The an {@link Optional} String with the branch.
     */
    public static Optional<String> getCommitSHA(Path path) {
        try {
            return Files.lines(getHead(path)).map(String::trim)
                    .filter(l -> l.startsWith(REF) && l.contains(COLN))
                    .map(s -> s.substring(s.lastIndexOf(COLN) + 1).trim())
                    .map(ref -> path.resolve(DOT_GIT).resolve(ref))
                    .filter(ref -> ref.toFile().exists())
                    .map(Git::read)
                    .map(String::trim)
                    .findAny();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Create a predicate function that tracks if the a line is defined in the specified remote section.
     *
     * @param remote The target remote.
     * @param state An atomic boolean which holds the predicate state.
     * @reuturn The predicate.
     */
    public static Predicate<String> inRemote(String remote, AtomicBoolean state) {
        return l -> {
            if (l.startsWith(OB) && l.contains(REMOTE) && l.contains(remote) && l.endsWith(CB)) {
                state.set(true);
            } else if (l.startsWith(OB) && l.endsWith(CB)) {
                state.set(false);
            }
            return state.get();
        };
    }

    public static Optional<String> remoteValue(String line) {
        Pattern p = Pattern.compile(REMOTE_PATTERN);
        Matcher m = p.matcher(line);
        if (m.matches()) {
            return Optional.of(m.group(1));
        } else {
            return Optional.empty();
        }
    }

    private static String read(Path path) {
        try {
            return new String(Files.readAllBytes(path));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
