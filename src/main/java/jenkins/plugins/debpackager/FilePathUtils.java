package jenkins.plugins.debpackager;

import hudson.FilePath;
import hudson.FilePath.FileCallable;
import hudson.model.BuildListener;
import hudson.remoting.VirtualChannel;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class FilePathUtils {

    public static void chown(FilePath filepath, final String owner, final String group)
            throws IOException, InterruptedException {
        chown(filepath, owner, group, true);
    }

    @SuppressWarnings("serial")
    public static void chown(FilePath filepath, final String owner, final String group,
            final boolean recursive) throws IOException, InterruptedException {

        if (!isUnix())
            return;
        filepath.act(new FileCallable<Void>() {
            public Void invoke(File f, VirtualChannel channel) throws IOException,
                    InterruptedException {
                chown(f, owner, group, recursive);
                return null;
            }
        });
    }

    @SuppressWarnings("serial")
    public static void sudoDeleteRecursive(FilePath path) throws IOException, InterruptedException {
        path.act(new FileCallable<Void>() {
            public Void invoke(File f, VirtualChannel channel) throws IOException,
                    InterruptedException {
                sudoDeleteRecursive(f);
                return null;
            }
        });
    }

    private static int chown(File f, String owner, String group, boolean recursive)
            throws IOException, InterruptedException {
        String command = String.format("sudo chown %s %s:%s %s", (recursive ? "-R" : ""), owner,
                group, f.getAbsolutePath());
        Process p = Runtime.getRuntime().exec(command);
        return p.waitFor();
    }

    private static int sudoDeleteRecursive(File f) throws IOException, InterruptedException {
        String command = String.format("sudo rm -rf %s", f.getAbsolutePath());
        Process p = Runtime.getRuntime().exec(command);
        return p.waitFor();
    }

    public static boolean isUnix() {
        return File.pathSeparatorChar != ';';
    }

    public static int copyRecursiveWithPermissions(FilePath source, String includes,
                                                   String excludes, FilePath target, final BuildListener listener) throws IOException,
            InterruptedException {
        FilePath[] files = source.list(includes, excludes);
        listener.getLogger().println("Preparing to copy " + files.length + " file(s)");

        for (FilePath file : files) {
            int i = 0;
            while (i < file.getRemote().length() && i < source.getRemote().length()
                    && file.getRemote().charAt(i  ) == source.getRemote().charAt(i)) {
                i++;
            }

            listener.getLogger().println(
                    "Copying " + source.getRemote() + " : " + file.getRemote().substring(i)
                            + " -> " + target.getRemote());
            String dest = file.getRemote().substring(i);
            if (dest.startsWith("/")) {
                dest = dest.substring(1);
            }

            final String destPath = target.child(dest).getRemote();
            file.act(new FileCallable<Void>() {
                @Override
                public Void invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
                    listener.getLogger().println(destPath);
                    Files.createDirectories(Paths.get(destPath).getParent());
                    Files.copy(Paths.get(f.getAbsolutePath()), Paths.get(destPath), COPY_ATTRIBUTES, REPLACE_EXISTING);
                    return null;
                }
            });
        }
        return 0;
    }
}
